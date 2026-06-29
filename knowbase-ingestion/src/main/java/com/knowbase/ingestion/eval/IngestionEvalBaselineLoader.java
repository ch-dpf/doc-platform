package com.knowbase.ingestion.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IngestionEvalBaselineLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IngestionEvalBaselineLoader() {
    }

    public static Path defaultBaselinePath() {
        return repoRoot().resolve("sample-documents/ingestion-eval-baseline.json");
    }

    public static Path repoRoot() {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.isRegularFile(cwd.resolve("sample-documents/ingestion-eval-baseline.json"))) {
            return cwd;
        }
        if (Files.isRegularFile(cwd.resolve("sample-documents/retrieval-eval-samples.json"))) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null) {
            if (Files.isRegularFile(parent.resolve("sample-documents/ingestion-eval-baseline.json"))) {
                return parent;
            }
            if (Files.isRegularFile(parent.resolve("sample-documents/retrieval-eval-samples.json"))) {
                return parent;
            }
        }
        return cwd;
    }

    public static IngestionEvalBaseline.Baseline load(Path path) throws Exception {
        JsonNode root = MAPPER.readTree(Files.readString(path));
        Map<String, IngestionEvalBaseline.DocumentEntry> documents = new LinkedHashMap<>();
        JsonNode documentsNode = root.get("documents");
        if (documentsNode != null && documentsNode.isObject()) {
            documentsNode.fields().forEachRemaining(entry -> documents.put(
                    entry.getKey(),
                    readDocumentEntry(entry.getValue())
            ));
        }
        return new IngestionEvalBaseline.Baseline(
                root.path("version").asText("1"),
                root.path("minimumAverageCitationScore").asDouble(0d),
                Map.copyOf(documents)
        );
    }

    private static IngestionEvalBaseline.DocumentEntry readDocumentEntry(JsonNode node) {
        List<String> fingerprints = new ArrayList<>();
        JsonNode fingerprintNode = node.get("indexableFingerprints");
        if (fingerprintNode != null && fingerprintNode.isArray()) {
            fingerprintNode.forEach(item -> fingerprints.add(item.asText("")));
        }
        return new IngestionEvalBaseline.DocumentEntry(
                node.path("minimumCitationScore").asDouble(0d),
                node.path("blockCount").asInt(0),
                node.path("tableRowCount").asInt(0),
                node.path("indexableChunkCount").asInt(0),
                node.path("totalChunkCount").asInt(0),
                node.path("maxIndexableTokens").asInt(0),
                List.copyOf(fingerprints)
        );
    }
}
