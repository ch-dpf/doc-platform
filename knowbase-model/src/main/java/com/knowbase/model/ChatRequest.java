package com.knowbase.model;

import java.util.Map;

public record ChatRequest(
        String systemPrompt,
        String userMessage,
        String context,
        Map<String, Object> parameters
) {
}
