package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.retrieval.TemporalHitMatcher;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
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

    private static final Pattern CITATION_REF = Pattern.compile("\\[(\\d+)]");

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
        return enforceGrounding(answer, question, hits, Map.of(), TemporalQueryScope.none());
    }

    public static String enforceGrounding(
            String answer, String question, List<SearchHit> hits, Map<UUID, String> fileNames) {
        TemporalQueryScope scope = RagTemporalQueryParser.parse(question, List.of());
        return enforceGrounding(answer, question, hits, fileNames, scope);
    }

    public static String enforceGrounding(
            String answer,
            String question,
            List<SearchHit> hits,
            Map<UUID, String> fileNames,
            TemporalQueryScope scope) {
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
            if (RagMonthlyWorkSummarySupport.isMonthlyCompletedWorkQuestion(question)) {
                var monthly = RagMonthlyWorkSummarySupport.tryRuleBasedAnswer(
                        question, hits, fileNames != null ? fileNames : Map.of(), List.of());
                if (monthly.isPresent()) {
                    return monthly.get();
                }
            }
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

        if (scope != null && scope.scoped() && hits != null && !hits.isEmpty()) {
            String temporalViolation = validateTemporalCitations(trimmed, hits, scope, fileNames);
            if (temporalViolation != null) {
                return temporalViolation;
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

    static String validateTemporalCitations(
            String answer,
            List<SearchHit> hits,
            TemporalQueryScope scope,
            Map<UUID, String> fileNames) {
        Set<Integer> cited = new HashSet<>();
        Matcher matcher = CITATION_REF.matcher(answer);
        while (matcher.find()) {
            cited.add(Integer.parseInt(matcher.group(1)));
        }
        if (cited.isEmpty()) {
            return null;
        }
        Map<UUID, String> names = fileNames != null ? fileNames : Map.of();
        for (int index : cited) {
            if (index < 1 || index > hits.size()) {
                continue;
            }
            SearchHit hit = hits.get(index - 1);
            String fileName = names.get(hit.docId());
            if (!TemporalHitMatcher.matches(hit, scope, fileName, hit.temporalMetadataMap())) {
                return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
            }
        }
        return null;
    }

    private static boolean containsIsoLikeDateRange(String text) {
        return text.matches(".*\\d{4}年\\d{1,2}月\\d{1,2}日.*")
                || text.matches(".*\\d{1,2}月\\d{1,2}日.*");
    }
}
