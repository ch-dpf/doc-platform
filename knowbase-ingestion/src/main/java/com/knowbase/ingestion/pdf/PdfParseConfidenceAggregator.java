package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PdfParseConfidenceAggregator {

    private PdfParseConfidenceAggregator() {
    }

    public record PdfParseConfidence(
            double score,
            List<String> reasons,
            int tableRegionCount,
            int blocksWithBbox,
            int footerBlocks
    ) {
        public PdfParseConfidence {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }

    public static PdfParseConfidence aggregate(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return new PdfParseConfidence(0.5d, List.of("no_structure_blocks"), 0, 0, 0);
        }
        int withBbox = 0;
        int footerBlocks = 0;
        int tableRows = 0;
        Set<Object> regionIds = new HashSet<>();
        for (StructuralBlock block : blocks) {
            Map<String, Object> metadata = block.metadata();
            if (metadata == null) {
                continue;
            }
            if (metadata.get("bbox") != null) {
                withBbox++;
            }
            if ("footer".equals(String.valueOf(metadata.getOrDefault("layoutRole", "")))) {
                footerBlocks++;
            }
            if ("table_row".equals(block.blockType()) || "table".equals(metadata.get("layoutRole"))) {
                tableRows++;
                Object regionId = metadata.get("tableRegionId");
                if (regionId != null) {
                    regionIds.add(regionId);
                }
            }
        }
        List<String> reasons = new ArrayList<>();
        double score = 0.72d;
        double bboxRatio = (double) withBbox / blocks.size();
        if (bboxRatio >= 0.9d) {
            score += 0.12d;
        } else if (bboxRatio >= 0.6d) {
            score += 0.04d;
        } else {
            score -= 0.12d;
            reasons.add("low_bbox_coverage");
        }
        if (tableRows > 0 && regionIds.isEmpty()) {
            score -= 0.08d;
            reasons.add("table_rows_without_region");
        }
        if (footerBlocks > blocks.size() / 3) {
            score -= 0.06d;
            reasons.add("high_footer_noise");
        }
        if (blocks.size() < 2) {
            score -= 0.05d;
            reasons.add("sparse_blocks");
        }
        score = Math.max(0.1d, Math.min(0.98d, score));
        return new PdfParseConfidence(score, reasons, regionIds.size(), withBbox, footerBlocks);
    }

    public static java.util.Map<String, Object> toDocumentMetadata(PdfParseConfidence confidence) {
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("parseConfidence", confidence.score());
        metadata.put("parseConfidenceSource", "pdf-layout");
        metadata.put("tableRegionCount", confidence.tableRegionCount());
        if (!confidence.reasons().isEmpty()) {
            metadata.put("lowConfidenceReasons", confidence.reasons());
        }
        metadata.put("pdfBlocksWithBbox", confidence.blocksWithBbox());
        metadata.put("pdfFooterBlocks", confidence.footerBlocks());
        return metadata;
    }
}
