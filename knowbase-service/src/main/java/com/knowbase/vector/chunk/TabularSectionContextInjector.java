package com.knowbase.vector.chunk;

import com.knowbase.ingest.parse.TabularRowLinearizer;
import com.knowbase.pipeline.chunk.PipelineChunk;
import com.knowbase.pipeline.content.ContentFamily;

import java.util.ArrayList;
import java.util.List;

/** 为表格类分块注入 section 上下文前缀（P0：补偿表头块过滤丢失的语义）。 */
public final class TabularSectionContextInjector {

    private TabularSectionContextInjector() {}

    public static boolean shouldApply(ContentFamily family, String text) {
        if (family == ContentFamily.TABULAR) {
            return true;
        }
        return text != null && TabularRowLinearizer.looksTabular(text);
    }

    public static List<PipelineChunk> inject(List<PipelineChunk> chunks, String sourceText) {
        return inject(chunks, sourceText, null);
    }

    public static List<PipelineChunk> inject(List<PipelineChunk> chunks, String sourceText, String fileName) {
        if (chunks == null || chunks.isEmpty() || sourceText == null || sourceText.isBlank()) {
            return chunks == null ? List.of() : chunks;
        }
        String normalizedSource = TabularContinuationNormalizer.joinContinuations(sourceText);
        TabularSectionContextIndex index = TabularSectionContextIndex.parse(normalizedSource, fileName);
        if (index.isEmpty()) {
            return chunks;
        }
        List<PipelineChunk> out = new ArrayList<>(chunks.size());
        for (PipelineChunk chunk : chunks) {
            String injected = index.injectPrefix(chunk.content());
            if (injected.equals(chunk.content())) {
                out.add(chunk);
            } else {
                out.add(new PipelineChunk(injected, chunk.parentContext(), chunk.parentIndex()));
            }
        }
        return out;
    }
}
