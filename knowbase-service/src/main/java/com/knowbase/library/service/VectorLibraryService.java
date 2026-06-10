package com.knowbase.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.dto.PageResponse;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.config.VectorLibraryConfigFactory;
import com.knowbase.library.config.VectorLibraryConfigMerger;
import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.library.dto.CreateVectorLibraryRequest;
import com.knowbase.library.dto.DeleteVectorLibraryResponse;
import com.knowbase.library.dto.UpdateVectorLibrarySettingsRequest;
import com.knowbase.library.dto.VectorLibraryListItemResponse;
import com.knowbase.library.dto.VectorLibraryListQuery;
import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.dto.VectorLibraryUpdateResponse;
import com.knowbase.library.mapper.VectorLibraryMapper;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class VectorLibraryService {

    public static final UUID DEFAULT_LIBRARY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final VectorLibraryMapper mapper;
    private final IngestProperties ingestProperties;
    private final LibraryDeletionService libraryDeletionService;
    private final DocMetadataStore metadataStore;
    private final DocumentChunkMapper chunkMapper;

    public VectorLibraryService(
            VectorLibraryMapper mapper,
            IngestProperties ingestProperties,
            LibraryDeletionService libraryDeletionService,
            DocMetadataStore metadataStore,
            DocumentChunkMapper chunkMapper) {
        this.mapper = mapper;
        this.ingestProperties = ingestProperties;
        this.libraryDeletionService = libraryDeletionService;
        this.metadataStore = metadataStore;
        this.chunkMapper = chunkMapper;
    }

    public PageResponse<VectorLibraryListItemResponse> list(VectorLibraryListQuery query) {
        LambdaQueryWrapper<VectorLibrary> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorLibrary::getTenantId, query.tenantId().trim());
        if (StringUtils.hasText(query.keyword())) {
            String pattern = "%" + query.keyword().trim() + "%";
            wrapper.and(w -> w.like(VectorLibrary::getName, pattern).or().like(VectorLibrary::getDescription, pattern));
        }
        applyTagFilter(wrapper, query.tag());
        wrapper.orderByDesc(VectorLibrary::getUpdatedAt);

        int pageNum = query.page();
        int pageSize = query.size();
        IPage<VectorLibrary> result = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        long total = result.getTotal();
        if (total > 0 && result.getRecords().isEmpty() && pageNum > 1) {
            int maxPage = (int) Math.min(Integer.MAX_VALUE, (total + pageSize - 1L) / pageSize);
            pageNum = Math.max(1, maxPage);
            result = mapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        }

        List<VectorLibraryListItemResponse> items = result.getRecords().stream()
                .map(VectorLibraryListItemResponse::from)
                .toList();
        return new PageResponse<>(items, total, pageNum, pageSize);
    }

    public VectorLibraryResponse get(UUID libraryId) {
        VectorLibrary lib = require(libraryId);
        int documentCount = liveDocumentCount(libraryId);
        int chunkCount = liveChunkCount(libraryId);
        syncCountsIfNeeded(lib, documentCount, chunkCount);
        return VectorLibraryResponse.from(lib, documentCount, chunkCount);
    }

    public List<String> listDistinctTags(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        List<String> tags = mapper.selectDistinctTags(tenantId.trim());
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return List.copyOf(new LinkedHashSet<>(tags));
    }

    private static void applyTagFilter(LambdaQueryWrapper<VectorLibrary> wrapper, String tag) {
        if (!StringUtils.hasText(tag)) {
            return;
        }
        String fragment = JsonSupport.toJson(Map.of("tags", List.of(tag.trim())));
        wrapper.apply("config_json::jsonb @> {0}::jsonb", fragment);
    }

    @Transactional
    public DeleteVectorLibraryResponse delete(UUID libraryId, String tenantId) {
        return libraryDeletionService.delete(libraryId, tenantId);
    }

    @Transactional
    public VectorLibraryResponse create(CreateVectorLibraryRequest request) {
        VectorLibraryConfig cfg = request.config() != null ? request.config() : defaultConfig();
        VectorLibraryConfigFactory.applyPhase2Defaults(cfg, ingestProperties.getAllowedMimeTypes(), cfg.getWizardMode());
        if (request.tags() != null && !request.tags().isEmpty()) {
            cfg.setTags(request.tags());
        }
        cfg.setConfigVersion(1);
        cfg.setAllowedMimeTypes(VectorLibraryConfigFactory.resolveMimeTypes(
                cfg.getIngestAccess() != null ? cfg.getIngestAccess().getSupportedFileTypes() : null,
                ingestProperties.getAllowedMimeTypes()));

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
        int documentCount = liveDocumentCount(libraryId);
        int chunkCount = liveChunkCount(libraryId);
        syncCountsIfNeeded(lib, documentCount, chunkCount);
        boolean lockPipeline = documentCount > 0 || chunkCount > 0;
        VectorLibraryConfigMerger.mergeSafeFields(existing, request.config(), lockPipeline);
        validateEmbeddingProvider(existing.getEmbeddingProvider());
        syncAllowedMimeTypes(existing);

        List<String> warnings = new ArrayList<>();
        if (embeddingConfigChanged(prevModel, prevDimension, prevProvider, existing)
                && chunkCount > 0) {
            warnings.add(
                    "Embedding 模型、维度或提供方已变更：已有向量与检索可能不一致，请在文档库中对相关文档执行补偿重索引。");
        }

        existing.setConfigVersion(Math.max(1, existing.getConfigVersion()) + 1);
        lib.setConfigJson(JsonSupport.toJson(existing));
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);

        return new VectorLibraryUpdateResponse(
                VectorLibraryResponse.from(require(libraryId), documentCount, chunkCount), warnings);
    }

    private int liveDocumentCount(UUID libraryId) {
        return metadataStore.countActiveByLibraryId(libraryId);
    }

    private int liveChunkCount(UUID libraryId) {
        return chunkMapper.countByLibraryId(libraryId);
    }

    private void syncCountsIfNeeded(VectorLibrary lib, int documentCount, int chunkCount) {
        if (lib.getDocumentCount() == documentCount && lib.getChunkCount() == chunkCount) {
            return;
        }
        lib.setDocumentCount(documentCount);
        lib.setChunkCount(chunkCount);
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);
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

    public void adjustChunkCount(UUID libraryId, int delta) {
        incrementChunkCount(libraryId, delta);
    }

    private void syncAllowedMimeTypes(VectorLibraryConfig cfg) {
        List<String> fileTypes =
                cfg.getIngestAccess() != null ? cfg.getIngestAccess().getSupportedFileTypes() : null;
        cfg.setAllowedMimeTypes(VectorLibraryConfigFactory.resolveMimeTypes(
                fileTypes, ingestProperties.getAllowedMimeTypes()));
    }

    private VectorLibraryConfig defaultConfig() {
        return VectorLibraryConfigFactory.quickDefaults(ingestProperties.getAllowedMimeTypes());
    }
}
