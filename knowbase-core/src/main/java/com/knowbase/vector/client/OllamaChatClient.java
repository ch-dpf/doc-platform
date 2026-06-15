package com.knowbase.vector.client;

import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.dto.RagChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
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
        return chat(systemPrompt, List.of(), userMessage, ollamaProperties.getChatModel());
    }

    public String chat(String systemPrompt, String userMessage, String model) {
        return chat(systemPrompt, List.of(), userMessage, model);
    }

    public String chat(
            String systemPrompt,
            List<RagChatMessage> history,
            String userMessage,
            String model) {
        String chatModel = model != null && !model.isBlank() ? model : ollamaProperties.getChatModel();
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            for (RagChatMessage msg : history) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> options = new java.util.LinkedHashMap<>();
        options.put("temperature", ollamaProperties.getChatTemperature());
        if (ollamaProperties.getChatSeed() != null) {
            options.put("seed", ollamaProperties.getChatSeed());
        }

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", chatModel);
        body.put("messages", messages);
        body.put("stream", false);
        body.put("options", options);

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

    public Flux<String> streamChat(
            String systemPrompt,
            List<RagChatMessage> history,
            String userMessage,
            String model) {
        String chatModel = model != null && !model.isBlank() ? model : ollamaProperties.getChatModel();
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        if (history != null) {
            for (RagChatMessage msg : history) {
                messages.add(Map.of("role", msg.role(), "content", msg.content()));
            }
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        Map<String, Object> options = new java.util.LinkedHashMap<>();
        options.put("temperature", ollamaProperties.getChatTemperature());
        if (ollamaProperties.getChatSeed() != null) {
            options.put("seed", ollamaProperties.getChatSeed());
        }

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", chatModel);
        body.put("messages", messages);
        body.put("stream", true);
        body.put("options", options);

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .flatMap(chunk -> Flux.fromArray(chunk.split("\n")))
                .filter(line -> line != null && !line.isBlank())
                .mapNotNull(line -> {
                    try {
                        JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(line);
                        if (node.has("message") && node.get("message").has("content")) {
                            String content = node.get("message").get("content").asText("");
                            return content.isEmpty() ? null : content;
                        }
                    } catch (Exception e) {
                        log.debug("Skip Ollama stream line: {}", line);
                    }
                    return null;
                });
    }
}
