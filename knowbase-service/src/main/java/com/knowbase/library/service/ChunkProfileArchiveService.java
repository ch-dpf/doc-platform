package com.knowbase.library.service;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.library.dto.ArchiveCandidateItem;
import com.knowbase.library.dto.ArchiveCandidatesResponse;
import com.knowbase.library.dto.ArchiveChunkProfileResponse;
import com.knowbase.pipeline.config.ChunkProfileService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ChunkProfileArchiveService {

    private final ChunkProfileService chunkProfileService;
    private final DocMetadataStore docMetadataStore;
    private final LibraryBatchJobService batchJobService;
    private final LibraryBatchJobWorker batchJobWorker;
    private final LibraryConfigResolver libraryConfigResolver;

    public ChunkProfileArchiveService(
            ChunkProfileService chunkProfileService,
            DocMetadataStore docMetadataStore,
            LibraryBatchJobService batchJobService,
            LibraryBatchJobWorker batchJobWorker,
            LibraryConfigResolver libraryConfigResolver) {
        this.chunkProfileService = chunkProfileService;
        this.docMetadataStore = docMetadataStore;
        this.batchJobService = batchJobService;
        this.batchJobWorker = batchJobWorker;
        this.libraryConfigResolver = libraryConfigResolver;
    }

    private static final int ARCHIVE_PREVIEW_LIMIT = 20;

    public ArchiveCandidatesResponse listArchiveCandidates(
            UUID libraryId, String tenantId, String chunkProfileId) {
        libraryConfigResolver.requireLibrary(libraryId);
        String profile = chunkProfileId.trim();
        chunkProfileService.requireExistingProfile(libraryId, profile);
        if (chunkProfileService.isPrimaryProfile(libraryId, profile)) {
            throw new IllegalArgumentException("不能预览主分块档归档候选");
        }
        List<DocMetadata> docs =
                docMetadataStore.findActiveByChunkProfile(libraryId, tenantId.trim(), profile);
        List<ArchiveCandidateItem> preview = docs.stream()
                .limit(ARCHIVE_PREVIEW_LIMIT)
                .map(d -> new ArchiveCandidateItem(d.getDocId(), d.getFileName()))
                .toList();
        return new ArchiveCandidatesResponse(docs.size(), preview);
    }

    public ArchiveChunkProfileResponse scheduleArchive(UUID libraryId, String tenantId, String chunkProfileId) {
        libraryConfigResolver.requireLibrary(libraryId);
        String profile = chunkProfileId.trim();
        chunkProfileService.requireExistingProfile(libraryId, profile);
        if (chunkProfileService.isPrimaryProfile(libraryId, profile)) {
            throw new IllegalArgumentException("不能归档主分块档，请先切换主档");
        }
        String tid = tenantId.trim();
        List<DocMetadata> docs = docMetadataStore.findActiveByChunkProfile(libraryId, tid, profile);
        int count = docs.size();
        UUID jobId = null;
        if (count > 0) {
            jobId = batchJobService.createJob(libraryId, tid, LibraryBatchJobType.ARCHIVE, profile, count);
            batchJobWorker.runArchive(jobId, libraryId, tid, profile);
        }
        String message = count > 0
                ? "已提交分块档 " + profile + " 下 " + count + " 个文档的归档清理任务（异步）"
                : "该分块档下没有可归档的文档";
        return new ArchiveChunkProfileResponse(count, message, profile, jobId);
    }
}
