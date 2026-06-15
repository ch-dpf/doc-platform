package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 周报主要内容汇总：规则抽取工作内容，避免 LLM 复述参考资料原文。 */
public final class RagWeeklyReportSummarySupport {

    private static final int MAX_ITEMS = 25;

    private RagWeeklyReportSummarySupport() {}

    public static Optional<String> tryRuleBasedAnswer(
            String question,
            List<SearchHit> hits,
            Map<UUID, String> fileNames) {
        if (!RagQuestionAnalyzer.isWeeklyReportSummaryQuestion(question)) {
            return Optional.empty();
        }
        if (hits == null || hits.isEmpty()) {
            return Optional.empty();
        }
        List<WeeklyReportWorkItemExtractor.WorkItem> items =
                WeeklyReportWorkItemExtractor.extract(hits, fileNames);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        var submitters = WeeklyReportWorkItemExtractor.extractSubmitterNames(hits, fileNames);
        return Optional.of(formatAnswer(items, submitters));
    }

    static String formatAnswer(
            List<WeeklyReportWorkItemExtractor.WorkItem> items,
            java.util.LinkedHashSet<String> submitters) {
        StringBuilder sb = new StringBuilder("根据参考资料，周报主要工作内容包括：\n\n");
        int limit = Math.min(items.size(), MAX_ITEMS);
        for (int i = 0; i < limit; i++) {
            WeeklyReportWorkItemExtractor.WorkItem item = items.get(i);
            sb.append(i + 1).append(". ");
            if (item.project() != null && !item.project().isBlank()) {
                sb.append("【").append(item.project().strip()).append("】");
            }
            sb.append(item.content().strip()).append(" [").append(item.refIndex()).append("]\n");
        }
        if (items.size() > MAX_ITEMS) {
            sb.append("\n（另有 ").append(items.size() - MAX_ITEMS).append(" 条未列出）\n");
        }
        if (submitters != null && !submitters.isEmpty()) {
            sb.append("\n提交人：").append(String.join("、", submitters));
            sb.append("。（已去重归纳，统计来自知识库文档）");
        } else {
            sb.append("\n（已去重归纳，统计来自知识库文档）");
        }
        return sb.toString().strip();
    }

    /** LLM 复述参考资料块（含 fileName=/docId=）时用于兜底替换。 */
    public static boolean looksLikeReferenceEcho(String answer) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        return answer.contains("fileName=") && answer.contains("docId=");
    }
}
