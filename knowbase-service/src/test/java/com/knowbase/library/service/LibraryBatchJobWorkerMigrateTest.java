package com.knowbase.library.service;

import com.knowbase.event.DocumentReadyForIndexEvent;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.service.DocumentPipelineService;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.domain.LibraryBatchJob;
import com.knowbase.library.domain.LibraryBatchJobStatus;
import com.knowbase.library.domain.LibraryBatchJobType;
import com.knowbase.library.dto.CleanupOrphanChunksResponse;
import com.knowbase.vector.service.IndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryBatchJobWorkerMigrateTest {

    private static final UUID JOB_ID = UUID.fromString("00000000-0000-4000-8000-000000000099");
    private static final UUID LIBRARY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID DOC_ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    @Mock
    private DocMetadataStore docMetadataStore;
    @Mock
    private IndexingService indexingService;
    @Mock
    private DocumentPipelineService documentPipelineService;
    @Mock
    private LibraryBatchJobService batchJobService;
    @Mock
    private ChunkProfileCleanupService chunkProfileCleanupService;

    private LibraryBatchJobWorker worker;

    @BeforeEach
    void setUp() {
        worker = new LibraryBatchJobWorker(
                docMetadataStore,
                indexingService,
                documentPipelineService,
                batchJobService,
                chunkProfileCleanupService);
    }

    @Test
    void migrateJobUsesPrimaryMigrationIndexPath() {
        LibraryBatchJob job = new LibraryBatchJob();
        job.setJobId(JOB_ID);
        job.setLibraryId(LIBRARY_ID);
        job.setTenantId("demo");
        job.setJobType(LibraryBatchJobType.MIGRATE);
        job.setStatus(LibraryBatchJobStatus.QUEUED);
        when(batchJobService.requireJob(JOB_ID)).thenReturn(job);
        when(chunkProfileCleanupService.cleanupOrphanNonPrimaryChunks(LIBRARY_ID, "demo"))
                .thenReturn(new CleanupOrphanChunksResponse(0, 0));

        DocMetadata doc = new DocMetadata();
        doc.setDocId(DOC_ID);
        doc.setLibraryId(LIBRARY_ID);
        doc.setTenantId("demo");
        doc.setVersion(1);
        doc.setParseStatus(ParseStatus.PARSED);
        doc.setParsedTextKey("demo/parsed.txt");
        doc.setMimeType("text/plain");

        worker.runRebuildForDocs(JOB_ID, List.of(doc));

        verify(indexingService).indexMigratingToPrimary(any());
        verify(indexingService, never()).index(any(DocumentReadyForIndexEvent.class));
        verify(chunkProfileCleanupService).cleanupOrphanNonPrimaryChunks(LIBRARY_ID, "demo");
    }

    @Test
    void rebuildJobUsesLibraryRulesIndexPath() {
        LibraryBatchJob job = new LibraryBatchJob();
        job.setJobId(JOB_ID);
        job.setLibraryId(LIBRARY_ID);
        job.setTenantId("demo");
        job.setJobType(LibraryBatchJobType.REBUILD);
        job.setStatus(LibraryBatchJobStatus.QUEUED);
        when(batchJobService.requireJob(JOB_ID)).thenReturn(job);

        DocMetadata doc = new DocMetadata();
        doc.setDocId(DOC_ID);
        doc.setLibraryId(LIBRARY_ID);
        doc.setTenantId("demo");
        doc.setVersion(1);
        doc.setParseStatus(ParseStatus.PARSED);
        doc.setParsedTextKey("demo/parsed.txt");
        doc.setMimeType("text/plain");

        when(chunkProfileCleanupService.cleanupOrphanNonPrimaryChunks(LIBRARY_ID, "demo"))
                .thenReturn(new CleanupOrphanChunksResponse(0, 0));

        worker.runRebuildForDocs(JOB_ID, List.of(doc));

        verify(indexingService).indexMigratingToPrimary(any());
        verify(indexingService, never()).index(any(DocumentReadyForIndexEvent.class));
        verify(chunkProfileCleanupService).cleanupOrphanNonPrimaryChunks(LIBRARY_ID, "demo");
    }
}
