package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser-agnostic parse confidence when a format-specific aggregator did not run.
 */
public final class UniversalParseConfidenceAggregator {

    private UniversalParseConfidenceAggregator() {
    }

    public record ParseConfidence(
            double score,
            List<String> reasons,
            int indexableBlockCount,
            int tableRegionCount
    ) {
        public ParseConfidence {
            reasons = reasons == null ? List.of() : List.copyOf(reasons);
        }
    }

    public static ParseConfidence aggregate(ParsedDocument document, List<StructuralBlock> blocks) {
        Map<String, Object> metadata = document == null ? Map.of() : document.metadata();
        Object existing = metadata == null ? null : metadata.get("parseConfidence");
        if (existing instanceof Number number) {
            return mergeExisting(number.doubleValue(), metadata, blocks);
        }
        if (blocks == null || blocks.isEmpty()) {
            return new ParseConfidence(0.5d, List.of("no_structure_blocks"), 0, 0);
        }
        int indexableBlocks = 0;
        int withBoundary = 0;
        int withBbox = 0;
        Set<Object> regionIds = new HashSet<>();
        for (StructuralBlock block : blocks) {
            if (StructuralBlockIndexabilityPolicy.resolveIndexableHint(block)) {
                indexableBlocks++;
            }
            Map<String, Object> blockMetadata = block.metadata();
            if (blockMetadata.containsKey("boundaryType") || blockMetadata.containsKey("layoutRole")) {
                withBoundary++;
            }
            if (blockMetadata.containsKey("bbox")) {
                withBbox++;
            }
            Object regionId = blockMetadata.get("tableRegionId");
            if (regionId != null) {
                regionIds.add(regionId);
            }
        }
        List<String> reasons = new ArrayList<>();
        double score = 0.68d;
        double indexableRatio = (double) indexableBlocks / blocks.size();
        if (indexableRatio >= 0.35d) {
            score += 0.1d;
        } else {
            score -= 0.08d;
            reasons.add("low_indexable_block_ratio");
        }
        if (withBoundary >= blocks.size() * 0.8d) {
            score += 0.08d;
        } else {
            reasons.add("sparse_structure_boundaries");
        }
        if (withBbox > 0) {
            score += Math.min(0.08d, withBbox / (double) blocks.size() * 0.08d);
        }
        if (blocks.size() < 2) {
            score -= 0.05d;
            reasons.add("sparse_blocks");
        }
        score = Math.max(0.1d, Math.min(0.95d, score));
        return new ParseConfidence(score, reasons, indexableBlocks, regionIds.size());
    }

    public static Map<String, Object> toDocumentMetadata(ParseConfidence confidence, String parserCode) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("parseConfidence", confidence.score());
        metadata.put("parseConfidenceSource", parserCode == null || parserCode.isBlank()
                ? "structure-universal"
                : parserCode);
        metadata.put("indexableBlockCount", confidence.indexableBlockCount());
        metadata.put("tableRegionCount", confidence.tableRegionCount());
        if (!confidence.reasons().isEmpty()) {
            metadata.put("lowConfidenceReasons", confidence.reasons());
        }
        return metadata;
    }

    private static ParseConfidence mergeExisting(double score, Map<String, Object> metadata, List<StructuralBlock> blocks) {
        int indexable = countIndexable(blocks);
        int regions = metadata.get("tableRegionCount") instanceof Number number
                ? number.intValue()
                : countRegions(blocks);
        List<String> reasons = metadata.get("lowConfidenceReasons") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return new ParseConfidence(score, reasons, indexable, regions);
    }

    private static int countIndexable(List<StructuralBlock> blocks) {
        int count = 0;
        for (StructuralBlock block : blocks) {
            if (StructuralBlockIndexabilityPolicy.resolveIndexableHint(block)) {
                count++;
            }
        }
        return count;
    }

    private static int countRegions(List<StructuralBlock> blocks) {
        Set<Object> regionIds = new HashSet<>();
        for (StructuralBlock block : blocks) {
            Object regionId = block.metadata().get("tableRegionId");
            if (regionId != null) {
                regionIds.add(regionId);
            }
        }
        return regionIds.size();
    }
}
