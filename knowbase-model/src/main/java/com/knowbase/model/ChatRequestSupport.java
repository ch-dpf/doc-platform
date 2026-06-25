package com.knowbase.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class ChatRequestSupport {

    private static final Set<String> OLLAMA_OPTION_KEYS = Set.of(
            "temperature",
            "num_predict",
            "top_p",
            "top_k",
            "repeat_penalty",
            "seed",
            "num_ctx",
            "min_p"
    );

    private ChatRequestSupport() {
    }

    public static String purpose(ChatRequest request) {
        if (request.parameters() == null) {
            return "";
        }
        Object purpose = request.parameters().get("purpose");
        return purpose == null ? "" : String.valueOf(purpose).trim();
    }

    public static Map<String, Object> ollamaOptions(ChatRequest request) {
        if (request.parameters() == null || request.parameters().isEmpty()) {
            return Map.of();
        }
        Map<String, Object> options = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : request.parameters().entrySet()) {
            if (OLLAMA_OPTION_KEYS.contains(entry.getKey())) {
                options.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(options);
    }

    public static String buildUserMessage(ChatRequest request) {
        String question = request.userMessage() == null ? "" : request.userMessage().trim();
        String context = request.context() == null ? "" : request.context().trim();
        if ("document_summary".equals(purpose(request))) {
            if (context.isBlank()) {
                return question;
            }
            return """
                    %s

                    Document content:
                    %s
                    """.formatted(question, context).trim();
        }
        if (context.isBlank()) {
            return question;
        }
        return """
                请基于以下证据回答问题。如果证据不足，请明确说明无法回答。

                证据：
                %s

                问题：
                %s
                """.formatted(context, question).trim();
    }
}
