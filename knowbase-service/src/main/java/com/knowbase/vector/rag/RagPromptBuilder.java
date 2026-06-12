package com.knowbase.vector.rag;

import com.knowbase.vector.config.RagProperties;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.dto.SearchHit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RagPromptBuilder {

    private final RagProperties ragProperties;

    public RagPromptBuilder(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public String buildUserMessage(String question, List<SearchHit> hits) {
        return buildUserMessage(question, hits, List.of());
    }

    public String buildUserMessage(String question, List<SearchHit> hits, List<RagChatMessage> history) {
        return buildUserMessage(question, hits, history, Map.of());
    }

    public String buildUserMessage(
            String question,
            List<SearchHit> hits,
            List<RagChatMessage> history,
            Map<UUID, String> fileNames) {
        StringBuilder context = new StringBuilder();
        int used = 0;
        int index = 1;
        for (SearchHit hit : hits) {
            String block = formatReference(index, hit, fileNames);
            if (used > 0 && used + block.length() > ragProperties.getMaxContextChars()) {
                break;
            }
            context.append(block);
            used += block.length();
            index++;
        }
        String followUpHint = history != null && !history.isEmpty()
                ? "说明：用户可能在追问前文，请结合对话历史理解指代；事实性内容仍须来自下列参考资料。\n\n"
                : "";
        String strictHint = buildStrictHint(question);
        String synthesisHint = buildSynthesisHint(question, hits, fileNames);
        return """
                请仅根据下列「参考资料」回答「用户问题」。参考资料是回答的唯一依据。
                - 不得使用参考资料以外的任何信息，不得推测、不得联想。
                - 每个事实性陈述句末须标注引用编号，如 [1]；引用内容须与陈述严格对应。
                %s- 若参考资料与问题无关、或未明确记载所问事实，必须只回复这一句（不要其它内容）：%s
                %s%s%s
                【参考资料】
                %s
                【用户问题】
                %s
                """.formatted(
                RagQuestionAnalyzer.isSynthesisQuestion(question)
                        ? ""
                        : "- 仅当参考资料中明确写出所问事实（含同义表述）时方可作答；不得从报告周期、工作区间推断「截止时间」。\n",
                RagAnswerTemplates.INSUFFICIENT_IN_PROMPT,
                synthesisHint,
                strictHint,
                followUpHint,
                context,
                question.trim());
    }

    private static String buildSynthesisHint(String question, List<SearchHit> hits, Map<UUID, String> fileNames) {
        if (!RagQuestionAnalyzer.isSynthesisQuestion(question)) {
            return "";
        }
        var extracted = RagEmployeeNameExtractor.extract(hits, fileNames);
        String rosterHint = "";
        if (!extracted.isEmpty()
                && (RagQuestionAnalyzer.isEmployeeExistenceQuestion(question)
                || RagQuestionAnalyzer.isEmployeeListQuestion(question)
                || RagQuestionAnalyzer.isEmployeeCountQuestion(question)
                || RagQuestionAnalyzer.isCombinedEmployeeWorkQuestion(question))) {
            rosterHint = "\n                - 【系统已从参考资料结构化抽取的提交人姓名（去重，不含表头字段）】："
                    + String.join("、", extracted)
                    + "。名单类问题请优先采用；若还问主要工作内容，须继续归纳各员工工作事项并标注引用。\n";
        }
        return """

                【跨文档汇总类问题】
                - 用户希望从多条参考资料归纳名单或主要工作，请综合 [1][2]… 中可见信息作答，不要因信息分散在不同片段而直接回复「未找到」。
                - 问「哪些员工/谁上传了周报」：只统计「姓名/责任人/部门负责人」字段及文件名（如 XXX-周报.xlsx）中的中文人名；2–4 字；去重后列出。
                - 问「多少/几个人提交了周报」：按上述规则去重统计人数（不是文档份数），列出姓名并写明总人数。
                - 问「是否存在/有没有其他员工提交周报」：同上规则抽取提交人姓名；若去重后仅一人则答「不存在其他员工」；多人则答「存在」并列出。
                - 表格行中的 5 位数字（如 45896、45969）是 Excel 计划完成时间，不是员工编号；不得把「数字+姓名+完成…任务」整行当作一个姓名。
                - 工作内容里提到的经理、主任、同事是协作对象，不是周报提交人，不得计入提交员工名单。
                %s                - 问「主要工作内容」：按员工或按主题归纳片段中的工作事项，分条列出并标注引用；可合并多条片段中的同一员工信息。
                - 归纳时须对相同或高度相似的工作项去重，不要出现「（第二次）」等自创标记；条目顺序按参考资料编号 [1][2]… 稳定排列。
                - 仍禁止编造参考资料中未出现的人名、项目或结论。
                - 问「今年是哪一年」：以当前日历年为准作答，不得仅凭库内路径或文件名中的 2025/2026 断定「今年」。
                - 问「哪周周报含某项目」：周次以文件名中的日期区间为准；若库内仅有往年周报，须说明「今年」指当前日历年且当年无匹配周次，并列出历史年份周次。

                """.formatted(rosterHint);
    }

    private static String buildStrictHint(String question) {
        if (!RagQuestionAnalyzer.isDeadlineQuestion(question)) {
            return "";
        }
        return """
                
                【截止/期限类问题 — 严格规则】
                - 仅当参考资料中出现「截止」「提交截止」「截止日期」「须…前提交」等明文时，才可回答截止时间。
                - 周报标题或正文中的日期区间（如 8月25日-8月29日）是报告覆盖的工作周期，绝不是提交截止时间。
                - 禁止在回答中出现「虽然没有明确截止时间」后继续列举日期或人名；无明文则整段只回复「未找到」句式。
                
                """;
    }

    public String excerpt(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
        int max = ragProperties.getExcerptMaxChars();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "…";
    }

    private String formatReference(int index, SearchHit hit, Map<UUID, String> fileNames) {
        String fileName = fileNames != null ? fileNames.getOrDefault(hit.docId(), "") : "";
        String fileLine = fileName.isBlank() ? "" : " fileName=" + fileName;
        return "[" + index + "]" + fileLine + " docId=" + hit.docId()
                + " chunk=" + hit.chunkIndex()
                + " score=" + String.format("%.4f", hit.score())
                + "\n" + hit.contextForPrompt() + "\n\n";
    }
}
