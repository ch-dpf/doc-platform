package com.knowbase.ingestion.eval;

import java.util.List;
import java.util.Map;

/**
 * Committed offline eval baseline loaded from {@code sample-documents/ingestion-eval-baseline.json}.
 */
public final class IngestionEvalBaseline {

    public record DocumentEntry(
            double minimumCitationScore,
            int blockCount,
            int tableRowCount,
            int indexableChunkCount,
            int totalChunkCount,
            int maxIndexableTokens,
            List<String> indexableFingerprints
    ) {
    }

    public record Baseline(
            String version,
            double minimumAverageCitationScore,
            Map<String, DocumentEntry> documents
    ) {
    }

    public record ActualDocumentMetrics(
            String fixtureId,
            double citationScore,
            int blockCount,
            int tableRowCount,
            int indexableChunkCount,
            int totalChunkCount,
            int maxIndexableTokens,
            List<String> indexableFingerprints
    ) {
    }

    public record ComparisonResult(
            String fixtureId,
            boolean passed,
            List<String> violations
    ) {
    }

    private IngestionEvalBaseline() {
    }

    public static ComparisonResult compare(DocumentEntry expected, ActualDocumentMetrics actual) {
        List<String> violations = new java.util.ArrayList<>();
        if (actual.citationScore() + 0.0001d < expected.minimumCitationScore()) {
            violations.add("citationScore " + actual.citationScore() + " < minimum " + expected.minimumCitationScore());
        }
        if (actual.blockCount() != expected.blockCount()) {
            violations.add("blockCount expected " + expected.blockCount() + " got " + actual.blockCount());
        }
        if (actual.tableRowCount() != expected.tableRowCount()) {
            violations.add("tableRowCount expected " + expected.tableRowCount() + " got " + actual.tableRowCount());
        }
        if (actual.indexableChunkCount() != expected.indexableChunkCount()) {
            violations.add("indexableChunkCount expected " + expected.indexableChunkCount()
                    + " got " + actual.indexableChunkCount());
        }
        if (actual.totalChunkCount() != expected.totalChunkCount()) {
            violations.add("totalChunkCount expected " + expected.totalChunkCount()
                    + " got " + actual.totalChunkCount());
        }
        if (actual.maxIndexableTokens() != expected.maxIndexableTokens()) {
            violations.add("maxIndexableTokens expected " + expected.maxIndexableTokens()
                    + " got " + actual.maxIndexableTokens());
        }
        if (!expected.indexableFingerprints().equals(actual.indexableFingerprints())) {
            violations.add("indexableFingerprints expected " + expected.indexableFingerprints()
                    + " got " + actual.indexableFingerprints());
        }
        return new ComparisonResult(actual.fixtureId(), violations.isEmpty(), List.copyOf(violations));
    }
}
