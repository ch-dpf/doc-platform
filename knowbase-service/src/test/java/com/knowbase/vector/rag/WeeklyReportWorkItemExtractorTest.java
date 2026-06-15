package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyReportWorkItemExtractorTest {

    private static final String HEADER = """
            周报3月

            星图深海软件事业部工作周报

            部门\t软件事业部\t\t姓名\t杜鹏飞\t\t\t更新日期\t2025.8.29

            2025年8月25日--8月29日

            序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明
            """;

    private static final String ROWS = """
            1\t海图项目\t配合苏研院同事编写用户手册\t\t45896\t杜鹏飞\t配合完成用户手册编写\t\t已完成

            2\t海图项目\t优化开发综合服务板块\t\t45896\t杜鹏飞\t修复相关问题\t\t已完成

            3\t海图项目\t配合武经理为刘主任演示系统\t\t45898\t杜鹏飞\t系统演示与问题文档编辑\t\t已完成
            """;

    @Test
    void detectsHeaderOnlyChunk() {
        assertTrue(WeeklyReportWorkItemExtractor.isHeaderOnlyChunk(HEADER));
        assertFalse(WeeklyReportWorkItemExtractor.isHeaderOnlyChunk(ROWS));
    }

    @Test
    void extractsAndDedupesWorkItems() {
        UUID docId = UUID.randomUUID();
        SearchHit headerHit = new SearchHit(UUID.randomUUID(), docId, "demo", 1, 0, HEADER, 0.56);
        SearchHit rowHit = new SearchHit(UUID.randomUUID(), docId, "demo", 1, 2, ROWS, 0.68);
        SearchHit dupHit = new SearchHit(UUID.randomUUID(), docId, "demo", 1, 3, ROWS, 0.65);

        var items = WeeklyReportWorkItemExtractor.extract(List.of(headerHit, rowHit, dupHit), Map.of());

        assertEquals(3, items.size());
        assertTrue(items.get(0).content().contains("配合苏研院"));
        assertEquals("海图项目", items.get(0).project());
    }

    @Test
    void extractCompletedKeepsOnlyCompletedRows() {
        UUID docId = UUID.randomUUID();
        String mixed = """
                1\t海图项目\t完成项目部署文档编写，补充用户培训ppt\t\t2025.9.4\t杜鹏飞\t完成文档编写\t\t已完成

                2\t海图项目\t下周待开展任务\t\t2025.9.5\t杜鹏飞\t待开展\t\t待开展
                """;
        SearchHit hit = new SearchHit(UUID.randomUUID(), docId, "demo", 1, 0, mixed, 0.8);
        var items = WeeklyReportWorkItemExtractor.extractCompleted(List.of(hit), Map.of());
        assertEquals(1, items.size());
        assertTrue(items.getFirst().content().contains("部署文档"));
    }
}
