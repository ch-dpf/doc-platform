package com.knowbase.ingest.parse;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelStructuredExtractorTest {

    private static DocumentParseOptions structuredOptions() {
        return new DocumentParseOptions(
                false,
                "chi_sim+eng",
                true,
                "zh-CN",
                null,
                TableExtractionMode.STRUCTURED,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);
    }

    @Test
    void structuredModeProducesMarkdownTableFromMinimalXlsx() throws Exception {
        byte[] xlsx = minimalXlsx(new String[][] {
            {"项目", "负责人", "状态"},
            {"海图项目", "杜鹏飞", "已完成"},
            {"遥感项目", "张三", "进行中"}
        });

        String result = ExcelStructuredExtractor.extract(
                xlsx,
                "sample.xlsx",
                structuredOptions(),
                request -> "<html><body><p>no tables</p></body></html>");

        assertTrue(result.contains("| 项目 | 负责人 | 状态 |"));
        assertTrue(result.contains("| 海图项目 | 杜鹏飞 | 已完成 |"));
        assertTrue(result.contains("| 遥感项目 | 张三 | 进行中 |"));
    }

    @Test
    void prefersTikaHtmlTablesWhenPresent() {
        String html =
                """
                <html><body>
                <table>
                  <tr><th>Name</th><th>Score</th></tr>
                  <tr><td>Alice</td><td>95</td></tr>
                </table>
                </body></html>
                """;

        String result = ExcelStructuredExtractor.extract(
                "not-used".getBytes(),
                "sample.xlsx",
                structuredOptions(),
                request -> html);

        assertTrue(result.contains("| Name | Score |"));
        assertTrue(result.contains("| Alice | 95 |"));
    }

    @Test
    void excelMimeSupportDetectsSpreadsheetTypes() {
        assertTrue(ExcelMimeSupport.isExcel(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "report.xlsx"));
        assertTrue(ExcelMimeSupport.isExcel(null, "weekly.xls"));
        assertFalse(ExcelMimeSupport.isExcel("application/pdf", "report.pdf"));
    }

    private static byte[] minimalXlsx(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
