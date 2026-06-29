package com.knowbase.ingestion.office;

import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.ArrayList;
import java.util.List;

public final class DocxTableStructureExtractor {

    private DocxTableStructureExtractor() {
    }

    public record DocxCellModel(int columnIndex, String text, int rowSpan, int columnSpan) {
    }

    public record DocxRowModel(int rowIndex, List<DocxCellModel> cells, boolean headerRow) {
    }

    public record DocxTableModel(int tableIndex, List<DocxRowModel> rows, boolean floating) {
        public DocxTableModel(int tableIndex, List<DocxRowModel> rows) {
            this(tableIndex, rows, false);
        }
    }

    public static List<DocxTableModel> extractTables(List<XWPFTable> tables) {
        List<DocxTableModel> models = new ArrayList<>();
        for (int tableIndex = 0; tableIndex < tables.size(); tableIndex++) {
            XWPFTable table = tables.get(tableIndex);
            List<DocxRowModel> rows = new ArrayList<>();
            int rowIndex = 0;
            for (XWPFTableRow row : table.getRows()) {
                List<DocxCellModel> cells = new ArrayList<>();
                int cellIndex = 0;
                for (XWPFTableCell cell : row.getTableCells()) {
                    if (isVerticalMergeContinue(cell)) {
                        continue;
                    }
                    cells.add(new DocxCellModel(
                            cellIndex++,
                            cell.getText().trim(),
                            verticalMergeSpan(cell),
                            gridSpan(cell)
                    ));
                }
                if (cells.isEmpty()) {
                    continue;
                }
                boolean headerRow = rowIndex == 0 || cells.stream().anyMatch(cell -> isHeaderCell(cell.text()));
                rows.add(new DocxRowModel(rowIndex++, cells, headerRow));
            }
            models.add(new DocxTableModel(tableIndex, rows, isFloatingTable(table)));
        }
        return models;
    }

    private static boolean isFloatingTable(XWPFTable table) {
        try {
            if (table.getCTTbl().getTblPr() != null && table.getCTTbl().getTblPr().getTblpPr() != null) {
                return true;
            }
        } catch (RuntimeException ignored) {
            return false;
        }
        return false;
    }

    private static boolean isHeaderCell(String text) {
        return text != null && !text.isBlank() && text.length() <= 40;
    }

    private static int gridSpan(XWPFTableCell cell) {
        try {
            if (cell.getCTTc().getTcPr() != null && cell.getCTTc().getTcPr().getGridSpan() != null) {
                return Math.max(1, cell.getCTTc().getTcPr().getGridSpan().getVal().intValue());
            }
        } catch (RuntimeException ignored) {
            return 1;
        }
        return 1;
    }

    private static int verticalMergeSpan(XWPFTableCell cell) {
        try {
            if (cell.getCTTc().getTcPr() != null
                    && cell.getCTTc().getTcPr().getVMerge() != null
                    && cell.getCTTc().getTcPr().getVMerge().getVal() != null
                    && "restart".equalsIgnoreCase(String.valueOf(cell.getCTTc().getTcPr().getVMerge().getVal()))) {
                return 2;
            }
        } catch (RuntimeException ignored) {
            return 1;
        }
        return 1;
    }

    private static boolean isVerticalMergeContinue(XWPFTableCell cell) {
        try {
            if (cell.getCTTc().getTcPr() == null || cell.getCTTc().getTcPr().getVMerge() == null) {
                return false;
            }
            if (cell.getCTTc().getTcPr().getVMerge().getVal() == null) {
                return true;
            }
            return "continue".equalsIgnoreCase(String.valueOf(cell.getCTTc().getTcPr().getVMerge().getVal()));
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
