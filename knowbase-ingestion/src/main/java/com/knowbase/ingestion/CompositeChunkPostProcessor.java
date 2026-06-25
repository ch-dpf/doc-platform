package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CompositeChunkPostProcessor implements ChunkPostProcessor {

    private final List<ChunkPostProcessor> processors;

    public CompositeChunkPostProcessor(List<ChunkPostProcessor> processors) {
        this.processors = List.copyOf(Objects.requireNonNull(processors, "processors"));
    }

    @Override
    public boolean supports(ChunkPostProcessContext context) {
        return processors.stream().anyMatch(processor -> processor.supports(context));
    }

    @Override
    public List<DocumentChunk> process(List<DocumentChunk> chunks, ChunkPostProcessContext context) {
        List<DocumentChunk> result = chunks;
        for (ChunkPostProcessor processor : processors) {
            if (processor.supports(context)) {
                result = processor.process(result, context);
            }
        }
        return result;
    }

    public static ChunkPostProcessor of(ChunkPostProcessor... processors) {
        return new CompositeChunkPostProcessor(List.of(processors));
    }

    public static ChunkPostProcessor noop() {
        return new CompositeChunkPostProcessor(List.of());
    }
}
