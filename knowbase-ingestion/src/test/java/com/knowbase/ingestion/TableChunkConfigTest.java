package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableChunkConfigTest {

    @Test
    void resolvesTokenWindowFromChunkingStrategyAndLibraryChunkMax() {
        LibraryProfile library = libraryProfile(512, 64);
        DocumentProfile profile = tableProfile("table_row_token_window", Map.of(
                "chunkEngine", "smart",
                "tableIndexMinFields", 4
        ));

        TableChunkConfig config = TableChunkConfig.resolve(library, profile, Map.of());

        assertEquals(TableChunkConfig.RowGroupingMode.TOKEN_WINDOW, config.rowGroupingMode());
        assertEquals(512, config.chunkMaxTokens());
        assertEquals(4, config.tableIndexMinFields());
        assertTrue(config.usesTokenWindowGrouping());
    }

    @Test
    void flattensNestedDocumentProfileOptionsFromRequest() {
        LibraryProfile library = libraryProfile(384, 48);
        DocumentProfile profile = tableProfile("table_row_token_window", Map.of("tableIndexMinFields", 4));
        Map<String, Object> request = Map.of(
                "documentProfileOptions",
                Map.of("tableIndexMinFields", 3, "prependSheetContext", false)
        );

        TableChunkConfig config = TableChunkConfig.resolve(library, profile, request);

        assertEquals(3, config.tableIndexMinFields());
        assertEquals(false, config.prependSheetContext());
    }

    private static DocumentProfile tableProfile(String strategy, Map<String, Object> options) {
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                strategy,
                null,
                Map.of(),
                options,
                true
        );
    }

    private static LibraryProfile libraryProfile(int chunkMaxTokens, int chunkOverlapTokens) {
        UUID libraryId = UUID.randomUUID();
        return new LibraryProfile(
                UUID.randomUUID(),
                libraryId,
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                chunkMaxTokens,
                chunkOverlapTokens,
                12,
                Map.of(),
                Instant.now()
        );
    }
}
