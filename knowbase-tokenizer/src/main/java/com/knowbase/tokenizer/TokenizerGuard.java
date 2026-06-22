package com.knowbase.tokenizer;

public final class TokenizerGuard {

    private final boolean allowApproximateForIndexing;

    public TokenizerGuard(boolean allowApproximateForIndexing) {
        this.allowApproximateForIndexing = allowApproximateForIndexing;
    }

    public void validateForIndexing(ModelTokenizer tokenizer, String provider, String modelName) {
        if (tokenizer == null) {
            throw new IllegalArgumentException("Tokenizer 不存在: " + provider + "/" + modelName);
        }
        if (tokenizer.approximate() && !allowApproximateForIndexing) {
            throw new IllegalStateException(
                    "当前配置禁止使用近似 tokenizer 发布索引: provider=%s, model=%s, tokenizer=%s"
                            .formatted(provider, modelName, tokenizer.tokenizerId())
            );
        }
    }
}
