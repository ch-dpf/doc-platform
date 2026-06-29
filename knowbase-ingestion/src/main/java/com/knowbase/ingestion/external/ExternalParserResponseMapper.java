package com.knowbase.ingestion.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps Docling / Unstructured style JSON responses to {@link ParsedDocument}.
 */
public final class ExternalParserResponseMapper {

    public static final String SCHEMA_VERSION = "1.0";
    private static final List<String> SUPPORTED_SCHEMA_VERSIONS = List.of("1.0", "1.1");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ExternalParserResponseMapper() {
    }

    public static ParsedDocument map(
            String sourceUri,
            String title,
            String responseBody,
            String parserCode,
            Map<String, Object> requestMetadata
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            return emptyDocument(sourceUri, title, parserCode, requestMetadata);
        }
        try {
            JsonNode root = MAPPER.readTree(responseBody);
            if (root.isObject()) {
                validateServiceError(root);
                validateSchemaVersion(root);
                return mapJsonObject(sourceUri, title, root, parserCode, requestMetadata);
            }
        } catch (ExternalParserServiceException exception) {
            throw exception;
        } catch (Exception ignored) {
            // fall through to legacy text extraction
        }
        return legacyTextDocument(sourceUri, title, responseBody, parserCode, requestMetadata);
    }

    private static ParsedDocument mapJsonObject(
            String sourceUri,
            String title,
            JsonNode root,
            String parserCode,
            Map<String, Object> requestMetadata
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (requestMetadata != null) {
            metadata.putAll(requestMetadata);
        }
        metadata.put("parser", parserCode);
        metadata.put("parserCode", parserCode);
        metadata.put("externalParserSchemaVersion", root.path("schemaVersion").asText(SCHEMA_VERSION));
        metadata.put("structureAware", true);
        metadata.put("layoutParsing", true);

        JsonNode responseMetadata = root.get("metadata");
        if (responseMetadata != null && responseMetadata.isObject()) {
            responseMetadata.fields().forEachRemaining(entry ->
                    metadata.put(entry.getKey(), jsonValue(entry.getValue())));
        }

        List<StructuralBlock> blocks = new ArrayList<>();
        blocks.addAll(mapBlocks(root.get("blocks"), metadata));
        blocks.addAll(mapTables(root.get("tables")));
        if (blocks.isEmpty()) {
            String text = root.path("text").asText("");
            if (text.isBlank()) {
                text = collectPageText(root.get("pages"));
            }
            blocks = StructureParsingSupport.parseMarkdownPublic(text);
            metadata.put("externalParserMapping", "text-fallback");
            return buildDocument(sourceUri, title, text, metadata, blocks);
        }

        metadata.put("externalParserMapping", "blocks");
        metadata.put("blockCount", blocks.size());
        String text = root.path("text").asText("");
        if (text.isBlank()) {
            text = StructureParsingSupport.blocksToTextPublic(blocks);
        }
        attachImages(metadata, root.get("images"));
        attachPages(metadata, root.get("pages"));
        return buildDocument(sourceUri, title, text, metadata, blocks);
    }

    private static List<StructuralBlock> mapBlocks(JsonNode blocksNode, Map<String, Object> baseMetadata) {
        if (blocksNode == null || !blocksNode.isArray()) {
            return List.of();
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (JsonNode node : blocksNode) {
            String content = node.path("content").asText("").trim();
            if (content.isBlank()) {
                continue;
            }
            String type = normalizeBlockType(node.path("type").asText("paragraph"));
            int level = node.path("level").asInt(0);
            Map<String, Object> blockMetadata = new HashMap<>(baseMetadata == null ? Map.of() : baseMetadata);
            blockMetadata.put("boundaryType", type);
            blockMetadata.put("layoutParsing", true);
            blockMetadata.put("bboxSource", node.has("bbox") ? "engine" : "unavailable");
            copyIfPresent(node, blockMetadata, "pageNumber");
            copyIfPresent(node, blockMetadata, "readingOrder");
            copyIfPresent(node, blockMetadata, "tableRegionId");
            copyIfPresent(node, blockMetadata, "tableRegionLabel");
            if (node.has("confidence")) {
                blockMetadata.put("ocrConfidence", node.get("confidence").asDouble());
                blockMetadata.put("ocrConfidenceSource", "external");
            }
            JsonNode bbox = node.get("bbox");
            if (bbox != null && bbox.isArray() && bbox.size() >= 4) {
                blockMetadata.put("bbox", readBbox(bbox));
            }
            JsonNode extra = node.get("metadata");
            if (extra != null && extra.isObject()) {
                extra.fields().forEachRemaining(entry ->
                        blockMetadata.put(entry.getKey(), jsonValue(entry.getValue())));
            }
            if (!blockMetadata.containsKey("readingOrder")) {
                blockMetadata.put("readingOrder", ordinal);
            }
            blocks.add(new StructuralBlock(type, level, content, ordinal++, Map.copyOf(blockMetadata)));
        }
        return blocks;
    }

    private static List<StructuralBlock> mapTables(JsonNode tablesNode) {
        if (tablesNode == null || !tablesNode.isArray()) {
            return List.of();
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (JsonNode table : tablesNode) {
            int tableRegionId = table.path("tableRegionId").asInt(blocks.size());
            String tableRegionLabel = table.path("tableRegionLabel").asText("external-table-" + tableRegionId);
            JsonNode rows = table.get("rows");
            if (rows == null || !rows.isArray()) {
                continue;
            }
            List<Double> tableRegionBbox = table.has("bbox") ? readBbox(table.get("bbox")) : List.of();
            int pageNumber = table.path("pageNumber").asInt(1);
            int rowIndex = 0;
            for (JsonNode row : rows) {
                String content = rowContent(row);
                if (content.isBlank()) {
                    continue;
                }
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("boundaryType", "table_row");
                metadata.put("layoutRole", "table");
                metadata.put("tableFormat", "external");
                metadata.put("tableRegionId", tableRegionId);
                metadata.put("tableRegionLabel", tableRegionLabel);
                metadata.put("pageNumber", pageNumber);
                metadata.put("tableRegionRowIndex", rowIndex);
                metadata.put("rowRole", row.path("rowRole").asText("DATA"));
                metadata.put("layoutParsing", true);
                if (!tableRegionBbox.isEmpty()) {
                    metadata.put("tableRegionBbox", tableRegionBbox);
                }
                if (row.has("bbox")) {
                    metadata.put("bbox", readBbox(row.get("bbox")));
                    metadata.put("bboxSource", "engine");
                } else {
                    metadata.put("bboxSource", "unavailable");
                }
                if (row.has("headerPath") && row.get("headerPath").isArray()) {
                    List<String> headerPath = new ArrayList<>();
                    row.get("headerPath").forEach(item -> headerPath.add(item.asText()));
                    metadata.put("headerPath", headerPath);
                }
                metadata.put("cellCoordinates", buildExternalCellCoordinates(row, rowIndex));
                metadata.put("indexableHint", !"HEADER".equalsIgnoreCase(String.valueOf(metadata.get("rowRole"))));
                blocks.add(new StructuralBlock("table_row", 0, content, ordinal++, Map.copyOf(metadata)));
                rowIndex++;
            }
        }
        return blocks;
    }

    private static List<Map<String, Object>> buildExternalCellCoordinates(JsonNode row, int rowIndex) {
        JsonNode cells = row.get("cells");
        if (cells == null || !cells.isArray() || cells.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> coordinates = new ArrayList<>();
        for (int columnIndex = 0; columnIndex < cells.size(); columnIndex++) {
            JsonNode cell = cells.get(columnIndex);
            Map<String, Object> coordinate = new HashMap<>();
            coordinate.put("rowIndex", rowIndex);
            coordinate.put("columnIndex", columnIndex);
            coordinate.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            if (cell.isObject()) {
                coordinate.put("value", cell.path("text").asText(cell.path("value").asText("")));
                if (cell.has("rowSpan")) {
                    coordinate.put("rowSpan", cell.get("rowSpan").asInt());
                }
                if (cell.has("columnSpan")) {
                    coordinate.put("columnSpan", cell.get("columnSpan").asInt());
                }
                if (cell.has("merged")) {
                    coordinate.put("merged", cell.get("merged").asBoolean());
                }
                if (cell.has("bbox")) {
                    coordinate.put("bbox", readBbox(cell.get("bbox")));
                    coordinate.put("bboxSource", "engine");
                }
            } else {
                coordinate.put("value", cell.asText(""));
            }
            if (!coordinate.containsKey("merged")) {
                coordinate.put("merged", false);
            }
            coordinates.add(Map.copyOf(coordinate));
        }
        return coordinates;
    }

    private static void validateSchemaVersion(JsonNode root) {
        if (!root.has("schemaVersion")) {
            return;
        }
        String version = root.path("schemaVersion").asText(SCHEMA_VERSION);
        if (!SUPPORTED_SCHEMA_VERSIONS.contains(version)) {
            throw new ExternalParserServiceException(
                    ExternalParserFallbackReason.SCHEMA_UNSUPPORTED,
                    "Unsupported external parser schemaVersion: " + version
            );
        }
    }

    private static void validateServiceError(JsonNode root) {
        JsonNode error = root.get("error");
        if (error == null || error.isNull()) {
            return;
        }
        String code = error.path("code").asText(ExternalParserFallbackReason.SERVICE_ERROR);
        String message = error.path("message").asText("external parser service error");
        throw new ExternalParserServiceException(code, message);
    }

    private static String rowContent(JsonNode row) {
        if (row.has("content") && !row.path("content").asText("").isBlank()) {
            return row.path("content").asText("").trim();
        }
        JsonNode cells = row.get("cells");
        if (cells != null && cells.isArray()) {
            return java.util.stream.StreamSupport.stream(cells.spliterator(), false)
                    .map(ExternalParserResponseMapper::cellText)
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(" | "));
        }
        return "";
    }

    private static String cellText(JsonNode cell) {
        if (cell == null || cell.isNull()) {
            return "";
        }
        if (cell.isObject()) {
            return cell.path("text").asText(cell.path("value").asText("")).trim();
        }
        return cell.asText("").trim();
    }

    private static void attachImages(Map<String, Object> metadata, JsonNode imagesNode) {
        if (imagesNode == null || !imagesNode.isArray() || imagesNode.isEmpty()) {
            return;
        }
        List<Map<String, Object>> images = new ArrayList<>();
        for (JsonNode image : imagesNode) {
            Map<String, Object> item = new HashMap<>();
            if (image.has("pageNumber")) {
                item.put("pageNumber", image.get("pageNumber").asInt());
            }
            if (image.has("assetUri")) {
                item.put("assetUri", image.get("assetUri").asText());
            }
            if (image.has("bbox")) {
                item.put("bbox", readBbox(image.get("bbox")));
            }
            if (image.has("description")) {
                item.put("description", image.get("description").asText());
            }
            images.add(Map.copyOf(item));
        }
        metadata.put("externalParserImages", images);
    }

    private static void attachPages(Map<String, Object> metadata, JsonNode pagesNode) {
        if (pagesNode == null || !pagesNode.isArray() || pagesNode.isEmpty()) {
            return;
        }
        metadata.put("pageCount", pagesNode.size());
        List<Map<String, Object>> pages = new ArrayList<>();
        for (JsonNode page : pagesNode) {
            Map<String, Object> item = new HashMap<>();
            item.put("pageNumber", page.path("pageNumber").asInt(pages.size() + 1));
            if (page.has("width")) {
                item.put("width", page.get("width").asDouble());
            }
            if (page.has("height")) {
                item.put("height", page.get("height").asDouble());
            }
            if (page.has("assetUri")) {
                item.put("assetUri", page.get("assetUri").asText());
            }
            pages.add(Map.copyOf(item));
        }
        metadata.put("externalParserPages", pages);
    }

    private static String collectPageText(JsonNode pagesNode) {
        if (pagesNode == null || !pagesNode.isArray()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (JsonNode page : pagesNode) {
            String text = page.path("text").asText("");
            if (!text.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                builder.append(text.trim());
            }
        }
        return builder.toString();
    }

    private static ParsedDocument legacyTextDocument(
            String sourceUri,
            String title,
            String responseBody,
            String parserCode,
            Map<String, Object> requestMetadata
    ) {
        String text = ExternalDocumentParserLegacySupport.extractText(responseBody);
        Map<String, Object> metadata = new HashMap<>();
        if (requestMetadata != null) {
            metadata.putAll(requestMetadata);
        }
        metadata.put("parser", parserCode);
        metadata.put("parserCode", parserCode);
        metadata.put("externalParserMapping", "legacy-regex");
        return buildDocument(sourceUri, title, text, metadata, StructureParsingSupport.parseMarkdownPublic(text));
    }

    private static ParsedDocument emptyDocument(
            String sourceUri,
            String title,
            String parserCode,
            Map<String, Object> requestMetadata
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (requestMetadata != null) {
            metadata.putAll(requestMetadata);
        }
        metadata.put("parser", parserCode);
        metadata.put("parserCode", parserCode);
        return buildDocument(sourceUri, title, "", metadata, List.of());
    }

    private static ParsedDocument buildDocument(
            String sourceUri,
            String title,
            String text,
            Map<String, Object> metadata,
            List<StructuralBlock> blocks
    ) {
        return new ParsedDocument(
                sourceUri,
                title,
                text,
                ContentFamily.RICH_TEXT,
                Map.copyOf(metadata),
                blocks
        );
    }

    private static String normalizeBlockType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "paragraph";
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "title", "heading", "header" -> "heading";
            case "table", "table_row" -> "table_row";
            case "list_item", "list" -> "list_item";
            case "code" -> "code_block";
            default -> raw.trim().toLowerCase(Locale.ROOT);
        };
    }

    private static void copyIfPresent(JsonNode node, Map<String, Object> target, String field) {
        if (node.has(field)) {
            target.put(field, jsonValue(node.get(field)));
        }
    }

    private static Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isNumber()) {
            return node.numberValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isArray()) {
            List<Object> values = new ArrayList<>();
            node.forEach(child -> values.add(jsonValue(child)));
            return values;
        }
        return node.asText();
    }

    private static List<Double> readBbox(JsonNode bbox) {
        return List.of(
                bbox.get(0).asDouble(),
                bbox.get(1).asDouble(),
                bbox.get(2).asDouble(),
                bbox.get(3).asDouble()
        );
    }
}
