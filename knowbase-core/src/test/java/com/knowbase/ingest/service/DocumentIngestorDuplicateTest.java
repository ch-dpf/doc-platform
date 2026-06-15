package com.knowbase.ingest.service;

import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.domain.SourceType;
import com.knowbase.ingest.storage.ObjectStorageService;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.VersionPolicySettings;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.library.service.LibraryCapacityValidator;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.pipeline.config.ChunkProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentIngestorDuplicateTest {

    private static final UUID LIBRARY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final String TENANT = "demo";
    private static final byte[] BYTES = "same-content".getBytes(StandardCharsets.UTF_8);

    @Mock
    private DocMetadataStore repository;
    @Mock
    private ObjectStorageService storageService;
    @Mock
    private DocumentParseService parseService;
    @Mock
    private DocumentPipelineService pipelineService;
    @Mock
    private LibraryConfigResolver libraryConfigResolver;
    @Mock
    private LibraryCapacityValidator capacityValidator;
    @Mock
    private VectorLibraryService vectorLibraryService;
    @Mock
    private ChunkProfileService chunkProfileService;

    private DocumentIngestor ingestor;

    @BeforeEach
    void setUp() {
        IngestProperties ingestProperties = new IngestProperties();
        ingestProperties.setMaxFileSize(DataSize.ofMegabytes(50));
        ingestor = new DocumentIngestor(
                repository,
                storageService,
                parseService,
                pipelineService,
                libraryConfigResolver,
                capacityValidator,
                vectorLibraryService,
                ingestProperties,
                chunkProfileService);

        when(libraryConfigResolver.requireLibrary(LIBRARY_ID)).thenReturn(new VectorLibrary());
        when(parseService.detectMimeType(any(), anyString())).thenReturn("text/plain");
        when(libraryConfigResolver.allowedMimeTypes(LIBRARY_ID)).thenReturn(java.util.List.of("text/plain"));
        doNothing().when(chunkProfileService).validateNewProfileAllowed(any(), any(), any());
    }

    @Test
    void rejectsDuplicateUploadWithDifferentChunkOverride() {
        DocMetadata existing = existingDoc(null, "cp_primary");
        when(repository.findByLibraryTenantChecksum(eq(LIBRARY_ID), eq(TENANT), anyString()))
                .thenReturn(Optional.of(existing));

        InvalidDocumentException ex = assertThrows(
                InvalidDocumentException.class,
                () -> ingestor.ingestOne(
                        LIBRARY_ID,
                        TENANT,
                        BYTES,
                        "report.txt",
                        true,
                        null,
                        "{\"chunkSize\":1200}"));

        assertEquals(InvalidDocumentException.CODE_DUPLICATE_DIFFERENT_CHUNK_PROFILE, ex.getErrorCode());
        verify(pipelineService, never()).scheduleProcessAfterCommit(any(), anyInt(), any(), anyString());
    }

    @Test
    void allowsDuplicateUploadWithSameChunkOverride() {
        DocMetadata existing = existingDoc("{\"chunkSize\":1200}", "cp_alt");
        when(repository.findByLibraryTenantChecksum(eq(LIBRARY_ID), eq(TENANT), anyString()))
                .thenReturn(Optional.of(existing));
        VersionPolicySettings policy = new VersionPolicySettings();
        policy.setUpdateStrategy("overwrite");
        when(libraryConfigResolver.versionPolicyFor(LIBRARY_ID)).thenReturn(policy);
        when(libraryConfigResolver.requiresManualReview(LIBRARY_ID)).thenReturn(false);

        ingestor.ingestOne(LIBRARY_ID, TENANT, BYTES, "report.txt", true, null, "{\"chunkSize\":1200}");

        verify(pipelineService).scheduleProcessAfterCommit(eq(existing.getDocId()), eq(existing.getVersion()), any(), eq("report.txt"));
    }

    private static DocMetadata existingDoc(String ingestProfileJson, String chunkProfileId) {
        DocMetadata doc = new DocMetadata();
        doc.setDocId(UUID.randomUUID());
        doc.setLibraryId(LIBRARY_ID);
        doc.setTenantId(TENANT);
        doc.setFileName("report.txt");
        doc.setMimeType("text/plain");
        doc.setVersion(1);
        doc.setParseStatus(ParseStatus.PARSED);
        doc.setSourceType(SourceType.UPLOAD);
        doc.setSizeBytes(BYTES.length);
        doc.setIngestProfileJson(ingestProfileJson);
        doc.setChunkProfileId(chunkProfileId);
        return doc;
    }
}
