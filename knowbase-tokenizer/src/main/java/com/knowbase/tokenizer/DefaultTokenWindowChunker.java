package com.knowbase.tokenizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DefaultTokenWindowChunker implements TokenWindowChunker {

    @Override
    public List<TokenChunk> chunk(List<String> structuralSegments, ModelTokenizer tokenizer, ChunkingOptions options) {
        List<TokenChunk> chunks = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int bufferTokens = 0;
        int ordinal = 0;

        for (String segment : structuralSegments) {
            if (segment == null || segment.isBlank()) {
                continue;
            }
            int segmentTokens = tokenizer.count(segment).tokens();
            if (segmentTokens > options.maxTokens()) {
                flush(buffer, bufferTokens, ordinal++, chunks, options);
                buffer = new StringBuilder();
                bufferTokens = 0;
                chunks.addAll(splitLargeSegment(segment, tokenizer, options, ordinal));
                ordinal = chunks.size();
                continue;
            }
            if (bufferTokens + segmentTokens > options.maxTokens() && bufferTokens > 0) {
                flush(buffer, bufferTokens, ordinal++, chunks, options);
                buffer = new StringBuilder();
                bufferTokens = 0;
                if (options.overlapTokens() > 0 && !chunks.isEmpty()) {
                    String overlap = tailForOverlap(
                            chunks.get(chunks.size() - 1).content(),
                            tokenizer,
                            options.overlapTokens()
                    );
                    buffer.append(overlap);
                    bufferTokens = tokenizer.count(overlap).tokens();
                }
            }
            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(segment);
            bufferTokens += segmentTokens;
        }
        flush(buffer, bufferTokens, ordinal, chunks, options);
        return chunks;
    }

    private static void flush(
            StringBuilder buffer,
            int bufferTokens,
            int ordinal,
            List<TokenChunk> chunks,
            ChunkingOptions options
    ) {
        if (buffer.isEmpty() || bufferTokens < options.minTokens()) {
            return;
        }
        chunks.add(new TokenChunk(
                buffer.toString().trim(),
                bufferTokens,
                ordinal,
                "token_window",
                Map.of()
        ));
    }

    private static List<TokenChunk> splitLargeSegment(
            String segment,
            ModelTokenizer tokenizer,
            ChunkingOptions options,
            int startOrdinal
    ) {
        if (tokenizer instanceof TokenIdCapable capable && !tokenizer.approximate()) {
            return splitLargeSegmentByTokenIds(segment, capable, tokenizer, options, startOrdinal);
        }
        List<TokenChunk> chunks = new ArrayList<>();
        List<String> tokens = tokenizer.encode(segment);
        int start = 0;
        int ordinal = startOrdinal;
        while (start < tokens.size()) {
            int end = Math.min(tokens.size(), start + options.maxTokens());
            String content = String.join(" ", tokens.subList(start, end));
            int tokenCount = tokenizer.count(content).tokens();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("split", true);
            chunks.add(new TokenChunk(content, tokenCount, ordinal++, "token_window_split", metadata));
            if (end >= tokens.size()) {
                break;
            }
            start = Math.max(start + 1, end - options.overlapTokens());
        }
        return chunks;
    }

    private static List<TokenChunk> splitLargeSegmentByTokenIds(
            String segment,
            TokenIdCapable capable,
            ModelTokenizer tokenizer,
            ChunkingOptions options,
            int startOrdinal
    ) {
        List<TokenChunk> chunks = new ArrayList<>();
        List<Integer> tokenIds = capable.tokenizeToIds(segment);
        int start = 0;
        int ordinal = startOrdinal;
        while (start < tokenIds.size()) {
            int end = Math.min(tokenIds.size(), start + options.maxTokens());
            String content = capable.detokenize(tokenIds.subList(start, end)).trim();
            if (!content.isBlank()) {
                int tokenCount = end - start;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("split", true);
                chunks.add(new TokenChunk(content, tokenCount, ordinal++, "token_window_split", metadata));
            }
            if (end >= tokenIds.size()) {
                break;
            }
            start = Math.max(start + 1, end - options.overlapTokens());
        }
        return chunks;
    }

    private static String tailForOverlap(String content, ModelTokenizer tokenizer, int overlapTokens) {
        if (content == null || content.isBlank() || overlapTokens <= 0) {
            return "";
        }
        if (tokenizer instanceof TokenIdCapable capable && !tokenizer.approximate()) {
            List<Integer> tokenIds = capable.tokenizeToIds(content);
            int start = Math.max(0, tokenIds.size() - overlapTokens);
            return capable.detokenize(tokenIds.subList(start, tokenIds.size()));
        }
        return tailForOverlap(content, overlapTokens);
    }

    private static String tailForOverlap(String content, int overlapTokens) {
        if (content == null || content.isBlank() || overlapTokens <= 0) {
            return "";
        }
        String[] parts = content.split("\\s+");
        int start = Math.max(0, parts.length - overlapTokens);
        return String.join(" ", java.util.Arrays.copyOfRange(parts, start, parts.length));
    }
}
