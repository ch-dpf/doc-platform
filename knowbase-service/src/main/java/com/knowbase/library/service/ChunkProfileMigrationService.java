package com.knowbase.library.service;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.library.dto.MigrateToPrimaryResponse;
import com.knowbase.library.dto.MigrationCandidatesResponse;
import com.knowbase.library.dto.MigrationProfileBreakdown;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChunkProfileMigrationService {

    private final DocMetadataStore docMetadataStore;
    private final LibraryConfigResolver libraryConfigResolver;
    private final LibraryBatchJobService batchJobService;
    private final LibraryBatchJobWorker batchJobWorker;

    public ChunkProfileMigrationService(
            DocMetadataStore docMetadataStore,
            LibraryConfigResolver libraryConfigResolver,
            LibraryBatchJobService batchJobService,
            LibraryBatchJobWorker batchJobWorker) {
        this.docMetadataStore = docMetadataStore;
        this.libraryConfigResolver = libraryConfigResolver;
        this.batchJobService = batchJobService;
        this.batchJobWorker = batchJobWorker;
    }

    public MigrationCandidatesResponse getMigrationCandidates(UUID libraryId, String tenantId) {
        libraryConfigResolver.requireLibrary(libraryId);
        String primary = primaryProfileId(libraryId);
        List<DocMetadata> docs =
                docMetadataStore.findParsedWithTextKeyNotOnPrimary(libraryId, tenantId.trim(), primary);
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        for (DocMetadata doc : docs) {
            String profile = doc.getChunkProfileId();
            String key = StringUtils.hasText(profile) ? profile.trim() : "(未标注)";
            breakdown.merge(key, 1, Integer::sum);
        }
        List<MigrationProfileBreakdown> rows = new ArrayList<>(breakdown.size());
        breakdown.forEach((profileId, count) -> rows.add(new MigrationProfileBreakdown(profileId, count)));
        return new MigrationCandidatesResponse(primary, docs.size(), rows);
    }

    public MigrateToPrimaryResponse scheduleMigrateToPrimary(UUID libraryId, String tenantId) {
        libraryConfigResolver.requireLibrary(libraryId);
        String primary = primaryProfileId(libraryId);
        String tid = tenantId.trim();
        List<DocMetadata> docs =
                docMetadataStore.findParsedWithTextKeyNotOnPrimary(libraryId, tid, primary);
        int count = docs.size();
        UUID jobId = null;
        if (count > 0) {
            jobId = batchJobService.createJob(libraryId, tid, LibraryBatchJobType.MIGRATE, null, count);
            batchJobWorker.runRebuildForDocs(jobId, docs);
        }
        String message = count > 0
                ? "已提交 " + count + " 个文档迁移到主档 " + primary + " 的重索引任务（异步）"
                : "所有已解析文档已在主档，无需迁移";
        return new MigrateToPrimaryResponse(count, message, primary, jobId);
    }

    private String primaryProfileId(UUID libraryId) {
        VectorLibraryConfig cfg = libraryConfigResolver.config(libraryId);
        String primary = cfg.getPrimaryChunkProfileId();
        return primary != null ? primary.trim() : "";
    }
}
