package com.knowbase.library.service;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.domain.LibraryBatchJob;
import com.knowbase.library.domain.LibraryBatchJobStatus;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.library.dto.RetryBatchJobResponse;
import com.knowbase.library.support.FailedDocIdsJson;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryBatchJobRetryService {

    private final LibraryBatchJobService batchJobService;
    private final LibraryBatchJobWorker batchJobWorker;
    private final DocMetadataStore docMetadataStore;

    public LibraryBatchJobRetryService(
            LibraryBatchJobService batchJobService,
            LibraryBatchJobWorker batchJobWorker,
            DocMetadataStore docMetadataStore) {
        this.batchJobService = batchJobService;
        this.batchJobWorker = batchJobWorker;
        this.docMetadataStore = docMetadataStore;
    }

    @Transactional
    public RetryBatchJobResponse retry(UUID sourceJobId) {
        LibraryBatchJob source = batchJobService.requireJob(sourceJobId);
        if (source.getStatus() != LibraryBatchJobStatus.PARTIAL
                && source.getStatus() != LibraryBatchJobStatus.FAILED) {
            throw new IllegalArgumentException("仅部分失败或失败的任务可重试");
        }
        List<UUID> failedIds = FailedDocIdsJson.parse(source.getFailedDocIdsJson());
        if (failedIds.isEmpty()) {
            throw new IllegalArgumentException("没有可重试的失败文档");
        }
        List<DocMetadata> docs = docMetadataStore.findActiveByIds(failedIds);
        if (docs.isEmpty()) {
            throw new IllegalArgumentException("失败文档已不存在或已删除，无法重试");
        }
        UUID jobId = batchJobService.createJob(
                source.getLibraryId(),
                source.getTenantId(),
                source.getJobType(),
                source.getChunkProfileId(),
                docs.size());
        if (source.getJobType() == LibraryBatchJobType.REBUILD) {
            batchJobWorker.runRebuildForDocs(jobId, docs);
        } else {
            batchJobWorker.runArchiveForDocs(jobId, docs);
        }
        String message = "已提交 " + docs.size() + " 个失败项的重试任务";
        return new RetryBatchJobResponse(sourceJobId, jobId, docs.size(), message);
    }
}
