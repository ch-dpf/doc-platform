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

class SegmentationConfigResolverTest {

    @Test
    void smartModeDefaultsToStructureFirstTokenBudgetWithCharacterFallback() {
        SegmentationConfig config = SegmentationConfigResolver.resolve(
                sampleLibraryProfile(),
                sampleDocumentProfile(),
                Map.of("segmentationMode", "smart")
        );
        assertEquals(SegmentationConfig.ChunkMode.FLAT, config.chunkMode());
        assertEquals(SegmentationConfig.SplitMode.RECURSIVE, config.splitMode());
        assertEquals(SegmentationConfig.SizeUnit.TOKEN, config.sizeUnit());
        assertEquals(512, config.chunkMaxTokens());
        assertEquals(64, config.chunkOverlapTokens());
        assertEquals(500, config.chunkMaxChars());
        assertEquals(50, config.chunkOverlapChars());
        assertTrue(config.prependHeadingContext());
        assertTrue(config.preserveStructureBoundary());
    }

    @Test
    void advancedModeUsesCustomSeparators() {
        SegmentationConfig config = SegmentationConfigResolver.resolve(
                sampleLibraryProfile(),
                sampleDocumentProfile(),
                Map.of(
                        "segmentationMode", "advanced",
                        "customSeparators", "###|\\n\\n|\\n"
                )
        );
        assertEquals(3, config.separators().size());
        assertEquals("###", config.separators().getFirst());
        assertEquals("\n\n", config.separators().get(1));
    }

    @Test
    void smartModeOverridesPresetChunkLimits() {
        DocumentProfile profileWithSmallChunks = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_docx",
                ContentFamily.RICH_TEXT,
                "docx-structure",
                "structure_token_window",
                null,
                Map.of(),
                Map.of("chunkMaxChars", 500, "chunkOverlapChars", 50),
                true
        );
        SegmentationConfig config = SegmentationConfigResolver.resolve(
                sampleLibraryProfile(),
                profileWithSmallChunks,
                Map.of("segmentationMode", "smart")
        );
        assertEquals(SegmentationConfig.SizeUnit.TOKEN, config.sizeUnit());
        assertEquals(512, config.chunkMaxTokens());
        assertEquals(64, config.chunkOverlapTokens());
    }

    private static LibraryProfile sampleLibraryProfile() {
        return new LibraryProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                "ollama",
                "bge-m3",
                1024,
                null,
                512,
                64,
                8,
                Map.of(),
                Instant.now()
        );
    }

    private static DocumentProfile sampleDocumentProfile() {
        return new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_markdown",
                ContentFamily.RICH_TEXT,
                "markdown-structure",
                "structure_token_window",
                null,
                Map.of(),
                Map.of("preserveStructureBoundary", true),
                true
        );
    }
}
