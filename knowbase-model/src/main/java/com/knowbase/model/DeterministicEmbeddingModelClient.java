package com.knowbase.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DeterministicEmbeddingModelClient implements EmbeddingModelClient {

    private final String provider;
    private final String modelName;
    private final int dimension;

    public DeterministicEmbeddingModelClient(String provider, String modelName, int dimension) {
        this.provider = provider;
        this.modelName = modelName;
        this.dimension = dimension;
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
        return dimension;
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embedOne(text));
        }
        return vectors;
    }

    private float[] embedOne(String text) {
        float[] vector = new float[dimension];
        if (text == null || text.isBlank()) {
            return vector;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        byte[] bytes = normalized.getBytes(StandardCharsets.UTF_8);
        for (int index = 0; index < bytes.length; index++) {
            vector[index % dimension] += (bytes[index] & 0xFF) / 255.0f;
        }
        for (String token : normalized.split("\\s+")) {
            int hash = token.hashCode();
            vector[Math.floorMod(hash, dimension)] += 1.0f;
        }
        normalize(vector);
        return vector;
    }

    private static void normalize(float[] vector) {
        double sum = 0.0d;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0d) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= norm;
        }
    }
}
