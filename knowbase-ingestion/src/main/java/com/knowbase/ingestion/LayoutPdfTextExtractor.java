package com.knowbase.ingestion;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 版面解析：基于 TextPosition 聚类行/块，识别标题、正文、表格行。
 */
public final class LayoutPdfTextExtractor {

    private static final float LINE_Y_TOLERANCE = 3.0f;
    private static final float BLOCK_GAP_FACTOR = 1.6f;

    private LayoutPdfTextExtractor() {
    }

    public static List<StructuralBlock> extract(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PositionCollectingStripper stripper = new PositionCollectingStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);
            List<LayoutLine> lines = enrichReadingOrderAndColumns(stripper.lines());
            if (lines.isEmpty()) {
                return List.of();
            }
            float bodyFontSize = medianFontSize(lines);
            List<LayoutBlock> layoutBlocks = clusterBlocks(lines, bodyFontSize);
            return toStructuralBlocks(layoutBlocks);
        }
    }

    private static float medianFontSize(List<LayoutLine> lines) {
        List<Float> sizes = lines.stream().map(LayoutLine::fontSize).sorted().toList();
        if (sizes.isEmpty()) {
            return 12f;
        }
        return sizes.get(sizes.size() / 2);
    }

    private static List<LayoutBlock> clusterBlocks(List<LayoutLine> lines, float bodyFontSize) {
        List<LayoutBlock> blocks = new ArrayList<>();
        List<LayoutLine> current = new ArrayList<>();
        LayoutLine previous = null;

        for (LayoutLine line : lines) {
            if (previous != null && isBlockBreak(previous, line)) {
                blocks.add(buildBlock(current, bodyFontSize));
                current = new ArrayList<>();
            }
            current.add(line);
            previous = line;
        }
        if (!current.isEmpty()) {
            blocks.add(buildBlock(current, bodyFontSize));
        }
        return blocks;
    }

    private static boolean isBlockBreak(LayoutLine previous, LayoutLine current) {
        if (previous.pageNumber() != current.pageNumber()) {
            return true;
        }
        float gap = previous.y() - current.y();
        float threshold = Math.max(8f, previous.fontSize() * BLOCK_GAP_FACTOR);
        return gap > threshold;
    }

    private static LayoutBlock buildBlock(List<LayoutLine> lines, float bodyFontSize) {
        String content = String.join("\n", lines.stream().map(LayoutLine::text).toList()).trim();
        LayoutLine first = lines.getFirst();
        float avgFont = (float) lines.stream().mapToDouble(LayoutLine::fontSize).average().orElse(bodyFontSize);
        float minX = (float) lines.stream().mapToDouble(LayoutLine::minX).min().orElse(first.minX());
        float maxX = (float) lines.stream().mapToDouble(LayoutLine::maxX).max().orElse(first.maxX());
        float topY = (float) lines.stream().mapToDouble(LayoutLine::y).max().orElse(first.y());
        float bottomY = (float) lines.stream().mapToDouble(line -> line.y() - line.height()).min().orElse(first.y());
        String layoutRole = classifyRole(content, lines, avgFont, bodyFontSize);
        int readingOrder = lines.stream().mapToInt(LayoutLine::readingOrder).min().orElse(first.readingOrder());
        int columnIndex = dominantColumn(lines);
        int columnCount = lines.stream().mapToInt(LayoutLine::columnCount).max().orElse(first.columnCount());
        int headingLevel = "title".equals(layoutRole) || "heading".equals(layoutRole)
                ? headingLevel(avgFont, bodyFontSize)
                : 0;
        return new LayoutBlock(
                first.pageNumber(),
                readingOrder,
                columnIndex,
                columnCount,
                layoutRole,
                headingLevel,
                content,
                minX,
                bottomY,
                Math.max(1f, maxX - minX),
                Math.max(1f, topY - bottomY),
                avgFont
        );
    }

    private static List<LayoutLine> enrichReadingOrderAndColumns(List<LayoutLine> lines) {
        if (lines.isEmpty()) {
            return lines;
        }
        Map<Integer, PageColumnStats> pageStats = pageColumnStats(lines);
        List<LayoutLine> enriched = new ArrayList<>();
        int currentPage = -1;
        int readingOrder = 0;
        for (LayoutLine line : lines) {
            if (line.pageNumber() != currentPage) {
                currentPage = line.pageNumber();
                readingOrder = 0;
            }
            PageColumnStats stats = pageStats.getOrDefault(line.pageNumber(), PageColumnStats.singleColumn());
            int columnIndex = stats.columnCount() > 1 && line.minX() > stats.splitX() ? 1 : 0;
            enriched.add(line.withReadingOrder(readingOrder++, columnIndex, stats.columnCount()));
        }
        return enriched;
    }

    private static Map<Integer, PageColumnStats> pageColumnStats(List<LayoutLine> lines) {
        Map<Integer, List<LayoutLine>> byPage = new HashMap<>();
        for (LayoutLine line : lines) {
            byPage.computeIfAbsent(line.pageNumber(), ignored -> new ArrayList<>()).add(line);
        }
        Map<Integer, PageColumnStats> stats = new HashMap<>();
        for (Map.Entry<Integer, List<LayoutLine>> entry : byPage.entrySet()) {
            List<Float> starts = entry.getValue().stream().map(LayoutLine::minX).sorted().toList();
            if (starts.size() < 4) {
                stats.put(entry.getKey(), PageColumnStats.singleColumn());
                continue;
            }
            float min = starts.getFirst();
            float max = starts.getLast();
            float split = starts.get(starts.size() / 2);
            long left = starts.stream().filter(value -> value <= split).count();
            long right = starts.size() - left;
            boolean looksMultiColumn = max - min > 180f && left >= 2 && right >= 2;
            stats.put(entry.getKey(), looksMultiColumn ? new PageColumnStats(2, split) : PageColumnStats.singleColumn());
        }
        return stats;
    }

    private static int dominantColumn(List<LayoutLine> lines) {
        long right = lines.stream().filter(line -> line.columnIndex() > 0).count();
        return right > lines.size() / 2 ? 1 : 0;
    }

    private static String classifyRole(String content, List<LayoutLine> lines, float avgFont, float bodyFontSize) {
        if (content.isBlank()) {
            return "body";
        }
        if (lines.stream().anyMatch(LayoutLine::tableLike)) {
            return "table";
        }
        if (avgFont >= bodyFontSize * 1.18f && content.length() <= 120) {
            return avgFont >= bodyFontSize * 1.35f ? "title" : "heading";
        }
        if (PDF_HEADING.matcher(content.split("\n", 2)[0].trim()).matches()) {
            return "heading";
        }
        return "body";
    }

    private static final java.util.regex.Pattern PDF_HEADING = java.util.regex.Pattern.compile(
            "^(?:第\\s*[\\d一二三四五六七八九十百]+\\s*[章节篇部].+|Chapter\\s+\\d+.+|\\d+(?:\\.\\d+)*\\s+.+|[一二三四五六七八九十]+、.+)$"
    );

    private static int headingLevel(float avgFont, float bodyFontSize) {
        float ratio = avgFont / Math.max(1f, bodyFontSize);
        if (ratio >= 1.45f) {
            return 1;
        }
        if (ratio >= 1.28f) {
            return 2;
        }
        return 3;
    }

    private static List<StructuralBlock> toStructuralBlocks(List<LayoutBlock> layoutBlocks) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (LayoutBlock block : layoutBlocks) {
            if (block.content().isBlank()) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("boundaryType", block.layoutRole());
            metadata.put("layoutRole", block.layoutRole());
            metadata.put("pageNumber", block.pageNumber());
            metadata.put("readingOrder", block.readingOrder());
            metadata.put("columnIndex", block.columnIndex());
            metadata.put("columnCount", block.columnCount());
            metadata.put("multiColumn", block.columnCount() > 1);
            metadata.put("bbox", List.of(
                    round(block.x()),
                    round(block.y()),
                    round(block.width()),
                    round(block.height())
            ));
            metadata.put("fontSize", round(block.fontSize()));
            metadata.put("layoutParsing", true);

            StructuralBlock structuralBlock = switch (block.layoutRole()) {
                case "title", "heading" -> StructuralBlock.heading(block.level(), block.content(), ordinal);
                case "table" -> StructuralBlock.tableRow(
                        block.content().replaceAll("\\s{2,}|\\t+", " | "),
                        ordinal,
                        ordinal
                );
                default -> StructuralBlock.paragraph(block.content(), ordinal);
            };
            Map<String, Object> merged = new HashMap<>(structuralBlock.metadata());
            merged.putAll(metadata);
            blocks.add(new StructuralBlock(
                    structuralBlock.blockType(),
                    structuralBlock.level(),
                    structuralBlock.content(),
                    ordinal++,
                    merged
            ));
        }
        return StructureParsingSupport.enrichHeadingPathsPublic(blocks);
    }

    private static double round(float value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record LayoutLine(
            int pageNumber,
            String text,
            float y,
            float height,
            float fontSize,
            float minX,
            float maxX,
            int readingOrder,
            int columnIndex,
            int columnCount
    ) {
        LayoutLine withReadingOrder(int readingOrder, int columnIndex, int columnCount) {
            return new LayoutLine(pageNumber, text, y, height, fontSize, minX, maxX, readingOrder, columnIndex, columnCount);
        }

        boolean tableLike() {
            return text.contains("\t") || text.matches(".*\\S\\s{3,}\\S.*");
        }
    }

    private record LayoutBlock(
            int pageNumber,
            int readingOrder,
            int columnIndex,
            int columnCount,
            String layoutRole,
            int level,
            String content,
            float x,
            float y,
            float width,
            float height,
            float fontSize
    ) {
    }

    private record PageColumnStats(int columnCount, float splitX) {
        private static PageColumnStats singleColumn() {
            return new PageColumnStats(1, Float.MAX_VALUE);
        }
    }

    private static final class PositionCollectingStripper extends PDFTextStripper {

        private final List<PositionSample> positions = new ArrayList<>();
        private int currentPage = 1;

        private PositionCollectingStripper() throws IOException {
            super();
        }

        @Override
        protected void startPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
            currentPage = getCurrentPageNo();
            super.startPage(page);
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            for (TextPosition position : textPositions) {
                positions.add(new PositionSample(currentPage, position));
            }
            super.writeString(text, textPositions);
        }

        List<LayoutLine> lines() {
            if (positions.isEmpty()) {
                return List.of();
            }
            List<PositionSample> sorted = new ArrayList<>(positions);
            sorted.sort(Comparator
                    .comparingInt(PositionSample::pageNumber)
                    .thenComparing((PositionSample left, PositionSample right) ->
                            Float.compare(normalizedY(right.position()), normalizedY(left.position())))
                    .thenComparing(sample -> sample.position().getXDirAdj()));

            List<LayoutLine> lines = new ArrayList<>();
            List<PositionSample> currentLine = new ArrayList<>();
            PositionSample previous = null;

            for (PositionSample sample : sorted) {
                if (previous != null
                        && (previous.pageNumber() != sample.pageNumber()
                        || Math.abs(normalizedY(previous.position()) - normalizedY(sample.position())) > LINE_Y_TOLERANCE)) {
                    lines.add(toLine(currentLine));
                    currentLine = new ArrayList<>();
                }
                currentLine.add(sample);
                previous = sample;
            }
            if (!currentLine.isEmpty()) {
                lines.add(toLine(currentLine));
            }
            return lines;
        }

        private static float normalizedY(TextPosition position) {
            return position.getYDirAdj();
        }

        private static LayoutLine toLine(List<PositionSample> lineSamples) {
            lineSamples.sort(Comparator.comparing(sample -> sample.position().getXDirAdj()));
            StringBuilder builder = new StringBuilder();
            TextPosition previous = null;
            float minX = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float fontSizeSum = 0f;
            for (PositionSample sample : lineSamples) {
                TextPosition position = sample.position();
                minX = Math.min(minX, position.getXDirAdj());
                maxX = Math.max(maxX, position.getXDirAdj() + position.getWidthDirAdj());
                fontSizeSum += position.getFontSizeInPt();
                if (previous != null) {
                    float gap = position.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
                    if (gap > Math.max(2f, previous.getWidthOfSpace() * 0.8f)) {
                        builder.append(gap > previous.getWidthOfSpace() * 2.5f ? '\t' : ' ');
                    }
                }
                builder.append(position.getUnicode());
                previous = position;
            }
            PositionSample anchor = lineSamples.getFirst();
            TextPosition anchorPosition = anchor.position();
            float avgFont = fontSizeSum / lineSamples.size();
            return new LayoutLine(
                    anchor.pageNumber(),
                    builder.toString().trim(),
                    normalizedY(anchorPosition),
                    anchorPosition.getHeightDir(),
                    avgFont,
                    minX,
                    maxX,
                    0,
                    0,
                    1
            );
        }
    }

    private record PositionSample(int pageNumber, TextPosition position) {
    }
}
