package com.knowbase.vector.dto;

import java.util.List;

public record RagChatResponse(
        String answer,
        List<RagCitation> citations,
        int retrievedCount,
        boolean usedLlm,
        /** 是否在知识库中检索到可用片段（false 时不应出现编造内容） */
        boolean found,
        /** 实际参与 LLM 上下文的历史消息条数 */
        int historyUsed,
        /** 本轮用于向量检索的语句（追问时可能与 question 不同） */
        String searchQuery,
        /** 是否为对话/元问题（打招呼、询问助手身份等），未使用知识库检索 */
        boolean conversational
) {
    public RagChatResponse(
            String answer,
            List<RagCitation> citations,
            int retrievedCount,
            boolean usedLlm,
            boolean found) {
        this(answer, citations, retrievedCount, usedLlm, found, 0, null, false);
    }

    public RagChatResponse(
            String answer,
            List<RagCitation> citations,
            int retrievedCount,
            boolean usedLlm,
            boolean found,
            int historyUsed,
            String searchQuery) {
        this(answer, citations, retrievedCount, usedLlm, found, historyUsed, searchQuery, false);
    }
}
