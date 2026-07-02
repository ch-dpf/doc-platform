package com.knowbase.tokenizer;

import java.util.List;

public final class ProfileBackedTokenizer implements ModelTokenizer {

    private final String tokenizerId;
    private final String tokenizerVersion;
    private final boolean approximate;
    private final ModelTokenizer delegate;

    public ProfileBackedTokenizer(
            String tokenizerId,
            String tokenizerVersion,
            boolean approximate,
            ModelTokenizer delegate
    ) {
        this.tokenizerId = tokenizerId;
        this.tokenizerVersion = tokenizerVersion;
        this.approximate = approximate;
        this.delegate = delegate;
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
        return approximate || delegate.approximate();
    }

    @Override
    public TokenCount count(String text) {
        TokenCount count = delegate.count(text);
        return new TokenCount(tokenizerId(), tokenizerVersion(), count.tokens(), approximate());
    }

    @Override
    public List<String> encode(String text) {
        return delegate.encode(text);
    }
}
