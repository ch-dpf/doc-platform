package com.knowbase.model.ollama;

import com.knowbase.model.EmbeddingModelClient;

import java.util.List;

public final class OllamaEmbeddingModelClient implements EmbeddingModelClient {

    private final OllamaClient ollamaClient;
    private final String provider;
    private final String modelName;
    private final int configuredDimension;
    private volatile int detectedDimension;

    public OllamaEmbeddingModelClient(
            OllamaClient ollamaClient,
            String provider,
            String modelName,
            int configuredDimension
    ) {
        this.ollamaClient = ollamaClient;
        this.provider = provider;
        this.modelName = modelName;
        this.configuredDimension = configuredDimension;
        this.detectedDimension = configuredDimension;
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
    public int dimension() {
        return detectedDimension > 0 ? detectedDimension : configuredDimension;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return embed(modelName, texts);
    }

    public List<float[]> embed(String model, List<String> texts) {
        List<float[]> vectors = ollamaClient.embed(model, texts);
        if (!vectors.isEmpty() && vectors.getFirst().length > 0) {
            detectedDimension = vectors.getFirst().length;
        }
        return vectors;
    }
}
