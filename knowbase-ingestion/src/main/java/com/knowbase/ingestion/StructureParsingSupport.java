package com.knowbase.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StructureParsingSupport {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern MARKDOWN_LIST = Pattern.compile("^(\\s*)[-*+]\\s+(.+)$");
    private static final Pattern MARKDOWN_ORDERED_LIST = Pattern.compile("^(\\s*)\\d+\\.\\s+(.+)$");
    private static final Pattern PDF_HEADING = Pattern.compile(
            "^(?:第\\s*[\\d一二三四五六七八九十百]+\\s*[章节篇部].+|Chapter\\s+\\d+.+|\\d+(?:\\.\\d+)*\\s+.+|[一二三四五六七八九十]+、.+)$"
    );
    private static final Pattern PDF_TABLE_ROW = Pattern.compile("\\S+(?:\\s{2,}|\\t+)\\S+");

    private StructureParsingSupport() {
    }

    static List<StructuralBlock> parseMarkdown(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        boolean inCodeFence = false;
        int ordinal = 0;
        int tableRegionId = 0;
        int lineIndex = 0;

        while (lineIndex < lines.length) {
            String rawLine = lines[lineIndex];
            String line = rawLine == null ? "" : rawLine;
            if (line.trim().startsWith("```")) {
                if (inCodeFence) {
                    blocks.add(StructuralBlock.codeBlock(codeBlock.toString().trim(), ordinal++));
                    codeBlock.setLength(0);
                    inCodeFence = false;
                } else {
                    if (paragraph.length() > 0) {
                        blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                        paragraph.setLength(0);
                    }
                    inCodeFence = true;
                }
                lineIndex++;
                continue;
            }
            if (inCodeFence) {
                if (codeBlock.length() > 0) {
                    codeBlock.append('\n');
                }
                codeBlock.append(line);
                lineIndex++;
                continue;
            }

            if (isMarkdownPipeRow(line.trim())) {
                MarkdownTableSlice tableSlice = tryParseMarkdownPipeTable(lines, lineIndex, tableRegionId, ordinal);
                if (!tableSlice.blocks().isEmpty()) {
                    if (paragraph.length() > 0) {
                        blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                        paragraph.setLength(0);
                    }
                    blocks.addAll(tableSlice.blocks());
                    ordinal = tableSlice.nextOrdinal();
                    tableRegionId++;
                    lineIndex = tableSlice.nextLineIndex();
                    continue;
                }
            }

            Matcher headingMatcher = MARKDOWN_HEADING.matcher(line.trim());
            if (headingMatcher.matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.heading(
                        headingMatcher.group(1).length(),
                        headingMatcher.group(2).trim(),
                        ordinal++
                ));
                lineIndex++;
                continue;
            }

            Matcher listMatcher = MARKDOWN_LIST.matcher(line);
            if (listMatcher.matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                int level = Math.max(1, listMatcher.group(1).length() / 2 + 1);
                blocks.add(StructuralBlock.listItem(listMatcher.group(2).trim(), ordinal++, level));
                lineIndex++;
                continue;
            }

            Matcher orderedMatcher = MARKDOWN_ORDERED_LIST.matcher(line);
            if (orderedMatcher.matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                int level = Math.max(1, orderedMatcher.group(1).length() / 2 + 1);
                blocks.add(StructuralBlock.listItem(orderedMatcher.group(2).trim(), ordinal++, level));
                lineIndex++;
                continue;
            }

            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                lineIndex++;
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(line.trim());
            lineIndex++;
        }

        if (inCodeFence && codeBlock.length() > 0) {
            blocks.add(StructuralBlock.codeBlock(codeBlock.toString().trim(), ordinal++));
        } else if (paragraph.length() > 0) {
            blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
        }
        return enrichHeadingPaths(blocks);
    }

    private record MarkdownTableSlice(List<StructuralBlock> blocks, int nextLineIndex, int nextOrdinal) {
    }

    private static boolean isMarkdownPipeRow(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String trimmed = line.trim();
        return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length() > 2;
    }

    private static boolean isMarkdownPipeSeparator(String line) {
        if (!isMarkdownPipeRow(line)) {
            return false;
        }
        return line.trim().replace("|", "").replace(":", "").replace("-", "").trim().isEmpty();
    }

    private static String[] splitPipeCells(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        String[] parts = trimmed.split("\\|", -1);
        for (int index = 0; index < parts.length; index++) {
            parts[index] = parts[index].trim();
        }
        return parts;
    }

    private static String joinPipeCells(String[] cells) {
        return String.join(" | ", cells);
    }

    private static MarkdownTableSlice tryParseMarkdownPipeTable(
            String[] lines,
            int start,
            int tableRegionId,
            int ordinal
    ) {
        if (start >= lines.length || !isMarkdownPipeRow(lines[start].trim())) {
            return new MarkdownTableSlice(List.of(), start + 1, ordinal);
        }
        boolean hasSeparator = start + 1 < lines.length && isMarkdownPipeSeparator(lines[start + 1].trim());
        List<String[]> rows = new ArrayList<>();
        int index = start;
        if (hasSeparator) {
            rows.add(splitPipeCells(lines[index]));
            index += 2;
        }
        while (index < lines.length && isMarkdownPipeRow(lines[index].trim()) && !isMarkdownPipeSeparator(lines[index].trim())) {
            rows.add(splitPipeCells(lines[index]));
            index++;
        }
        if (rows.isEmpty()) {
            return new MarkdownTableSlice(List.of(), start + 1, ordinal);
        }
        if (!hasSeparator && rows.size() < 2) {
            return new MarkdownTableSlice(List.of(), start + 1, ordinal);
        }
        String tableRegionLabel = "md-table-" + tableRegionId;
        List<StructuralBlock> blocks = new ArrayList<>(rows.size());
        int rowOrdinal = ordinal;
        String[] previousDataRow = null;
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            boolean headerRow = hasSeparator && rowIndex == 0;
            String[] cells = rows.get(rowIndex);
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("boundaryType", "table_row");
            metadata.put("layoutRole", "table");
            metadata.put("tableRegionId", tableRegionId);
            metadata.put("tableRegionLabel", tableRegionLabel);
            metadata.put("tableFormat", "markdown");
            metadata.put("rowRole", headerRow ? "HEADER" : "DATA");
            metadata.put("rowIndex", rowIndex);
            metadata.put("indexableHint", !headerRow);
            metadata.put("cellCoordinates", buildMarkdownCellCoordinates(
                    rowIndex,
                    cells,
                    headerRow,
                    previousDataRow
            ));
            if (!headerRow) {
                previousDataRow = cells;
            }
            blocks.add(new StructuralBlock(
                    "table_row",
                    0,
                    joinPipeCells(cells),
                    rowOrdinal++,
                    Map.copyOf(metadata)
            ));
        }
        return new MarkdownTableSlice(List.copyOf(blocks), index, rowOrdinal);
    }

    private static List<Map<String, Object>> buildMarkdownCellCoordinates(
            int rowIndex,
            String[] cells,
            boolean headerRow,
            String[] previousDataRow
    ) {
        List<Map<String, Object>> coordinates = new ArrayList<>(cells.length);
        for (int columnIndex = 0; columnIndex < cells.length; columnIndex++) {
            String value = cells[columnIndex] == null ? "" : cells[columnIndex].trim();
            Map<String, Object> cell = new java.util.HashMap<>();
            cell.put("rowIndex", rowIndex);
            cell.put("columnIndex", columnIndex);
            cell.put("coordinate", "R" + (rowIndex + 1) + "C" + (columnIndex + 1));
            cell.put("value", value);
            if (isMarkdownMergePlaceholder(value)) {
                cell.put("merged", true);
                cell.put("mergeContinuation", true);
                cell.put("columnSpan", 0);
                if (previousDataRow != null && columnIndex < previousDataRow.length) {
                    cell.put("mergeSourceValue", previousDataRow[columnIndex].trim());
                }
            } else if (isMarkdownColspanPlaceholder(value)) {
                cell.put("merged", true);
                cell.put("columnSpan", 0);
                cell.put("mergeContinuation", true);
            } else {
                cell.put("merged", false);
            }
            if (headerRow) {
                cell.put("headerPath", List.of(value.isBlank() ? "Col" + (columnIndex + 1) : value));
            }
            coordinates.add(Map.copyOf(cell));
        }
        return coordinates;
    }

    private static boolean isMarkdownMergePlaceholder(String value) {
        return "^".equals(value) || "↑".equals(value) || "«".equals(value);
    }

    private static boolean isMarkdownColspanPlaceholder(String value) {
        return ">>".equals(value) || "→".equals(value);
    }

    static List<StructuralBlock> parsePlainText(String text) {
        if (HeadingPatternDetector.hasOutlineStructure(text)) {
            return parseOutlineText(text);
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (String paragraph : text.split("\\R{2,}")) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            blocks.add(StructuralBlock.paragraph(paragraph.trim(), ordinal++));
        }
        if (blocks.isEmpty() && text != null && !text.isBlank()) {
            blocks.add(StructuralBlock.paragraph(text.trim(), 0));
        }
        return blocks;
    }

    static List<StructuralBlock> parseHtml(String html) {
        return parseHtmlWithJsoup(html);
    }

    static List<StructuralBlock> parseHtmlWithJsoup(String html) {
        List<StructuralBlock> blocks = new ArrayList<>();
        org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(html);
        document.select("script, style, nav, footer, header, noscript").remove();
        org.jsoup.select.Elements elements = document.body() == null
                ? document.select("h1,h2,h3,h4,h5,h6,p,li,table:not(table table)")
                : document.body().select("h1,h2,h3,h4,h5,h6,p,li,table:not(table table)");
        int ordinal = 0;
        int tableIndex = 0;
        for (org.jsoup.nodes.Element element : elements) {
            String tag = element.tagName().toLowerCase();
            String text = element.text().trim();
            if ("table".equals(tag)) {
                List<com.knowbase.ingestion.office.HtmlTableStructureExtractor.HtmlTableModel> models =
                        com.knowbase.ingestion.office.HtmlTableStructureExtractor.extractTableTree(element, tableIndex);
                tableIndex += models.size();
                for (com.knowbase.ingestion.office.HtmlTableStructureExtractor.HtmlTableModel model : models) {
                    List<StructuralBlock> tableBlocks = com.knowbase.ingestion.office.OfficeTableBlockMapper.fromHtmlTable(model, ordinal);
                    blocks.addAll(tableBlocks);
                    ordinal += tableBlocks.size();
                }
                continue;
            }
            if (text.isBlank()) {
                continue;
            }
            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> blocks.add(StructuralBlock.heading(
                        Integer.parseInt(tag.substring(1)),
                        text,
                        ordinal++
                ));
                case "p" -> blocks.add(StructuralBlock.paragraph(text, ordinal++));
                case "li" -> blocks.add(StructuralBlock.listItem(text, ordinal++, 1));
                default -> blocks.add(StructuralBlock.domBlock(tag, text, ordinal++));
            }
        }
        if (blocks.isEmpty()) {
            String plain = document.text().trim();
            if (!plain.isBlank()) {
                return parsePlainText(plain);
            }
        }
        return enrichHeadingPaths(blocks);
    }

    private static List<StructuralBlock> enrichHeadingPaths(List<StructuralBlock> blocks) {
        List<String> path = new ArrayList<>();
        List<StructuralBlock> enriched = new ArrayList<>();
        for (StructuralBlock block : blocks) {
            if ("heading".equals(block.blockType())) {
                while (path.size() >= Math.max(1, block.level())) {
                    path.remove(path.size() - 1);
                }
                path.add(block.content());
            }
            java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(block.metadata());
            if (!path.isEmpty()) {
                metadata.put("sectionPath", String.join(" > ", path));
            }
            enriched.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    metadata
            ));
        }
        return enriched;
    }

    static List<StructuralBlock> splitPdfPages(String pageText, int pageNumber, int startOrdinal) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = startOrdinal;
        String[] lines = pageText.split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                continue;
            }
            if (PDF_HEADING.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.heading(2, line, ordinal++));
                continue;
            }
            if (PDF_TABLE_ROW.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.tableRow(line.replaceAll("\\s{2,}|\\t+", " | "), ordinal++, ordinal));
                continue;
            }
            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(line);
        }
        if (paragraph.length() > 0) {
            blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
        }
        if (blocks.isEmpty() && pageText != null && !pageText.isBlank()) {
            blocks.add(StructuralBlock.page(pageText.trim(), ordinal, pageNumber));
        } else {
            for (int index = 0; index < blocks.size(); index++) {
                StructuralBlock block = blocks.get(index);
                java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(block.metadata());
                metadata.put("pageNumber", pageNumber);
                blocks.set(index, new StructuralBlock(
                        block.blockType(),
                        block.level(),
                        block.content(),
                        block.ordinal(),
                        metadata
                ));
            }
        }
        return blocks;
    }

    static List<StructuralBlock> parseOutlineText(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        int ordinal = 0;
        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                continue;
            }
            java.util.OptionalInt headingLevel = HeadingPatternDetector.detectLevel(line);
            if (headingLevel.isPresent()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.heading(headingLevel.getAsInt(), line, ordinal++));
            } else {
                if (paragraph.length() > 0) {
                    paragraph.append('\n');
                }
                paragraph.append(line);
            }
        }
        if (paragraph.length() > 0) {
            blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
        }
        if (blocks.isEmpty() && text != null && !text.isBlank()) {
            return parsePlainTextLegacyParagraphs(text);
        }
        return enrichHeadingPaths(blocks);
    }

    private static List<StructuralBlock> parsePlainTextLegacyParagraphs(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (String paragraph : text.split("\\R{2,}")) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            blocks.add(StructuralBlock.paragraph(paragraph.trim(), ordinal++));
        }
        return blocks;
    }

    public static List<StructuralBlock> enrichHeadingPathsPublic(List<StructuralBlock> blocks) {
        return enrichHeadingPaths(blocks);
    }

    public static List<StructuralBlock> parseMarkdownPublic(String text) {
        return parseMarkdown(text);
    }

    public static String blocksToTextPublic(List<StructuralBlock> blocks) {
        return blocksToText(blocks);
    }

    static List<StructuralBlock> parseOcrLayout(String text) {
        return parseOcrLayout(text, Map.of());
    }

    public static List<StructuralBlock> parseOcrLayout(String text, Map<String, Object> ocrMetadata) {
        List<StructuralBlock> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocks;
        }
        Map<String, Object> sharedMetadata = ocrBlockMetadata(ocrMetadata);
        String[] lines = text.split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        int paragraphStartLine = 1;
        int ordinal = 0;
        int lineIndex = 0;
        for (String rawLine : lines) {
            lineIndex++;
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal++, paragraphStartLine, lineIndex - 1, sharedMetadata));
                    paragraph.setLength(0);
                }
                continue;
            }
            if (PDF_HEADING.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal++, paragraphStartLine, lineIndex - 1, sharedMetadata));
                    paragraph.setLength(0);
                }
                blocks.add(layoutHeading(line, ordinal++, lineIndex, sharedMetadata));
                continue;
            }
            if (PDF_TABLE_ROW.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal++, paragraphStartLine, lineIndex - 1, sharedMetadata));
                    paragraph.setLength(0);
                }
                blocks.add(layoutTableRow(line, ordinal++, lineIndex, sharedMetadata));
                continue;
            }
            if (paragraph.length() == 0) {
                paragraphStartLine = lineIndex;
            } else {
                paragraph.append('\n');
            }
            paragraph.append(line);
        }
        if (paragraph.length() > 0) {
            blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal, paragraphStartLine, lineIndex, sharedMetadata));
        }
        return enrichHeadingPaths(blocks);
    }

    private static StructuralBlock layoutParagraph(
            String content,
            int ordinal,
            int startLine,
            int endLine,
            Map<String, Object> sharedMetadata
    ) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(sharedMetadata);
        metadata.put("boundaryType", "paragraph");
        metadata.put("layoutRole", "body");
        metadata.put("ocrLineStart", startLine);
        metadata.put("ocrLineEnd", endLine);
        metadata.put("readingOrder", ordinal);
        metadata.put("bbox", estimatedOcrBbox(startLine, endLine));
        metadata.put("bboxSource", "ocr-text-line-estimate");
        return new StructuralBlock(
                "paragraph",
                0,
                content,
                ordinal,
                metadata
        );
    }

    private static StructuralBlock layoutHeading(String content, int ordinal, int lineIndex, Map<String, Object> sharedMetadata) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(sharedMetadata);
        metadata.put("boundaryType", "section");
        metadata.put("layoutRole", "heading");
        metadata.put("ocrLineStart", lineIndex);
        metadata.put("ocrLineEnd", lineIndex);
        metadata.put("readingOrder", ordinal);
        metadata.put("bbox", estimatedOcrBbox(lineIndex, lineIndex));
        metadata.put("bboxSource", "ocr-text-line-estimate");
        return new StructuralBlock(
                "heading",
                2,
                content,
                ordinal,
                metadata
        );
    }

    private static StructuralBlock layoutTableRow(String content, int ordinal, int lineIndex, Map<String, Object> sharedMetadata) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(sharedMetadata);
        metadata.put("boundaryType", "table_row");
        metadata.put("layoutRole", "table");
        metadata.put("ocrLineStart", lineIndex);
        metadata.put("ocrLineEnd", lineIndex);
        metadata.put("readingOrder", ordinal);
        metadata.put("bbox", estimatedOcrBbox(lineIndex, lineIndex));
        metadata.put("bboxSource", "ocr-text-line-estimate");
        return new StructuralBlock(
                "table_row",
                0,
                content.replaceAll("\\s{2,}|\\t+", " | "),
                ordinal,
                metadata
        );
    }

    private static Map<String, Object> ocrBlockMetadata(Map<String, Object> ocrMetadata) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("layoutParsing", true);
        metadata.put("ocrApplied", true);
        metadata.put("pageNumber", intMetadata(ocrMetadata, "pageNumber", 1));
        metadata.put("columnIndex", intMetadata(ocrMetadata, "columnIndex", 0));
        metadata.put("columnCount", intMetadata(ocrMetadata, "columnCount", 1));
        metadata.put("multiColumn", intMetadata(ocrMetadata, "columnCount", 1) > 1);
        Object confidence = ocrMetadata == null ? null : firstPresent(ocrMetadata, "ocrConfidence", "confidence");
        if (confidence instanceof Number number) {
            metadata.put("ocrConfidence", number.doubleValue());
            metadata.put("ocrConfidenceSource", "metadata");
        } else if (confidence != null) {
            try {
                metadata.put("ocrConfidence", Double.parseDouble(String.valueOf(confidence)));
                metadata.put("ocrConfidenceSource", "metadata");
            } catch (NumberFormatException ignored) {
                metadata.put("ocrConfidence", -1d);
                metadata.put("ocrConfidenceSource", "unavailable");
            }
        } else {
            metadata.put("ocrConfidence", -1d);
            metadata.put("ocrConfidenceSource", "unavailable");
        }
        return metadata;
    }

    private static Object firstPresent(Map<String, Object> metadata, String first, String second) {
        Object value = metadata.get(first);
        return value == null ? metadata.get(second) : value;
    }

    private static int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        if (metadata == null) {
            return fallback;
        }
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static List<Double> estimatedOcrBbox(int startLine, int endLine) {
        double top = Math.max(0, startLine - 1);
        double height = Math.max(1, endLine - startLine + 1);
        return List.of(0d, top, 1d, height);
    }

    static String blocksToText(List<StructuralBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (StructuralBlock block : blocks) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            if ("heading".equals(block.blockType())) {
                builder.append("#".repeat(Math.max(1, block.level()))).append(' ').append(block.content());
            } else {
                builder.append(block.content());
            }
        }
        return builder.toString();
    }

    private static String stripTags(String html) {
        return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
