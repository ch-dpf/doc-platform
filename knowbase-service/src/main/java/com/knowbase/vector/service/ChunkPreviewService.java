package com.knowbase.vector.service;

import com.knowbase.vector.chunk.ChunkPipelineResult;
import com.knowbase.vector.chunk.LibraryChunkPipeline;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.dto.ChunkPreviewRequest;
import com.knowbase.vector.dto.ChunkPreviewResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkPreviewService {

    private final LibraryChunkPipeline libraryChunkPipeline;
    private final ChunkingProperties defaultChunkingProperties;

    public ChunkPreviewService(
            LibraryChunkPipeline libraryChunkPipeline,
            ChunkingProperties defaultChunkingProperties) {
        this.libraryChunkPipeline = libraryChunkPipeline;
        this.defaultChunkingProperties = defaultChunkingProperties;
    }

    public ChunkPreviewResponse preview(ChunkPreviewRequest request) {
        ChunkPipelineResult result;
        if (request.libraryId() != null) {
            result = libraryChunkPipeline.chunk(request.libraryId(), request.sampleText());
        } else {
            result = libraryChunkPipeline.chunkWithRequestConfig(
                    null,
                    request.sampleText(),
                    request.textNormalizationEnabled(),
                    request.textNormalization(),
                    request.cleaning(),
                    toProperties(request));
        }
        return toResponse(result);
    }

    private ChunkPreviewResponse toResponse(ChunkPipelineResult result) {
        List<ChunkPreviewResponse.ChunkPreviewItem> items = new ArrayList<>();
        for (int i = 0; i < result.chunks().size(); i++) {
            String c = result.chunks().get(i);
            items.add(new ChunkPreviewResponse.ChunkPreviewItem(i, c.length(), truncate(c, 500)));
        }
        return new ChunkPreviewResponse(
                result.chunks().size(),
                result.processedText().length(),
                items,
                result.rawTotalChunks(),
                result.filteredOutCount());
    }

    private ChunkingProperties toProperties(ChunkPreviewRequest request) {
        ChunkingProperties p = new ChunkingProperties();
        p.setStrategy(request.chunkingStrategy());
        p.setChunkSize(clamp(request.chunkSize(), 100, 8000));
        p.setOverlap(clamp(request.chunkOverlap(), 0, 2000));
        p.setMinChunkSize(clamp(request.minChunkSize(), 20, 2000));
        p.setMaxChunkSize(clamp(Math.max(request.maxChunkSize(), p.getChunkSize()), 200, 16000));
        p.setMinParagraphLength(clamp(request.minParagraphLength(), 0, 500));
        p.setNormalizeBeforeChunk(request.normalizeBeforeChunk());
        p.setSemanticSimilarityThreshold(defaultChunkingProperties.getSemanticSimilarityThreshold());
        return p;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
