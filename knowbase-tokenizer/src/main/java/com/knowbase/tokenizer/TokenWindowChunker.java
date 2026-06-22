package com.knowbase.tokenizer;

import java.util.List;

public interface TokenWindowChunker {

    List<TokenChunk> chunk(List<String> structuralSegments, ModelTokenizer tokenizer, ChunkingOptions options);
}
