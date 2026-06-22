package com.knowbase.model;

public record ChatCompletion(
        String answer,
        int promptTokens,
        int completionTokens,
        String rawResponse
) {
}
