package com.knowbase.vector.rag;

import com.knowbase.vector.dto.RagChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 多轮 RAG 对话辅助：历史裁剪、追问检索语句拼接。
 */
public final class RagConversationSupport {

    private RagConversationSupport() {}

    public static List<RagChatMessage> trimHistory(List<RagChatMessage> history, int maxMessages) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        if (maxMessages <= 0 || history.size() <= maxMessages) {
            return List.copyOf(history);
        }
        return List.copyOf(history.subList(history.size() - maxMessages, history.size()));
    }

    public static List<RagChatMessage> sanitizeHistory(
            List<RagChatMessage> history, int maxMessages, int maxCharsPerMessage) {
        return trimHistory(history, maxMessages).stream()
                .filter(msg -> msg.content() != null && !msg.content().isBlank())
                .map(msg -> new RagChatMessage(msg.role(), truncate(msg.content().strip(), maxCharsPerMessage)))
                .toList();
    }

    private static String truncate(String text, int maxChars) {
        if (maxChars <= 0 || text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "…";
    }

    /**
     * 对短追问或含指代词的问题，将上一轮用户问题拼入检索语句以提升召回。
     */
    public static String resolveSearchQuery(String question, List<RagChatMessage> history) {
        String trimmed = question == null ? "" : question.trim();
        if (trimmed.isEmpty() || history == null || history.isEmpty()) {
            return trimmed;
        }
        if (trimmed.length() >= 32) {
            return trimmed;
        }
        String lastUser = findLastUserMessage(history, trimmed);
        if (lastUser == null || lastUser.isBlank()) {
            return trimmed;
        }
        if (looksLikeFollowUp(trimmed)) {
            String previous = lastUser.strip();
            String previousNorm = normalizeComparable(previous);
            String trimmedNorm = normalizeComparable(trimmed);
            if (previousNorm.equals(trimmedNorm)
                    || previousNorm.endsWith(trimmedNorm)
                    || trimmedNorm.endsWith(previousNorm)) {
                return trimmed;
            }
            return previous + " " + trimmed;
        }
        return trimmed;
    }

    private static String findLastUserMessage(List<RagChatMessage> history, String currentQuestion) {
        String currentNorm = normalizeComparable(currentQuestion);
        for (int i = history.size() - 1; i >= 0; i--) {
            RagChatMessage msg = history.get(i);
            if ("user".equals(msg.role())) {
                String content = msg.content() == null ? "" : msg.content().strip();
                if (!currentNorm.isEmpty() && normalizeComparable(content).equals(currentNorm)) {
                    continue;
                }
                return msg.content();
            }
        }
        return null;
    }

    static String normalizeComparable(String text) {
        if (text == null) {
            return "";
        }
        return text.strip()
                .replaceAll("[?？!！。,，;；]+$", "")
                .replaceAll("\\s+", " ");
    }

    static boolean looksLikeFollowUp(String question) {
        if (RagQuestionAnalyzer.isLibraryStatsQuestion(question)
                || RagQuestionAnalyzer.isEmployeeCountQuestion(question)
                || RagQuestionAnalyzer.isEmployeeListQuestion(question)
                || RagQuestionAnalyzer.isEmployeeProjectQuestion(question)
                || RagQuestionAnalyzer.isLibraryPurposeQuestion(question)
                || RagQuestionAnalyzer.isSynthesisQuestion(question)
                || RagQuestionAnalyzer.isCalendarYearQuestion(question)
                || RagQuestionAnalyzer.isWeeklyReportWeekQuestion(question)
                || RagQuestionAnalyzer.isProjectRecountQuestion(question)) {
            return false;
        }
        if (question.length() <= 24) {
            return true;
        }
        return question.contains("它")
                || question.contains("这个")
                || question.contains("那个")
                || question.contains("上面")
                || question.contains("前面")
                || question.contains("继续")
                || question.contains("还有")
                || question.contains("呢")
                || question.startsWith("那")
                || question.startsWith("这")
                || question.startsWith("再");
    }
}
