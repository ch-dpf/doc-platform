package com.knowbase.ingestion.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowbase.ingestion.testsupport.IngestionEvalFixtureFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleDocumentCatalogCoverageTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void programmaticPdfAndXlsxFixturesAreRegistered() {
        assertTrue(IngestionEvalFixtureFactory.pdfFixtureIds().size() >= 3);
        assertTrue(IngestionEvalFixtureFactory.xlsxFixtureIds().size() >= 2);
    }

    @Test
    void testResourceCatalogHasMinimumSamplesPerCategory() throws Exception {
        Path resources = Path.of("src/test/resources/sample-documents");
        Map<String, Integer> counts = new HashMap<>();
        counts.put("markdown", countFiles(resources.resolve("markdown")));
        counts.put("plain", countFiles(resources.resolve("plain")));
        counts.put("table", countFiles(resources.resolve("table")));
        counts.put("html", countFiles(resources.resolve("html")));
        counts.put("ocr", countFiles(resources.resolve("ocr")));
        counts.put("config", countFiles(resources.resolve("config")));
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            assertTrue(entry.getValue() >= 3,
                    entry.getKey() + " expected >=3 samples, got " + entry.getValue());
        }
    }

    @Test
    void retrievalEvalManifestCoversCategories() throws Exception {
        Path manifest = repoRoot().resolve("sample-documents/retrieval-eval-samples.json");
        assertTrue(Files.exists(manifest), "missing " + manifest);
        JsonNode root = MAPPER.readTree(Files.readString(manifest));
        Map<String, Integer> enabledByCategory = new HashMap<>();
        for (JsonNode sample : root.get("samples")) {
            if (sample.hasNonNull("enabled") && !sample.get("enabled").asBoolean()) {
                continue;
            }
            enabledByCategory.merge(sample.get("category").asText(), 1, Integer::sum);
        }
        for (String category : new String[] {
                "markdown", "table", "html", "ocr", "plain", "config", "pdf-programmatic", "xlsx-programmatic"
        }) {
            int count = enabledByCategory.getOrDefault(category, 0);
            assertTrue(count >= 3, category + " enabled samples expected >=3, got " + count);
        }
    }

    private static int countFiles(Path dir) throws Exception {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (var stream = Files.list(dir)) {
            return (int) stream.filter(Files::isRegularFile).count();
        }
    }

    private static Path repoRoot() {
        return IngestionEvalBaselineLoader.repoRoot();
    }
}
