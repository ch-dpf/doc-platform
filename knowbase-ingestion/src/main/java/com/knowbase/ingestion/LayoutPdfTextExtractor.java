package com.knowbase.ingestion;

import com.knowbase.ingestion.pdf.PdfLayoutRoleClassifier;
import com.knowbase.ingestion.pdf.PdfStreamTableDetector;
import com.knowbase.ingestion.pdf.PdfNestedTableSegmenter;
import com.knowbase.ingestion.pdf.PdfTableCellExtractor;
import com.knowbase.ingestion.pdf.PdfTableLayoutAnalyzer;
import com.knowbase.ingestion.pdf.PdfTableRegionMerger;
import com.knowbase.ingestion.pdf.PdfTableRowInput;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
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
            Map<Integer, Float> pageHeights = pageHeights(document);
            Map<Integer, Float> pageWidths = pageWidths(document);
            PositionCollectingStripper stripper = new PositionCollectingStripper();
            stripper.setSortByPosition(true);
            stripper.getText(document);
            List<LayoutLine> lines = enrichReadingOrderAndColumns(stripper.lines());
            if (lines.isEmpty()) {
                return List.of();
            }
            float bodyFontSize = medianFontSize(lines);
            ExtractionContext context = new ExtractionContext(pageHeights, pageWidths, bodyFontSize);
            List<LayoutBlock> layoutBlocks = clusterBlocks(lines, context);
            return toStructuralBlocks(layoutBlocks, context);
        }
    }

    /**
     * Extracts a single PDF page via TextPosition layout (local layout model path).
     */
    public static List<StructuralBlock> extractPage(byte[] pdfBytes, int pageNumber) throws IOException {
        if (pageNumber <= 0) {
            return List.of();
        }
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            if (pageNumber > document.getNumberOfPages()) {
                return List.of();
            }
            Map<Integer, Float> pageHeights = pageHeights(document);
            Map<Integer, Float> pageWidths = pageWidths(document);
            PositionCollectingStripper stripper = new PositionCollectingStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            stripper.getText(document);
            List<LayoutLine> lines = enrichReadingOrderAndColumns(stripper.lines());
            if (lines.isEmpty()) {
                return List.of();
            }
            float bodyFontSize = medianFontSize(lines);
            ExtractionContext context = new ExtractionContext(pageHeights, pageWidths, bodyFontSize);
            List<LayoutBlock> layoutBlocks = clusterBlocks(lines, context);
            return toStructuralBlocks(layoutBlocks, context);
        }
    }

    private static Map<Integer, Float> pageHeights(PDDocument document) {
        Map<Integer, Float> heights = new HashMap<>();
        for (int index = 0; index < document.getNumberOfPages(); index++) {
            PDPage page = document.getPage(index);
            heights.put(index + 1, page.getMediaBox().getHeight());
        }
        return heights;
    }

    private static Map<Integer, Float> pageWidths(PDDocument document) {
        Map<Integer, Float> widths = new HashMap<>();
        for (int index = 0; index < document.getNumberOfPages(); index++) {
            PDPage page = document.getPage(index);
            widths.put(index + 1, page.getMediaBox().getWidth());
        }
        return widths;
    }

    private static float medianFontSize(List<LayoutLine> lines) {
        List<Float> sizes = lines.stream().map(LayoutLine::fontSize).sorted().toList();
        if (sizes.isEmpty()) {
            return 12f;
        }
        return sizes.get(sizes.size() / 2);
    }

    private static List<LayoutBlock> clusterBlocks(List<LayoutLine> lines, ExtractionContext context) {
        List<LayoutBlock> blocks = new ArrayList<>();
        List<LayoutLine> current = new ArrayList<>();
        LayoutLine previous = null;

        for (LayoutLine line : lines) {
            if (previous != null && isBlockBreak(previous, line)) {
                blocks.add(buildBlock(current, context));
                current = new ArrayList<>();
            }
            current.add(line);
            previous = line;
        }
        if (!current.isEmpty()) {
            blocks.add(buildBlock(current, context));
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

    private static LayoutBlock buildBlock(List<LayoutLine> lines, ExtractionContext context) {
        String content = String.join("\n", lines.stream().map(LayoutLine::text).toList()).trim();
        LayoutLine first = lines.getFirst();
        float bodyFontSize = context.bodyFontSize();
        float avgFont = (float) lines.stream().mapToDouble(LayoutLine::fontSize).average().orElse(bodyFontSize);
        float minX = (float) lines.stream().mapToDouble(LayoutLine::minX).min().orElse(first.minX());
        float maxX = (float) lines.stream().mapToDouble(LayoutLine::maxX).max().orElse(first.maxX());
        float topY = (float) lines.stream().mapToDouble(LayoutLine::y).max().orElse(first.y());
        float bottomY = (float) lines.stream().mapToDouble(line -> line.y() - line.height()).min().orElse(first.y());
        float pageHeight = context.pageHeights().getOrDefault(first.pageNumber(), 0f);
        String layoutRole = classifyRole(content, lines, avgFont, bodyFontSize, topY, pageHeight);
        int readingOrder = lines.stream().mapToInt(LayoutLine::readingOrder).min().orElse(first.readingOrder());
        int columnIndex = dominantColumn(lines);
        int columnCount = lines.stream().mapToInt(LayoutLine::columnCount).max().orElse(first.columnCount());
        int headingLevel = "title".equals(layoutRole) || "heading".equals(layoutRole)
                ? headingLevel(avgFont, bodyFontSize)
                : 0;
        List<Float> cellBoundaryX = dominantCellBoundaries(lines);
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
                avgFont,
                cellBoundaryX
        );
    }

    private static List<Float> dominantCellBoundaries(List<LayoutLine> lines) {
        List<LayoutLine> tableLines = lines.stream().filter(LayoutLine::tableLike).toList();
        if (tableLines.isEmpty()) {
            return List.of();
        }
        return tableLines.getFirst().cellBoundaryX();
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
            float pageMaxX = (float) entry.getValue().stream().mapToDouble(LayoutLine::maxX).max().orElse(max);
            float split = starts.get(starts.size() / 2);
            long left = starts.stream().filter(value -> value <= split).count();
            long right = starts.size() - left;
            boolean separatedStarts = max - min > 140f && left >= 2 && right >= 2;
            boolean wideTextSpread = pageMaxX - min > 260f && starts.size() >= 4;
            float effectiveSplit = separatedStarts ? split : min + (pageMaxX - min) / 2f;
            stats.put(entry.getKey(), (separatedStarts || wideTextSpread)
                    ? new PageColumnStats(2, effectiveSplit)
                    : PageColumnStats.singleColumn());
        }
        return stats;
    }

    private static int dominantColumn(List<LayoutLine> lines) {
        long right = lines.stream().filter(line -> line.columnIndex() > 0).count();
        return right > lines.size() / 2 ? 1 : 0;
    }

    private static String classifyRole(
            String content,
            List<LayoutLine> lines,
            float avgFont,
            float bodyFontSize,
            float topY,
            float pageHeight
    ) {
        if (content.isBlank()) {
            return "body";
        }
        if (lines.stream().anyMatch(LayoutLine::tableLike) || PdfTableLayoutAnalyzer.isTableRun(lines)) {
            return "table";
        }
        String role = PdfLayoutRoleClassifier.classify(content, topY, pageHeight, avgFont, bodyFontSize);
        if ("body".equals(role) && PDF_HEADING.matcher(content.split("\n", 2)[0].trim()).matches()) {
            return "heading";
        }
        return role;
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

    private static List<StructuralBlock> toStructuralBlocks(List<LayoutBlock> layoutBlocks, ExtractionContext context) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        int tableRegionId = 0;
        List<LayoutBlock> currentTableRegion = new ArrayList<>();
        TableRegionTracker tableTracker = new TableRegionTracker();
        for (LayoutBlock block : layoutBlocks) {
            if (block.content().isBlank()) {
                continue;
            }
            if ("table".equals(block.layoutRole())) {
                currentTableRegion.add(block);
                continue;
            }
            if (!currentTableRegion.isEmpty()) {
                ordinal = flushTableRegion(blocks, currentTableRegion, tableRegionId, ordinal, tableTracker, context);
                tableRegionId = tableTracker.nextAssignableRegionId();
                currentTableRegion = new ArrayList<>();
            }
            ordinal = appendLayoutBlock(blocks, block, ordinal, context);
        }
        if (!currentTableRegion.isEmpty()) {
            flushTableRegion(blocks, currentTableRegion, tableRegionId, ordinal, tableTracker, context);
        }
        return StructureParsingSupport.enrichHeadingPathsPublic(blocks);
    }

    private static int appendLayoutBlock(List<StructuralBlock> blocks, LayoutBlock block, int ordinal, ExtractionContext context) {
        Map<String, Object> metadata = layoutMetadata(block, context);
        metadata.put("indexableHint", PdfLayoutRoleClassifier.isIndexableRole(block.layoutRole()));

        StructuralBlock structuralBlock = switch (block.layoutRole()) {
            case "title", "heading" -> StructuralBlock.heading(block.level(), block.content(), ordinal);
            default -> StructuralBlock.paragraph(block.content(), ordinal);
        };
        Map<String, Object> merged = new HashMap<>(structuralBlock.metadata());
        merged.putAll(metadata);
        blocks.add(new StructuralBlock(
                structuralBlock.blockType(),
                structuralBlock.level(),
                structuralBlock.content(),
                ordinal,
                merged
        ));
        return ordinal + 1;
    }

    private static Map<String, Object> layoutMetadata(LayoutBlock block, ExtractionContext context) {
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
        Float pageHeight = context.pageHeights().get(block.pageNumber());
        Float pageWidth = context.pageWidths().get(block.pageNumber());
        if (pageHeight != null) {
            metadata.put("pageHeight", round(pageHeight));
        }
        if (pageWidth != null) {
            metadata.put("pageWidth", round(pageWidth));
        }
        metadata.put("fontSize", round(block.fontSize()));
        metadata.put("layoutParsing", true);
        return metadata;
    }

    private static int flushTableRegion(
            List<StructuralBlock> blocks,
            List<LayoutBlock> tableRegion,
            int tableRegionId,
            int ordinal,
            TableRegionTracker tracker,
            ExtractionContext context
    ) {
        List<PdfTableRowInput> rows = tableRegion.stream().map(LayoutPdfTextExtractor::toRowInput).toList();
        String tableDetection = PdfTableLayoutAnalyzer.tableDetectionSource(tableRegion.stream()
                .map(block -> (PdfTableLayoutAnalyzer.TableLineCandidate) new TableLineProxy(block))
                .toList());
        List<PdfNestedTableSegmenter.TableSegment> segments = PdfNestedTableSegmenter.segment(rows);
        if (segments.size() <= 1) {
            return flushSingleTableSegment(
                    blocks, rows, tableRegionId, ordinal, tracker, context, tableDetection, segments.isEmpty() ? 0 : segments.getFirst().nestedDepth());
        }
        int nextOrdinal = ordinal;
        int assignableRegionId = tableRegionId;
        for (PdfNestedTableSegmenter.TableSegment segment : segments) {
            if (segment.rows().isEmpty()) {
                continue;
            }
            PdfTableRegionMerger.PdfTableRegionSlice slice =
                    new PdfTableRegionMerger.PdfTableRegionSlice(assignableRegionId, segment.rows());
            if (tracker.lastSlice != null && PdfTableRegionMerger.isContinuation(tracker.lastSlice.rows(), slice.rows())) {
                for (int index = 0; index < tracker.lastBlockCount; index++) {
                    blocks.remove(blocks.size() - 1);
                }
                List<PdfTableRowInput> combined = new ArrayList<>(tracker.lastSlice.rows());
                combined.addAll(segment.rows());
                int mergedId = tracker.lastSlice.tableRegionId();
                int added = appendTableRegion(
                        blocks, combined, mergedId, tracker.lastStartOrdinal, context, tableDetection, segment.nestedDepth());
                tracker.lastSlice = new PdfTableRegionMerger.PdfTableRegionSlice(mergedId, combined);
                tracker.lastBlockCount = added;
                nextOrdinal = tracker.lastStartOrdinal + added;
                continue;
            }
            int added = appendTableRegion(
                    blocks, segment.rows(), assignableRegionId, nextOrdinal, context, tableDetection, segment.nestedDepth());
            tracker.lastSlice = slice;
            tracker.lastStartOrdinal = nextOrdinal;
            tracker.lastBlockCount = added;
            nextOrdinal += added;
            assignableRegionId++;
        }
        tracker.nextAssignableRegionId = assignableRegionId;
        return nextOrdinal;
    }

    private static int flushSingleTableSegment(
            List<StructuralBlock> blocks,
            List<PdfTableRowInput> rows,
            int tableRegionId,
            int ordinal,
            TableRegionTracker tracker,
            ExtractionContext context,
            String tableDetection,
            int nestedDepth
    ) {
        PdfTableRegionMerger.PdfTableRegionSlice slice = new PdfTableRegionMerger.PdfTableRegionSlice(tableRegionId, rows);
        if (tracker.lastSlice != null && PdfTableRegionMerger.isContinuation(tracker.lastSlice.rows(), slice.rows())) {
            for (int index = 0; index < tracker.lastBlockCount; index++) {
                blocks.remove(blocks.size() - 1);
            }
            List<PdfTableRowInput> combined = new ArrayList<>(tracker.lastSlice.rows());
            combined.addAll(rows);
            int mergedId = tracker.lastSlice.tableRegionId();
            int added = appendTableRegion(blocks, combined, mergedId, tracker.lastStartOrdinal, context, tableDetection, nestedDepth);
            tracker.lastSlice = new PdfTableRegionMerger.PdfTableRegionSlice(mergedId, combined);
            tracker.lastBlockCount = added;
            return tracker.lastStartOrdinal + added;
        }
        int added = appendTableRegion(blocks, rows, tableRegionId, ordinal, context, tableDetection, nestedDepth);
        tracker.lastSlice = slice;
        tracker.lastStartOrdinal = ordinal;
        tracker.lastBlockCount = added;
        tracker.nextAssignableRegionId = tableRegionId + 1;
        return ordinal + added;
    }

    private static int appendTableRegion(
            List<StructuralBlock> blocks,
            List<PdfTableRowInput> rows,
            int tableRegionId,
            int startOrdinal,
            ExtractionContext context,
            String tableDetection,
            int nestedDepth
    ) {
        List<StructuralBlock> tableBlocks = PdfTableCellExtractor.toStructuralBlocks(rows, tableRegionId, startOrdinal, tableDetection);
        for (int index = 0; index < tableBlocks.size(); index++) {
            StructuralBlock block = tableBlocks.get(index);
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            if (nestedDepth > 0) {
                metadata.put("nestedTableDepth", nestedDepth);
                metadata.put("nestedTable", true);
            }
            tableBlocks.set(index, enrichTableBlockPageDimensions(
                    new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(metadata)),
                    context
            ));
        }
        blocks.addAll(tableBlocks);
        return tableBlocks.size();
    }

    private static int appendTableRegion(
            List<StructuralBlock> blocks,
            List<PdfTableRowInput> rows,
            int tableRegionId,
            int startOrdinal,
            ExtractionContext context,
            String tableDetection
    ) {
        return appendTableRegion(blocks, rows, tableRegionId, startOrdinal, context, tableDetection, 0);
    }

    private static StructuralBlock enrichTableBlockPageDimensions(StructuralBlock block, ExtractionContext context) {
        Object pageNumber = block.metadata().get("pageNumber");
        if (!(pageNumber instanceof Number page)) {
            return block;
        }
        Float pageWidth = context.pageWidths().get(page.intValue());
        Float pageHeight = context.pageHeights().get(page.intValue());
        if (pageWidth == null && pageHeight == null) {
            return block;
        }
        Map<String, Object> metadata = new HashMap<>(block.metadata());
        if (pageWidth != null) {
            metadata.put("pageWidth", round(pageWidth));
        }
        if (pageHeight != null) {
            metadata.put("pageHeight", round(pageHeight));
        }
        return new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(metadata));
    }

    private static final class TableRegionTracker {
        private PdfTableRegionMerger.PdfTableRegionSlice lastSlice;
        private int lastStartOrdinal;
        private int lastBlockCount;
        private int nextAssignableRegionId = 0;

        int nextAssignableRegionId() {
            return nextAssignableRegionId;
        }
    }

    private record ExtractionContext(Map<Integer, Float> pageHeights, Map<Integer, Float> pageWidths, float bodyFontSize) {
    }

    private static PdfTableRowInput toRowInput(LayoutBlock block) {
        return new PdfTableRowInput(
                block.pageNumber(),
                block.readingOrder(),
                block.columnIndex(),
                block.columnCount(),
                block.content(),
                block.x(),
                block.y(),
                block.width(),
                block.height(),
                block.cellBoundaryX()
        );
    }

    private static double round(float value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record TableLineProxy(LayoutBlock block) implements PdfTableLayoutAnalyzer.TableLineCandidate {
        @Override
        public String text() {
            return block.content();
        }

        @Override
        public float minX() {
            return block.x();
        }

        @Override
        public boolean tableLike() {
            return block.cellBoundaryX() != null && block.cellBoundaryX().size() >= 3;
        }

        @Override
        public List<Float> cellBoundaryX() {
            return block.cellBoundaryX();
        }
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
            int columnCount,
            List<Float> cellBoundaryX
    ) implements PdfTableLayoutAnalyzer.TableLineCandidate {
        LayoutLine withReadingOrder(int readingOrder, int columnIndex, int columnCount) {
            return new LayoutLine(
                    pageNumber, text, y, height, fontSize, minX, maxX,
                    readingOrder, columnIndex, columnCount, cellBoundaryX
            );
        }

        public boolean tableLike() {
            if (cellBoundaryX != null && cellBoundaryX.size() >= 3) {
                return true;
            }
            return PdfStreamTableDetector.isStreamTableRow(text);
        }

        @Override
        public List<Float> cellBoundaryX() {
            return cellBoundaryX;
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
            float fontSize,
            List<Float> cellBoundaryX
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
            List<Float> cellBoundaryX = new ArrayList<>();
            for (PositionSample sample : lineSamples) {
                TextPosition position = sample.position();
                if (cellBoundaryX.isEmpty()) {
                    cellBoundaryX.add(position.getXDirAdj());
                }
                minX = Math.min(minX, position.getXDirAdj());
                maxX = Math.max(maxX, position.getXDirAdj() + position.getWidthDirAdj());
                fontSizeSum += position.getFontSizeInPt();
                if (previous != null) {
                    float gap = position.getXDirAdj() - (previous.getXDirAdj() + previous.getWidthDirAdj());
                    if (gap > Math.max(2f, previous.getWidthOfSpace() * 0.8f)) {
                        if (gap > previous.getWidthOfSpace() * 2.5f) {
                            builder.append('\t');
                            cellBoundaryX.add(position.getXDirAdj());
                        } else {
                            builder.append(' ');
                        }
                    }
                }
                builder.append(position.getUnicode());
                previous = position;
            }
            cellBoundaryX.add(maxX);
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
                    1,
                    List.copyOf(cellBoundaryX)
            );
        }
    }

    private record PositionSample(int pageNumber, TextPosition position) {
    }
}
