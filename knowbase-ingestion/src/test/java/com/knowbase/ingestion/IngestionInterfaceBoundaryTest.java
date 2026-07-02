package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.DocumentMetadataEnricher.MetadataContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionInterfaceBoundaryTest {

    @Test
    void textNormalizerCanBeUsedThroughInterface() {
        DocumentNormalizer normalizer = new DocumentTextNormalizer();

        String normalized = normalizer.normalizeText("A\u0001\n\n\nB\u3000C");

        assertEquals("A\n\nB C", normalized);
    }

    @Test
    void defaultMetadataEnricherAddsProfileAndStructureContext() {
        UUID libraryId = UUID.randomUUID();
        UUID documentProfileId = UUID.randomUUID();
        UUID libraryProfileId = UUID.randomUUID();
        ParsedDocument document = new ParsedDocument(
                "memory://guide.md",
                "Guide",
                "# Install\n\nPrepare package.",
                ContentFamily.RICH_TEXT,
                Map.of("parser", "markdown-structure"),
                List.of(
                        StructuralBlock.heading(1, "Install", 0),
                        StructuralBlock.paragraph("Prepare package.", 1)
                )
        );
        LibraryProfile libraryProfile = new LibraryProfile(
                libraryProfileId,
                libraryId,
                1,
                "ollama",
                "nomic-embed-text",
                768,
                null,
                512,
                64,
                5,
                Map.of(),
                Instant.now()
        );
        DocumentProfile documentProfile = new DocumentProfile(
                documentProfileId,
                libraryId,
                "markdown-default",
                ContentFamily.RICH_TEXT,
                "markdown-structure",
                "structure_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        ParsedDocument enriched = new DefaultDocumentMetadataEnricher().enrich(
                document,
                new MetadataContext(
                        document.sourceUri(),
                        libraryId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        libraryProfile,
                        documentProfile,
                        Map.of()
                )
        );

        assertEquals("default", enriched.metadata().get("metadataEnricher"));
        assertEquals("Install", enriched.metadata().get("firstHeading"));
        assertEquals(2, enriched.metadata().get("blockCount"));
        assertTrue((Boolean) enriched.metadata().get("structureAware"));
        assertEquals(documentProfileId, enriched.metadata().get("documentProfileId"));
        assertEquals(libraryProfileId, enriched.metadata().get("libraryProfileId"));
    }
}
