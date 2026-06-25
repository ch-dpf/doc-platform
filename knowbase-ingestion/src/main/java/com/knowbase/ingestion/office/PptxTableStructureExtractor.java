package com.knowbase.ingestion.office;

import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;

import java.util.ArrayList;
import java.util.List;

public final class PptxTableStructureExtractor {

    private PptxTableStructureExtractor() {
    }

    public record PptxCellModel(int columnIndex, String text) {
    }

    public record PptxRowModel(int rowIndex, List<PptxCellModel> cells, boolean headerRow) {
    }

    public record PptxTableModel(int tableIndex, List<PptxRowModel> rows) {
    }

    public static PptxTableModel extract(XSLFTable table, int tableIndex) {
        List<PptxRowModel> rows = new ArrayList<>();
        List<XSLFTableRow> tableRows = table.getRows();
        for (int rowIndex = 0; rowIndex < tableRows.size(); rowIndex++) {
            XSLFTableRow row = tableRows.get(rowIndex);
            List<PptxCellModel> cells = new ArrayList<>();
            List<XSLFTableCell> tableCells = row.getCells();
            for (int cellIndex = 0; cellIndex < tableCells.size(); cellIndex++) {
                cells.add(new PptxCellModel(cellIndex, tableCells.get(cellIndex).getText().trim()));
            }
            if (cells.isEmpty()) {
                continue;
            }
            boolean headerRow = rowIndex == 0;
            rows.add(new PptxRowModel(rowIndex, cells, headerRow));
        }
        return new PptxTableModel(tableIndex, rows);
    }
}
