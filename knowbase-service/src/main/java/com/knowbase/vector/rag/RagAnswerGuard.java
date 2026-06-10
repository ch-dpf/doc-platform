package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 对 LLM 回答做 grounded 校验，拦截「承认无依据仍猜测」「把报告周期当截止时间」等不合理输出。
 */
public final class RagAnswerGuard {

    private static final Pattern EXPLICIT_DEADLINE_IN_TEXT = Pattern.compile(
            "(提交)?截止(时间|日期|日)?|提交期限|须.{0,12}前.{0,6}提交|之前提交|前提交|deadline",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern HEDGING_NO_EXPLICIT = Pattern.compile(
            "(虽然|尽管)?.*(没有|无|未|并不).{0,8}(明确|清晰|直接).{0,8}(截止|期限|说明|记载)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DENIES_DEADLINE_THEN_GUESSES = Pattern.compile(
            "(不是|并非|不能当作|不能作为|不代表).{0,6}截止",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BOGUS_TABLE_NAME = Pattern.compile(
            "\\d{5}[\\u4e00-\\u9fff]{2,4}(完成|配合|开发|相关|任务)");

    private RagAnswerGuard() {}

    /** 参考资料中是否出现提交截止/期限类明文表述（不含单纯的工作周期日期）。 */
    public static boolean sourcesMentionExplicitDeadline(List<SearchHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return false;
        }
        for (SearchHit hit : hits) {
            if (hit.content() == null || hit.content().isBlank()) {
                continue;
            }
            if (EXPLICIT_DEADLINE_IN_TEXT.matcher(hit.content()).find()) {
                return true;
            }
        }
        return false;
    }

    public static String enforceGrounding(String answer, String question, List<SearchHit> hits) {
        return enforceGrounding(answer, question, hits, Map.of());
    }

    public static String enforceGrounding(
            String answer, String question, List<SearchHit> hits, Map<UUID, String> fileNames) {
        if (answer == null || answer.isBlank()) {
            return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
        }
        String trimmed = answer.strip();
        if (trimmed.startsWith("未找到")) {
            return trimmed;
        }

        if (RagQuestionAnalyzer.isEmployeeExistenceQuestion(question)
                || RagQuestionAnalyzer.isEmployeeListQuestion(question)
                || RagQuestionAnalyzer.isEmployeeCountQuestion(question)) {
            var rule = RagEmployeeRosterSupport.tryRuleBasedAnswer(
                    question, hits, fileNames != null ? fileNames : Map.of());
            if (rule.isPresent()) {
                return rule.get();
            }
            if (BOGUS_TABLE_NAME.matcher(trimmed).find()) {
                return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
            }
        }

        if (RagQuestionAnalyzer.isSynthesisQuestion(question)) {
            if (RagWeeklyReportSummarySupport.looksLikeReferenceEcho(trimmed)) {
                var recovered = RagWeeklyReportSummarySupport.tryRuleBasedAnswer(
                        question, hits, fileNames != null ? fileNames : Map.of());
                if (recovered.isPresent()) {
                    return recovered.get();
                }
            }
            return trimmed;
        }

        if (RagQuestionAnalyzer.isDeadlineQuestion(question)) {
            if (!sourcesMentionExplicitDeadline(hits)) {
                return RagAnswerTemplates.NO_EXPLICIT_DEADLINE;
            }
            if (HEDGING_NO_EXPLICIT.matcher(trimmed).find()
                    || DENIES_DEADLINE_THEN_GUESSES.matcher(trimmed).find()) {
                return RagAnswerTemplates.NO_EXPLICIT_DEADLINE;
            }
            if (!EXPLICIT_DEADLINE_IN_TEXT.matcher(trimmed).find()
                    && containsIsoLikeDateRange(trimmed)) {
                return RagAnswerTemplates.NO_EXPLICIT_DEADLINE;
            }
        }

        String lower = trimmed.toLowerCase();
        if (lower.contains("无法确定")
                || lower.contains("无法回答")
                || lower.contains("不足以回答")
                || lower.contains("没有相关")
                || lower.contains("未检索到")) {
            return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
        }
        if (HEDGING_NO_EXPLICIT.matcher(trimmed).find()) {
            return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
        }
        return trimmed;
    }

    private static boolean containsIsoLikeDateRange(String text) {
        return text.matches(".*\\d{4}年\\d{1,2}月\\d{1,2}日.*")
                || text.matches(".*\\d{1,2}月\\d{1,2}日.*");
    }
}
