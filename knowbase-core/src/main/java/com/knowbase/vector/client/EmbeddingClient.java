package com.knowbase.vector.client;

import java.util.List;

/**
 * 向量化模型可插拔接口（一期默认 Ollama 实现）。
 */
public interface EmbeddingClient {

    String providerId();

    float[] embed(String text);

    List<float[]> embedBatch(List<String> texts);

    void validateOnStartup();
}
