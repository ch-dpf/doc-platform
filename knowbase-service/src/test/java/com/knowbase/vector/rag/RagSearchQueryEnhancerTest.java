package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagSearchQueryEnhancerTest {

    @Test
    void extractsKeywordsFromWeeklyReportQuestion() {
        String keywordQuery = RagSearchQueryEnhancer.toKeywordQuery("2025年周报截止时间？");
        assertTrue(keywordQuery.contains("2025"));
        assertTrue(keywordQuery.contains("周报"));
        assertTrue(keywordQuery.contains("截止时间"));
        assertFalse(keywordQuery.contains(" 时间 "));
        assertTrue(RagSearchQueryEnhancer.extractTerms("2025年周报截止时间？").containsAll(
                java.util.List.of("2025", "周报", "截止时间")));
    }

    @Test
    void expandsProjectParticipationQuestion() {
        assertEquals(
                "参与人 项目 员工 主要",
                RagSearchQueryEnhancer.expandSynthesisQuery("本库中员工主要参与了哪些项目？"));
    }

    @Test
    void prunesGenericDeadlineTokensFromFieldListText() {
        var terms = RagSearchQueryEnhancer.extractTerms(
                "知识库「项目参与人库」：项目名称、参与人、部门、工作内容、截止时间");
        assertTrue(terms.contains("项目名称"));
        assertTrue(terms.contains("参与人"));
        assertFalse(terms.contains("截止"));
        assertFalse(terms.contains("时间"));
    }
}
