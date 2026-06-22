package com.knowbase.tokenizer;

import java.util.ArrayList;
import java.util.List;

public final class ApproximateTokenizer implements ModelTokenizer {

    private final String tokenizerId;
    private final String tokenizerVersion;

    public ApproximateTokenizer(String tokenizerId, String tokenizerVersion) {
        this.tokenizerId = tokenizerId;
        this.tokenizerVersion = tokenizerVersion;
    }

    @Override
    public String tokenizerId() {
        return tokenizerId;
    }

    @Override
    public String tokenizerVersion() {
        return tokenizerVersion;
    }

    @Override
    public boolean approximate() {
        return true;
    }

    @Override
    public TokenCount count(String text) {
        int tokens = estimateTokens(text);
        return new TokenCount(tokenizerId, tokenizerVersion, tokens, true);
    }

    @Override
    public List<String> encode(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String segment : text.split("\\s+")) {
            if (!segment.isBlank()) {
                tokens.add(segment);
            }
        }
        return tokens;
    }

    static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }
}
