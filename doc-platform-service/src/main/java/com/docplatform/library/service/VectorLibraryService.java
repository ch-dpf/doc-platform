package com.docplatform.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docplatform.ingest.config.IngestProperties;
import com.docplatform.library.config.VectorLibraryConfig;
import com.docplatform.library.config.VectorLibraryConfigMerger;
import com.docplatform.library.domain.LibraryStatus;
import com.docplatform.library.domain.VectorLibrary;
import com.docplatform.library.dto.CreateVectorLibraryRequest;
import com.docplatform.library.dto.UpdateVectorLibrarySettingsRequest;
import com.docplatform.library.dto.VectorLibraryResponse;
import com.docplatform.library.dto.VectorLibraryUpdateResponse;
import com.docplatform.library.mapper.VectorLibraryMapper;
import com.docplatform.platform.JsonSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class VectorLibraryService {

    public static final UUID DEFAULT_LIBRARY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final VectorLibraryMapper mapper;
    private final IngestProperties ingestProperties;

    public VectorLibraryService(
            VectorLibraryMapper mapper,
            IngestProperties ingestProperties) {
        this.mapper = mapper;
        this.ingestProperties = ingestProperties;
    }

    public List<VectorLibraryResponse> list(String tenantId) {
        return mapper.selectList(new LambdaQueryWrapper<VectorLibrary>()
                        .eq(VectorLibrary::getTenantId, tenantId)
                        .orderByDesc(VectorLibrary::getUpdatedAt))
                .stream()
                .map(VectorLibraryResponse::from)
                .toList();
    }

    public VectorLibraryResponse get(UUID libraryId) {
        return VectorLibraryResponse.from(require(libraryId));
    }

    @Transactional
    public VectorLibraryResponse create(CreateVectorLibraryRequest request) {
        VectorLibraryConfig cfg = request.config() != null ? request.config() : defaultConfig();
        if (cfg.getAllowedMimeTypes() == null) {
            cfg.setAllowedMimeTypes(ingestProperties.getAllowedMimeTypes());
        }

        UUID libraryId = UUID.randomUUID();
        VectorLibrary lib = new VectorLibrary();
        lib.setLibraryId(libraryId);
        lib.setTenantId(request.tenantId().trim());
        lib.setName(request.name().trim());
        lib.setDescription(request.description());
        lib.setStatus(LibraryStatus.ACTIVE);
        lib.setConfigJson(JsonSupport.toJson(cfg));
        lib.setDocumentCount(0);
        lib.setChunkCount(0);
        Instant now = Instant.now();
        lib.setCreatedAt(now);
        lib.setUpdatedAt(now);
        mapper.insert(lib);

        return VectorLibraryResponse.from(lib);
    }

    @Transactional
    public VectorLibraryUpdateResponse updateSettings(UUID libraryId, UpdateVectorLibrarySettingsRequest request) {
        VectorLibrary lib = require(libraryId);
        lib.setName(request.name().trim());
        lib.setDescription(request.description());

        VectorLibraryConfig existing = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        String prevModel = existing.getEmbeddingModel();
        int prevDimension = existing.getEmbeddingDimension();
        String prevProvider = existing.getEmbeddingProvider();

        if (request.config() != null) {
            validateEmbeddingProvider(request.config().getEmbeddingProvider());
        }
        VectorLibraryConfigMerger.mergeSafeFields(existing, request.config());
        validateEmbeddingProvider(existing.getEmbeddingProvider());

        List<String> warnings = new ArrayList<>();
        if (embeddingConfigChanged(prevModel, prevDimension, prevProvider, existing)
                && lib.getChunkCount() > 0) {
            warnings.add(
                    "Embedding 模型、维度或提供方已变更：已有向量与检索可能不一致，请在文档库中对相关文档执行补偿重索引。");
        }

        lib.setConfigJson(JsonSupport.toJson(existing));
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);

        return new VectorLibraryUpdateResponse(VectorLibraryResponse.from(require(libraryId)), warnings);
    }

    private static void validateEmbeddingProvider(String provider) {
        if (provider != null && !provider.isBlank() && !"ollama".equalsIgnoreCase(provider)) {
            throw new UnsupportedEmbeddingProviderException(provider);
        }
    }

    private static boolean embeddingConfigChanged(
            String prevModel,
            int prevDimension,
            String prevProvider,
            VectorLibraryConfig updated) {
        return !Objects.equals(normalize(prevModel), normalize(updated.getEmbeddingModel()))
                || (prevDimension > 0 && updated.getEmbeddingDimension() > 0 && prevDimension != updated.getEmbeddingDimension())
                || !Objects.equals(normalize(prevProvider), normalize(updated.getEmbeddingProvider()));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public VectorLibrary require(UUID libraryId) {
        VectorLibrary lib = mapper.selectById(libraryId);
        if (lib == null) {
            throw new LibraryNotFoundException(libraryId);
        }
        return lib;
    }

    public void incrementDocumentCount(UUID libraryId, int delta) {
        VectorLibrary lib = require(libraryId);
        lib.setDocumentCount(Math.max(0, lib.getDocumentCount() + delta));
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);
    }

    public void incrementChunkCount(UUID libraryId, int delta) {
        VectorLibrary lib = require(libraryId);
        lib.setChunkCount(Math.max(0, lib.getChunkCount() + delta));
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);
    }

    private VectorLibraryConfig defaultConfig() {
        VectorLibraryConfig cfg = new VectorLibraryConfig();
        cfg.setAllowedMimeTypes(ingestProperties.getAllowedMimeTypes());
        return cfg;
    }
}
