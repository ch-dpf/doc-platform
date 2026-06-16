package com.knowbase.vector.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.knowbase.library.dto.EmbeddingCatalogResponse;
import com.knowbase.library.dto.EmbeddingModelDescriptor;
import com.knowbase.vector.client.OllamaEmbeddingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Pattern;

/** 枚举本地 Ollama 已拉取且实测支持 /api/embed 的模型。 */
@Service
public class OllamaEmbeddingCatalogService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingCatalogService.class);
    private static final String PROBE_TEXT = "dimension probe";

    private static final Pattern LIKELY_EMBED = Pattern.compile(
            "embed|bge|e5|mxbai|arctic|nomic-embed|gte|sentence|minilm",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LIKELY_CHAT = Pattern.compile(
            "llama|qwen|mistral|gemma|phi|deepseek|command|vicuna|mixtral|codellama|starcoder",
            Pattern.CASE_INSENSITIVE);

    private final WebClient webClient;
    private final OllamaEmbeddingClient embeddingClient;

    public OllamaEmbeddingCatalogService(
            WebClient ollamaWebClient,
            OllamaEmbeddingClient embeddingClient) {
        this.webClient = ollamaWebClient;
        this.embeddingClient = embeddingClient;
    }

    public EmbeddingCatalogResponse catalog() {
        List<String> localModels = listLocalModelIds();
        List<EmbeddingModelDescriptor> embeddingModels = new ArrayList<>();
        for (String modelId : localModels) {
            if (!shouldProbe(modelId)) {
                continue;
            }
            OptionalInt dimension = probeDimension(modelId);
            if (dimension.isPresent()) {
                embeddingModels.add(new EmbeddingModelDescriptor(modelId, dimension.getAsInt()));
            }
        }
        embeddingModels.sort(Comparator.comparing(EmbeddingModelDescriptor::modelId));
        return new EmbeddingCatalogResponse("ollama", "pgvector", List.copyOf(embeddingModels));
    }

    List<String> listLocalModelIds() {
        Set<String> ids = new LinkedHashSet<>();
        try {
            JsonNode response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            if (response == null || !response.has("models")) {
                return List.of();
            }
            for (JsonNode node : response.get("models")) {
                if (node.has("name")) {
                    ids.add(normalizeModelId(node.get("name").asText()));
                } else if (node.has("model")) {
                    ids.add(normalizeModelId(node.get("model").asText()));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to list Ollama models: {}", e.getMessage());
        }
        return List.copyOf(ids);
    }

    static String normalizeModelId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.strip();
        int colon = trimmed.indexOf(':');
        return colon > 0 ? trimmed.substring(0, colon) : trimmed;
    }

    private boolean shouldProbe(String modelId) {
        if (LIKELY_EMBED.matcher(modelId).find()) {
            return true;
        }
        if (LIKELY_CHAT.matcher(modelId).find()) {
            return false;
        }
        return true;
    }

    private OptionalInt probeDimension(String modelId) {
        try {
            float[] vector = embeddingClient.embed(PROBE_TEXT, modelId, 0);
            if (vector.length > 0) {
                return OptionalInt.of(vector.length);
            }
        } catch (Exception e) {
            log.debug("Skip non-embedding Ollama model {}: {}", modelId, e.getMessage());
        }
        return OptionalInt.empty();
    }
}
