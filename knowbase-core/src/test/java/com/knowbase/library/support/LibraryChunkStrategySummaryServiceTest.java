package com.knowbase.library.support;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.dto.ChunkStrategySummaryRow;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.vector.chunk.ChunkingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryChunkStrategySummaryServiceTest {

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    @InjectMocks
    private LibraryChunkStrategySummaryService service;

    @Test
    void summarizeAutoShowsPerFileTypeDefaults() {
        UUID libraryId = UUID.randomUUID();
        VectorLibraryConfig cfg = new VectorLibraryConfig();
        cfg.setChunkingStrategy(ChunkingStrategy.AUTO);
        cfg.setAllowedMimeTypes(List.of(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain"));

        when(libraryConfigResolver.config(libraryId)).thenReturn(cfg);

        List<ChunkStrategySummaryRow> rows = service.summarize(libraryId);

        assertEquals(3, rows.size());
        assertEquals("paragraph-first", findRow(rows, "pdf").chunkingStrategy());
        assertEquals("heading-level", findRow(rows, "word").chunkingStrategy());
        assertEquals("paragraph-first", findRow(rows, "txt").chunkingStrategy());
    }

    @Test
    void summarizeExplicitStrategyIsUniform() {
        UUID libraryId = UUID.randomUUID();
        VectorLibraryConfig cfg = new VectorLibraryConfig();
        cfg.setChunkingStrategy(ChunkingStrategy.SEMANTIC);
        cfg.setAllowedMimeTypes(List.of("application/pdf", "text/plain"));

        when(libraryConfigResolver.config(libraryId)).thenReturn(cfg);

        List<ChunkStrategySummaryRow> rows = service.summarize(libraryId);

        assertTrue(rows.stream().allMatch(row -> "semantic".equals(row.chunkingStrategy())));
    }

    private static ChunkStrategySummaryRow findRow(List<ChunkStrategySummaryRow> rows, String fileType) {
        return rows.stream().filter(r -> fileType.equals(r.fileType())).findFirst().orElseThrow();
    }
}
