package com.knowbase.vector.chunk;

import com.knowbase.ingest.support.DocumentCleaningService;
import com.knowbase.ingest.support.ParsedTextNormalizer;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.TextNormalizationSettings;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.service.ChunkingService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryChunkPipeline {

    private final ChunkingService chunkingService;
    private final ParsedTextNormalizer textNormalizer;
    private final DocumentCleaningService documentCleaningService;
    private final LibraryConfigResolver libraryConfigResolver;

    public LibraryChunkPipeline(
            ChunkingService chunkingService,
            ParsedTextNormalizer textNormalizer,
            DocumentCleaningService documentCleaningService,
            LibraryConfigResolver libraryConfigResolver) {
        this.chunkingService = chunkingService;
        this.textNormalizer = textNormalizer;
        this.documentCleaningService = documentCleaningService;
        this.libraryConfigResolver = libraryConfigResolver;
    }

    /** Full preview path for an existing library: normalize (if enabled), clean, chunk, filter. */
    public ChunkPipelineResult chunk(UUID libraryId, String rawText) {
        String text = rawText;
        if (libraryConfigResolver.config(libraryId).isTextNormalizationEnabled()) {
            TextNormalizationSettings settings = libraryConfigResolver.normalizationFor(libraryId);
            text = textNormalizer.normalize(text, settings);
        }
        text = documentCleaningService.apply(text, libraryConfigResolver.cleaningFor(libraryId));
        ChunkingProperties chunking = libraryConfigResolver.chunkingFor(libraryId);
        return chunkAndFilter(libraryId, text, chunking);
    }

    /** Preview path with request-level config (wizard pre-create, libraryId absent). */
    public ChunkPipelineResult chunkWithRequestConfig(
            UUID libraryId,
            String rawText,
            boolean textNormalizationEnabled,
            TextNormalizationSettings textNormalization,
            CleaningRulesSettings cleaning,
            ChunkingProperties chunking) {
        String text = rawText;
        if (textNormalizationEnabled) {
            text = textNormalization != null
                    ? textNormalizer.normalize(text, textNormalization)
                    : textNormalizer.normalize(text);
        }
        text = documentCleaningService.apply(text, cleaning);
        return chunkAndFilter(libraryId, text, chunking);
    }

    /** Index path: parsed.txt is already normalized+cleaned — chunk and filter only. */
    public ChunkPipelineResult chunkIndexedText(UUID libraryId, String parsedText) {
        ChunkingProperties chunking = libraryConfigResolver.chunkingFor(libraryId);
        return chunkAndFilter(libraryId, parsedText, chunking);
    }

    private ChunkPipelineResult chunkAndFilter(UUID libraryId, String text, ChunkingProperties chunking) {
        List<String> rawChunks = chunkingService.chunk(libraryId, text, chunking);
        int rawTotalChunks = rawChunks.size();
        List<String> filtered = IndexingChunkFilter.removeHeaderOnlyChunks(rawChunks);
        int filteredOutCount = rawTotalChunks - filtered.size();
        return new ChunkPipelineResult(filtered, rawTotalChunks, filteredOutCount, text);
    }
}
