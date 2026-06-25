package com.knowbase.model.ollama;

import java.util.List;

public record OllamaMessage(String role, String content, List<String> images) {

    public OllamaMessage {
        images = images == null || images.isEmpty() ? List.of() : List.copyOf(images);
    }

    public OllamaMessage(String role, String content) {
        this(role, content, List.of());
    }

    public static OllamaMessage userWithImages(String content, List<String> base64Images) {
        return new OllamaMessage("user", content, base64Images);
    }
}
