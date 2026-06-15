package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagAnswerGuardTest {

    @Test
    void detectsDeadlineQuestion() {
        assertTrue(RagQuestionAnalyzer.isDeadlineQuestion("2025年周报截止时间？"));
        assertFalse(RagQuestionAnalyzer.isDeadlineQuestion("2025年8月周报主要内容"));
    }

    @Test
    void weeklyReportPeriodIsNotExplicitDeadline() {
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "2025年8月25日--8月29日 杜鹏飞 工作周报\n本周完成…",
                0.9);
        assertFalse(RagAnswerGuard.sourcesMentionExplicitDeadline(List.of(hit)));
    }

    @Test
    void explicitDeadlineInSourceIsRecognized() {
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "各部门周报须于每周五18:00前提交。",
                0.9);
        assertTrue(RagAnswerGuard.sourcesMentionExplicitDeadline(List.of(hit)));
    }

    @Test
    void rejectsHedgingGuessAnswer() {
        String bad = "虽然没有明确的截止时间，但据 [2] 看，2025年8月25日--8月29日是杜鹏飞的周报截止。";
        SearchHit hit = new SearchHit(
                UUID.randomUUID(), UUID.randomUUID(), "demo", 1, 0,
                "2025年8月25日--8月29日 杜鹏飞 工作周报", 0.9);
        String result = RagAnswerGuard.enforceGrounding(bad, "2025年周报截止时间？", List.of(hit));
        assertEquals(RagAnswerTemplates.NO_EXPLICIT_DEADLINE, result);
    }

    @Test
    void overridesBogusEmployeeExistenceAnswer() {
        String bad = "存在多个不同姓名：杜鹏飞、45969杜鹏飞完成相关任务、45982杜鹏飞完成开发任务";
        SearchHit hit = new SearchHit(
                UUID.randomUUID(), UUID.randomUUID(), "demo", 1, 0,
                "1\t海图项目\t任务\t\t45969\t杜鹏飞\t完成相关任务\t\t已完成\n姓名\t杜鹏飞", 0.9);
        String result = RagAnswerGuard.enforceGrounding(
                bad, "是否存在其他员工提交周报？", List.of(hit));
        assertTrue(result.contains("不存在其他员工"));
        assertTrue(result.contains("杜鹏飞"));
        assertFalse(result.contains("45969"));
    }

    @Test
    void rejectsCitationOutsideTemporalScope() {
        TemporalQueryScope scope = TemporalQueryScope.scopedSingleMonth(
                2025, 9, "杜鹏飞", true, TemporalParseConfidence.HIGH);
        SearchHit wrongPerson = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "【王明·工作周报】",
                0.9,
                null,
                null,
                "2025",
                "2025-09-01",
                "2025-09-07",
                "9",
                "王明",
                "工作周报",
                "true");
        String answer = "王明完成了开发任务 [1]";
        String result = RagAnswerGuard.enforceGrounding(
                answer, "杜鹏飞2025年9月工作", List.of(wrongPerson), Map.of(), scope);
        assertEquals(RagAnswerTemplates.INSUFFICIENT_IN_PROMPT, result);
    }

    @Test
    void prefersRuleBasedMonthlyWorkOverIncompleteLlmAnswer() {
        UUID docId = UUID.randomUUID();
        SearchHit chunk = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                """
                【杜鹏飞·工作周报·2025年9月1日--9月6日】
                1\t海图项目\t完成项目部署文档编写\t\t2025.9.4\t杜鹏飞\t完成文档编写\t\t已完成
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
        String llmAnswer = "根据周报，杜鹏飞完成了部署文档编写。";
        String result = RagAnswerGuard.enforceGrounding(
                llmAnswer,
                "杜鹏飞2025年9月都完成了哪些工作？",
                List.of(chunk),
                Map.of(docId, "杜鹏飞-周报（9.1-9.6）.xlsx"));
        assertTrue(result.contains("1."));
        assertTrue(result.contains("2."));
        assertTrue(result.contains("部署文档"));
    }
}
