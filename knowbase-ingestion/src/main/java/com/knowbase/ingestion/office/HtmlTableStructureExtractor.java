package com.knowbase.ingestion.office;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public final class HtmlTableStructureExtractor {

    private HtmlTableStructureExtractor() {
    }

    public record HtmlCellModel(int columnIndex, String text, int rowSpan, int columnSpan, boolean headerCell) {
    }

    public record HtmlRowModel(int rowIndex, List<HtmlCellModel> cells, boolean headerRow) {
    }

    public record HtmlTableModel(
            int tableIndex,
            List<HtmlRowModel> rows,
            boolean nested,
            Integer parentTableRegionId,
            boolean floating
    ) {
        public HtmlTableModel(int tableIndex, List<HtmlRowModel> rows) {
            this(tableIndex, rows, false, null, false);
        }

        public HtmlTableModel(int tableIndex, List<HtmlRowModel> rows, boolean nested) {
            this(tableIndex, rows, nested, null, false);
        }
    }

    public static List<HtmlTableModel> extract(Document document) {
        Elements tables = document.select("table:not(table table)");
        List<HtmlTableModel> models = new ArrayList<>();
        int tableIndex = 0;
        for (Element table : tables) {
            models.addAll(extractTableTree(table, tableIndex));
            tableIndex += countTablesInTree(table);
        }
        return models;
    }

    /**
     * Parses a table and any nested {@code <table>} elements inside its cells as separate regions.
     */
    public static List<HtmlTableModel> extractTableTree(Element table, int startIndex) {
        List<HtmlTableModel> models = new ArrayList<>();
        models.add(parseTable(table, startIndex, false, null));
        int nextIndex = startIndex + 1;
        for (Element nested : table.select("table")) {
            if (nested == table) {
                continue;
            }
            models.add(parseTable(nested, nextIndex++, true, startIndex));
        }
        return List.copyOf(models);
    }

    public static int countTablesInTree(Element table) {
        return table.select("table").size();
    }

    public static HtmlTableModel parseTable(Element table, int tableIndex, boolean nested) {
        return parseTable(table, tableIndex, nested, null);
    }

    public static HtmlTableModel parseTable(
            Element table,
            int tableIndex,
            boolean nested,
            Integer parentTableRegionId
    ) {
        List<HtmlRowModel> rows = new ArrayList<>();
        Elements tableRows = table.select("> tbody > tr, > tr");
        if (tableRows.isEmpty()) {
            tableRows = table.select("> tr");
        }
        if (tableRows.isEmpty()) {
            tableRows = table.select("tr");
        }
        for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
            Element row = tableRows.get(rowIndex);
            if (row.parent() != null && row.parent().closest("table") != table) {
                continue;
            }
            List<HtmlCellModel> cells = new ArrayList<>();
            Elements cellElements = row.select("> th, > td");
            for (int cellIndex = 0; cellIndex < cellElements.size(); cellIndex++) {
                Element cell = cellElements.get(cellIndex);
                boolean headerCell = cell.tagName().equalsIgnoreCase("th");
                cells.add(new HtmlCellModel(
                        cellIndex,
                        cellTextExcludingNestedTables(cell),
                        parseInt(cell.attr("rowspan"), 1),
                        parseInt(cell.attr("colspan"), 1),
                        headerCell
                ));
            }
            if (cells.isEmpty()) {
                continue;
            }
            boolean headerRow = row.select("> th").size() > 0 || rowIndex == 0;
            rows.add(new HtmlRowModel(rowIndex, cells, headerRow));
        }
        return new HtmlTableModel(
                tableIndex,
                rows,
                nested,
                parentTableRegionId,
                isFloatingTable(table)
        );
    }

    static String cellTextExcludingNestedTables(Element cell) {
        if (cell == null) {
            return "";
        }
        if (cell.select("table").isEmpty()) {
            String own = cell.ownText().trim();
            return own.isBlank() ? cell.text().trim() : own;
        }
        Element clone = cell.clone();
        clone.select("table").remove();
        String text = clone.text().trim();
        if (!text.isBlank()) {
            return text;
        }
        return cell.ownText().trim();
    }

    static boolean isFloatingTable(Element table) {
        if (table == null) {
            return false;
        }
        String style = table.attr("style").toLowerCase();
        if (style.contains("float:") && !style.contains("float:none")) {
            return true;
        }
        String className = table.className().toLowerCase();
        return className.contains("float");
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
