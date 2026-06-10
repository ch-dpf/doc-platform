package com.knowbase.vector.dto;

import java.util.List;
import java.util.UUID;

public record RagStreamEvent(
        String type,
        String content,
        List<RagCitation> citations,
        Integer retrievedCount,
        Boolean usedLlm,
        Boolean found,
        Integer historyUsed,
        String searchQuery,
        Boolean conversational,
        UUID messageId
) {
    public static RagStreamEvent chunk(String content) {
        return new RagStreamEvent("chunk", content, null, null, null, null, null, null, null, null);
    }

    public static RagStreamEvent done(RagChatResponse response, UUID messageId) {
        return new RagStreamEvent(
                "done",
                response.answer(),
                response.citations(),
                response.retrievedCount(),
                response.usedLlm(),
                response.found(),
                response.historyUsed(),
                response.searchQuery(),
                response.conversational(),
                messageId);
    }

    public static RagStreamEvent error(String message) {
        return new RagStreamEvent("error", message, null, null, null, null, null, null, null, null);
    }
}
