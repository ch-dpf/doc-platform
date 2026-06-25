package com.knowbase.model.ollama;

import com.knowbase.model.vision.VisionDocumentModelClient;
import com.knowbase.model.vision.VisionDocumentPrompts;

import java.util.List;
import java.util.Map;

/**
 * Calls an Ollama-hosted vision-language model for per-page document parsing.
 */
public final class OllamaVisionDocumentModelClient implements VisionDocumentModelClient {

    private final OllamaClient ollamaClient;
    private final String modelName;

    public OllamaVisionDocumentModelClient(OllamaClient ollamaClient, String modelName) {
        this.ollamaClient = ollamaClient;
        this.modelName = modelName == null ? "" : modelName.trim();
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String recognizePage(byte[] imageBytes, String mimeType, int pageNumber, Map<String, Object> options) {
        if (modelName.isBlank()) {
            throw new IllegalStateException("vision language model 未配置");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return "";
        }
        String prompt = resolvePrompt(options);
        String base64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
        OllamaChatResponse response = ollamaClient.chat(
                modelName,
                List.of(OllamaMessage.userWithImages(prompt, List.of(base64))),
                Map.of("temperature", 0.1d)
        );
        return response.answer() == null ? "" : response.answer().trim();
    }

    private static String resolvePrompt(Map<String, Object> options) {
        return VisionDocumentPrompts.resolvePagePrompt(options);
    }
}
