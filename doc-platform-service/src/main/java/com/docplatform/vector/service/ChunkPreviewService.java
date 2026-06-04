package com.docplatform.vector.service;

import com.docplatform.ingest.support.ParsedTextNormalizer;
import com.docplatform.library.config.TextNormalizationSettings;
import com.docplatform.vector.config.ChunkingProperties;
import com.docplatform.vector.dto.ChunkPreviewRequest;
import com.docplatform.vector.dto.ChunkPreviewResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkPreviewService {

    private final ChunkingService chunkingService;
    private final ParsedTextNormalizer textNormalizer;

    public ChunkPreviewService(ChunkingService chunkingService, ParsedTextNormalizer textNormalizer) {
        this.chunkingService = chunkingService;
        this.textNormalizer = textNormalizer;
    }

    public ChunkPreviewResponse preview(ChunkPreviewRequest request) {
        String text = request.sampleText();
        if (request.textNormalizationEnabled()) {
            TextNormalizationSettings settings = request.textNormalization();
            text = settings != null ? textNormalizer.normalize(text, settings) : textNormalizer.normalize(text);
        }
        ChunkingProperties props = toProperties(request);
        List<String> chunks = chunkingService.chunk(text, props);
        List<ChunkPreviewResponse.ChunkPreviewItem> items = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String c = chunks.get(i);
            items.add(new ChunkPreviewResponse.ChunkPreviewItem(i, c.length(), truncate(c, 500)));
        }
        return new ChunkPreviewResponse(chunks.size(), text.length(), items);
    }

    private static ChunkingProperties toProperties(ChunkPreviewRequest request) {
        ChunkingProperties p = new ChunkingProperties();
        p.setStrategy(request.chunkingStrategy());
        p.setChunkSize(request.chunkSize());
        p.setOverlap(request.chunkOverlap());
        p.setMinChunkSize(request.minChunkSize());
        p.setMaxChunkSize(request.maxChunkSize());
        p.setMinParagraphLength(request.minParagraphLength());
        p.setNormalizeBeforeChunk(request.normalizeBeforeChunk());
        return p;
    }

    private static String truncate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "…";
    }
}
