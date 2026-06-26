package com.knowbase.ingestion.eval;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an offline ingestion eval report from one or more parsed documents.
 */
public final class IngestionEvalReportGenerator {

    private IngestionEvalReportGenerator() {
    }

    public record Report(
            String version,
            double averageCitationScore,
            int documentCount,
            List<Map<String, Object>> documents,
            Map<String, Integer> categoryCoverage
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("version", version);
            map.put("averageCitationScore", averageCitationScore);
            map.put("documentCount", documentCount);
            map.put("documents", documents);
            map.put("categoryCoverage", categoryCoverage);
            return Map.copyOf(map);
        }
    }

    public static Report generate(List<ParsedDocument> documents) {
        return generate(documents, Map.of());
    }

    public static Report generate(List<ParsedDocument> documents, Map<String, Integer> categoryCoverage) {
        List<Map<String, Object>> entries = new ArrayList<>();
        double total = 0d;
        for (ParsedDocument document : documents) {
            ParsedDocument enriched = ParsedDocumentParseEnricher.enrich(document);
            IngestionCitationCompletenessEvaluator.DocumentScore score =
                    IngestionCitationCompletenessEvaluator.evaluate(enriched);
            total += score.overallScore();
            entries.add(IngestionCitationCompletenessEvaluator.toReportMap(score));
        }
        double average = documents.isEmpty() ? 0d : total / documents.size();
        return new Report(
                "1",
                Math.round(average * 1000d) / 1000d,
                documents.size(),
                List.copyOf(entries),
                categoryCoverage == null ? Map.of() : Map.copyOf(categoryCoverage)
        );
    }
}
