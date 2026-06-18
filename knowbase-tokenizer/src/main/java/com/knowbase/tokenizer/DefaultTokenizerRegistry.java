package com.knowbase.tokenizer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DefaultTokenizerRegistry implements TokenizerRegistry {

    private final Map<String, ModelTokenizer> tokenizers = new ConcurrentHashMap<>();

    public DefaultTokenizerRegistry() {
        register("ollama", "bge-m3", new ApproximateTokenizer("approx-bge-m3", "1"));
        register("ollama", "llama3.2", new ApproximateTokenizer("approx-llama3.2", "1"));
        register("ollama", "llama3", new ApproximateTokenizer("approx-llama3", "1"));
        register("default", "default", new ApproximateTokenizer("approx-default", "1"));
    }

    public void register(String provider, String modelName, ModelTokenizer tokenizer) {
        tokenizers.put(key(provider, modelName), tokenizer);
    }

    @Override
    public ModelTokenizer getTokenizer(String provider, String modelName) {
        ModelTokenizer tokenizer = tokenizers.get(key(provider, modelName));
        if (tokenizer != null) {
            return tokenizer;
        }
        return tokenizers.get(key("default", "default"));
    }

    private static String key(String provider, String modelName) {
        return provider + ":" + modelName;
    }
}
