package com.knowbase.pipeline.chunk;

import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.chunk.HeadingLevelChunker;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.service.ChunkingService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 多粒度分块：按标题切父段，父段内 paragraph-first 切子块；仅子块进入向量索引。
 */
public final class HierarchicalChunker {

    private static final int MAX_PARENT_CONTEXT_CHARS = 2400;

    private HierarchicalChunker() {
    }

    public static List<PipelineChunk> chunk(
            UUID libraryId, String text, ChunkingProperties parentProps, ChunkingService chunkingService) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> sections = HeadingLevelChunker.splitSections(text);
        if (sections.isEmpty()) {
            return List.of();
        }

        ChunkingProperties childProps = copyForChild(parentProps);
        List<PipelineChunk> out = new ArrayList<>();
        for (int parentIndex = 0; parentIndex < sections.size(); parentIndex++) {
            String section = sections.get(parentIndex).strip();
            if (section.isEmpty()) {
                continue;
            }
            if (section.length() <= parentProps.getChunkSize()) {
                out.add(PipelineChunk.leaf(section));
                continue;
            }
            String parentContext = truncate(section, MAX_PARENT_CONTEXT_CHARS);
            List<String> children = chunkingService.chunk(libraryId, section, childProps);
            for (String child : children) {
                if (child == null || child.isBlank()) {
                    continue;
                }
                out.add(new PipelineChunk(child.strip(), parentContext, parentIndex));
            }
        }
        return out;
    }

    private static ChunkingProperties copyForChild(ChunkingProperties parent) {
        ChunkingProperties child = new ChunkingProperties();
        child.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);
        child.setChunkSize(parent.getChunkSize());
        child.setOverlap(parent.getOverlap());
        child.setMinChunkSize(parent.getMinChunkSize());
        child.setMaxChunkSize(parent.getMaxChunkSize());
        child.setMinParagraphLength(parent.getMinParagraphLength());
        child.setNormalizeBeforeChunk(parent.isNormalizeBeforeChunk());
        child.setSemanticSimilarityThreshold(parent.getSemanticSimilarityThreshold());
        return child;
    }

    private static String truncate(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + "…";
    }
}
