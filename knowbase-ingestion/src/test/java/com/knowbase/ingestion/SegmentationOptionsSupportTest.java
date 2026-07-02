package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SegmentationOptionsSupportTest {

    @Test
    void smartModeIgnoresManualProfileCode() {
        assertNull(SegmentationOptionsSupport.resolveDocumentProfileCode(
                "default_markdown",
                Map.of("segmentationMode", "smart")
        ));
    }

    @Test
    void advancedModeAppliesChunkOverrides() {
        LibraryProfile profile = sampleLibraryProfile(512, 64);
        LibraryProfile overridden = SegmentationOptionsSupport.applyLibraryProfileOverrides(
                profile,
                Map.of(
                        "segmentationMode", "advanced",
                        "chunkMaxTokens", 768,
                        "chunkOverlapTokens", 96
                )
        );
        assertEquals(768, overridden.chunkMaxTokens());
        assertEquals(96, overridden.chunkOverlapTokens());
    }

    @Test
    void advancedModeAppliesChunkingStrategy() {
        DocumentProfile profile = sampleDocumentProfile("structure_token_window");
        DocumentProfile overridden = SegmentationOptionsSupport.applyDocumentProfileOverrides(
                profile,
                Map.of(
                        "segmentationMode", "advanced",
                        "chunkingStrategy", "paragraph_token_window",
                        "preserveStructureBoundary", false
                )
        );
        assertEquals("paragraph_token_window", overridden.chunkingStrategy());
        assertEquals(false, overridden.options().get("preserveStructureBoundary"));
    }

    private static LibraryProfile sampleLibraryProfile(int chunkMax, int overlap) {
        return new LibraryProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                chunkMax,
                overlap,
                8,
                Map.of(),
                Instant.now()
        );
    }

    private static DocumentProfile sampleDocumentProfile(String strategy) {
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_markdown",
                ContentFamily.RICH_TEXT,
                "text",
                strategy,
                null,
                Map.of(),
                Map.of("preserveStructureBoundary", true),
                true
        );
    }
}
