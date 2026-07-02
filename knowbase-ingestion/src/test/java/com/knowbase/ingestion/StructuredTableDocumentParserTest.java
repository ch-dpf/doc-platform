package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredTableDocumentParserTest {

    @Test
    void parsesCsvAsTableRowBlocks() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream("""
                        Region,Q1,Q2
                        APAC,10,12
                        EMEA,8,9
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of("filename", "sales.csv")
        ));

        assertEquals(ContentFamily.STRUCTURED_TABLE, parsed.contentFamily());
        assertEquals("table-deep", parsed.metadata().get("parser"));
        assertEquals("adaptive", parsed.metadata().get("rowSerialization"));
        assertEquals(3, parsed.metadata().get("rowGroupCount"));
        assertTrue(parsed.structureAware());
        assertEquals(3, parsed.blocks().size());
        assertTrue(parsed.blocks().stream().allMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().get(1).content().contains("Region: APAC"));
        assertTrue(parsed.blocks().get(1).content().contains("Q1: 10"));
        assertEquals("DATA", parsed.blocks().get(1).metadata().get("rowRole"));
        assertEquals("1", parsed.blocks().get(1).metadata().get("rowRange"));
        assertEquals("0:2", parsed.blocks().get(1).metadata().get("columnRange"));
        assertEquals(List.of("Region", "Q1", "Q2"), parsed.blocks().get(1).metadata().get("headerPath"));
        assertTrue(parsed.blocks().get(1).metadata().containsKey("cellCoordinates"));
    }

    @Test
    void tableRowsBecomeSemanticSegments() {
        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.csv",
                "sales.csv",
                "text/csv",
                new ByteArrayInputStream("""
                        Region,Q1
                        APAC,10
                        EMEA,8
                        """.getBytes(StandardCharsets.UTF_8)),
                Map.of()
        ));
        DocumentProfile tableProfile = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_table",
                ContentFamily.STRUCTURED_TABLE,
                "table-deep",
                "table_row_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        List<StructuralSegment> segments = new StructureSegmenter().segment(parsed, tableProfile);

        assertEquals(3, segments.size());
        assertTrue(segments.stream().allMatch(segment -> "table_row".equals(segment.boundaryType())));
    }

    @Test
    void parsesSpreadsheetWithMergedCellMetadata() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet ref = workbook.createSheet("Ref");
            ref.createRow(0).createCell(0).setCellValue(5);
            Sheet sheet = workbook.createSheet("Sales");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Region");
            header.createCell(1).setCellValue("Quarter");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 2));
            Row subHeader = sheet.createRow(1);
            subHeader.createCell(0).setCellValue("Region");
            subHeader.createCell(1).setCellValue("Q1");
            subHeader.createCell(2).setCellValue("Q2");
            Row data = sheet.createRow(2);
            data.createCell(0).setCellValue("APAC");
            data.createCell(1).setCellFormula("'Ref'!A1+5");
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2));
            sheet.setColumnHidden(2, true);
            data.setZeroHeight(true);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://sales.xlsx",
                "sales.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(workbookBytes),
                Map.of()
        ));

        assertEquals(4, parsed.blocks().size());
        List<StructuralBlock> salesRows = parsed.blocks().stream()
                .filter(block -> "Sales".equals(block.metadata().get("sheetName")))
                .toList();
        assertEquals(3, salesRows.size());
        StructuralBlock row = salesRows.get(2);
        assertEquals("Sales", row.metadata().get("sheetName"));
        assertEquals(2, row.metadata().get("rowStart"));
        assertEquals(2, row.metadata().get("columnEnd"));
        assertEquals(true, row.metadata().get("hiddenRow"));
        assertEquals(List.of(2), row.metadata().get("hiddenColumns"));
        assertEquals(List.of("Ref"), row.metadata().get("crossSheetReferences"));
        assertTrue(row.content().contains("Region: APAC") || row.content().contains("A: APAC"));
        assertTrue((Boolean) row.metadata().get("hasMergedCells"));
        assertTrue(row.metadata().containsKey("mergedCells"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) row.metadata().get("cellCoordinates");
        assertTrue(cells.stream().anyMatch(cell -> Boolean.TRUE.equals(cell.get("merged"))
                && "R3C2:R3C3".equals(cell.get("mergedRange"))));
        assertTrue(cells.stream().anyMatch(cell -> {
            Object headerPath = cell.get("headerPath");
            return headerPath instanceof List<?> list
                    && (List.of("B").equals(list) || List.of("Q1").equals(list) || List.of("Quarter").equals(list));
        }));
    }

    @Test
    void treatsMergedSectionTitleRowAsLayoutNotData() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("周报3月");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("序号");
            header.createCell(1).setCellValue("项目名称");
            header.createCell(2).setCellValue("工作内容");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("1");
            data.createCell(1).setCellValue("FB项目");
            data.createCell(2).setCellValue("配合三方测试");
            Row section = sheet.createRow(2);
            section.createCell(0).setCellValue("星图深海软件技术部下周工作计划");
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly-section.xlsx",
                "weekly-section.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(workbookBytes),
                Map.of()
        ));

        StructuralBlock sectionBlock = parsed.blocks().stream()
                .filter(block -> block.content() != null && block.content().contains("下周工作计划"))
                .findFirst()
                .orElseThrow();
        assertEquals("LAYOUT", sectionBlock.metadata().get("rowRole"));
        assertTrue(sectionBlock.content().contains("章节: 星图深海软件技术部下周工作计划"));
        assertFalse(sectionBlock.content().contains("序号: 星图深海"));
        assertFalse(Boolean.TRUE.equals(sectionBlock.metadata().get("indexableHint")));
    }

    @Test
    void preservesFormStyleMetadataRowsWithoutHeaderConsumption() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("周报3月");
            Row metadata = sheet.createRow(0);
            metadata.createCell(0).setCellValue("部门");
            metadata.createCell(1).setCellValue("软件技术部");
            metadata.createCell(2).setCellValue("姓名");
            metadata.createCell(3).setCellValue("杜鹏飞");
            metadata.createCell(4).setCellValue("汇报周期");
            metadata.createCell(5).setCellValue("2026年5月06日--5月09日");
            Row labels = sheet.createRow(1);
            labels.createCell(0).setCellValue("项目");
            labels.createCell(1).setCellValue("工作内容");
            labels.createCell(2).setCellValue("完成情况");
            Row task = sheet.createRow(2);
            task.createCell(0).setCellValue("FB项目");
            task.createCell(1).setCellValue("配合三方测试");
            task.createCell(2).setCellValue("已完成");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://weekly.xlsx",
                "weekly.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(workbookBytes),
                Map.of()
        ));

        assertEquals(3, parsed.blocks().size());
        assertTrue(parsed.blocks().getFirst().content().contains("部门: 软件技术部"));
        assertTrue(parsed.blocks().getFirst().content().contains("姓名: 杜鹏飞"));
        assertEquals("FORM_KV", parsed.blocks().getFirst().metadata().get("rowRole"));
        assertTrue(parsed.blocks().get(2).content().contains("项目: FB项目"));
        assertTrue(parsed.blocks().get(2).content().contains("完成情况: 已完成"));
        assertEquals("DATA", parsed.blocks().get(2).metadata().get("rowRole"));
        assertTrue(parsed.blocks().stream().noneMatch(block -> block.content().contains("部门=FB项目")));
    }

    @Test
    void collapsesHorizontalMergedCellDuplicates() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Title");
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Weekly Report Title");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://title.xlsx",
                "title.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(workbookBytes),
                Map.of()
        ));

        String row = parsed.blocks().getFirst().content();
        assertTrue(row.contains("Weekly Report Title"));
        assertTrue(row.contains("标题: Weekly Report Title") || row.contains("A: Weekly Report Title"));
    }

    @Test
    void formatsExcelDateCellsAsIsoDates() throws Exception {
        byte[] workbookBytes;
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Dates");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("汇报周期");
            org.apache.poi.ss.usermodel.Cell dateCell = row.createCell(1);
            dateCell.setCellValue(java.time.LocalDate.of(2026, 5, 9));
            org.apache.poi.ss.usermodel.CellStyle style = workbook.createCellStyle();
            style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd"));
            dateCell.setCellStyle(style);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        ParsedDocument parsed = new StructuredTableDocumentParser().parse(new DocumentSource(
                "memory://dates.xlsx",
                "dates.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(workbookBytes),
                Map.of()
        ));

        assertTrue(parsed.blocks().getFirst().content().contains("2026-05-09"));
    }
}
