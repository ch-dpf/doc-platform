package com.knowbase.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowbase.library.domain.LibraryBatchJob;
import com.knowbase.library.domain.LibraryBatchJobStatus;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.dto.FailedBatchJobItem;
import com.knowbase.library.dto.FailedBatchJobItemsResponse;
import com.knowbase.library.dto.LibraryBatchJobResponse;
import com.knowbase.library.mapper.LibraryBatchJobMapper;
import com.knowbase.library.support.FailedDocIdsJson;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LibraryBatchJobService {

    private final LibraryBatchJobMapper jobMapper;
    private final DocMetadataStore docMetadataStore;

    public LibraryBatchJobService(LibraryBatchJobMapper jobMapper, DocMetadataStore docMetadataStore) {
        this.jobMapper = jobMapper;
        this.docMetadataStore = docMetadataStore;
    }

    @KnowbaseTransactional
    public UUID createJob(
            UUID libraryId,
            String tenantId,
            LibraryBatchJobType jobType,
            String chunkProfileId,
            int totalCount) {
        UUID jobId = UUID.randomUUID();
        LibraryBatchJob job = new LibraryBatchJob();
        job.setJobId(jobId);
        job.setLibraryId(libraryId);
        job.setTenantId(tenantId.trim());
        job.setJobType(jobType);
        job.setChunkProfileId(chunkProfileId);
        job.setStatus(LibraryBatchJobStatus.QUEUED);
        job.setTotalCount(totalCount);
        job.setCompletedCount(0);
        job.setFailedCount(0);
        job.setFailedDocIdsJson(FailedDocIdsJson.empty());
        Instant now = Instant.now();
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        jobMapper.insert(job);
        return jobId;
    }

    public LibraryBatchJobResponse get(UUID jobId) {
        return LibraryBatchJobResponse.from(requireJob(jobId));
    }

    public FailedBatchJobItemsResponse listFailedItems(UUID jobId) {
        LibraryBatchJob job = requireJob(jobId);
        List<UUID> failedIds = FailedDocIdsJson.parse(job.getFailedDocIdsJson());
        if (failedIds.isEmpty()) {
            return new FailedBatchJobItemsResponse(List.of());
        }
        List<DocMetadata> docs = docMetadataStore.findAnyByDocIds(failedIds);
        Map<UUID, DocMetadata> byId = new java.util.HashMap<>();
        for (DocMetadata doc : docs) {
            byId.put(doc.getDocId(), doc);
        }
        List<FailedBatchJobItem> items = new ArrayList<>(failedIds.size());
        for (UUID docId : failedIds) {
            DocMetadata doc = byId.get(docId);
            if (doc != null) {
                items.add(new FailedBatchJobItem(docId, doc.getFileName(), doc.isDeleted()));
            } else {
                items.add(new FailedBatchJobItem(docId, null, true));
            }
        }
        return new FailedBatchJobItemsResponse(items);
    }

    public List<LibraryBatchJobResponse> listByLibrary(UUID libraryId, String tenantId, int limit) {
        int capped = Math.min(Math.max(limit, 1), 50);
        LambdaQueryWrapper<LibraryBatchJob> wrapper = new LambdaQueryWrapper<LibraryBatchJob>()
                .eq(LibraryBatchJob::getLibraryId, libraryId)
                .orderByDesc(LibraryBatchJob::getCreatedAt)
                .last("LIMIT " + capped);
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(LibraryBatchJob::getTenantId, tenantId.trim());
        }
        return jobMapper.selectList(wrapper).stream().map(LibraryBatchJobResponse::from).toList();
    }

    @KnowbaseTransactional
    public void markRunning(UUID jobId) {
        LibraryBatchJob job = requireJob(jobId);
        job.setStatus(LibraryBatchJobStatus.RUNNING);
        job.setUpdatedAt(Instant.now());
        jobMapper.updateById(job);
    }

    @KnowbaseTransactional
    public void recordSuccess(UUID jobId) {
        LibraryBatchJob job = requireJob(jobId);
        job.setCompletedCount(job.getCompletedCount() + 1);
        job.setUpdatedAt(Instant.now());
        jobMapper.updateById(job);
    }

    @KnowbaseTransactional
    public void recordFailure(UUID jobId, UUID docId, String error) {
        LibraryBatchJob job = requireJob(jobId);
        job.setFailedCount(job.getFailedCount() + 1);
        if (docId != null) {
            job.setFailedDocIdsJson(FailedDocIdsJson.append(job.getFailedDocIdsJson(), docId));
        }
        if (error != null && !error.isBlank()) {
            job.setLastError(truncate(error));
        }
        job.setUpdatedAt(Instant.now());
        jobMapper.updateById(job);
    }

    @KnowbaseTransactional
    public void finish(UUID jobId) {
        LibraryBatchJob job = requireJob(jobId);
        int done = job.getCompletedCount() + job.getFailedCount();
        if (job.getTotalCount() <= 0 || done == 0) {
            job.setStatus(LibraryBatchJobStatus.FAILED);
        } else if (job.getFailedCount() == 0) {
            job.setStatus(LibraryBatchJobStatus.COMPLETED);
        } else if (job.getCompletedCount() == 0) {
            job.setStatus(LibraryBatchJobStatus.FAILED);
        } else {
            job.setStatus(LibraryBatchJobStatus.PARTIAL);
        }
        job.setUpdatedAt(Instant.now());
        jobMapper.updateById(job);
    }

    public LibraryBatchJob requireJob(UUID jobId) {
        LibraryBatchJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("批量任务不存在: " + jobId);
        }
        return job;
    }

    private static String truncate(String error) {
        return error.length() <= 500 ? error : error.substring(0, 500);
    }
}
