package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagEmployeeNameExtractorTest {

    private static final String CHUNK_HEADER = """
            周报3月

            星图深海软件事业部工作周报

            部门\t软件事业部\t\t姓名\t杜鹏飞\t\t\t更新日期\t2025.8.29
            """;

    private static final String CHUNK_ROWS = """
            1\t海图项目\t配合苏研院同事编写用户手册\t\t45896\t杜鹏飞\t配合完成用户手册编写\t\t已完成

            2\t海图项目\t优化开发综合服务板块\t\t45896\t杜鹏飞\t修复相关问题\t\t已完成

            3\t海图项目\t配合武经理为刘主任演示系统\t\t45898\t杜鹏飞\t系统演示与问题文档编辑\t\t已完成
            """;

    @Test
    void extractsSingleSubmitterFromWeeklyReportChunks() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        RagEmployeeNameExtractor.collectFromText(CHUNK_HEADER, names);
        RagEmployeeNameExtractor.collectFromText(CHUNK_ROWS, names);
        RagEmployeeNameExtractor.collectFromFileName("2025/杜鹏飞-周报（8.25-8.29）.xlsx", names);

        assertEquals(1, names.size());
        assertTrue(names.contains("杜鹏飞"));
    }

    @Test
    void doesNotTreatExcelDateRowsAsSeparateNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        RagEmployeeNameExtractor.collectFromText(CHUNK_ROWS, names);

        assertFalse(names.contains("45896"));
        assertFalse(names.stream().anyMatch(n -> n.contains("完成")));
        assertEquals(1, names.size());
    }

    @Test
    void extractsFromFileName() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        RagEmployeeNameExtractor.collectFromFileName("2025/杜鹏飞-周报（9.8-9.12）.xlsx", names);
        assertTrue(names.contains("杜鹏飞"));
    }
}
