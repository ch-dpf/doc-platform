package com.knowbase.ingestion.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps PaddleOCR-VL {@code prunedResult.parsing_res_list} into {@link StructuralBlock}s
 * with engine bbox and reading order.
 */
public final class PaddleOcrVlPrunedResultMapper {

    private PaddleOcrVlPrunedResultMapper() {
    }

    public static List<StructuralBlock> fromPrunedResult(
            JsonNode prunedResult,
            LayoutPageRequest request,
            String providerCode,
            String modelName
    ) {
        if (prunedResult == null || !prunedResult.isObject()) {
            return List.of();
        }
        JsonNode list = prunedResult.path("parsing_res_list");
        if (!list.isArray() || list.isEmpty()) {
            return List.of();
        }
        Map<String, Object> pageMetadata = basePageMetadata(request, providerCode, modelName);
        List<StructuralBlock> blocks = new ArrayList<>();
        int tableRegionCounter = 0;
        for (JsonNode item : list) {
            String content = normalizeContent(item.path("block_content").asText(""));
            if (content.isBlank()) {
                continue;
            }
            String label = item.path("block_label").asText("text").toLowerCase(Locale.ROOT);
            int readingOrder = item.path("block_order").asInt(blocks.size());
            Map<String, Object> metadata = new HashMap<>(pageMetadata);
            metadata.put("readingOrder", readingOrder);
            metadata.put("readingOrderSource", "paddle-layout");
            metadata.put("layoutBlockLabel", label);
            List<Double> bbox = LayoutBboxSupport.toPdfPoints(
                    LayoutBboxSupport.readBbox(item.path("block_bbox")),
                    request.pageHeight()
            );
            if (bbox != null) {
                metadata.put("bbox", bbox);
                metadata.put("bboxSource", "paddle-layout");
            } else {
                metadata.put("bboxSource", "unavailable");
            }
            if (label.contains("table")) {
                int regionId = tableRegionCounter++;
                metadata.put("tableRegionId", regionId);
                metadata.put("tableRegionLabel", "paddleocr-vl-table-" + regionId);
                metadata.put("tableDetectionSource", "paddle-layout");
                if (bbox != null) {
                    metadata.put("tableRegionBbox", bbox);
                }
                blocks.addAll(toTableRows(content, metadata, readingOrder));
                continue;
            }
            BlockMapping mapping = mapLabel(label);
            metadata.put("boundaryType", mapping.boundaryType());
            metadata.put("layoutRole", mapping.layoutRole());
            blocks.add(new StructuralBlock(
                    mapping.blockType(),
                    mapping.level(),
                    content,
                    blocks.size(),
                    Map.copyOf(metadata)
            ));
        }
        if (blocks.isEmpty()) {
            return List.of();
        }
        return StructureParsingSupport.enrichHeadingPathsPublic(blocks);
    }

    public static List<StructuralBlock> mergeBboxesOntoMarkdownBlocks(
            List<StructuralBlock> markdownBlocks,
            JsonNode prunedResult,
            double pageHeight
    ) {
        if (markdownBlocks == null || markdownBlocks.isEmpty() || prunedResult == null) {
            return markdownBlocks;
        }
        JsonNode list = prunedResult.path("parsing_res_list");
        if (!list.isArray() || list.isEmpty()) {
            return markdownBlocks;
        }
        List<BboxEntry> entries = new ArrayList<>();
        for (JsonNode item : list) {
            String content = normalizeContent(item.path("block_content").asText(""));
            List<Double> bbox = LayoutBboxSupport.toPdfPoints(
                    LayoutBboxSupport.readBbox(item.path("block_bbox")),
                    pageHeight
            );
            if (bbox == null) {
                continue;
            }
            entries.add(new BboxEntry(content, bbox, item.path("block_order").asInt(entries.size())));
        }
        if (entries.isEmpty()) {
            return markdownBlocks;
        }
        List<StructuralBlock> merged = new ArrayList<>(markdownBlocks.size());
        for (int index = 0; index < markdownBlocks.size(); index++) {
            StructuralBlock block = markdownBlocks.get(index);
            BboxEntry match = findMatch(block.content(), entries, index);
            if (match == null) {
                merged.add(block);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.put("bbox", match.bbox());
            metadata.put("bboxSource", "paddle-layout");
            metadata.putIfAbsent("readingOrderSource", "paddle-layout");
            if (match.order() >= 0) {
                metadata.put("readingOrder", match.order());
            }
            merged.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return merged;
    }

    private static BboxEntry findMatch(String content, List<BboxEntry> entries, int blockIndex) {
        String normalized = normalizeContent(content);
        for (BboxEntry entry : entries) {
            if (entry.content().equals(normalized) || normalized.contains(entry.content()) || entry.content().contains(normalized)) {
                return entry;
            }
        }
        if (blockIndex < entries.size()) {
            return entries.get(blockIndex);
        }
        return null;
    }

    private static List<StructuralBlock> toTableRows(String content, Map<String, Object> baseMetadata, int readingOrder) {
        List<StructuralBlock> parsed = StructureParsingSupport.parseMarkdownPublic(content);
        List<StructuralBlock> rows = parsed.stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .toList();
        if (rows.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>(baseMetadata);
            metadata.put("boundaryType", "table_row");
            metadata.put("layoutRole", "table");
            metadata.put("rowRole", "DATA");
            metadata.put("readingOrder", readingOrder);
            return List.of(new StructuralBlock(
                    "table_row",
                    0,
                    content.replaceAll("\\s{2,}|\\t+", " | "),
                    0,
                    Map.copyOf(metadata)
            ));
        }
        List<StructuralBlock> result = new ArrayList<>(rows.size());
        for (int index = 0; index < rows.size(); index++) {
            StructuralBlock row = rows.get(index);
            Map<String, Object> metadata = new HashMap<>(baseMetadata);
            metadata.putAll(row.metadata());
            metadata.put("boundaryType", "table_row");
            metadata.put("layoutRole", "table");
            metadata.put("readingOrder", readingOrder + index);
            result.add(new StructuralBlock(
                    row.blockType(),
                    row.level(),
                    row.content(),
                    index,
                    Map.copyOf(metadata)
            ));
        }
        return result;
    }

    private static Map<String, Object> basePageMetadata(LayoutPageRequest request, String providerCode, String modelName) {
        Map<String, Object> metadata = new HashMap<>(request.effectiveOptions());
        metadata.put("pageNumber", request.pageNumber());
        metadata.put("pageWidth", request.pageWidth());
        metadata.put("pageHeight", request.pageHeight());
        metadata.put("vlApplied", true);
        metadata.put("layoutParsing", true);
        metadata.put("ocrApplied", true);
        metadata.put("ocrEngine", "vision-vl");
        metadata.put("ocrConfidence", 0.85d);
        metadata.put("ocrConfidenceSource", "paddle-layout");
        metadata.put("layoutProvider", providerCode);
        metadata.put("layoutAnalysisRoute", "paddleocr-vl-pruned");
        if (modelName != null && !modelName.isBlank()) {
            metadata.put("layoutModel", modelName);
        }
        return metadata;
    }

    private static BlockMapping mapLabel(String label) {
        if (label.contains("title") || label.contains("heading")) {
            return new BlockMapping("heading", 1, "heading", "title");
        }
        if (label.contains("list")) {
            return new BlockMapping("list_item", 0, "list_item", "list");
        }
        if (label.contains("caption") || label.contains("figure")) {
            return new BlockMapping("paragraph", 0, "caption", "figure");
        }
        return new BlockMapping("paragraph", 0, "paragraph", "body");
    }

    private static String normalizeContent(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", " ");
    }

    private record BlockMapping(String blockType, int level, String boundaryType, String layoutRole) {
    }

    private record BboxEntry(String content, List<Double> bbox, int order) {
    }
}
