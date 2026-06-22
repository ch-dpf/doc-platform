package com.knowbase.ingestion.parser;

import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.BlockType;
import com.knowbase.ingestion.document.ParsedDocument.ContentFamily;
import com.knowbase.ingestion.document.ParsedDocument.TableBlock;
import com.knowbase.ingestion.document.ParsedDocument.TableCell;
import com.knowbase.ingestion.document.ParsedDocument.TextBlock;
import com.knowbase.ingestion.parser.DocumentParser.ParseRequest;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

/**
 * JDK-only HTML parser focused on preserving table structure for RAG ingestion.
 */
public final class HtmlDocumentParser implements DocumentParser {

    @Override
    public boolean supports(ParseRequest request) {
        Objects.requireNonNull(request, "request");
        String mediaType = request.mediaType();
        String sourceName = request.metadata().getOrDefault("sourceName", "").toLowerCase(Locale.ROOT);
        return mediaType.equals("text/html")
                || mediaType.equals("application/xhtml+xml")
                || sourceName.endsWith(".html")
                || sourceName.endsWith(".htm");
    }

    @Override
    public ParsedDocument parse(ParseRequest request) {
        Objects.requireNonNull(request, "request");
        ParsedDocument parsed = parse(request.documentId(), request.content());
        Map<String, String> metadata = new LinkedHashMap<>(request.metadata());
        metadata.put("mediaType", request.mediaType());
        metadata.putAll(parsed.metadata());
        return parsed.withMetadata(metadata);
    }

    public ParsedDocument parse(String documentId, String html) {
        Objects.requireNonNull(html, "html");
        HtmlCollector collector = new HtmlCollector(documentId);
        try {
            new ParserDelegator().parse(new StringReader(html), collector, true);
            return collector.finish();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to parse HTML document", e);
        }
    }

    private static final class HtmlCollector extends HTMLEditorKit.ParserCallback {
        private final ParsedDocument.Builder document;
        private final StringBuilder textBuffer = new StringBuilder();
        private final StringBuilder headingBuffer = new StringBuilder();
        private final List<TableBuilder> tableStack = new ArrayList<>();
        private int headingLevel;
        private int tableSequence;

        private HtmlCollector(String documentId) {
            this.document = ParsedDocument.builder(documentId, ContentFamily.WEB_PAGE)
                    .metadata("parser", "html-jdk")
                    .metadata("tableStructurePreserved", "true");
        }

        @Override
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.TABLE) {
                flushTextBlock();
                tableStack.add(new TableBuilder(++tableSequence, attributesToMap(attributes)));
                return;
            }
            if (insideTable()) {
                currentTable().startTag(tag, attributes);
                return;
            }
            int level = headingLevel(tag);
            if (level > 0) {
                flushTextBlock();
                headingLevel = level;
                headingBuffer.setLength(0);
            } else if (isParagraphBoundary(tag)) {
                appendOutsideText("\n");
            }
        }

        @Override
        public void handleEndTag(HTML.Tag tag, int position) {
            if (insideTable()) {
                TableBuilder table = currentTable();
                table.endTag(tag);
                if (tag == HTML.Tag.TABLE) {
                    tableStack.remove(tableStack.size() - 1);
                    document.block(table.build());
                }
                return;
            }
            int level = headingLevel(tag);
            if (level > 0 && headingLevel == level) {
                String heading = normalizeWhitespace(headingBuffer.toString());
                if (!heading.isBlank()) {
                    document.block(new TextBlock(
                            BlockType.HEADING,
                            heading,
                            ParsedDocument.metadata("level", String.valueOf(level))
                    ));
                }
                headingLevel = 0;
                headingBuffer.setLength(0);
            } else if (isParagraphBoundary(tag)) {
                appendOutsideText("\n");
            }
        }

        @Override
        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (insideTable()) {
                currentTable().simpleTag(tag);
                return;
            }
            if (tag == HTML.Tag.BR) {
                appendOutsideText("\n");
            }
        }

        @Override
        public void handleText(char[] data, int position) {
            String text = new String(data);
            if (insideTable()) {
                currentTable().text(text);
            } else if (headingLevel > 0) {
                appendWithSpace(headingBuffer, text);
            } else {
                appendOutsideText(text);
            }
        }

        private ParsedDocument finish() {
            flushTextBlock();
            return document.build();
        }

        private boolean insideTable() {
            return !tableStack.isEmpty();
        }

        private TableBuilder currentTable() {
            return tableStack.get(tableStack.size() - 1);
        }

        private void appendOutsideText(String text) {
            appendWithSpace(textBuffer, text);
        }

        private void flushTextBlock() {
            String text = normalizeWhitespace(textBuffer.toString());
            if (!text.isBlank()) {
                document.block(new TextBlock(BlockType.PARAGRAPH, text, Map.of()));
            }
            textBuffer.setLength(0);
        }
    }

    private static final class TableBuilder {
        private final int sequence;
        private final Map<String, String> attributes;
        private final List<RowBuilder> rows = new ArrayList<>();
        private final StringBuilder caption = new StringBuilder();
        private RowBuilder currentRow;
        private CellBuilder currentCell;
        private boolean inCaption;

        private TableBuilder(int sequence, Map<String, String> attributes) {
            this.sequence = sequence;
            this.attributes = attributes;
        }

        private void startTag(HTML.Tag tag, MutableAttributeSet attrs) {
            if (tag == HTML.Tag.CAPTION) {
                inCaption = true;
                return;
            }
            if (tag == HTML.Tag.TR) {
                currentRow = new RowBuilder(rows.size());
                rows.add(currentRow);
                return;
            }
            if (tag == HTML.Tag.TH || tag == HTML.Tag.TD) {
                if (currentRow == null) {
                    currentRow = new RowBuilder(rows.size());
                    rows.add(currentRow);
                }
                currentCell = CellBuilder.from(tag, attrs);
                currentRow.cells.add(currentCell);
            }
        }

        private void endTag(HTML.Tag tag) {
            if (tag == HTML.Tag.CAPTION) {
                inCaption = false;
            } else if (tag == HTML.Tag.TH || tag == HTML.Tag.TD) {
                currentCell = null;
            } else if (tag == HTML.Tag.TR) {
                currentRow = null;
            }
        }

        private void simpleTag(HTML.Tag tag) {
            if (tag == HTML.Tag.BR) {
                text("\n");
            }
        }

        private void text(String text) {
            if (currentCell != null) {
                appendWithSpace(currentCell.value, text);
            } else if (inCaption) {
                appendWithSpace(caption, text);
            }
        }

        private TableBlock build() {
            List<TableCell> laidOutCells = layoutCells();
            int rowCount = 0;
            int columnCount = 0;
            for (TableCell cell : laidOutCells) {
                rowCount = Math.max(rowCount, cell.rowIndex() + cell.rowSpan());
                columnCount = Math.max(columnCount, cell.columnIndex() + cell.columnSpan());
            }
            List<TableCell> cellsWithHeaders = inheritHeaders(laidOutCells);
            Map<String, String> metadata = new LinkedHashMap<>(attributes);
            metadata.put("tableSequence", String.valueOf(sequence));
            metadata.put("summaryStrategy", "caption-dimensions-headers");
            return new TableBlock(normalizeWhitespace(caption.toString()), rowCount, columnCount, cellsWithHeaders, metadata);
        }

        private List<TableCell> layoutCells() {
            List<TableCell> cells = new ArrayList<>();
            Set<String> occupied = new HashSet<>();
            for (RowBuilder row : rows) {
                int columnIndex = 0;
                for (CellBuilder cell : row.cells) {
                    while (occupied.contains(key(row.rowIndex, columnIndex))) {
                        columnIndex++;
                    }
                    TableCell tableCell = new TableCell(
                            normalizeWhitespace(cell.value.toString()),
                            row.rowIndex,
                            columnIndex,
                            cell.rowSpan,
                            cell.columnSpan,
                            cell.header,
                            cell.scope,
                            Map.of(),
                            cell.attributes
                    );
                    cells.add(tableCell);
                    for (int rowOffset = 0; rowOffset < cell.rowSpan; rowOffset++) {
                        for (int columnOffset = 0; columnOffset < cell.columnSpan; columnOffset++) {
                            occupied.add(key(row.rowIndex + rowOffset, columnIndex + columnOffset));
                        }
                    }
                    columnIndex += cell.columnSpan;
                }
            }
            return cells;
        }

        private List<TableCell> inheritHeaders(List<TableCell> cells) {
            List<TableCell> headers = cells.stream().filter(TableCell::header).toList();
            if (headers.isEmpty()) {
                return cells;
            }
            List<TableCell> withHeaders = new ArrayList<>(cells.size());
            for (TableCell cell : cells) {
                if (cell.header()) {
                    withHeaders.add(cell);
                    continue;
                }
                Map<String, List<String>> inherited = new LinkedHashMap<>();
                inherited.put("column", collectColumnHeaders(cell, headers));
                inherited.put("row", collectRowHeaders(cell, headers));
                inherited.put("table", collectTableHeaders(headers));
                inherited.entrySet().removeIf(entry -> entry.getValue().isEmpty());
                withHeaders.add(cell.withInheritedHeaders(inherited));
            }
            return Collections.unmodifiableList(withHeaders);
        }

        private List<String> collectColumnHeaders(TableCell cell, List<TableCell> headers) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (TableCell header : headers) {
                boolean scopedToColumn = header.scope().isEmpty()
                        || header.scope().equals("col")
                        || header.scope().equals("colgroup");
                if (scopedToColumn && header.rowIndex() < cell.rowIndex() && rangesIntersect(
                        header.columnIndex(),
                        header.columnIndex() + header.columnSpan(),
                        cell.columnIndex(),
                        cell.columnIndex() + cell.columnSpan()
                )) {
                    appendHeader(values, header);
                }
            }
            return List.copyOf(values);
        }

        private List<String> collectRowHeaders(TableCell cell, List<TableCell> headers) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (TableCell header : headers) {
                boolean scopedToRow = header.scope().equals("row") || header.scope().equals("rowgroup");
                boolean sameRowToLeft = header.rowIndex() == cell.rowIndex() && header.columnIndex() < cell.columnIndex();
                boolean rowSpanToLeft = header.coversRow(cell.rowIndex()) && header.columnIndex() < cell.columnIndex();
                if ((scopedToRow || sameRowToLeft || rowSpanToLeft) && header.columnIndex() < cell.columnIndex()) {
                    appendHeader(values, header);
                }
            }
            return List.copyOf(values);
        }

        private List<String> collectTableHeaders(List<TableCell> headers) {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            for (TableCell header : headers) {
                if (header.rowIndex() == 0 || header.scope().equals("colgroup")) {
                    appendHeader(values, header);
                }
            }
            return List.copyOf(values);
        }

        private void appendHeader(Set<String> values, TableCell header) {
            if (!header.value().isBlank()) {
                values.add(header.value());
            }
        }
    }

    private record RowBuilder(int rowIndex, List<CellBuilder> cells) {
        private RowBuilder(int rowIndex) {
            this(rowIndex, new ArrayList<>());
        }
    }

    private static final class CellBuilder {
        private final StringBuilder value = new StringBuilder();
        private final boolean header;
        private final int rowSpan;
        private final int columnSpan;
        private final String scope;
        private final Map<String, String> attributes;

        private CellBuilder(boolean header, int rowSpan, int columnSpan, String scope, Map<String, String> attributes) {
            this.header = header;
            this.rowSpan = rowSpan;
            this.columnSpan = columnSpan;
            this.scope = scope;
            this.attributes = attributes;
        }

        private static CellBuilder from(HTML.Tag tag, MutableAttributeSet attrs) {
            Map<String, String> attributes = attributesToMap(attrs);
            return new CellBuilder(
                    tag == HTML.Tag.TH,
                    positiveInt(attributes.get("rowspan"), 1),
                    positiveInt(attributes.get("colspan"), 1),
                    attributes.getOrDefault("scope", ""),
                    attributes
            );
        }
    }

    private static boolean rangesIntersect(int leftStart, int leftEnd, int rightStart, int rightEnd) {
        return leftStart < rightEnd && rightStart < leftEnd;
    }

    private static String key(int rowIndex, int columnIndex) {
        return rowIndex + ":" + columnIndex;
    }

    private static int headingLevel(HTML.Tag tag) {
        String value = tag.toString().toLowerCase(Locale.ROOT);
        if (value.length() == 2 && value.charAt(0) == 'h' && Character.isDigit(value.charAt(1))) {
            return Character.digit(value.charAt(1), 10);
        }
        return 0;
    }

    private static boolean isParagraphBoundary(HTML.Tag tag) {
        return tag == HTML.Tag.P || tag == HTML.Tag.DIV || tag == HTML.Tag.LI || tag == HTML.Tag.BLOCKQUOTE;
    }

    private static int positiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(parsed, 1);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static Map<String, String> attributesToMap(MutableAttributeSet attributes) {
        if (attributes == null || attributes.getAttributeCount() == 0) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        Enumeration<?> names = attributes.getAttributeNames();
        while (names.hasMoreElements()) {
            Object name = names.nextElement();
            Object value = attributes.getAttribute(name);
            if (name != null && value != null) {
                values.put(name.toString().toLowerCase(Locale.ROOT), value.toString());
            }
        }
        return Collections.unmodifiableMap(values);
    }

    private static void appendWithSpace(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!builder.isEmpty() && !endsWithWhitespace(builder)) {
            builder.append(' ');
        }
        builder.append(text);
    }

    private static boolean endsWithWhitespace(StringBuilder builder) {
        return Character.isWhitespace(builder.charAt(builder.length() - 1));
    }

    private static String normalizeWhitespace(String value) {
        return value == null ? "" : value.trim().replaceAll("[ \\t\\x0B\\f\\r]+", " ").replaceAll("\\n\\s*", "\n");
    }
}
