package com.knowbase.tokenizer;

public record ChunkingOptions(
        int maxTokens,
        int overlapTokens,
        int minTokens,
        boolean preserveStructureBoundary
) {
}
