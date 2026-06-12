package com.knowbase.library.service;



import com.knowbase.event.DocumentReadyForIndexEvent;

import com.knowbase.ingest.domain.DocMetadata;

import com.knowbase.ingest.domain.ParseStatus;

import com.knowbase.ingest.service.DocumentPipelineService;

import com.knowbase.ingest.support.DocMetadataStore;

import com.knowbase.library.domain.LibraryBatchJob;

import com.knowbase.library.domain.LibraryBatchJobType;

import com.knowbase.vector.service.IndexingService;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;



import java.util.List;

import java.util.UUID;



@Component

public class LibraryBatchJobWorker {



    private static final Logger log = LoggerFactory.getLogger(LibraryBatchJobWorker.class);



    private final DocMetadataStore docMetadataStore;

    private final IndexingService indexingService;

    private final DocumentPipelineService documentPipelineService;

    private final LibraryBatchJobService batchJobService;

    private final ChunkProfileCleanupService chunkProfileCleanupService;



    public LibraryBatchJobWorker(

            DocMetadataStore docMetadataStore,

            IndexingService indexingService,

            DocumentPipelineService documentPipelineService,

            LibraryBatchJobService batchJobService,

            ChunkProfileCleanupService chunkProfileCleanupService) {

        this.docMetadataStore = docMetadataStore;

        this.indexingService = indexingService;

        this.documentPipelineService = documentPipelineService;

        this.batchJobService = batchJobService;

        this.chunkProfileCleanupService = chunkProfileCleanupService;

    }



    @Async

    public void runRebuild(UUID jobId, UUID libraryId, String tenantId, String chunkProfileId) {

        List<DocMetadata> docs = docMetadataStore.findParsedWithTextKey(libraryId, tenantId, chunkProfileId);

        log.info(

                "Starting rebuild job {} for {} tenant {} profile {} ({} docs)",

                jobId,

                libraryId,

                tenantId,

                chunkProfileId != null ? chunkProfileId : "*",

                docs.size());

        runRebuildForDocs(jobId, docs);

    }



    @Async

    public void runRebuildForDocs(UUID jobId, List<DocMetadata> docs) {

        batchJobService.markRunning(jobId);
        LibraryBatchJob job = batchJobService.requireJob(jobId);
        boolean useLibraryRules = job.getJobType() == LibraryBatchJobType.MIGRATE
                || job.getJobType() == LibraryBatchJobType.REBUILD;

        for (DocMetadata doc : docs) {

            try {

                rebuildFromStored(doc, useLibraryRules);

                batchJobService.recordSuccess(jobId);

            } catch (Exception e) {

                log.error("Rebuild failed for doc {} v{}: {}", doc.getDocId(), doc.getVersion(), e.getMessage(), e);

                batchJobService.recordFailure(jobId, doc.getDocId(), e.getMessage());

            }

        }

        batchJobService.finish(jobId);

        afterRebuildJob(jobId);

        log.info("Rebuild job {} finished ({} docs)", jobId, docs.size());

    }



    @Async

    public void runArchive(UUID jobId, UUID libraryId, String tenantId, String chunkProfileId) {

        List<DocMetadata> docs =

                docMetadataStore.findActiveByChunkProfile(libraryId, tenantId, chunkProfileId);

        log.info(

                "Starting archive job {} for profile {} ({} docs)",

                jobId,

                chunkProfileId,

                docs.size());

        runArchiveForDocs(jobId, docs);

    }



    @Async

    public void runArchiveForDocs(UUID jobId, List<DocMetadata> docs) {

        batchJobService.markRunning(jobId);

        for (DocMetadata doc : docs) {

            try {

                documentPipelineService.deleteDocument(doc.getDocId());

                batchJobService.recordSuccess(jobId);

            } catch (Exception e) {

                log.error("Archive failed for doc {}: {}", doc.getDocId(), e.getMessage(), e);

                batchJobService.recordFailure(jobId, doc.getDocId(), e.getMessage());

            }

        }

        batchJobService.finish(jobId);

        log.info("Archive job {} finished ({} docs)", jobId, docs.size());

    }



    private void afterRebuildJob(UUID jobId) {

        LibraryBatchJob job = batchJobService.requireJob(jobId);

        if (job.getJobType() != LibraryBatchJobType.MIGRATE
                && job.getJobType() != LibraryBatchJobType.REBUILD) {

            return;

        }

        var cleanup = chunkProfileCleanupService.cleanupOrphanNonPrimaryChunks(

                job.getLibraryId(), job.getTenantId());

        if (cleanup.removedChunkCount() > 0) {

            log.info(

                    "Post-migration cleanup for job {}: {} chunks across {} empty profiles",

                    jobId,

                    cleanup.removedChunkCount(),

                    cleanup.cleanedProfiles());

        }

    }



    private void rebuildFromStored(DocMetadata doc, boolean useLibraryRules) {

        String parsedKey = doc.getParsedTextKey();

        if (!StringUtils.hasText(parsedKey)) {

            throw new IllegalStateException("Missing parsed text key for doc " + doc.getDocId());

        }

        if (doc.getParseStatus() != ParseStatus.PARSED) {

            throw new IllegalStateException("Doc " + doc.getDocId() + " is not PARSED");

        }

        DocumentReadyForIndexEvent event = DocumentReadyForIndexEvent.create(

                doc.getLibraryId(),

                doc.getDocId(),

                doc.getTenantId(),

                doc.getVersion(),

                doc.getChecksumSha256() != null ? doc.getChecksumSha256() : "",

                doc.getMimeType() != null ? doc.getMimeType() : "text/plain",

                null,

                parsedKey.trim());
        if (useLibraryRules) {
            indexingService.indexMigratingToPrimary(event);
        } else {
            indexingService.index(event);
        }

    }

}

