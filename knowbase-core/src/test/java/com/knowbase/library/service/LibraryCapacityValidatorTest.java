package com.knowbase.library.service;

import com.knowbase.ingest.service.InvalidDocumentException;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.CapacityLimitsSettings;
import com.knowbase.library.config.IngestAccessSettings;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryCapacityValidatorTest {

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    @Mock
    private DocMetadataStore metadataStore;

    @Mock
    private DocumentChunkMapper chunkMapper;

    private LibraryCapacityValidator validator;
    private UUID libraryId;

    @BeforeEach
    void setUp() {
        validator = new LibraryCapacityValidator(libraryConfigResolver, metadataStore, chunkMapper);
        libraryId = UUID.randomUUID();
    }

    @Test
    void rejectsWhenDocumentLimitReached() {
        stubLimits(2, 1_000_000L, 1000);
        when(metadataStore.countActiveByLibraryId(libraryId)).thenReturn(2);

        InvalidDocumentException ex =
                assertThrows(InvalidDocumentException.class, () -> validator.requireNewDocument(libraryId, 100, "a.pdf"));
        assertEquals(InvalidDocumentException.CODE_LIBRARY_DOCUMENT_LIMIT, ex.getErrorCode());
    }

    @Test
    void rejectsWhenTotalSizeExceeded() {
        stubLimits(0, 1000L, 0);
        when(metadataStore.sumSizeBytesByLibraryId(libraryId)).thenReturn(900L);

        InvalidDocumentException ex =
                assertThrows(InvalidDocumentException.class, () -> validator.requireNewDocument(libraryId, 200, "a.pdf"));
        assertEquals(InvalidDocumentException.CODE_LIBRARY_SIZE_LIMIT, ex.getErrorCode());
    }

    @Test
    void rejectsWhenChunkLimitExceeded() {
        stubLimits(0, 0L, 10);
        when(chunkMapper.countByLibraryId(libraryId)).thenReturn(9);

        LibraryCapacityExceededException ex = assertThrows(
                LibraryCapacityExceededException.class,
                () -> validator.requireChunkCapacity(libraryId, 1, 3));
        assertEquals(LibraryCapacityExceededException.CODE_CHUNK_LIMIT, ex.getErrorCode());
    }

    private void stubLimits(int maxDocuments, long maxTotalSizeBytes, int maxChunkEntries) {
        CapacityLimitsSettings limits = new CapacityLimitsSettings();
        limits.setMaxDocuments(maxDocuments);
        limits.setMaxTotalSizeBytes(maxTotalSizeBytes);
        limits.setMaxChunkEntries(maxChunkEntries);

        VectorLibraryConfig cfg = new VectorLibraryConfig();
        IngestAccessSettings access = new IngestAccessSettings();
        access.setCapacityLimits(limits);
        cfg.setIngestAccess(access);

        when(libraryConfigResolver.capacityLimitsFor(libraryId)).thenReturn(limits);
    }
}
