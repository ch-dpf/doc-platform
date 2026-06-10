package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagWeeklyReportSummarySupportTest {

    @Test
    void detectsReferenceEcho() {
        assertTrue(RagWeeklyReportSummarySupport.looksLikeReferenceEcho(
                "[1] fileName=2025/a.xlsx docId=abc chunk=0 score=0.5\ncontent"));
        assertFalse(RagWeeklyReportSummarySupport.looksLikeReferenceEcho("根据参考资料，主要工作包括…"));
    }

    @Test
    void buildsSummaryFromWorkRows() {
        String rows = "1\t海图项目\t开发优化综合服务模块的后台接口\t\t45877\t杜鹏飞\t已完成\t\t已完成\n";
        SearchHit hit = new SearchHit(
                UUID.randomUUID(), UUID.randomUUID(), "demo", 1, 3, rows, 0.58);

        var answer = RagWeeklyReportSummarySupport.tryRuleBasedAnswer(
                "周报主要内容汇总？", List.of(hit), Map.of());

        assertTrue(answer.isPresent());
        assertTrue(answer.get().contains("开发优化综合服务模块"));
        assertFalse(answer.get().contains("fileName="));
        assertTrue(answer.get().contains("杜鹏飞"));
    }
}
