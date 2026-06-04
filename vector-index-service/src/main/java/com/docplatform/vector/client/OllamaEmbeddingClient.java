package com.docplatform.vector.client;

import com.docplatform.vector.config.EmbeddingProperties;
import com.docplatform.vector.config.OllamaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OllamaEmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingClient.class);

    private final WebClient webClient;
    private final OllamaProperties ollamaProperties;
    private final EmbeddingProperties embeddingProperties;

    public OllamaEmbeddingClient(
            WebClient ollamaWebClient,
            OllamaProperties ollamaProperties,
            EmbeddingProperties embeddingProperties) {
        this.webClient = ollamaWebClient;
        this.ollamaProperties = ollamaProperties;
        this.embeddingProperties = embeddingProperties;
    }

    public void validateOnStartup() {
        try {
            webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(10));
            float[] probe = embed("dimension probe");
            if (probe.length != embeddingProperties.getDimension()) {
                throw new IllegalStateException(
                        "Embedding dimension mismatch: expected "
                                + embeddingProperties.getDimension()
                                + " got "
                                + probe.length);
            }
            log.info("Ollama embedding model {} validated (dim={})",
                    ollamaProperties.getEmbeddingModel(), probe.length);
        } catch (Exception e) {
            log.warn("Ollama not reachable at startup ({}). Indexing will fail until available.",
                    e.getMessage());
        }
    }

    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        int batchSize = ollamaProperties.getBatchSize();
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            results.addAll(requestEmbeddings(batch));
        }
        return results;
    }

    private List<float[]> requestEmbeddings(List<String> inputs) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", ollamaProperties.getEmbeddingModel());
        if (inputs.size() == 1) {
            body.put("input", inputs.get(0));
        } else {
            body.put("input", inputs);
        }

        try {
            JsonNode response = webClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(ollamaProperties.getTimeoutSeconds()));

            if (response != null && response.has("embeddings")) {
                List<float[]> vectors = parseEmbeddingsArray(response.get("embeddings"));
                if (vectors.size() == inputs.size()) {
                    return vectors;
                }
                throw new EmbeddingException(
                        "Ollama /api/embed returned " + vectors.size() + " vectors for " + inputs.size() + " inputs");
            }
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.debug("Ollama /api/embed failed, trying /api/embeddings: {}", e.getMessage());
        }

        return requestEmbeddingsLegacy(inputs);
    }

    private List<float[]> requestEmbeddingsLegacy(List<String> inputs) {
        List<float[]> vectors = new ArrayList<>();
        for (String input : inputs) {
            Map<String, Object> body = Map.of(
                    "model", ollamaProperties.getEmbeddingModel(),
                    "prompt", input);
            JsonNode response = webClient.post()
                    .uri("/api/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(ollamaProperties.getTimeoutSeconds()));
            if (response == null || !response.has("embedding")) {
                throw new EmbeddingException("Invalid Ollama embeddings response");
            }
            vectors.add(parseEmbedding(response.get("embedding")));
        }
        return vectors;
    }

    private List<float[]> parseEmbeddingsArray(JsonNode embeddingsNode) {
        List<float[]> vectors = new ArrayList<>();
        for (JsonNode node : embeddingsNode) {
            vectors.add(parseEmbedding(node));
        }
        return vectors;
    }

    private float[] parseEmbedding(JsonNode node) {
        float[] vector = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vector[i] = (float) node.get(i).asDouble();
        }
        return vector;
    }
}
