package com.knowbase.tokenizer;

public record TokenCount(
        String tokenizerId,
        String tokenizerVersion,
        int tokens,
        boolean approximate
) {
}
