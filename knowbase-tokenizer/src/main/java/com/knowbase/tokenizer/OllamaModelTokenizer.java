package com.knowbase.tokenizer;

import com.knowbase.model.ollama.OllamaClient;

import java.util.List;

public final class OllamaModelTokenizer implements ModelTokenizer, TokenIdCapable {

    private final OllamaClient ollamaClient;
    private final String modelName;
    private final String tokenizerId;
    private final ApproximateTokenizer fallback;
    private final boolean nativeTokenizer;

    public OllamaModelTokenizer(OllamaClient ollamaClient, String provider, String modelName) {
        this.ollamaClient = ollamaClient;
        this.modelName = modelName;
        this.tokenizerId = provider + ":" + modelName;
        this.fallback = new ApproximateTokenizer(tokenizerId + "-approx", modelName);
        this.nativeTokenizer = ollamaClient.supportsTokenize(modelName);
    }

    @Override
    public String tokenizerId() {
        return tokenizerId;
    }

    @Override
    public String tokenizerVersion() {
        return modelName;
    }

    @Override
    public boolean approximate() {
        return !nativeTokenizer;
    }

    @Override
    public TokenCount count(String text) {
        if (nativeTokenizer) {
            int tokens = tokenizeToIds(text).size();
            return new TokenCount(tokenizerId, modelName, tokens, false);
        }
        return fallback.count(text);
    }

    @Override
    public List<String> encode(String text) {
        return fallback.encode(text);
    }

    @Override
    public List<Integer> tokenizeToIds(String text) {
        if (!nativeTokenizer) {
            throw new UnsupportedOperationException("当前 Ollama 版本不支持 /api/tokenize，请升级 Ollama 或启用近似分词");
        }
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return ollamaClient.tokenize(modelName, text);
    }

    @Override
    public String detokenize(List<Integer> tokenIds) {
        if (!nativeTokenizer) {
            throw new UnsupportedOperationException("当前 Ollama 版本不支持 /api/detokenize，请升级 Ollama 或启用近似分词");
        }
        if (tokenIds == null || tokenIds.isEmpty()) {
            return "";
        }
        return ollamaClient.detokenize(modelName, tokenIds);
    }
}
