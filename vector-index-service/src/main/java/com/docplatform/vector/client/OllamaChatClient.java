package com.docplatform.vector.client;

import com.docplatform.vector.config.OllamaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class OllamaChatClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaChatClient.class);

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;

    public OllamaChatClient(WebClient ollamaWebClient, OllamaProperties ollamaProperties) {
        this.webClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
    }

    public void validateOnStartup() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            String model = ollamaProperties.getChatModel();
            log.info("Ollama chat model configured: {} (pull with: ollama pull {})", model, model);
        } catch (Exception e) {
            log.warn("Ollama not reachable for chat validation ({}). RAG will fail until available.",
                    e.getMessage());
        }
    }

    public String chat(String systemPrompt, String userMessage) {
        List<Map<String, String>> messages = List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage));
        Map<String, Object> body = Map.of(
                "model", ollamaProperties.getChatModel(),
                "messages", messages,
                "stream", false);

        try {
            JsonNode response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(ollamaProperties.getChatTimeoutSeconds()));

            if (response == null || !response.has("message")) {
                throw new ChatException("Invalid Ollama chat response: missing message");
            }
            JsonNode content = response.get("message").get("content");
            if (content == null || content.isNull()) {
                throw new ChatException("Invalid Ollama chat response: empty content");
            }
            return content.asText().trim();
        } catch (ChatException e) {
            throw e;
        } catch (Exception e) {
            throw new ChatException("Ollama chat failed: " + e.getMessage(), e);
        }
    }
}
