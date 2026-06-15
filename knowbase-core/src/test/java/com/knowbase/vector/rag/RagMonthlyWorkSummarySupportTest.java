package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagMonthlyWorkSummarySupportTest {

    @Test
    void weekScopedQuestionUsesRulePath() {
        assertTrue(RagMonthlyWorkSummarySupport.isMonthlyCompletedWorkQuestion(
                "杜鹏飞在2025年9月第一周完成了哪些工作？"));
    }

    @Test
    void ruleAnswerListsThreeCompletedItemsForFirstWeek() {
        UUID docId = UUID.randomUUID();
        SearchHit chunk0 = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                1\t海图项目\t完成项目部署文档编写，补充用户培训ppt，并为下周用户培训做准备\t\t2025.9.4\t杜鹏飞\t完成文档编写\t\t已完成
                2\t海图项目\t优化开发出所测试提出的部分问题\t\t2025.9.5\t杜鹏飞\t修复相关问题\t\t已完成
                """,
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        SearchHit chunk1 = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                1,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                3\t海图项目\t配合苏研院同事完成电子海图全球矢量瓦片切片工作\t\t2025.9.6\t杜鹏飞\t完成相关任务\t\t已完成
                """,
                0.8,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        String fileName = "2025/杜鹏飞-周报（9.1-9.6）.xlsx";
        var answer = RagMonthlyWorkSummarySupport.tryRuleBasedAnswer(
                "杜鹏飞在2025年9月第一周完成了哪些工作？",
                List.of(chunk0, chunk1),
                Map.of(docId, fileName),
                List.of());
        assertTrue(answer.isPresent());
        String text = answer.get();
        assertTrue(text.contains("9月第1周"));
        assertTrue(text.contains("按项目分组"));
        assertTrue(text.contains("【海图项目】（3 项）"));
        assertEquals(3, text.lines().filter(line -> line.strip().matches("\\d+\\. .*")).count());
        assertTrue(text.contains("瓦片切片"));
        assertTrue(text.contains("共计 3 项工作，涉及 1 个项目。"));
    }

    @Test
    void ruleAnswerGroupsItemsByProject() {
        UUID docId = UUID.randomUUID();
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                1\t海图项目\t完成海图部署文档\t\t2025.9.4\t杜鹏飞\t完成\t\t已完成
                2\tFB项目\t搭建项目框架\t\t2025.9.5\t杜鹏飞\t完成\t\t已完成
                3\t海图项目\t优化测试问题\t\t2025.9.6\t杜鹏飞\t完成\t\t已完成
                """,
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        var answer = RagMonthlyWorkSummarySupport.tryRuleBasedAnswer(
                "杜鹏飞在2025年9月第一周完成了哪些工作？",
                List.of(hit),
                Map.of(docId, "2025/杜鹏飞-周报（9.1-9.6）.xlsx"),
                List.of());
        assertTrue(answer.isPresent());
        String text = answer.get();
        int haiTuIndex = text.indexOf("【海图项目】（2 项）");
        int fbIndex = text.indexOf("【上海fb项目】（1 项）");
        assertTrue(haiTuIndex >= 0);
        assertTrue(fbIndex >= 0);
        assertTrue(haiTuIndex < fbIndex, "larger project group should appear first");
        assertTrue(text.contains("共计 3 项工作，涉及 2 个项目。"));
    }

    @Test
    void ruleAnswerMergesShanghaiFbFamilyProjects() {
        UUID docId = UUID.randomUUID();
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                1\tFB项目\t搭建项目框架\t\t2025.9.4\t杜鹏飞\t完成\t\t已完成
                2\t上海浮标项目\t开发浮标接口\t\t2025.9.5\t杜鹏飞\t完成\t\t已完成
                3\t上海fb项目\t修复系统问题\t\t2025.9.6\t杜鹏飞\t完成\t\t已完成
                """,
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-06",
                "9",
                "杜鹏飞",
                "工作周报",
                "true");
        var answer = RagMonthlyWorkSummarySupport.tryRuleBasedAnswer(
                "杜鹏飞2025年9月都完成了哪些工作？",
                List.of(hit),
                Map.of(docId, "2025/杜鹏飞-周报（9.1-9.6）.xlsx"),
                List.of());
        assertTrue(answer.isPresent());
        String text = answer.get();
        assertTrue(text.contains("【上海fb项目】（3 项）"));
        assertTrue(text.contains("共计 3 项工作，涉及 1 个项目。"));
    }
}
