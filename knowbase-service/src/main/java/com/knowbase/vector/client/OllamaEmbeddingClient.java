package com.knowbase.vector.client;

import com.knowbase.vector.config.EmbeddingProperties;
import com.knowbase.vector.config.OllamaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OllamaEmbeddingClient implements EmbeddingClient {

    @Override
    public String providerId() {
        return "ollama";
    }

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

    @Override
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

    @Override
    public float[] embed(String text) {
        return embed(text, ollamaProperties.getEmbeddingModel(), embeddingProperties.getDimension());
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        return embedBatch(texts, ollamaProperties.getEmbeddingModel(), embeddingProperties.getDimension());
    }

    public float[] embed(String text, String model, int expectedDimension) {
        return embedBatch(List.of(text), model, expectedDimension).get(0);
    }

    public List<float[]> embedBatch(List<String> texts, String model, int expectedDimension) {
        List<float[]> results = new ArrayList<>();
        int batchSize = ollamaProperties.getBatchSize();
        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            results.addAll(requestEmbeddings(batch, model, expectedDimension));
        }
        return results;
    }

    private List<float[]> requestEmbeddings(List<String> inputs) {
        return requestEmbeddings(inputs, ollamaProperties.getEmbeddingModel(), embeddingProperties.getDimension());
    }

    private List<float[]> requestEmbeddings(List<String> inputs, String model, int expectedDimension) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        if (inputs.size() == 1) {
            body.put("input", inputs.get(0));
        } else {
            body.put("input", inputs);
        }

        Exception primaryError = null;
        try {
            JsonNode response = webClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(ollamaProperties.getTimeoutSeconds()));

            if (response != null && response.has("embeddings")) {
                List<float[]> vectors = parseEmbeddingsArray(response.get("embeddings"), expectedDimension);
                if (vectors.size() == inputs.size()) {
                    return vectors;
                }
                throw new EmbeddingException(
                        "Ollama /api/embed returned " + vectors.size() + " vectors for " + inputs.size() + " inputs");
            }
            primaryError = new EmbeddingException("Ollama /api/embed returned empty response for model: " + model);
        } catch (EmbeddingException e) {
            throw e;
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                throw modelNotFound(model, e);
            }
            primaryError = e;
            log.debug("Ollama /api/embed failed for model {}: {}", model, e.getMessage());
        } catch (Exception e) {
            primaryError = e;
            log.debug("Ollama /api/embed failed for model {}: {}", model, e.getMessage());
        }

        try {
            return requestEmbeddingsLegacy(inputs, model, expectedDimension);
        } catch (Exception legacyError) {
            throw wrapEmbeddingFailure(model, primaryError, legacyError);
        }
    }

    private static EmbeddingException modelNotFound(String model, Throwable cause) {
        return new EmbeddingException(
                "Ollama 模型未安装或不可用: \"" + model + "\"。请执行 ollama pull " + model
                        + "；或在知识库「检索」中将 Rerank 模型留空（使用库 Embedding 模型），或暂时关闭重排序。",
                cause);
    }

    private static EmbeddingException wrapEmbeddingFailure(String model, Exception primaryError, Exception legacyError) {
        String detail = primaryError != null ? primaryError.getMessage() : legacyError.getMessage();
        return new EmbeddingException(
                "Ollama Embedding 失败（模型: " + model + "）: " + detail
                        + "。新版 Ollama 请使用 /api/embed；请确认模型已 pull 且知识库 Rerank 配置正确。",
                legacyError);
    }

    private List<float[]> requestEmbeddingsLegacy(List<String> inputs) {
        return requestEmbeddingsLegacy(
                inputs, ollamaProperties.getEmbeddingModel(), embeddingProperties.getDimension());
    }

    private List<float[]> requestEmbeddingsLegacy(List<String> inputs, String model, int expectedDimension) {
        List<float[]> vectors = new ArrayList<>();
        for (String input : inputs) {
            Map<String, Object> body = Map.of(
                    "model", model,
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
            vectors.add(parseEmbedding(response.get("embedding"), expectedDimension));
        }
        return vectors;
    }

    private List<float[]> parseEmbeddingsArray(JsonNode embeddingsNode, int expectedDimension) {
        List<float[]> vectors = new ArrayList<>();
        for (JsonNode node : embeddingsNode) {
            vectors.add(parseEmbedding(node, expectedDimension));
        }
        return vectors;
    }

    private float[] parseEmbedding(JsonNode node, int expectedDimension) {
        float[] vector = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            vector[i] = (float) node.get(i).asDouble();
        }
        if (expectedDimension > 0 && vector.length != expectedDimension) {
            throw new EmbeddingException(
                    "Embedding dimension mismatch for model: expected "
                            + expectedDimension
                            + " got "
                            + vector.length);
        }
        return vector;
    }
}
