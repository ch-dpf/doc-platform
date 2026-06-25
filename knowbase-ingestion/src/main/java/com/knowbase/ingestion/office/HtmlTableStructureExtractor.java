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

    public record HtmlTableModel(int tableIndex, List<HtmlRowModel> rows, boolean nested) {
        public HtmlTableModel(int tableIndex, List<HtmlRowModel> rows) {
            this(tableIndex, rows, false);
        }
    }

    public static List<HtmlTableModel> extract(Document document) {
        Elements tables = document.select("table:not(table table)");
        List<HtmlTableModel> models = new ArrayList<>();
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            models.add(parseTable(tables.get(tableIndex), tableIndex, false));
        }
        return models;
    }

    public static HtmlTableModel parseTable(Element table, int tableIndex, boolean nested) {
        List<HtmlRowModel> rows = new ArrayList<>();
        Elements tableRows = table.select("> tbody > tr, > tr");
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
                        cell.ownText().trim().isBlank() ? cell.text().trim() : cell.ownText().trim(),
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
        return new HtmlTableModel(tableIndex, rows, nested);
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
