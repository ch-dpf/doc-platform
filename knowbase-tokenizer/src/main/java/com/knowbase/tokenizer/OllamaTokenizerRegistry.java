package com.knowbase.tokenizer;

import com.knowbase.model.ollama.OllamaClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class OllamaTokenizerRegistry implements TokenizerRegistry {

    private final OllamaClient ollamaClient;
    private final String provider;
    private final TokenizerRegistry fallbackRegistry;
    private final Map<String, ModelTokenizer> cache = new ConcurrentHashMap<>();

    public OllamaTokenizerRegistry(
            OllamaClient ollamaClient,
            String provider,
            TokenizerRegistry fallbackRegistry
    ) {
        this.ollamaClient = ollamaClient;
        this.provider = provider;
        this.fallbackRegistry = fallbackRegistry;
    }

    @Override
    public ModelTokenizer getTokenizer(String provider, String modelName) {
        if (!this.provider.equals(provider)) {
            return fallbackRegistry.getTokenizer(provider, modelName);
        }
        return cache.computeIfAbsent(
                modelName,
                model -> new OllamaModelTokenizer(ollamaClient, this.provider, model)
        );
    }
}
