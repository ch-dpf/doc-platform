package com.knowbase.vector.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从用户问题提取检索关键词，便于 BM25 / 二次检索（中文整句 plainto_tsquery 效果较差）。
 */
public final class RagSearchQueryEnhancer {

    private static final Pattern YEAR = Pattern.compile("(\\d{4})");
    /** 长词优先匹配，避免「截止时间」同时抽出「截止」「时间」。 */
    private static final List<String> DOMAIN_TERMS = List.of(
            "项目名称", "参与项目", "参与人", "工作内容", "主要工作", "截止时间", "截止日期",
            "周报", "月报", "日报", "季报", "年报", "报告", "截止", "时间",
            "负责人", "提交", "审批", "流程", "规定", "政策", "制度", "总结", "计划", "目标",
            "员工", "参与", "项目", "工作", "材料", "上传", "其他", "部门");

    private RagSearchQueryEnhancer() {}

    public static List<String> extractTerms(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        Set<String> terms = new LinkedHashSet<>();
        Matcher yearMatcher = YEAR.matcher(question);
        while (yearMatcher.find()) {
            terms.add(yearMatcher.group(1));
        }
        for (String term : DOMAIN_TERMS) {
            if (question.contains(term)) {
                terms.add(term);
            }
        }
        pruneSubstringTerms(terms);
        if (terms.isEmpty()) {
            String stripped = question.replaceAll("[?？!！。,，；;\\s]+", "");
            if (stripped.length() >= 2 && stripped.length() <= 24) {
                terms.add(stripped);
            }
        }
        return List.copyOf(terms);
    }

    /**
     * 汇总类问题（项目参与、员工名单等）的规则扩展，避免 LLM 改写出库描述或字段清单。
     */
    public static String expandEmployeeProjectQuery(String question) {
        String person = RagProjectParticipationSupport.extractTargetPerson(question);
        if (person == null || person.isBlank()) {
            return "";
        }
        return person + " 项目 参与";
    }

    public static String expandSynthesisQuery(String question) {
        if (question == null || question.isBlank()) {
            return "";
        }
        String q = question.strip();
        if (!q.contains("项目")) {
            return "";
        }
        Set<String> terms = new LinkedHashSet<>();
        if (q.contains("项目")) {
            terms.add("项目");
            if (q.contains("参与")) {
                terms.add("参与");
                terms.add("参与人");
            }
            if (q.contains("名称")) {
                terms.add("项目名称");
            }
        }
        if (q.contains("员工") || q.contains("人员") || q.contains("同事")) {
            terms.add("员工");
        }
        if (q.contains("部门")) {
            terms.add("部门");
        }
        if (q.contains("主要")) {
            terms.add("主要");
        }
        terms.addAll(extractTerms(q));
        if (q.contains("项目") && !RagQuestionAnalyzer.isDeadlineQuestion(q)) {
            terms.removeIf(t -> "截止".equals(t) || "截止时间".equals(t) || "时间".equals(t));
        }
        pruneSubstringTerms(terms);
        if (terms.size() < 2) {
            return "";
        }
        return String.join(" ", terms);
    }

    /** 合并多路关键词，去重保序。 */
    public static String mergeKeywordQueries(String... queries) {
        Set<String> terms = new LinkedHashSet<>();
        if (queries != null) {
            for (String query : queries) {
                if (query == null || query.isBlank()) {
                    continue;
                }
                for (String part : query.strip().split("\\s+")) {
                    if (!part.isBlank()) {
                        terms.add(part);
                    }
                }
            }
        }
        return terms.isEmpty() ? "" : String.join(" ", terms);
    }

    /** BM25 友好：空格分隔关键词，如「2025 周报 截止 时间」 */
    public static String toKeywordQuery(String question) {
        List<String> terms = extractTerms(question);
        if (terms.isEmpty()) {
            return question == null ? "" : question.strip();
        }
        return String.join(" ", terms);
    }

    private static void pruneSubstringTerms(Set<String> terms) {
        List<String> sorted = terms.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .toList();
        Set<String> pruned = new LinkedHashSet<>();
        for (String term : sorted) {
            boolean subsumed = false;
            for (String kept : pruned) {
                if (kept.contains(term) && !kept.equals(term)) {
                    subsumed = true;
                    break;
                }
            }
            if (!subsumed) {
                pruned.add(term);
            }
        }
        terms.clear();
        terms.addAll(pruned);
    }
}
