package com.knowbase.vector.service;



import com.knowbase.event.DocumentDeletedEvent;

import com.knowbase.event.DocumentReadyForIndexEvent;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.library.service.LibraryCapacityValidator;
import com.knowbase.library.service.LibraryConfigResolver;

import com.knowbase.library.service.VectorLibraryService;

import com.knowbase.platform.IndexStatusUpdater;

import com.knowbase.vector.chunk.ChunkPipelineResult;
import com.knowbase.vector.chunk.LibraryChunkPipeline;

import com.knowbase.vector.domain.DocumentIndexJob;

import com.knowbase.vector.domain.IndexJobStatus;

import com.knowbase.vector.mapper.DocumentChunkMapper;

import com.knowbase.vector.retrieval.ChunkMetadataBuilder;
import com.knowbase.vector.support.DocumentIndexJobStore;

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

    private final LibraryEmbeddingService libraryEmbeddingService;

    private final ParsedTextFetcher textFetcher;

    private final IndexStatusUpdater indexStatusUpdater;

    private final LibraryConfigResolver libraryConfigResolver;

    private final VectorLibraryService vectorLibraryService;

    private final LibraryChunkPipeline libraryChunkPipeline;

    private final LibraryCapacityValidator capacityValidator;

    private final DocMetadataStore docMetadataStore;



    public IndexingService(

            DocumentIndexJobStore jobStore,

            DocumentChunkMapper chunkMapper,

            LibraryEmbeddingService libraryEmbeddingService,

            ParsedTextFetcher textFetcher,

            IndexStatusUpdater indexStatusUpdater,

            LibraryConfigResolver libraryConfigResolver,

            VectorLibraryService vectorLibraryService,
            LibraryChunkPipeline libraryChunkPipeline,
            LibraryCapacityValidator capacityValidator,
            DocMetadataStore docMetadataStore) {

        this.jobStore = jobStore;

        this.chunkMapper = chunkMapper;

        this.libraryEmbeddingService = libraryEmbeddingService;

        this.textFetcher = textFetcher;

        this.indexStatusUpdater = indexStatusUpdater;

        this.libraryConfigResolver = libraryConfigResolver;

        this.vectorLibraryService = vectorLibraryService;

        this.libraryChunkPipeline = libraryChunkPipeline;

        this.capacityValidator = capacityValidator;

        this.docMetadataStore = docMetadataStore;

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

            ChunkPipelineResult pipeline = libraryChunkPipeline.chunkIndexedText(libraryId, text);
            List<String> chunks = pipeline.chunks();

            int replacingChunks = chunkMapper.countByDocIdAndVersion(docId, event.version());
            capacityValidator.requireChunkCapacity(libraryId, replacingChunks, chunks.size());

            chunkMapper.deleteByDocIdAndVersion(docId, event.version());



            if (chunks.isEmpty()) {

                completeJob(job, event, 0);
                vectorLibraryService.adjustChunkCount(libraryId, -replacingChunks);

                return;

            }



            List<float[]> embeddings = libraryEmbeddingService.embedBatch(libraryId, chunks);

            if (embeddings.size() != chunks.size()) {

                throw new IllegalStateException("Embedding count mismatch");

            }

            String chunkMetadataJson = resolveChunkMetadataJson(libraryId, docId);

            for (int i = 0; i < chunks.size(); i++) {

                chunkMapper.insertChunk(

                        UUID.randomUUID(),

                        libraryId,

                        docId,

                        event.tenantId(),

                        event.version(),

                        i,

                        chunks.get(i),

                        chunkMetadataJson,

                        embeddings.get(i));

            }

            completeJob(job, event, chunks.size());

            vectorLibraryService.adjustChunkCount(libraryId, chunks.size() - replacingChunks);

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

    private String resolveChunkMetadataJson(UUID libraryId, UUID docId) {
        RetrievalRulesSettings retrieval = libraryConfigResolver.retrievalFor(libraryId);
        if (!retrieval.isRetainChunkMetadata()) {
            return null;
        }
        DocMetadata doc = docMetadataStore.findById(docId).orElse(null);
        return ChunkMetadataBuilder.buildJson(doc);
    }

}


