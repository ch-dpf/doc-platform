package com.knowbase.ingestion.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.ingestion.testsupport.IngestionEvalFixtureFactory;
import com.knowbase.ingestion.testsupport.IngestionEvalHarness;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Temporary probe to capture baseline numbers. Run with {@code -Dingestion.eval.probe=true}.
 * Generate committed baseline with {@code -Dingestion.eval.generate=true}.
 */
class IngestionEvalBaselineProbeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void printCurrentMetrics() throws Exception {
        assumeTrue(Boolean.getBoolean("ingestion.eval.probe"));
        Map<String, Object> documents = collectDocuments();
        for (Map.Entry<String, Object> entry : documents.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
        if (Boolean.getBoolean("ingestion.eval.generate")) {
            writeBaseline(documents);
        }
    }

    private static Map<String, Object> collectDocuments() {
        Map<String, Object> documents = new LinkedHashMap<>();
        for (String fixtureId : IngestionEvalFixtureFactory.pdfFixtureIds()) {
            documents.put(fixtureId, IngestionEvalHarness.evaluatePdfFixture(fixtureId).toBaselineMap());
        }
        for (String fixtureId : IngestionEvalFixtureFactory.xlsxFixtureIds()) {
            documents.put(fixtureId, IngestionEvalHarness.evaluateXlsxFixture(fixtureId).toBaselineMap());
        }
        return documents;
    }

    private static void writeBaseline(Map<String, Object> documents) throws Exception {
        double averageCitation = documents.values().stream()
                .map(value -> (Map<?, ?>) value)
                .mapToDouble(map -> ((Number) map.get("minimumCitationScore")).doubleValue())
                .average()
                .orElse(0d);
        ObjectNode root = MAPPER.createObjectNode();
        root.put("version", "1");
        root.put("minimumAverageCitationScore", Math.round(averageCitation * 1000d) / 1000d);
        root.set("documents", MAPPER.valueToTree(documents));
        Path output = IngestionEvalBaselineLoader.defaultBaselinePath();
        Files.createDirectories(output.getParent());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), root);
        System.out.println("Wrote baseline to " + output.toAbsolutePath());
    }
}
