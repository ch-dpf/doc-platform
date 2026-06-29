package com.knowbase.ingestion.eval;

import com.knowbase.ingestion.testsupport.IngestionEvalFixtureFactory;
import com.knowbase.ingestion.testsupport.IngestionEvalHarness;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionEvalBaselineTest {

    @Test
    void programmaticFixturesMatchCommittedBaseline() throws Exception {
        Path baselinePath = IngestionEvalBaselineLoader.defaultBaselinePath();
        assertTrue(Files.isRegularFile(baselinePath), "missing baseline: " + baselinePath);
        IngestionEvalBaseline.Baseline baseline = IngestionEvalBaselineLoader.load(baselinePath);

        List<String> failures = new ArrayList<>();
        double citationTotal = 0d;
        int compared = 0;
        for (String fixtureId : IngestionEvalFixtureFactory.pdfFixtureIds()) {
            IngestionEvalHarness.DocumentMetrics metrics = IngestionEvalHarness.evaluatePdfFixture(fixtureId);
            compared += compareFixture(baseline, failures, metrics);
            citationTotal += metrics.citationScore();
        }
        for (String fixtureId : IngestionEvalFixtureFactory.xlsxFixtureIds()) {
            IngestionEvalHarness.DocumentMetrics metrics = IngestionEvalHarness.evaluateXlsxFixture(fixtureId);
            compared += compareFixture(baseline, failures, metrics);
            citationTotal += metrics.citationScore();
        }

        double averageCitation = compared == 0 ? 0d : citationTotal / compared;
        if (averageCitation + 0.0001d < baseline.minimumAverageCitationScore()) {
            failures.add("averageCitationScore " + averageCitation
                    + " < minimum " + baseline.minimumAverageCitationScore());
        }
        assertTrue(failures.isEmpty(), String.join(System.lineSeparator(), failures));
    }

    private static int compareFixture(
            IngestionEvalBaseline.Baseline baseline,
            List<String> failures,
            IngestionEvalHarness.DocumentMetrics metrics
    ) {
        IngestionEvalBaseline.DocumentEntry expected = baseline.documents().get(metrics.fixtureId());
        if (expected == null) {
            failures.add(metrics.fixtureId() + ": missing baseline entry");
            return 0;
        }
        IngestionEvalBaseline.ComparisonResult result = IngestionEvalBaseline.compare(
                expected,
                new IngestionEvalBaseline.ActualDocumentMetrics(
                        metrics.fixtureId(),
                        metrics.citationScore(),
                        metrics.blockCount(),
                        metrics.tableRowCount(),
                        metrics.chunkSignature().indexableChunks(),
                        metrics.chunkSignature().totalChunks(),
                        metrics.chunkSignature().maxIndexableTokens(),
                        metrics.chunkSignature().indexableFingerprints()
                )
        );
        if (!result.passed()) {
            failures.add(metrics.fixtureId() + ": " + String.join("; ", result.violations()));
        }
        return 1;
    }
}
