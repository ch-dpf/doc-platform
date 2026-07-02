package com.knowbase.ingestion.eval;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scores citation-relevant metadata completeness on parsed documents (offline eval).
 */
public final class IngestionCitationCompletenessEvaluator {

    private static final Set<String> PDF_TABLE_FIELDS = Set.of(
            "pageNumber", "tableRegionId", "cellCoordinates", "readingOrder"
    );
    private static final Set<String> PDF_TEXT_FIELDS = Set.of("pageNumber", "bbox");
    private static final Set<String> OCR_FIELDS = Set.of("ocrConfidence", "bbox", "pageNumber");
    private static final Set<String> TABLE_SHEET_FIELDS = Set.of("sheetName", "headerPath", "rowRange");
    private static final Set<String> FORMULA_FIELDS = Set.of("formulaLatex", "formulaFormat");

    private IngestionCitationCompletenessEvaluator() {
    }

    public record BlockScore(String blockType, double score, List<String> missingFields) {
    }

    public record DocumentScore(
            String sourceUri,
            double overallScore,
            int blockCount,
            int tableRowCount,
            int formulaBlockCount,
            List<BlockScore> blockScores,
            List<String> documentLevelMissing
    ) {
    }

    public static DocumentScore evaluate(ParsedDocument document) {
        if (document == null || document.blocks().isEmpty()) {
            return new DocumentScore("", 0d, 0, 0, 0, List.of(), List.of("blocks"));
        }
        List<BlockScore> blockScores = new ArrayList<>();
        int tableRows = 0;
        int formulas = 0;
        double total = 0d;
        for (StructuralBlock block : document.blocks()) {
            BlockScore score = scoreBlock(block);
            blockScores.add(score);
            total += score.score();
            if ("table_row".equals(block.blockType())) {
                tableRows++;
            }
            if ("formula".equals(block.blockType())) {
                formulas++;
            }
        }
        List<String> documentMissing = new ArrayList<>();
        if (!document.metadata().containsKey("parseConfidence")) {
            documentMissing.add("parseConfidence");
        }
        double overall = total / blockScores.size();
        if (!documentMissing.isEmpty()) {
            overall = Math.max(0d, overall - 0.05d * documentMissing.size());
        }
        return new DocumentScore(
                document.sourceUri(),
                round(overall),
                document.blocks().size(),
                tableRows,
                formulas,
                List.copyOf(blockScores),
                List.copyOf(documentMissing)
        );
    }

    private static BlockScore scoreBlock(StructuralBlock block) {
        Map<String, Object> metadata = block.metadata();
        List<String> requiredFields = requiredFields(block, metadata);
        if (requiredFields.isEmpty()) {
            return new BlockScore(block.blockType(), 1d, List.of());
        }
        List<String> missing = new ArrayList<>();
        for (String field : requiredFields) {
            if (!hasField(metadata, field)) {
                missing.add(field);
            }
        }
        double score = (double) (requiredFields.size() - missing.size()) / requiredFields.size();
        return new BlockScore(block.blockType(), round(score), List.copyOf(missing));
    }

    private static List<String> requiredFields(StructuralBlock block, Map<String, Object> metadata) {
        if ("table_row".equals(block.blockType())) {
            if (metadata.containsKey("sheetName")) {
                return new ArrayList<>(TABLE_SHEET_FIELDS);
            }
            List<String> fields = new ArrayList<>(PDF_TABLE_FIELDS);
            fields.add("cellBbox");
            return fields;
        }
        if ("formula".equals(block.blockType())) {
            return new ArrayList<>(FORMULA_FIELDS);
        }
        if (metadata.containsKey("ocrConfidence") || metadata.containsKey("ocrApplied")) {
            return new ArrayList<>(OCR_FIELDS);
        }
        if (metadata.containsKey("pageNumber")) {
            return new ArrayList<>(PDF_TEXT_FIELDS);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static boolean hasField(Map<String, Object> metadata, String field) {
        if ("cellBbox".equals(field)) {
            return hasCellBbox(metadata);
        }
        Object value = metadata.get(field);
        if (value == null) {
            return false;
        }
        if (value instanceof String text) {
            return !text.isBlank();
        }
        if (value instanceof List<?> list) {
            return !list.isEmpty();
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean hasCellBbox(Map<String, Object> metadata) {
        Object raw = metadata.get("cellCoordinates");
        if (raw instanceof List<?> cells) {
            for (Object item : cells) {
                if (item instanceof Map<?, ?> cell && cell.get("bbox") instanceof List<?> bbox && bbox.size() >= 4) {
                    return true;
                }
            }
        }
        return metadata.get("bbox") instanceof List<?> rowBbox && rowBbox.size() >= 4;
    }

    private static double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }

    public static Map<String, Object> toReportMap(DocumentScore score) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("sourceUri", score.sourceUri());
        report.put("overallScore", score.overallScore());
        report.put("blockCount", score.blockCount());
        report.put("tableRowCount", score.tableRowCount());
        report.put("formulaBlockCount", score.formulaBlockCount());
        report.put("documentLevelMissing", score.documentLevelMissing());
        List<Map<String, Object>> blocks = new ArrayList<>();
        for (BlockScore blockScore : score.blockScores()) {
            if (blockScore.score() >= 1d && blockScore.missingFields().isEmpty()) {
                continue;
            }
            blocks.add(Map.of(
                    "blockType", blockScore.blockType(),
                    "score", blockScore.score(),
                    "missingFields", blockScore.missingFields()
            ));
        }
        report.put("lowScoringBlocks", blocks);
        return Map.copyOf(report);
    }
}
