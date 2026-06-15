package com.knowbase.vector.rag;

import com.knowbase.vector.dto.RagChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagConversationSupportTest {

    @Test
    void sanitizeHistoryFiltersBlankAndTruncates() {
        String longText = "x".repeat(2500);
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "  "),
                new RagChatMessage("user", longText),
                new RagChatMessage("assistant", "ok"));
        List<RagChatMessage> sanitized = RagConversationSupport.sanitizeHistory(history, 10, 2000);
        assertEquals(2, sanitized.size());
        assertTrue(sanitized.get(0).content().endsWith("…"));
        assertEquals(2001, sanitized.get(0).content().length());
    }

    @Test
    void trimHistoryKeepsTail() {
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "q1"),
                new RagChatMessage("assistant", "a1"),
                new RagChatMessage("user", "q2"),
                new RagChatMessage("assistant", "a2"));
        List<RagChatMessage> trimmed = RagConversationSupport.trimHistory(history, 2);
        assertEquals(2, trimmed.size());
        assertEquals("q2", trimmed.get(0).content());
    }

    @Test
    void resolveSearchQueryExpandsShortFollowUp() {
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "2025年周报提交截止时间"),
                new RagChatMessage("assistant", "截止时间为周五 18:00 [1]"));
        String query = RagConversationSupport.resolveSearchQuery("那负责人呢？", history);
        assertTrue(query.contains("2025年周报提交截止时间"));
        assertTrue(query.contains("那负责人呢"));
    }

    @Test
    void resolveSearchQueryKeepsLongQuestion() {
        List<RagChatMessage> history = List.of(new RagChatMessage("user", "之前的问题"));
        String longQ = "请详细说明知识库中关于文档分块策略、重叠长度以及向量化模型的全部配置要求";
        assertEquals(longQ, RagConversationSupport.resolveSearchQuery(longQ, history));
    }

    @Test
    void resolveSearchQueryDoesNotDuplicateCurrentTurnInHistory() {
        String question = "大概有多少员工周报";
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "本库中员工主要参与了哪些项目？"),
                new RagChatMessage("assistant", "…"),
                new RagChatMessage("user", question));
        assertEquals(question, RagConversationSupport.resolveSearchQuery(question, history));
    }

    @Test
    void resolveSearchQueryDoesNotChainEmployeeCountToPreviousTopic() {
        String question = "大概有多少员工周报";
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "本库中员工主要参与了哪些项目？"),
                new RagChatMessage("assistant", "…"));
        assertEquals(question, RagConversationSupport.resolveSearchQuery(question, history));
    }

    @Test
    void resolveSearchQueryIgnoresPunctuationOnlyDifferenceInHistory() {
        String question = "杜鹏飞参与了多少个项目";
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "大概有多少员工周报"),
                new RagChatMessage("assistant", "…"),
                new RagChatMessage("user", "杜鹏飞参与了多少个项目？"));
        assertEquals(question, RagConversationSupport.resolveSearchQuery(question, history));
    }

    @Test
    void resolveSearchQueryDoesNotChainCalendarYearToPreviousTopic() {
        String question = "今年是哪一年";
        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "今年哪一周有参与海图项目的？"),
                new RagChatMessage("assistant", "…"));
        assertEquals(question, RagConversationSupport.resolveSearchQuery(question, history));
    }
}
