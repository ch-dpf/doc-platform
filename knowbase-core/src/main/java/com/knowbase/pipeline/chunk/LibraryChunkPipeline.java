package com.knowbase.pipeline.chunk;



import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.ingest.support.DocumentCleaningService;

import com.knowbase.ingest.support.ParsedTextNormalizer;

import com.knowbase.library.service.LibraryConfigResolver;

import com.knowbase.pipeline.config.EffectiveConfigResolver;

import com.knowbase.pipeline.config.EffectivePipelineConfig;

import com.knowbase.pipeline.config.IngestProfile;

import com.knowbase.pipeline.config.IngestProfileSupport;

import com.knowbase.pipeline.config.PlatformPipelineDefaults;
import com.knowbase.pipeline.content.ContentSignalsSupport;

import com.knowbase.pipeline.content.ContentFamily;
import com.knowbase.vector.chunk.ChunkTextPreprocessor;
import com.knowbase.vector.chunk.IndexingChunkFilter;
import com.knowbase.vector.chunk.TabularSectionContextInjector;

import com.knowbase.vector.config.ChunkingProperties;

import com.knowbase.vector.service.ChunkingService;

import org.springframework.stereotype.Service;



import java.util.ArrayList;

import java.util.List;

import java.util.UUID;



@Service

public class LibraryChunkPipeline {



    private final ChunkingService chunkingService;

    private final ParsedTextNormalizer textNormalizer;

    private final DocumentCleaningService documentCleaningService;

    private final LibraryConfigResolver libraryConfigResolver;

    private final EffectiveConfigResolver effectiveConfigResolver;

    private final DocMetadataStore docMetadataStore;



    public LibraryChunkPipeline(

            ChunkingService chunkingService,

            ParsedTextNormalizer textNormalizer,

            DocumentCleaningService documentCleaningService,

            LibraryConfigResolver libraryConfigResolver,

            EffectiveConfigResolver effectiveConfigResolver,

            DocMetadataStore docMetadataStore) {

        this.chunkingService = chunkingService;

        this.textNormalizer = textNormalizer;

        this.documentCleaningService = documentCleaningService;

        this.libraryConfigResolver = libraryConfigResolver;

        this.effectiveConfigResolver = effectiveConfigResolver;

        this.docMetadataStore = docMetadataStore;

    }



    public ChunkPipelineResult chunkForDocument(UUID libraryId, UUID docId, String rawText) {

        EffectivePipelineConfig effective =

                effectiveConfigResolver.forDocumentWithContent(libraryId, docId, rawText);

        return chunkWithEffective(libraryId, docId, effective, rawText);

    }



    public ChunkPipelineResult chunkForIngest(

            UUID libraryId, String mimeType, String ingestProfileJson, String rawText) {

        IngestProfile profile = IngestProfileSupport.parse(ingestProfileJson);

        EffectivePipelineConfig effective =

                effectiveConfigResolver.forIngestWithContent(libraryId, mimeType, profile, rawText);

        return chunkWithEffective(libraryId, null, effective, rawText);

    }



    public ChunkPipelineResult chunk(UUID libraryId, String rawText) {

        return chunkForIngest(libraryId, null, null, rawText);

    }



    public ChunkPipelineResult chunkWithRequestConfig(

            UUID libraryId, String rawText, ChunkingProperties chunking) {

        String text = rawText;

        if (libraryConfigResolver.systemTextNormalizationEnabled()) {

            text = textNormalizer.normalize(text, libraryConfigResolver.systemNormalization());

        }

        text = documentCleaningService.apply(text, PlatformPipelineDefaults.baselineCleaning());

        return chunkAndFilter(libraryId, text, chunking, null, null);

    }



    public ChunkPipelineResult chunkIndexedText(UUID libraryId, UUID docId, String parsedText) {

        EffectivePipelineConfig effective =

                effectiveConfigResolver.forDocumentWithContent(libraryId, docId, parsedText);

        return chunkAndFilter(libraryId, parsedText, effective.chunking(), effective, docId);

    }

    /** 批量重索引 / 迁移到主档：按当前库规则重切，忽略采集级分块覆盖。 */
    public ChunkPipelineResult chunkForLibraryRebuild(UUID libraryId, UUID docId, String parsedText) {
        EffectivePipelineConfig effective =
                effectiveConfigResolver.forDocumentLibraryRebuild(libraryId, docId, parsedText);
        return chunkWithEffective(libraryId, docId, effective, parsedText);
    }



    /** @deprecated 使用 {@link #chunkIndexedText(UUID, UUID, String)} */

    public ChunkPipelineResult chunkIndexedText(UUID libraryId, String parsedText) {

        ChunkingProperties chunking = libraryConfigResolver.chunkingFor(libraryId);

        return chunkAndFilter(libraryId, parsedText, chunking, null, null);

    }



    private ChunkPipelineResult chunkWithEffective(

            UUID libraryId, UUID docId, EffectivePipelineConfig effective, String rawText) {

        String text = rawText;

        if (effective.isTextNormalizationEnabled()) {

            text = textNormalizer.normalize(text, effective.normalization());

        }

        text = documentCleaningService.apply(text, effective.cleaning());

        return chunkAndFilter(libraryId, text, effective.chunking(), effective, docId);

    }



    private ChunkPipelineResult chunkAndFilter(

            UUID libraryId,
            String text,
            ChunkingProperties chunking,
            EffectivePipelineConfig effective,
            UUID docId) {

        text = ChunkTextPreprocessor.prepare(text);

        boolean multiGranularity = effective != null

                && HierarchicalChunkingPolicy.shouldApply(

                        effective.contentFamily(), chunking, effective.contentSignals(), text);



        List<PipelineChunk> rawPipelineChunks;

        if (multiGranularity) {

            rawPipelineChunks = HierarchicalChunker.chunk(libraryId, text, chunking, chunkingService);

        } else {

            List<String> rawChunks = chunkingService.chunk(libraryId, text, chunking);

            rawPipelineChunks = rawChunks.stream().map(PipelineChunk::leaf).toList();

        }

        if (shouldInjectTabularSectionContext(effective, text)) {
            rawPipelineChunks = TabularSectionContextInjector.inject(
                    rawPipelineChunks, text, resolveFileName(docId));
        }

        int rawTotalChunks = rawPipelineChunks.size();

        List<PipelineChunk> filtered = filterPipelineChunks(rawPipelineChunks);

        int filteredOutCount = rawTotalChunks - filtered.size();



        return new ChunkPipelineResult(

                filtered,

                rawTotalChunks,

                filteredOutCount,

                text,

                traceFamily(effective),

                traceStrategy(effective),

                traceAdjustment(effective),
                multiGranularity,
                ContentSignalsSupport.toJson(effective != null ? effective.contentSignals() : null));

    }



    private String resolveFileName(UUID docId) {
        if (docId == null) {
            return null;
        }
        return docMetadataStore.findById(docId)
                .map(DocMetadata::getFileName)
                .orElse(null);
    }

    private static boolean shouldInjectTabularSectionContext(
            EffectivePipelineConfig effective, String text) {
        ContentFamily family = effective != null ? effective.contentFamily() : null;
        return TabularSectionContextInjector.shouldApply(family, text);
    }

    private static List<PipelineChunk> filterPipelineChunks(List<PipelineChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<String> contents = chunks.stream().map(PipelineChunk::content).toList();
        List<String> filtered = IndexingChunkFilter.removeHeaderOnlyChunks(contents);
        if (filtered.size() == contents.size() || filtered.isEmpty()) {
            return chunks;
        }
        List<PipelineChunk> kept = new ArrayList<>(filtered.size());
        int fi = 0;
        for (int i = 0; i < chunks.size(); i++) {
            if (fi < filtered.size() && contents.get(i).equals(filtered.get(fi))) {
                kept.add(chunks.get(i));
                fi++;
            }
        }
        return kept.isEmpty() ? chunks : kept;
    }



    private static String traceFamily(EffectivePipelineConfig effective) {

        return effective != null && effective.contentFamily() != null

                ? effective.contentFamily().toWire()

                : null;

    }



    private static String traceStrategy(EffectivePipelineConfig effective) {

        if (effective == null || effective.chunking() == null || effective.chunking().getStrategy() == null) {

            return null;

        }

        return effective.chunking().getStrategy().toWire();

    }



    private static String traceAdjustment(EffectivePipelineConfig effective) {

        return effective != null && effective.contentSignals() != null

                ? effective.contentSignals().getChunkingAdjustmentReason()

                : null;

    }

}

