package com.knowbase.ingest.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcelSerialDateNormalizerTest {

    @Test
    void convertsSerialInDateColumnAfterHeader() {
        String input = """
                序号\t类别\t工作内容\t计划完成时间\t责任人
                1\t海图项目\t开发接口\t45919\t杜鹏飞""";

        String out = ExcelSerialDateNormalizer.normalize(input);

        assertTrue(out.contains("1\t海图项目\t开发接口\t2025.9.19\t杜鹏飞"));
    }

    @Test
    void inheritsDateColumnsWhenSecondSectionOmitsHeader() {
        String input = """
                序号\t类别\t工作内容\t计划完成时间\t责任人
                1\t海图项目\t已完成任务\t45912\t杜鹏飞
                部门\t软件事业部\t\t部门负责人\t杜鹏飞\t\t\t更新日期\t2025.9.12
                9.15-9.19
                1\t海图项目\t待开展任务\t45919\t杜鹏飞""";

        String out = ExcelSerialDateNormalizer.normalize(input);

        assertTrue(out.contains("1\t海图项目\t待开展任务\t2025.9.19\t杜鹏飞"));
        assertTrue(out.contains("1\t海图项目\t已完成任务\t2025.9.12\t杜鹏飞"));
    }

    @Test
    void leavesNonDateColumnsUntouched() {
        String input = """
                产品编号\t数量\t计划完成时间
                SKU-100\t45919\t45919""";

        String out = ExcelSerialDateNormalizer.normalize(input);

        assertTrue(out.contains("SKU-100\t45919\t2025.9.19"));
    }

    @Test
    void leavesAlreadyFormattedDatesUntouched() {
        assertEquals("2025.9.19", ExcelSerialDateNormalizer.formatSerialCell("2025.9.19"));
        assertEquals("2025-09-19", ExcelSerialDateNormalizer.formatSerialCell("2025-09-19"));
    }

    @Test
    void convertsMarkdownTableDateCells() {
        String input = """
                | 序号 | 计划完成时间 | 责任人 |
                | --- | --- | --- |
                | 1 | 45919 | 杜鹏飞 |""";

        String out = ExcelSerialDateNormalizer.normalize(input);

        assertTrue(out.contains("| 1 | 2025.9.19 | 杜鹏飞 |"));
    }

    @Test
    void isIdempotent() {
        String once = """
                序号\t计划完成时间
                1\t45919""";
        String first = ExcelSerialDateNormalizer.normalize(once);
        String second = ExcelSerialDateNormalizer.normalize(first);
        assertEquals(first, second);
    }
}
