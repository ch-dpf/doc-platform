package com.knowbase.ingestion.ocr;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.parse.OcrDownweightMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class OcrConfidencePolicy {

    public record Decision(boolean indexable, String reason) {
    }

    private OcrConfidencePolicy() {
    }

    public static Decision evaluate(StructuralBlock block, double threshold) {
        return evaluate(block, threshold, OcrDownweightMode.DOWNWEIGHT);
    }

    public static Decision evaluate(StructuralBlock block, double threshold, OcrDownweightMode mode) {
        if (block == null || block.metadata() == null) {
            return new Decision(true, "no_metadata");
        }
        Object confidence = block.metadata().get("ocrConfidence");
        if (!(confidence instanceof Number number)) {
            return new Decision(true, "confidence_unavailable");
        }
        double value = number.doubleValue();
        if (value < 0) {
            return new Decision(true, "confidence_unavailable");
        }
        if (value < threshold) {
            return switch (mode) {
                case FILTER -> new Decision(false, "low_ocr_confidence");
                case REVIEW, DOWNWEIGHT -> new Decision(true, "low_ocr_confidence_downweight");
            };
        }
        return new Decision(true, "ok");
    }

    public static double aggregateDocumentScore(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return 0.5d;
        }
        double sum = 0d;
        int count = 0;
        for (StructuralBlock block : blocks) {
            Object confidence = block.metadata() == null ? null : block.metadata().get("ocrConfidence");
            if (confidence instanceof Number number && number.doubleValue() >= 0) {
                sum += number.doubleValue();
                count++;
            }
        }
        return count == 0 ? 0.5d : sum / count;
    }

    public static List<StructuralBlock> apply(List<StructuralBlock> blocks, double threshold) {
        return apply(blocks, threshold, OcrDownweightMode.DOWNWEIGHT);
    }

    public static List<StructuralBlock> apply(List<StructuralBlock> blocks, double threshold, OcrDownweightMode mode) {
        if (blocks == null || blocks.isEmpty()) {
            return List.of();
        }
        return blocks.stream().map(block -> annotate(block, threshold, mode)).toList();
    }

    private static StructuralBlock annotate(StructuralBlock block, double threshold, OcrDownweightMode mode) {
        Decision decision = evaluate(block, threshold, mode);
        Map<String, Object> metadata = new HashMap<>(block.metadata());
        Object confidence = metadata.get("ocrConfidence");
        if (confidence instanceof Number number && number.doubleValue() >= 0 && number.doubleValue() < threshold) {
            metadata.put("lowConfidenceOcr", true);
            metadata.put("ocrDownweightFactor", Math.max(0.1d, number.doubleValue() / threshold));
            metadata.put("ocrDownweightMode", mode.wireValue());
            if (mode == OcrDownweightMode.REVIEW) {
                metadata.put("reviewRequired", true);
            }
        }
        if (!decision.indexable()) {
            metadata.put("indexableHint", false);
            metadata.put("ocrFilterReason", decision.reason());
        }
        return new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(metadata));
    }
}
