package com.knowbase.ingest.service;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.parse.DocumentParseOptions;
import com.knowbase.ingest.parse.FormulaExtractionMode;
import com.knowbase.ingest.parse.ImageExtractionMode;
import com.knowbase.ingest.parse.TableExtractionMode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParseServiceExcelTest {

    private DocumentParseService parseService;

    @BeforeEach
    void setUp() {
        OcrProperties ocrProperties = new OcrProperties();
        parseService = new DocumentParseService(new DocumentOcrService(ocrProperties), ocrProperties);
    }

    @Test
    void structuredExcelProducesMarkdownTables() throws Exception {
        byte[] xlsx = minimalXlsx(new String[][] {
            {"序号", "类别", "工作内容", "责任人"},
            {"1", "海图项目", "调整生产环境影像服务", "杜鹏飞"}
        });

        DocumentParseOptions options = new DocumentParseOptions(
                false,
                "chi_sim+eng",
                true,
                "zh-CN",
                null,
                TableExtractionMode.STRUCTURED,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);

        String result = parseService.extractText(
                xlsx,
                "2025/杜鹏飞-周报（8.25-8.29）.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                options);

        assertTrue(result.contains("| 序号 | 类别 | 工作内容 | 责任人 |"));
        assertTrue(result.contains("| 1 | 海图项目 | 调整生产环境影像服务 | 杜鹏飞 |"));
    }

    @Test
    void textOnlyExcelKeepsTabSeparatedPlainTextForWeeklyReportParity() throws Exception {
        byte[] xlsx = minimalXlsx(new String[][] {
            {"序号", "类别", "工作内容", "责任人"},
            {"1", "海图项目", "调整生产环境影像服务", "杜鹏飞"}
        });

        DocumentParseOptions options = new DocumentParseOptions(
                false,
                "chi_sim+eng",
                true,
                "zh-CN",
                null,
                TableExtractionMode.TEXT_ONLY,
                ImageExtractionMode.SKIP,
                FormulaExtractionMode.SKIP);

        String result = parseService.extractText(
                xlsx,
                "2025/杜鹏飞-周报（8.25-8.29）.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                options);

        assertTrue(result.contains("序号"));
        assertTrue(result.contains("杜鹏飞"));
        assertFalse(result.contains("| 序号 |"));
        assertFalse(result.contains(" --- |"));
    }

    private static byte[] minimalXlsx(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("周报");
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
