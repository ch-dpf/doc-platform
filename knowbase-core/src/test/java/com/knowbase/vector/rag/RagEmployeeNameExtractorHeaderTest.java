package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEmployeeNameExtractorHeaderTest {

    private static final String HEADER = """
            部门\t软件事业部\t\t姓名\t杜鹏飞\t\t\t更新日期\t2025.8.29

            2025年8月25日--8月29日

            序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明""";

    @Test
    void headerLineDoesNotExtractUpdateDateLabel() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        RagEmployeeNameExtractor.collectFromText(HEADER, names);
        assertEquals(1, names.size());
        assertTrue(names.contains("杜鹏飞"));
        assertFalse(names.contains("更新日期"));
        assertFalse(names.contains("执行要求"));
    }

    @Test
    void tableHeaderRowDoesNotExtractColumnLabels() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        String headerRow = "序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明";
        RagEmployeeNameExtractor.collectFromText(headerRow, names);
        assertTrue(names.isEmpty());
    }
}
