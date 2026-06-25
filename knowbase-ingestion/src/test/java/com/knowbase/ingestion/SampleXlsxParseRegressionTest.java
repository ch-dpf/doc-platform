package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleXlsxParseRegressionTest {

    @Test
    void xlsxProducesAdaptiveTableRowsWithSheetMetadata() throws Exception {
        byte[] bytes = buildMetricsWorkbook();
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://metrics.xlsx",
                "metrics.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertNotNull(parsed.metadata().get("parseConfidence"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "DATA".equals(block.metadata().get("rowRole"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "张三".equals(block.content()) || block.content().contains("张三")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "Metrics".equals(block.metadata().get("sheetName"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
        assertEquals("table-deep", parsed.metadata().get("parser"));
    }

    private static byte[] buildMetricsWorkbook() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Metrics");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("部门");
            header.createCell(2).setCellValue("得分");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("张三");
            data.createCell(1).setCellValue("研发");
            data.createCell(2).setCellValue(95);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
