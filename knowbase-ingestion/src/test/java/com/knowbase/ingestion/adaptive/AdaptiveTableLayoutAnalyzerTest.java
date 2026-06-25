package com.knowbase.ingestion.adaptive;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveTableLayoutAnalyzerTest {

    @Test
    void formKvWithDateRangeIsNotSeparator() {
        List<String> row = List.of(
                "部门", "软件技术部", "姓名", "杜鹏飞", "汇报周期", "2026年5月06日--5月09日"
        );
        TableRowRole role = AdaptiveTableLayoutAnalyzer.detectRole(row, List.of("项目", "工作内容"), null, 6);
        assertEquals(TableRowRole.FORM_KV, role);
    }

    @Test
    void detectsCsvHeaderRow() {
        List<String> header = List.of("Region", "Q1", "Q2");
        List<String> data = List.of("APAC", "10", "12");
        assertEquals(TableRowRole.HEADER, AdaptiveTableLayoutAnalyzer.detectRole(header, data, null, 3));
    }

    @Test
    void detectsFormKvMetadataRow() {
        List<String> row = List.of("部门", "软件技术部", "", "姓名", "杜鹏飞", "", "更新日期", "2026.5.09");
        TableRowRole role = AdaptiveTableLayoutAnalyzer.detectRole(row, List.of("序号", "项目", "内容"), null, 6);
        assertEquals(TableRowRole.FORM_KV, role);
    }

    @Test
    void detectsHeaderAndDataSequence() {
        List<String> header = List.of("序号", "项目名称", "工作内容", "", "责任人", "", "执行情况", "");
        List<String> data = List.of("1", "FB项目", "配合三方测试", "", "杜鹏飞", "", "已完成", "");
        assertEquals(TableRowRole.HEADER, AdaptiveTableLayoutAnalyzer.detectRole(header, data, null, 5));
        String[] active = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(header);
        assertEquals(TableRowRole.DATA, AdaptiveTableLayoutAnalyzer.detectRole(data, List.of(), active, 4));
    }

    @Test
    void serializesTabularDataWithHeaderNames() {
        String[] headers = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(
                List.of("Region", "Q1", "Q2", "", "", "", "", "", "", "", "", "")
        );
        String text = AdaptiveTableTextSerializer.serialize(
                TableRowRole.DATA,
                "CSV",
                List.of("APAC", "10", "12", "", "", "", "", "", "", "", "", ""),
                headers
        );
        assertTrue(text.contains("Region: APAC"));
        assertTrue(text.contains("Q1: 10"));
        assertTrue(text.contains("Q2: 12"));
    }

    @Test
    void detectsWideUniformRowAsLayoutWithoutDomainKeywords() {
        String title = "Regional Sales Summary Q2 2026";
        List<String> row = java.util.Collections.nCopies(8, title);
        String[] headers = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(
                List.of("ID", "Project", "Description", "Owner", "Status", "Due")
        );
        TableRowRole role = AdaptiveTableLayoutAnalyzer.detectRole(
                row,
                List.of("ID", "Project", "Description"),
                headers,
                8,
                RowLayoutContext.of(8, 6)
        );
        assertEquals(TableRowRole.LAYOUT, role);
    }

    @Test
    void doesNotTreatDistinctTabularRowAsLayout() {
        List<String> data = List.of("1", "Alpha", "Build feature", "Alice", "Done", "2026-06-01");
        String[] headers = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(
                List.of("ID", "Project", "Description", "Owner", "Status", "Due")
        );
        TableRowRole role = AdaptiveTableLayoutAnalyzer.detectRole(
                data,
                List.of(),
                headers,
                6,
                RowLayoutContext.of(6, 0)
        );
        assertEquals(TableRowRole.DATA, role);
    }

    @Test
    void detectsMergedSectionTitleAsLayout() {
        String title = "星图深海软件技术部下周工作计划";
        List<String> row = java.util.Collections.nCopies(12, title);
        String[] headers = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(
                List.of("序号", "项目名称", "工作内容", "工作内容", "计划完成时间", "责任人")
        );
        TableRowRole role = AdaptiveTableLayoutAnalyzer.detectRole(
                row,
                List.of("序号", "项目名称", "工作内容"),
                headers,
                12,
                RowLayoutContext.of(12, 6)
        );
        assertEquals(TableRowRole.LAYOUT, role);
    }

    @Test
    void serializesMergedSectionTitleAsSectionNotData() {
        String title = "Regional Sales Summary Q2 2026";
        List<String> row = java.util.Collections.nCopies(6, title);
        String[] headers = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(
                List.of("ID", "Project", "Description", "Description", "Due", "Owner")
        );
        String text = AdaptiveTableTextSerializer.serialize(TableRowRole.LAYOUT, "Sheet1", row, headers);
        assertTrue(text.contains("章节: " + title));
        assertFalse(text.contains("ID: " + title));
        assertFalse(text.contains("Project: " + title));
    }

    @Test
    void dataSerializerFallsBackForUniformValues() {
        String title = "Regional Sales Summary Q2 2026";
        List<String> row = java.util.Collections.nCopies(6, title);
        String[] headers = AdaptiveTableLayoutAnalyzer.buildColumnHeaders(
                List.of("ID", "Project", "Description", "Description", "Due", "Owner")
        );
        String text = AdaptiveTableTextSerializer.serialize(TableRowRole.DATA, "Sheet1", row, headers);
        assertTrue(text.contains("章节: " + title));
        assertFalse(text.contains("ID: " + title));
    }

    @Test
    void layoutRowsAreNotIndexableByDefault() {
        assertFalse(AdaptiveTableTextSerializer.defaultIndexable(TableRowRole.LAYOUT));
        assertFalse(AdaptiveTableTextSerializer.defaultIndexable(TableRowRole.HEADER));
        assertTrue(AdaptiveTableTextSerializer.defaultIndexable(TableRowRole.DATA));
    }
}
