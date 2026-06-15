package com.knowbase.pipeline.config;

import com.knowbase.ingest.service.InvalidDocumentException;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.dto.ChunkProfileBackfillResponse;
import com.knowbase.library.dto.ChunkProfileSummaryResponse;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.ingest.mapper.DocMetadataMapper;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkProfileService {

    private final EffectiveConfigResolver effectiveConfigResolver;
    private final LibraryConfigResolver libraryConfigResolver;
    private final DocMetadataMapper docMetadataMapper;
    private final DocMetadataStore docMetadataStore;
    private final DocumentChunkMapper chunkMapper;

    public ChunkProfileService(
            EffectiveConfigResolver effectiveConfigResolver,
            LibraryConfigResolver libraryConfigResolver,
            DocMetadataMapper docMetadataMapper,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        this.effectiveConfigResolver = effectiveConfigResolver;
        this.libraryConfigResolver = libraryConfigResolver;
        this.docMetadataMapper = docMetadataMapper;
        this.docMetadataStore = docMetadataStore;
        this.chunkMapper = chunkMapper;
    }

    public String computeForIngest(UUID libraryId, String mimeType, String ingestProfileJson) {
        IngestProfile profile = IngestProfileSupport.parse(ingestProfileJson);
        EffectivePipelineConfig effective = effectiveConfigResolver.forIngest(libraryId, mimeType, profile);
        return ChunkProfileFingerprint.compute(libraryId, effective);
    }

    public String computeForIngestWithContent(
            UUID libraryId, String mimeType, String ingestProfileJson, String parsedText) {
        IngestProfile profile = IngestProfileSupport.parse(ingestProfileJson);
        EffectivePipelineConfig effective =
                effectiveConfigResolver.forIngestWithContent(libraryId, mimeType, profile, parsedText);
        return ChunkProfileFingerprint.compute(libraryId, effective);
    }

    public String refreshLibraryPrimary(VectorLibraryConfig cfg, UUID libraryId) {
        String primary = ChunkProfileFingerprint.computeLibraryPrimary(libraryId, cfg);
        cfg.setPrimaryChunkProfileId(primary);
        return primary;
    }

    public String resolvedPrimaryProfileId(UUID libraryId) {
        VectorLibraryConfig cfg = libraryConfigResolver.config(libraryId);
        String primary = cfg.getPrimaryChunkProfileId();
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return ChunkProfileFingerprint.computeLibraryPrimary(libraryId, cfg);
    }

    public void validateNewProfileAllowed(UUID libraryId, String mimeType, String ingestProfileJson) {
        VectorLibraryConfig cfg = libraryConfigResolver.config(libraryId);
        String prospective = computeForIngest(libraryId, mimeType, ingestProfileJson);
        String primary = cfg.getPrimaryChunkProfileId();
        if (primary != null && !primary.isBlank() && primary.equals(prospective)) {
            return;
        }

        boolean explicitOverride = IngestProfileSupport.hasChunkingOverride(ingestProfileJson);
        if (!explicitOverride) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_NOT_ALLOWED,
                    "文档分块档与库主档不一致，请刷新库主档配置后重试");
        }
        if (!cfg.isAllowCustomChunkProfiles()) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_NOT_ALLOWED,
                    "该知识库已禁止采集侧分块覆盖；覆盖后将进入非主档且默认问答不可检索");
        }
        if (docMetadataStore.existsChunkProfileId(libraryId, prospective)) {
            return;
        }
        int active = docMetadataStore.countDistinctChunkProfiles(libraryId);
        int max = Math.max(1, cfg.getMaxActiveChunkProfiles());
        if (active >= max) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_PROFILE_NOT_ALLOWED,
                    "该知识库活跃分块档已达上限（" + max + "），请使用库默认或清理旧档文档");
        }
    }

    public List<String> resolveRetrievalProfileIds(
            UUID libraryId, boolean includeAllChunkProfiles, List<String> chunkProfileIds) {
        if (includeAllChunkProfiles) {
            return null;
        }
        if (chunkProfileIds != null && !chunkProfileIds.isEmpty()) {
            return chunkProfileIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
        String primary = libraryConfigResolver.config(libraryId).getPrimaryChunkProfileId();
        if (primary == null || primary.isBlank()) {
            return null;
        }
        return List.of(primary);
    }

    public boolean isPrimaryProfile(UUID libraryId, String chunkProfileId) {
        if (chunkProfileId == null || chunkProfileId.isBlank()) {
            return false;
        }
        String primary = libraryConfigResolver.config(libraryId).getPrimaryChunkProfileId();
        return primary != null && primary.equals(chunkProfileId);
    }

    public List<ChunkProfileSummaryResponse> listProfiles(UUID libraryId) {
        String primary = libraryConfigResolver.config(libraryId).getPrimaryChunkProfileId();
        List<DocMetadataStore.ChunkProfileStatsRow> rows = docMetadataStore.listChunkProfileStats(libraryId);
        List<ChunkProfileSummaryResponse> result = new ArrayList<>(rows.size());
        for (DocMetadataStore.ChunkProfileStatsRow row : rows) {
            result.add(new ChunkProfileSummaryResponse(
                    row.chunkProfileId(),
                    row.docCount(),
                    row.chunkCount(),
                    primary != null && primary.equals(row.chunkProfileId())));
        }
        return result;
    }

    public int countActiveProfiles(UUID libraryId) {
        return docMetadataStore.countDistinctChunkProfiles(libraryId);
    }

    public ChunkProfileBackfillResponse backfillLibrary(UUID libraryId) {
        libraryConfigResolver.requireLibrary(libraryId);
        List<DocMetadata> docs = docMetadataStore.findMissingChunkProfile(libraryId);
        int backfilledDocs = 0;
        int updatedChunks = 0;
        for (DocMetadata doc : docs) {
            String profileId = computeForIngest(doc.getLibraryId(), doc.getMimeType(), doc.getIngestProfileJson());
            doc.setChunkProfileId(profileId);
            docMetadataStore.save(doc);
            backfilledDocs++;
            updatedChunks += chunkMapper.backfillChunkProfileMetadata(doc.getDocId(), doc.getVersion(), profileId);
        }
        return new ChunkProfileBackfillResponse(backfilledDocs, updatedChunks);
    }

    public void requireExistingProfile(UUID libraryId, String chunkProfileId) {
        if (chunkProfileId == null || chunkProfileId.isBlank()) {
            throw new IllegalArgumentException("chunkProfileId is required");
        }
        if (!docMetadataStore.existsChunkProfileId(libraryId, chunkProfileId.trim())) {
            throw new IllegalArgumentException("分块档不存在或未关联文档");
        }
    }
}
