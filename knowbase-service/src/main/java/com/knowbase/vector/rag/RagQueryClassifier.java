package com.knowbase.vector.rag;

import java.util.regex.Pattern;

/**
 * 区分「对话/元问题」（无需知识库检索）与「知识库问答」（走 RAG）。
 */
public final class RagQueryClassifier {

    private static final Pattern GREETING_ONLY = Pattern.compile(
            "^(你好|您好|hi|hello|hey|早上好|下午好|晚上好|在吗|在不在)[\\s!！。?？~,，]*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern THANKS_OR_BYE = Pattern.compile(
            "^(谢谢|感谢|多谢|再见|拜拜|bye)[\\s!！。?？~,，]*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ASSISTANT_META = Pattern.compile(
            ".*(你是谁|你是什么|你叫什么|介绍一下你自己|你能做什么|你会什么|你的能力|你能帮我什么"
                    + "|什么模型|哪个模型|用的什么模型|使用什么模型|大模型|llm|LLM|什么助手|哪种模型).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern KNOWLEDGE_SEEKING = Pattern.compile(
            ".*(什么是|如何|怎么|怎样|为什么|哪些|多少|是否|查询|查一下|帮我查|文档|报告|规定|流程|政策"
                    + "|知识库|入库|索引|分块|向量|检索|上传|截止|负责|总结|内容是什么).*",
            Pattern.CASE_INSENSITIVE);

    private RagQueryClassifier() {}

    /**
     * @return true 表示打招呼、身份/能力/模型等元问题，不应强制 RAG 检索
     */
    public static boolean isConversational(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String q = question.strip();
        String compact = q.replaceAll("\\s+", "");

        if (RagQuestionAnalyzer.isCalendarYearQuestion(q)) {
            return true;
        }
        if (KNOWLEDGE_SEEKING.matcher(q).matches()) {
            return false;
        }
        if (GREETING_ONLY.matcher(compact).matches() || THANKS_OR_BYE.matcher(compact).matches()) {
            return true;
        }
        if (ASSISTANT_META.matcher(q).matches()) {
            return true;
        }
        // 短句问候 + 元问题，如「你好，你是什么模型呢？」
        if (compact.length() <= 32 && ASSISTANT_META.matcher(q).find()) {
            return true;
        }
        if (compact.length() <= 12 && GREETING_ONLY.matcher(compact).find()) {
            return true;
        }
        return false;
    }
}
