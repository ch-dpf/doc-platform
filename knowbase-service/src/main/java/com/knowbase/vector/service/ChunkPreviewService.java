package com.knowbase.vector.service;



import com.knowbase.pipeline.config.ChunkProfileService;
import com.knowbase.pipeline.config.EffectiveConfigResolver;
import com.knowbase.pipeline.config.IngestProfileSupport;

import com.knowbase.pipeline.config.PlatformPipelineDefaults;

import com.knowbase.pipeline.chunk.ChunkPipelineResult;

import com.knowbase.pipeline.chunk.LibraryChunkPipeline;

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

    private final EffectiveConfigResolver effectiveConfigResolver;
    private final ChunkProfileService chunkProfileService;

    public ChunkPreviewService(
            LibraryChunkPipeline libraryChunkPipeline,
            ChunkingProperties defaultChunkingProperties,
            EffectiveConfigResolver effectiveConfigResolver,
            ChunkProfileService chunkProfileService) {
        this.libraryChunkPipeline = libraryChunkPipeline;
        this.defaultChunkingProperties = defaultChunkingProperties;
        this.effectiveConfigResolver = effectiveConfigResolver;
        this.chunkProfileService = chunkProfileService;
    }



    public ChunkPreviewResponse preview(ChunkPreviewRequest request) {

        ChunkPipelineResult result;

        if (request.libraryId() != null) {

            boolean hasIngestOverlay = (request.mimeType() != null && !request.mimeType().isBlank())

                    || (request.ingestProfileJson() != null && !request.ingestProfileJson().isBlank());

            if (hasIngestOverlay) {

                result = libraryChunkPipeline.chunkForIngest(

                        request.libraryId(),

                        request.mimeType(),

                        request.ingestProfileJson(),

                        request.sampleText());

            } else {

                result = libraryChunkPipeline.chunk(request.libraryId(), request.sampleText());

            }

        } else {

            result = libraryChunkPipeline.chunkWithRequestConfig(

                    null, request.sampleText(), toProperties(request));

        }

        return toResponse(request, result);
    }

    private ChunkPreviewResponse toResponse(ChunkPreviewRequest request, ChunkPipelineResult result) {

        List<ChunkPreviewResponse.ChunkPreviewItem> items = new ArrayList<>();

        for (int i = 0; i < result.chunks().size(); i++) {

            String c = result.chunks().get(i);

            items.add(new ChunkPreviewResponse.ChunkPreviewItem(i, c.length(), truncate(c, 500)));

        }

        String profileId = null;
        boolean primary = false;
        if (request.libraryId() != null) {
            String profileJson = IngestProfileSupport.prepareForUpload(request.ingestProfileJson());
            profileId = chunkProfileService.computeForIngestWithContent(
                    request.libraryId(), request.mimeType(), profileJson, request.sampleText());
            primary = chunkProfileService.isPrimaryProfile(request.libraryId(), profileId);
        }

        return new ChunkPreviewResponse(
                result.chunks().size(),
                result.processedText().length(),
                items,
                result.rawTotalChunks(),
                result.filteredOutCount(),
                result.contentFamilyWire(),
                result.chunkingStrategyWire(),
                result.chunkingAdjustmentReason(),
                result.multiGranularity(),
                profileId,
                primary);
    }



    private ChunkingProperties toProperties(ChunkPreviewRequest request) {

        ChunkingProperties base = resolveBaseChunking(request.mimeType());

        ChunkingProperties p = PlatformPipelineDefaults.copyChunking(base);

        p.setChunkSize(clamp(request.chunkSize(), 100, 8000));

        p.setOverlap(clamp(request.chunkOverlap(), 0, 2000));

        p.setMinChunkSize(clamp(request.minChunkSize(), 20, 2000));

        p.setMaxChunkSize(clamp(Math.max(request.maxChunkSize(), p.getChunkSize()), 200, 16000));

        p.setMinParagraphLength(clamp(request.minParagraphLength(), 0, 500));

        p.setNormalizeBeforeChunk(defaultChunkingProperties.isNormalizeBeforeChunk());

        p.setSemanticSimilarityThreshold(defaultChunkingProperties.getSemanticSimilarityThreshold());

        return p;

    }



    private ChunkingProperties resolveBaseChunking(String mimeType) {

        if (mimeType != null && !mimeType.isBlank()) {

            return effectiveConfigResolver.forMimeOnly(mimeType).chunking();

        }

        return defaultChunkingProperties;

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

