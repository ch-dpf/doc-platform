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
import com.knowbase.library.dto.ChunkProfileBackfillResponse;
import com.knowbase.library.dto.TemporalMetadataBackfillResponse;
import com.knowbase.library.dto.CreateVectorLibraryRequest;
import com.knowbase.library.dto.SetPrimaryChunkProfileRequest;
import com.knowbase.library.dto.UpdateChunkGovernanceRequest;
import com.knowbase.library.dto.DeleteVectorLibraryResponse;
import com.knowbase.library.dto.UpdateLibraryBasicRequest;
import com.knowbase.library.dto.UpdateLibraryIndexPipelineRequest;
import com.knowbase.library.dto.UpdateLibraryParsingRequest;
import com.knowbase.library.dto.UpdateLibraryRetrievalRequest;
import com.knowbase.library.dto.VectorLibraryListItemResponse;
import com.knowbase.library.dto.VectorLibraryListQuery;
import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.dto.VectorLibraryUpdateResponse;
import com.knowbase.library.mapper.VectorLibraryMapper;
import com.knowbase.pipeline.config.ChunkProfileService;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import com.knowbase.vector.service.TemporalMetadataBackfillService;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;
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
    private final ChunkProfileService chunkProfileService;
    private final TemporalMetadataBackfillService temporalMetadataBackfillService;

    public VectorLibraryService(
            VectorLibraryMapper mapper,
            IngestProperties ingestProperties,
            LibraryDeletionService libraryDeletionService,
            DocMetadataStore metadataStore,
            DocumentChunkMapper chunkMapper,
            ChunkProfileService chunkProfileService,
            TemporalMetadataBackfillService temporalMetadataBackfillService) {
        this.mapper = mapper;
        this.ingestProperties = ingestProperties;
        this.libraryDeletionService = libraryDeletionService;
        this.metadataStore = metadataStore;
        this.chunkMapper = chunkMapper;
        this.chunkProfileService = chunkProfileService;
        this.temporalMetadataBackfillService = temporalMetadataBackfillService;
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

        String tenantId = query.tenantId().trim();
        List<VectorLibraryListItemResponse> items = result.getRecords().stream()
                .map(lib -> VectorLibraryListItemResponse.from(lib, countPendingMigration(lib, tenantId)))
                .toList();
        return new PageResponse<>(items, total, pageNum, pageSize);
    }

    private int countPendingMigration(VectorLibrary lib, String tenantId) {
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        String primary = cfg.getPrimaryChunkProfileId();
        if (primary == null || primary.isBlank()) {
            return 0;
        }
        return metadataStore.countParsedWithTextKeyNotOnPrimary(
                lib.getLibraryId(), tenantId, primary.trim());
    }

    public List<com.knowbase.library.dto.ChunkProfileSummaryResponse> listChunkProfiles(UUID libraryId) {
        require(libraryId);
        return chunkProfileService.listProfiles(libraryId);
    }

    @KnowbaseTransactional
    public VectorLibraryResponse setPrimaryChunkProfile(UUID libraryId, SetPrimaryChunkProfileRequest request) {
        VectorLibrary lib = require(libraryId);
        String profileId = request.chunkProfileId().trim();
        chunkProfileService.requireExistingProfile(libraryId, profileId);
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        cfg.setPrimaryChunkProfileId(profileId);
        persistConfigOnly(lib, cfg);
        return get(libraryId);
    }

    @KnowbaseTransactional
    public VectorLibraryResponse updateChunkGovernance(UUID libraryId, UpdateChunkGovernanceRequest request) {
        VectorLibrary lib = require(libraryId);
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        cfg.setAllowCustomChunkProfiles(request.allowCustomChunkProfiles());
        cfg.setMaxActiveChunkProfiles(request.maxActiveChunkProfiles());
        persistConfigOnly(lib, cfg);
        return get(libraryId);
    }

    @KnowbaseTransactional
    public ChunkProfileBackfillResponse backfillChunkProfiles(UUID libraryId) {
        require(libraryId);
        return chunkProfileService.backfillLibrary(libraryId);
    }

    @KnowbaseTransactional
    public TemporalMetadataBackfillResponse backfillTemporalMetadata(UUID libraryId, String tenantId) {
        require(libraryId);
        return temporalMetadataBackfillService.backfillLibrary(libraryId, tenantId);
    }

    private void persistConfigOnly(VectorLibrary lib, VectorLibraryConfig cfg) {
        lib.setConfigJson(JsonSupport.toJson(cfg));
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);
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

    @KnowbaseTransactional
    public DeleteVectorLibraryResponse delete(UUID libraryId, String tenantId) {
        return libraryDeletionService.delete(libraryId, tenantId);
    }

    @KnowbaseTransactional
    public VectorLibraryResponse create(CreateVectorLibraryRequest request) {
        VectorLibraryConfig cfg = defaultConfig();
        VectorLibraryConfigFactory.applyDefaults(cfg, ingestProperties.getAllowedMimeTypes());
        if (request.tags() != null && !request.tags().isEmpty()) {
            cfg.setTags(request.tags());
        }
        if (request.indexPipeline() != null) {
            VectorLibraryConfigMerger.mergeIndexPipeline(cfg, request.indexPipeline());
            validateEmbeddingProvider(cfg.getEmbeddingProvider());
        }
        if (request.parsing() != null) {
            VectorLibraryConfigMerger.mergeParsing(cfg, request.parsing());
        }
        if (request.retrieval() != null) {
            VectorLibraryConfigMerger.mergeRetrieval(cfg, request.retrieval());
        }
        cfg.setConfigVersion(1);
        syncAllowedMimeTypes(cfg);

        UUID libraryId = UUID.randomUUID();
        chunkProfileService.refreshLibraryPrimary(cfg, libraryId);
        VectorLibrary lib = new VectorLibrary();
        lib.setLibraryId(libraryId);
        lib.setTenantId(request.tenantId().trim());
        lib.setName(request.name().trim());
        lib.setDescription(normalizeDescription(request.description()));
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

    @KnowbaseTransactional
    public VectorLibraryUpdateResponse updateBasic(UUID libraryId, UpdateLibraryBasicRequest request) {
        VectorLibrary lib = require(libraryId);
        lib.setName(request.name().trim());
        lib.setDescription(normalizeDescription(request.description()));

        VectorLibraryConfig existing = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        VectorLibraryConfigMerger.mergeBasic(existing, request.tags());
        syncAllowedMimeTypes(existing);

        int documentCount = liveDocumentCount(libraryId);
        int chunkCount = liveChunkCount(libraryId);
        syncCountsIfNeeded(lib, documentCount, chunkCount);
        return persistConfig(lib, existing, List.of(), documentCount, chunkCount);
    }

    @KnowbaseTransactional
    public VectorLibraryUpdateResponse updateIndexPipeline(UUID libraryId, UpdateLibraryIndexPipelineRequest request) {
        VectorLibrary lib = require(libraryId);
        int documentCount = liveDocumentCount(libraryId);
        int chunkCount = liveChunkCount(libraryId);
        syncCountsIfNeeded(lib, documentCount, chunkCount);

        if (chunkCount > 0) {
            throw new PipelineConfigLockedException();
        }

        VectorLibraryConfig existing = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        String prevModel = existing.getEmbeddingModel();
        int prevDimension = existing.getEmbeddingDimension();
        String prevProvider = existing.getEmbeddingProvider();

        VectorLibraryConfigMerger.mergeIndexPipeline(existing, request.indexPipeline());
        validateEmbeddingProvider(existing.getEmbeddingProvider());
        chunkProfileService.refreshLibraryPrimary(existing, libraryId);

        List<String> warnings = new ArrayList<>();
        if (embeddingConfigChanged(prevModel, prevDimension, prevProvider, existing) && chunkCount > 0) {
            warnings.add(
                    "Embedding 模型、维度或提供方已变更：已有向量与检索可能不一致，请在文档库中对相关文档执行补偿重索引。");
        }
        return persistConfig(lib, existing, warnings, documentCount, chunkCount);
    }

    @KnowbaseTransactional
    public VectorLibraryUpdateResponse updateParsing(UUID libraryId, UpdateLibraryParsingRequest request) {
        VectorLibrary lib = require(libraryId);
        VectorLibraryConfig existing = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        VectorLibraryConfigMerger.mergeParsing(existing, request.parsing());

        int documentCount = liveDocumentCount(libraryId);
        int chunkCount = liveChunkCount(libraryId);
        syncCountsIfNeeded(lib, documentCount, chunkCount);

        List<String> warnings = new ArrayList<>();
        if (documentCount > 0) {
            warnings.add("解析配置已变更：已有文档需重新解析并重索引后才会按新解析器生效。");
        }
        return persistConfig(lib, existing, warnings, documentCount, chunkCount);
    }

    @KnowbaseTransactional
    public VectorLibraryUpdateResponse updateRetrieval(UUID libraryId, UpdateLibraryRetrievalRequest request) {
        VectorLibrary lib = require(libraryId);
        VectorLibraryConfig existing = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        VectorLibraryConfigMerger.mergeRetrieval(existing, request.retrieval());

        int documentCount = liveDocumentCount(libraryId);
        int chunkCount = liveChunkCount(libraryId);
        syncCountsIfNeeded(lib, documentCount, chunkCount);
        return persistConfig(lib, existing, List.of(), documentCount, chunkCount);
    }

    private VectorLibraryUpdateResponse persistConfig(
            VectorLibrary lib,
            VectorLibraryConfig cfg,
            List<String> warnings,
            int documentCount,
            int chunkCount) {
        cfg.setConfigVersion(Math.max(1, cfg.getConfigVersion()) + 1);
        lib.setConfigJson(JsonSupport.toJson(cfg));
        lib.setUpdatedAt(Instant.now());
        mapper.updateById(lib);
        return new VectorLibraryUpdateResponse(
                VectorLibraryResponse.from(require(lib.getLibraryId()), documentCount, chunkCount), warnings);
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
        cfg.setAllowedMimeTypes(ingestProperties.getAllowedMimeTypes());
    }

    private static String normalizeDescription(String description) {
        return description != null ? description.trim() : "";
    }

    private VectorLibraryConfig defaultConfig() {
        return VectorLibraryConfigFactory.defaults(ingestProperties.getAllowedMimeTypes());
    }
}
