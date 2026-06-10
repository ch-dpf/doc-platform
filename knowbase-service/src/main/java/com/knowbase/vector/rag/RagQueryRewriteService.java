package com.knowbase.vector.rag;

import com.knowbase.vector.client.OllamaChatClient;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.dto.RagChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 用 LLM 将用户问题改写为更利于向量/BM25 检索的短查询。
 */
@Service
public class RagQueryRewriteService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryRewriteService.class);

    private static final Pattern LEADING_LABEL = Pattern.compile(
            "^(改写后?的?检索查询|检索查询|查询|改写)[:：\\s]+",
            Pattern.CASE_INSENSITIVE);

    private static final String SYSTEM_PROMPT = """
            你是企业知识库检索查询改写助手。根据用户问题，输出一行用于检索的查询文本。
            硬性规则：
            1. 只输出一行改写结果，不要解释、不要编号、不要 markdown。
            2. 保留年份、人名、部门、周报、月报、提交、工作内容、截止时间等关键词。
            3. 将「汇总/有哪些/多少人/主要内容」类问题扩展为具体检索词，例如「周报 工作内容 主要事项 员工」。
            4. 输出使用简体中文，长度不超过 80 字。
            5. 若问题本身已是简短检索词，可原样或略作压缩输出。
            """;

    private final OllamaChatClient chatClient;
    private final OllamaProperties ollamaProperties;
    private final RetrievalProperties retrievalProperties;

    public RagQueryRewriteService(
            OllamaChatClient chatClient,
            OllamaProperties ollamaProperties,
            RetrievalProperties retrievalProperties) {
        this.chatClient = chatClient;
        this.ollamaProperties = ollamaProperties;
        this.retrievalProperties = retrievalProperties;
    }

    public String rewrite(String conversationQuery, String originalQuestion, List<RagChatMessage> history) {
        String base = conversationQuery == null ? "" : conversationQuery.strip();
        if (!retrievalProperties.isQueryRewriteEnabled() || base.isEmpty()) {
            return base;
        }
        if (shouldSkipRewrite(base, originalQuestion)) {
            return base;
        }
        String question = originalQuestion == null ? base : originalQuestion.strip();
        if (RagQuestionAnalyzer.isEmployeeProjectQuestion(question)) {
            String expanded = RagSearchQueryEnhancer.expandEmployeeProjectQuery(question);
            if (!expanded.isBlank()) {
                log.debug("Employee project query rule-expanded: [{}] -> [{}]", base, expanded);
                return expanded;
            }
        }
        if (RagQuestionAnalyzer.isSynthesisQuestion(question)) {
            String expanded = RagSearchQueryEnhancer.expandSynthesisQuery(question);
            if (!expanded.isBlank()) {
                log.debug("Synthesis query rule-expanded: [{}] -> [{}]", base, expanded);
                return expanded;
            }
        }
        try {
            String userMessage = buildUserMessage(base, originalQuestion);
            String raw = chatClient.chat(
                    SYSTEM_PROMPT,
                    trimHistory(history),
                    userMessage,
                    ollamaProperties.getChatModel());
            String cleaned = sanitizeRewrite(raw);
            int maxChars = Math.max(24, retrievalProperties.getQueryRewriteMaxChars());
            if (cleaned.isBlank() || cleaned.length() > maxChars) {
                log.debug("Query rewrite rejected (empty or too long), fallback to conversation query");
                return base;
            }
            if (looksLikeLibraryMetadataRewrite(cleaned)) {
                log.debug("Query rewrite rejected (library metadata style), fallback to conversation query");
                return fallbackAfterRejectedRewrite(question, base);
            }
            if (cleaned.equals(base)) {
                return base;
            }
            log.debug("Query rewritten: [{}] -> [{}]", base, cleaned);
            return cleaned;
        } catch (Exception e) {
            log.warn("Query rewrite failed, using conversation query: {}", e.getMessage());
            return base;
        }
    }

    /** 拒绝把知识库简介/字段清单当作检索 query 的改写结果。 */
    public static boolean looksLikeLibraryMetadataRewrite(String rewritten) {
        if (rewritten == null || rewritten.isBlank()) {
            return false;
        }
        String text = rewritten.strip();
        if (text.contains("知识库") && text.contains("：")) {
            return true;
        }
        if (text.matches(".*「.+」.*[：:].*")) {
            return true;
        }
        return text.contains("项目名称、") || text.contains("参与人、部门");
    }

    private static String fallbackAfterRejectedRewrite(String originalQuestion, String conversationQuery) {
        String expanded = RagSearchQueryEnhancer.expandSynthesisQuery(originalQuestion);
        if (!expanded.isBlank()) {
            return expanded;
        }
        return conversationQuery;
    }

    static boolean shouldSkipRewrite(String conversationQuery, String originalQuestion) {
        if (RagQuestionAnalyzer.isSynthesisQuestion(originalQuestion)) {
            return false;
        }
        if (RagQuestionAnalyzer.isLibraryStatsQuestion(originalQuestion)
                || RagQuestionAnalyzer.isLibraryPurposeQuestion(originalQuestion)
                || RagQuestionAnalyzer.isDeadlineQuestion(originalQuestion)) {
            return true;
        }
        List<String> terms = RagSearchQueryEnhancer.extractTerms(conversationQuery);
        return terms.size() >= 2 && conversationQuery.length() <= 48;
    }

    private static List<RagChatMessage> trimHistory(List<RagChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, history.size() - 2);
        return List.copyOf(history.subList(from, history.size()));
    }

    private static String buildUserMessage(String conversationQuery, String originalQuestion) {
        String question = originalQuestion == null ? "" : originalQuestion.strip();
        if (question.isBlank() || question.equals(conversationQuery)) {
            return "用户问题：\n" + conversationQuery;
        }
        return """
                用户问题：
                %s

                检索上下文（含追问拼接）：
                %s
                """.formatted(question, conversationQuery).strip();
    }

    static String sanitizeRewrite(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw.strip();
        if (text.startsWith("```")) {
            int end = text.indexOf("```", 3);
            text = end > 0 ? text.substring(3, end).strip() : text.replace("```", "").strip();
        }
        int newline = text.indexOf('\n');
        if (newline >= 0) {
            text = text.substring(0, newline).strip();
        }
        text = text.replaceAll("^[\"'「『]+|[\"'」』]+$", "");
        text = LEADING_LABEL.matcher(text).replaceFirst("").strip();
        return text.replaceAll("[?？!！。；;]+$", "").strip();
    }
}
