package com.knowbase.model.ollama;

import com.knowbase.model.ChatCompletion;
import com.knowbase.model.ChatModelClient;
import com.knowbase.model.ChatRequest;
import com.knowbase.model.ChatRequestSupport;

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
        String userMessage = ChatRequestSupport.buildUserMessage(request);
        messages.add(new OllamaMessage("user", userMessage));
        Map<String, Object> options = ChatRequestSupport.ollamaOptions(request);
        OllamaChatResponse response = ollamaClient.chat(modelName, messages, options);
        return new ChatCompletion(
                response.answer(),
                response.promptTokens(),
                response.completionTokens(),
                response.rawResponse()
        );
    }
}
