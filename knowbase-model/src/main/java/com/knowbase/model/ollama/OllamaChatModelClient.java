package com.knowbase.model.ollama;

import com.knowbase.model.ChatCompletion;
import com.knowbase.model.ChatModelClient;
import com.knowbase.model.ChatRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OllamaChatModelClient implements ChatModelClient {

    private final OllamaClient ollamaClient;
    private final String provider;
    private final String modelName;

    public OllamaChatModelClient(OllamaClient ollamaClient, String provider, String modelName) {
        this.ollamaClient = ollamaClient;
        this.provider = provider;
        this.modelName = modelName;
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public ChatCompletion complete(ChatRequest request) {
        List<OllamaMessage> messages = new ArrayList<>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new OllamaMessage("system", request.systemPrompt()));
        }
        String userMessage = buildUserMessage(request);
        messages.add(new OllamaMessage("user", userMessage));
        Map<String, Object> options = request.parameters() == null ? Map.of() : request.parameters();
        OllamaChatResponse response = ollamaClient.chat(modelName, messages, options);
        return new ChatCompletion(
                response.answer(),
                response.promptTokens(),
                response.completionTokens(),
                response.rawResponse()
        );
    }

    private static String buildUserMessage(ChatRequest request) {
        String question = request.userMessage() == null ? "" : request.userMessage().trim();
        String context = request.context() == null ? "" : request.context().trim();
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
