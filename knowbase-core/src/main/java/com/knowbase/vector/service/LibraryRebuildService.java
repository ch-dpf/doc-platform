package com.knowbase.vector.service;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.library.service.LibraryBatchJobService;
import com.knowbase.library.service.LibraryBatchJobWorker;
import com.knowbase.vector.dto.RebuildCandidatesResponse;
import com.knowbase.vector.dto.RebuildLibraryResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryRebuildService {

    private final DocMetadataStore docMetadataStore;
    private final LibraryBatchJobService batchJobService;
    private final LibraryBatchJobWorker batchJobWorker;

    public LibraryRebuildService(
            DocMetadataStore docMetadataStore,
            LibraryBatchJobService batchJobService,
            LibraryBatchJobWorker batchJobWorker) {
        this.docMetadataStore = docMetadataStore;
        this.batchJobService = batchJobService;
        this.batchJobWorker = batchJobWorker;
    }

    public RebuildCandidatesResponse countCandidates(UUID libraryId, String tenantId, String chunkProfileId) {
        String profile = normalizeProfileFilter(chunkProfileId);
        int count = findCandidates(libraryId, tenantId, profile).size();
        return new RebuildCandidatesResponse(count, profile);
    }

    public int countCandidates(UUID libraryId, String tenantId) {
        return countCandidates(libraryId, tenantId, null).candidateCount();
    }

    public RebuildLibraryResponse scheduleRebuild(UUID libraryId, String tenantId, String chunkProfileId) {
        String profile = normalizeProfileFilter(chunkProfileId);
        String tid = tenantId.trim();
        int count = findCandidates(libraryId, tid, profile).size();
        UUID jobId = null;
        if (count > 0) {
            jobId = batchJobService.createJob(libraryId, tid, LibraryBatchJobType.REBUILD, profile, count);
            batchJobWorker.runRebuild(jobId, libraryId, tid, profile);
        }
        String message = buildMessage(count, profile);
        return new RebuildLibraryResponse(count, message, profile, jobId);
    }

    public RebuildLibraryResponse scheduleRebuild(UUID libraryId, String tenantId) {
        return scheduleRebuild(libraryId, tenantId, null);
    }

    private List<DocMetadata> findCandidates(UUID libraryId, String tenantId, String chunkProfileId) {
        return docMetadataStore.findParsedWithTextKey(libraryId, tenantId.trim(), chunkProfileId);
    }

    private static String normalizeProfileFilter(String chunkProfileId) {
        if (chunkProfileId == null || chunkProfileId.isBlank()) {
            return null;
        }
        return chunkProfileId.trim();
    }

    private static String buildMessage(int count, String chunkProfileId) {
        if (count <= 0) {
            if (chunkProfileId != null) {
                return "该分块档下没有可重索引的已解析文档";
            }
            return "当前库内没有可重索引的已解析文档";
        }
        if (chunkProfileId != null) {
            return "已提交分块档 " + chunkProfileId + " 下 " + count + " 个文档的批量重索引任务（异步）";
        }
        return "已提交 " + count + " 个文档的批量重索引任务（异步）";
    }
}
