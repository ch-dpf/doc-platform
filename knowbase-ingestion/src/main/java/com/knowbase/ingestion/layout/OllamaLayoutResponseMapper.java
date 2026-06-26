package com.knowbase.ingestion.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Ollama vision JSON (layout + table regions) into {@link StructuralBlock}s and {@link LayoutTableRegion}s.
 */
public final class OllamaLayoutResponseMapper {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OllamaLayoutResponseMapper() {
    }

    public static MappedPage fromJson(
            String json,
            LayoutPageRequest request,
            String providerCode,
            String modelName
    ) {
        if (json == null || json.isBlank()) {
            return MappedPage.empty();
        }
        try {
            String trimmed = extractJsonObject(json);
            JsonNode root = MAPPER.readTree(trimmed);
            return mapRoot(root, request, providerCode, modelName);
        } catch (Exception exception) {
            return MappedPage.empty();
        }
    }

    private static MappedPage mapRoot(
            JsonNode root,
            LayoutPageRequest request,
            String providerCode,
            String modelName
    ) {
        Map<String, Object> pageMetadata = basePageMetadata(request, providerCode, modelName);
        List<StructuralBlock> blocks = new ArrayList<>();
        List<LayoutTableRegion> tableRegions = new ArrayList<>();

        JsonNode tables = root.path("tables");
        if (tables.isArray()) {
            for (JsonNode table : tables) {
                int regionId = table.path("tableRegionId").asInt(tableRegions.size());
                String tableType = table.path("tableType").asText("borderless").toLowerCase(Locale.ROOT);
                int nestedDepth = table.path("nestedDepth").asInt(0);
                List<Double> regionBbox = normalizeBbox(table.path("bbox"), request.pageHeight());
                tableRegions.add(new LayoutTableRegion(
                        regionId,
                        "ollama-table-" + regionId,
                        regionBbox,
                        "ollama-" + tableType
                ));
                Map<String, Object> tableMeta = new HashMap<>(pageMetadata);
                tableMeta.put("tableRegionId", regionId);
                tableMeta.put("tableRegionLabel", "ollama-table-" + regionId);
                tableMeta.put("tableDetectionSource", "ollama-" + tableType);
                tableMeta.put("tableDetection", tableType);
                tableMeta.put("nestedTableDepth", nestedDepth);
                if (regionBbox != null) {
                    tableMeta.put("tableRegionBbox", regionBbox);
                }
                if (nestedDepth > 0) {
                    tableMeta.put("nestedTable", true);
                }
                JsonNode rows = table.path("rows");
                if (rows.isArray()) {
                    int rowIndex = 0;
                    for (JsonNode row : rows) {
                        blocks.addAll(toTableRowBlocks(row, tableMeta, rowIndex++, request.pageHeight()));
                    }
                }
            }
        }

        JsonNode blockArray = root.path("blocks");
        if (blockArray.isArray()) {
            for (JsonNode item : blockArray) {
                String type = item.path("type").asText("paragraph").toLowerCase(Locale.ROOT);
                if ("table_row".equals(type) && item.has("tableRegionId")) {
                    continue;
                }
                String content = normalizeContent(item.path("content").asText(""));
                if (content.isBlank()) {
                    continue;
                }
                BlockMapping mapping = mapType(type);
                Map<String, Object> metadata = new HashMap<>(pageMetadata);
                metadata.put("readingOrder", item.path("readingOrder").asInt(blocks.size()));
                metadata.put("readingOrderSource", "ollama-layout");
                metadata.put("columnIndex", item.path("columnIndex").asInt(0));
                metadata.put("columnCount", Math.max(1, item.path("columnCount").asInt(1)));
                List<Double> bbox = normalizeBbox(item.path("bbox"), request.pageHeight());
                if (bbox != null) {
                    metadata.put("bbox", bbox);
                    metadata.put("bboxSource", "ollama-layout");
                } else {
                    metadata.put("bboxSource", "unavailable");
                }
                blocks.add(new StructuralBlock(
                        mapping.blockType(),
                        mapping.level(),
                        content,
                        blocks.size(),
                        Map.copyOf(metadata)
                ));
            }
        }

        if (blocks.isEmpty()) {
            return MappedPage.empty();
        }
        blocks.sort((left, right) -> Integer.compare(
                ((Number) left.metadata().getOrDefault("readingOrder", left.ordinal())).intValue(),
                ((Number) right.metadata().getOrDefault("readingOrder", right.ordinal())).intValue()
        ));
        return new MappedPage(
                StructureParsingSupport.enrichHeadingPathsPublic(List.copyOf(blocks)),
                List.copyOf(tableRegions)
        );
    }

    private static List<StructuralBlock> toTableRowBlocks(
            JsonNode row,
            Map<String, Object> tableMeta,
            int rowIndex,
            double pageHeight
    ) {
        JsonNode cells = row.path("cells");
        if (!cells.isArray() || cells.isEmpty()) {
            String content = normalizeContent(row.path("content").asText(""));
            if (content.isBlank()) {
                return List.of();
            }
            Map<String, Object> metadata = new HashMap<>(tableMeta);
            metadata.put("rowIndex", rowIndex);
            metadata.put("readingOrderSource", "ollama-layout");
            metadata.put("boundaryType", "table_row");
            metadata.put("layoutRole", "table");
            return List.of(new StructuralBlock(
                    "table_row",
                    0,
                    content,
                    rowIndex,
                    Map.copyOf(metadata)
            ));
        }
        List<String> cellTexts = new ArrayList<>();
        List<Map<String, Object>> cellCoordinates = new ArrayList<>();
        for (int index = 0; index < cells.size(); index++) {
            String cellText = normalizeContent(cells.get(index).asText(""));
            cellTexts.add(cellText);
            List<Double> cellBbox = null;
            JsonNode cellBboxes = row.path("cellBboxes");
            if (cellBboxes.isArray() && index < cellBboxes.size()) {
                cellBbox = normalizeBbox(cellBboxes.get(index), pageHeight);
            }
            Map<String, Object> cell = new HashMap<>();
            cell.put("columnIndex", index);
            cell.put("text", cellText);
            if (cellBbox != null) {
                cell.put("bbox", cellBbox);
            }
            cellCoordinates.add(Map.copyOf(cell));
        }
        String rowContent = String.join(" | ", cellTexts);
        Map<String, Object> metadata = new HashMap<>(tableMeta);
        metadata.put("rowIndex", rowIndex);
        metadata.put("readingOrderSource", "ollama-layout");
        metadata.put("boundaryType", "table_row");
        metadata.put("layoutRole", "table");
        metadata.put("cellCoordinates", List.copyOf(cellCoordinates));
        return List.of(new StructuralBlock(
                "table_row",
                0,
                rowContent,
                rowIndex,
                Map.copyOf(metadata)
        ));
    }

    private static Map<String, Object> basePageMetadata(
            LayoutPageRequest request,
            String providerCode,
            String modelName
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pageNumber", request.pageNumber());
        metadata.put("layoutProvider", providerCode);
        metadata.put("layoutModel", modelName);
        metadata.put("layoutAnalysisRoute", "ollama-vision-layout");
        metadata.put("layoutParsing", true);
        if (request.pageWidth() > 0) {
            metadata.put("pageWidth", request.pageWidth());
        }
        if (request.pageHeight() > 0) {
            metadata.put("pageHeight", request.pageHeight());
        }
        return metadata;
    }

    private static List<Double> normalizeBbox(JsonNode bboxNode, double pageHeight) {
        List<Double> raw = LayoutBboxSupport.readBbox(bboxNode);
        if (raw == null) {
            return null;
        }
        return LayoutBboxSupport.toPdfPoints(raw, pageHeight);
    }

    private static String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replace('\r', '\n').trim();
    }

    private static BlockMapping mapType(String type) {
        if (type.contains("heading") || type.contains("title")) {
            return new BlockMapping("heading", 2, "heading");
        }
        return new BlockMapping("paragraph", 0, "body");
    }

    private static String extractJsonObject(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw.trim();
    }

    private record BlockMapping(String blockType, int level, String boundaryType) {
    }

    public record MappedPage(List<StructuralBlock> blocks, List<LayoutTableRegion> tableRegions) {
        static MappedPage empty() {
            return new MappedPage(List.of(), List.of());
        }

        boolean isEmpty() {
            return blocks.isEmpty();
        }
    }
}
