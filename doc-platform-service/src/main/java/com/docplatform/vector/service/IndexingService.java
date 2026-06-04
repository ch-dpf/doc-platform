package com.docplatform.vector.service;



import com.docplatform.event.DocumentDeletedEvent;

import com.docplatform.event.DocumentReadyForIndexEvent;

import com.docplatform.library.service.LibraryConfigResolver;

import com.docplatform.library.service.VectorLibraryService;

import com.docplatform.platform.IndexStatusUpdater;

import com.docplatform.vector.config.ChunkingProperties;

import com.docplatform.vector.domain.DocumentIndexJob;

import com.docplatform.vector.domain.IndexJobStatus;

import com.docplatform.vector.mapper.DocumentChunkMapper;

import com.docplatform.vector.support.DocumentIndexJobStore;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.time.Instant;

import java.util.List;

import java.util.UUID;



@Service

public class IndexingService {



    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);



    private final DocumentIndexJobStore jobStore;

    private final DocumentChunkMapper chunkMapper;

    private final ChunkingService chunkingService;

    private final LibraryEmbeddingService libraryEmbeddingService;

    private final ParsedTextFetcher textFetcher;

    private final IndexStatusUpdater indexStatusUpdater;

    private final LibraryConfigResolver libraryConfigResolver;

    private final VectorLibraryService vectorLibraryService;



    public IndexingService(

            DocumentIndexJobStore jobStore,

            DocumentChunkMapper chunkMapper,

            ChunkingService chunkingService,

            LibraryEmbeddingService libraryEmbeddingService,

            ParsedTextFetcher textFetcher,

            IndexStatusUpdater indexStatusUpdater,

            LibraryConfigResolver libraryConfigResolver,

            VectorLibraryService vectorLibraryService) {

        this.jobStore = jobStore;

        this.chunkMapper = chunkMapper;

        this.chunkingService = chunkingService;

        this.libraryEmbeddingService = libraryEmbeddingService;

        this.textFetcher = textFetcher;

        this.indexStatusUpdater = indexStatusUpdater;

        this.libraryConfigResolver = libraryConfigResolver;

        this.vectorLibraryService = vectorLibraryService;

    }



    @Transactional

    public void index(DocumentReadyForIndexEvent event) {

        UUID libraryId = event.libraryId();

        UUID docId = event.docId();

        DocumentIndexJob job = jobStore.findByDocIdAndVersion(docId, event.version())

                .orElseGet(() -> createJob(event));

        job.setStatus(IndexJobStatus.EMBEDDING);

        jobStore.save(job);



        try {

            String text = textFetcher.fetch(event.parsedTextKey(), event.parsedTextUrl());

            ChunkingProperties chunking = libraryConfigResolver.chunkingFor(libraryId);

            List<String> chunks = chunkingService.chunk(text, chunking);

            chunkMapper.deleteByDocIdAndVersion(docId, event.version());



            if (chunks.isEmpty()) {

                completeJob(job, event, 0);

                return;

            }



            List<float[]> embeddings = libraryEmbeddingService.embedBatch(libraryId, chunks);

            if (embeddings.size() != chunks.size()) {

                throw new IllegalStateException("Embedding count mismatch");

            }

            for (int i = 0; i < chunks.size(); i++) {

                chunkMapper.insertChunk(

                        UUID.randomUUID(),

                        libraryId,

                        docId,

                        event.tenantId(),

                        event.version(),

                        i,

                        chunks.get(i),

                        null,

                        embeddings.get(i));

            }

            completeJob(job, event, chunks.size());

            vectorLibraryService.incrementChunkCount(libraryId, chunks.size());

            log.info("Indexed doc {} v{} library {} with {} chunks", docId, event.version(), libraryId, chunks.size());

        } catch (Exception e) {

            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();

            log.error("Indexing failed for doc {} v{}: {}", docId, event.version(), message, e);

            job.setStatus(IndexJobStatus.FAILED);

            job.setErrorMessage(message.length() > 2000 ? message.substring(0, 2000) : message);

            job.setRetryCount(job.getRetryCount() + 1);

            jobStore.save(job);

            indexStatusUpdater.markFailed(docId, event.version());

            throw e;

        }

    }



    @Transactional

    public void delete(DocumentDeletedEvent event) {

        chunkMapper.deleteByDocId(event.docId());

        jobStore.findByDocIdAndVersion(event.docId(), event.version())

                .ifPresent(job -> {

                    job.setStatus(IndexJobStatus.COMPLETED);

                    job.setCompletedAt(Instant.now());

                    jobStore.save(job);

                });

        log.info("Removed vectors for doc {}", event.docId());

    }



    @Transactional

    public void rebuild(UUID libraryId, UUID docId, String tenantId, int version, String parsedTextUrl) {

        index(DocumentReadyForIndexEvent.create(

                libraryId, docId, tenantId, version, "", "text/plain", parsedTextUrl));

    }



    private void completeJob(DocumentIndexJob job, DocumentReadyForIndexEvent event, int chunkCount) {

        job.setStatus(IndexJobStatus.COMPLETED);

        job.setCompletedAt(Instant.now());

        job.setErrorMessage(null);

        jobStore.save(job);

        indexStatusUpdater.markIndexed(event.docId(), event.version());

    }



    private DocumentIndexJob createJob(DocumentReadyForIndexEvent event) {

        DocumentIndexJob job = new DocumentIndexJob();

        job.setJobId(UUID.randomUUID());

        job.setLibraryId(event.libraryId());

        job.setDocId(event.docId());

        job.setTenantId(event.tenantId());

        job.setVersion(event.version());

        job.setStatus(IndexJobStatus.QUEUED);

        job.setRetryCount(0);

        job.setCreatedAt(Instant.now());

        return jobStore.save(job);

    }

}


