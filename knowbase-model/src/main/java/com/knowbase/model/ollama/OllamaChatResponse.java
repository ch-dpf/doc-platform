package com.knowbase.model.ollama;

public record OllamaChatResponse(
        String answer,
        int promptTokens,
        int completionTokens,
        String rawResponse
) {
}
