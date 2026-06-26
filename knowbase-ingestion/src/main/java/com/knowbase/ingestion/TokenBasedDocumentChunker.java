package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.tokenizer.ChunkingOptions;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.TokenChunk;
import com.knowbase.tokenizer.TokenWindowChunker;
import com.knowbase.tokenizer.TokenizerGuard;
import com.knowbase.ingestion.smart.SmartStructureDocumentChunker;
import com.knowbase.ingestion.smart.SmartTableDocumentChunker;
import com.knowbase.ingestion.parse.OcrChunkMetadataSupport;
import com.knowbase.tokenizer.TokenizerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TokenBasedDocumentChunker implements DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(TokenBasedDocumentChunker.class);

    private final TokenizerRegistry tokenizerRegistry;
    private final TokenWindowChunker tokenWindowChunker;
    private final TokenizerGuard tokenizerGuard;
    private final StructureSegmenter structureSegmenter = new StructureSegmenter();
    private final BoundaryAdjuster boundaryAdjuster = new BoundaryAdjuster();
    private final RecursiveCharacterSplitter recursiveCharacterSplitter = new RecursiveCharacterSplitter();
    private final CharacterWindowChunker characterWindowChunker = new CharacterWindowChunker();
    private final SmartStructureDocumentChunker smartStructureDocumentChunker;
    private final SmartTableDocumentChunker smartTableDocumentChunker;

    public TokenBasedDocumentChunker(TokenizerRegistry tokenizerRegistry, TokenWindowChunker tokenWindowChunker) {
        this(tokenizerRegistry, tokenWindowChunker, new TokenizerGuard(true), new SmartStructureDocumentChunker(), new SmartTableDocumentChunker());
    }

    public TokenBasedDocumentChunker(
            TokenizerRegistry tokenizerRegistry,
            TokenWindowChunker tokenWindowChunker,
            TokenizerGuard tokenizerGuard
    ) {
        this(tokenizerRegistry, tokenWindowChunker, tokenizerGuard, new SmartStructureDocumentChunker(), new SmartTableDocumentChunker());
    }

    public TokenBasedDocumentChunker(
            TokenizerRegistry tokenizerRegistry,
            TokenWindowChunker tokenWindowChunker,
            TokenizerGuard tokenizerGuard,
            SmartStructureDocumentChunker smartStructureDocumentChunker
    ) {
        this(tokenizerRegistry, tokenWindowChunker, tokenizerGuard, smartStructureDocumentChunker, new SmartTableDocumentChunker());
    }

    public TokenBasedDocumentChunker(
            TokenizerRegistry tokenizerRegistry,
            TokenWindowChunker tokenWindowChunker,
            TokenizerGuard tokenizerGuard,
            SmartStructureDocumentChunker smartStructureDocumentChunker,
            SmartTableDocumentChunker smartTableDocumentChunker
    ) {
        this.tokenizerRegistry = tokenizerRegistry;
        this.tokenWindowChunker = tokenWindowChunker;
        this.tokenizerGuard = tokenizerGuard;
        this.smartStructureDocumentChunker = smartStructureDocumentChunker == null
                ? new SmartStructureDocumentChunker()
                : smartStructureDocumentChunker;
        this.smartTableDocumentChunker = smartTableDocumentChunker == null
                ? new SmartTableDocumentChunker()
                : smartTableDocumentChunker;
    }

    @Override
    public List<DocumentChunk> chunk(UUID libraryId, UUID documentId, ParsedDocument document) {
        throw new UnsupportedOperationException("请使用带 LibraryProfile 的 chunk 重载方法");
    }

    public List<DocumentChunk> chunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            LibraryProfile profile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer
    ) {
        return chunk(libraryId, documentId, indexVersionId, document, profile, documentProfile, tokenizer, Map.of());
    }

    public List<DocumentChunk> chunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            LibraryProfile profile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            Map<String, Object> requestOptions
    ) {
        tokenizerGuard.validateForIndexing(tokenizer, profile.embeddingProvider(), profile.embeddingModel());
        if (SmartTableDocumentChunker.shouldUse(document, documentProfile, requestOptions)) {
            List<DocumentChunk> chunks = smartTableDocumentChunker.chunk(
                    libraryId,
                    documentId,
                    indexVersionId,
                    document,
                    profile,
                    documentProfile,
                    tokenizer,
                    requestOptions
            );
            logChunkComplete(document, "smart-table", profile, documentProfile, chunks);
            return chunks;
        }
        if (SmartStructureDocumentChunker.shouldUseSmartEngine(documentProfile, requestOptions)) {
            List<DocumentChunk> chunks = smartStructureDocumentChunker.chunk(
                    libraryId,
                    documentId,
                    indexVersionId,
                    document,
                    profile,
                    documentProfile,
                    tokenizer,
                    requestOptions
            );
            logChunkComplete(document, "smart-structure", profile, documentProfile, chunks);
            return chunks;
        }
        SegmentationConfig config = SegmentationConfigResolver.resolve(profile, documentProfile, requestOptions);
        List<StructuralSegment> structuralSegments = structureSegmenter.segment(document, documentProfile);
        List<DocumentChunk> chunks = new ArrayList<>();
        int flatOrdinal = 0;
        for (StructuralSegment structuralSegment : structuralSegments) {
            if (config.chunkMode() == SegmentationConfig.ChunkMode.FLAT
                    && config.sizeUnit() == SegmentationConfig.SizeUnit.CHAR) {
                String wholeSegment = structuralSegment.content();
                if (wholeSegment != null
                        && !wholeSegment.isBlank()
                        && wholeSegment.length() <= config.chunkMaxChars()) {
                    chunks.add(flatChunkFromText(
                            libraryId,
                            documentId,
                            indexVersionId,
                            document,
                            documentProfile,
                            structuralSegment,
                            wholeSegment.trim(),
                            tokenizer,
                            profile.embeddingModel(),
                            flatOrdinal++
                    ));
                    continue;
                }
            }
            List<String> segmentTexts = buildCandidateTexts(structuralSegment, config);
            if (config.chunkMode() == SegmentationConfig.ChunkMode.FLAT) {
                if (config.sizeUnit() == SegmentationConfig.SizeUnit.CHAR) {
                    List<String> windows = characterWindowChunker.chunk(
                            segmentTexts,
                            config.chunkMaxChars(),
                            config.chunkOverlapChars(),
                            config.minChunkChars()
                    );
                    for (String window : windows) {
                        chunks.add(flatChunkFromText(
                                libraryId,
                                documentId,
                                indexVersionId,
                                document,
                                documentProfile,
                                structuralSegment,
                                window,
                                tokenizer,
                                profile.embeddingModel(),
                                flatOrdinal++
                        ));
                    }
                } else {
                    List<TokenChunk> tokenChunks = tokenChunksWithCharacterFallback(segmentTexts, tokenizer, config);
                    for (TokenChunk tokenChunk : tokenChunks) {
                        chunks.add(flatChunk(
                                libraryId,
                                documentId,
                                indexVersionId,
                                document,
                                documentProfile,
                                structuralSegment,
                                tokenChunk,
                                tokenizer,
                                profile.embeddingModel(),
                                flatOrdinal++
                        ));
                    }
                }
                continue;
            }

            UUID parentChunkId = UUID.randomUUID();
            chunks.add(parentChunk(
                    parentChunkId,
                    libraryId,
                    documentId,
                    indexVersionId,
                    structuralSegment,
                    document,
                    documentProfile,
                    tokenizer,
                    profile.embeddingModel()
            ));
            List<TokenChunk> tokenChunks = tokenChunksWithCharacterFallback(segmentTexts, tokenizer, config);
            for (TokenChunk tokenChunk : tokenChunks) {
                chunks.add(childChunk(
                        libraryId,
                        documentId,
                        indexVersionId,
                        parentChunkId,
                        document,
                        documentProfile,
                        structuralSegment,
                        tokenChunk,
                        tokenizer,
                        profile.embeddingModel()
                ));
            }
        }
        logChunkComplete(document, "token-window-" + config.chunkMode().name().toLowerCase(), profile, documentProfile, chunks);
        return chunks;
    }

    private static void logChunkComplete(
            ParsedDocument document,
            String engine,
            LibraryProfile profile,
            DocumentProfile documentProfile,
            List<DocumentChunk> chunks
    ) {
        int indexableCount = 0;
        for (DocumentChunk chunk : chunks) {
            if (isIndexableChunk(chunk)) {
                indexableCount++;
            }
        }
        log.info(
                "分块完成: sourceUri={}, engine={}, chunkMode={}, chunks={}, indexable={}, embeddingModel={}",
                document.sourceUri(),
                engine,
                documentProfile == null ? null : documentProfile.chunkingStrategy(),
                chunks.size(),
                indexableCount,
                profile.embeddingModel()
        );
    }

    private static boolean isIndexableChunk(DocumentChunk chunk) {
        if (chunk.parentChunkId() != null) {
            return true;
        }
        if (chunk.metadata() == null) {
            return true;
        }
        Object indexable = chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return true;
    }

    private List<TokenChunk> tokenChunksWithCharacterFallback(
            List<String> segmentTexts,
            ModelTokenizer tokenizer,
            SegmentationConfig config
    ) {
        List<TokenChunk> tokenChunks = tokenWindowChunker.chunk(
                segmentTexts,
                tokenizer,
                new ChunkingOptions(
                        config.chunkMaxTokens(),
                        config.chunkOverlapTokens(),
                        1,
                        config.preserveStructureBoundary()
                )
        );
        List<TokenChunk> budgeted = new ArrayList<>();
        int ordinal = 0;
        for (TokenChunk tokenChunk : tokenChunks) {
            if (tokenChunk.tokenCount() <= config.chunkMaxTokens()) {
                budgeted.add(new TokenChunk(
                        tokenChunk.content(),
                        tokenChunk.tokenCount(),
                        ordinal++,
                        tokenChunk.boundaryType(),
                        tokenChunk.metadata()
                ));
                continue;
            }
            List<String> fallbackParts = recursiveCharacterSplitter.split(
                    tokenChunk.content(),
                    config.separators(),
                    Math.max(1, config.chunkMaxTokens())
            );
            for (String fallbackPart : fallbackParts) {
                int tokenCount = tokenizer.count(fallbackPart).tokens();
                Map<String, Object> metadata = new HashMap<>(tokenChunk.metadata());
                metadata.put("fallback", "character");
                metadata.put("oversizedTokenCount", tokenChunk.tokenCount());
                budgeted.add(new TokenChunk(
                        fallbackPart,
                        tokenCount,
                        ordinal++,
                        "character_fallback",
                        Map.copyOf(metadata)
                ));
            }
        }
        return budgeted;
    }

    private List<String> buildCandidateTexts(StructuralSegment structuralSegment, SegmentationConfig config) {
        String content = structuralSegment.content();
        if (content == null || content.isBlank()) {
            return List.of();
        }
        int maxChars = config.effectiveMaxChars();
        List<String> segmentTexts;
        if (config.splitMode() == SegmentationConfig.SplitMode.RECURSIVE) {
            segmentTexts = recursiveCharacterSplitter.split(content, config.separators(), maxChars);
        } else {
            segmentTexts = boundaryAdjuster.adjust(
                    BoundaryAdjuster.splitLongSegment(content, maxChars),
                    config.preserveStructureBoundary()
            );
        }
        if (!config.prependHeadingContext()) {
            return segmentTexts;
        }
        String headingContext = headingContext(structuralSegment);
        if (headingContext == null || headingContext.isBlank()) {
            return segmentTexts;
        }
        List<String> enriched = new ArrayList<>(segmentTexts.size());
        for (String segmentText : segmentTexts) {
            if (segmentText == null || segmentText.isBlank()) {
                continue;
            }
            if (segmentText.startsWith(headingContext)) {
                enriched.add(segmentText);
            } else {
                enriched.add(headingContext + "\n\n" + segmentText.trim());
            }
        }
        return enriched;
    }

    private static String headingContext(StructuralSegment structuralSegment) {
        if (structuralSegment.metadata() != null) {
            Object sectionTitle = structuralSegment.metadata().get("sectionTitle");
            if (sectionTitle != null && !String.valueOf(sectionTitle).isBlank()) {
                return String.valueOf(sectionTitle).trim();
            }
            Object sectionPath = structuralSegment.metadata().get("sectionPath");
            if (sectionPath != null && !String.valueOf(sectionPath).isBlank()) {
                return String.valueOf(sectionPath).trim();
            }
        }
        return null;
    }

    private static DocumentChunk flatChunkFromText(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            StructuralSegment structuralSegment,
            String content,
            ModelTokenizer tokenizer,
            String embeddingModel,
            int flatOrdinal
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.putAll(structuralSegment.metadata());
        metadata.put("sourceUri", document.sourceUri());
        metadata.put("title", document.title());
        metadata.put("flatOrdinal", flatOrdinal);
        metadata.put("contentFamily", document.contentFamily().name());
        metadata.put("indexable", true);
        metadata.put("chunkRole", "flat");
        metadata.put("sizeUnit", "char");
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
            metadata.put("parserCode", documentProfile.parserCode());
            metadata.put("chunkingStrategy", documentProfile.chunkingStrategy());
        }
        return new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                indexVersionId,
                content,
                tokenizer.count(content).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                embeddingModel,
                structuralSegment.boundaryType(),
                null,
                Map.copyOf(metadata)
        );
    }

    private static DocumentChunk flatChunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            StructuralSegment structuralSegment,
            TokenChunk tokenChunk,
            ModelTokenizer tokenizer,
            String embeddingModel,
            int flatOrdinal
    ) {
        Map<String, Object> metadata = new HashMap<>(childMetadata(document, documentProfile, structuralSegment, tokenChunk));
        metadata.put("chunkRole", "flat");
        metadata.put("flatOrdinal", flatOrdinal);
        metadata.put("indexable", true);
        return new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                indexVersionId,
                tokenChunk.content(),
                tokenChunk.tokenCount(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                embeddingModel,
                tokenChunk.boundaryType(),
                null,
                Map.copyOf(metadata)
        );
    }

    private static DocumentChunk parentChunk(
            UUID parentChunkId,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            StructuralSegment structuralSegment,
            ParsedDocument document,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            String embeddingModel
    ) {
        return new DocumentChunk(
                parentChunkId,
                documentId,
                libraryId,
                indexVersionId,
                structuralSegment.content(),
                tokenizer.count(structuralSegment.content()).tokens(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                embeddingModel,
                structuralSegment.boundaryType(),
                null,
                parentMetadata(document, documentProfile, structuralSegment)
        );
    }

    private static DocumentChunk childChunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            UUID parentChunkId,
            ParsedDocument document,
            DocumentProfile documentProfile,
            StructuralSegment structuralSegment,
            TokenChunk tokenChunk,
            ModelTokenizer tokenizer,
            String embeddingModel
    ) {
        return new DocumentChunk(
                UUID.randomUUID(),
                documentId,
                libraryId,
                indexVersionId,
                tokenChunk.content(),
                tokenChunk.tokenCount(),
                tokenizer.tokenizerId(),
                tokenizer.tokenizerVersion(),
                embeddingModel,
                tokenChunk.boundaryType(),
                parentChunkId,
                childMetadata(document, documentProfile, structuralSegment, tokenChunk)
        );
    }

    private static Map<String, Object> parentMetadata(
            ParsedDocument document,
            DocumentProfile documentProfile,
            StructuralSegment structuralSegment
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.putAll(structuralSegment.metadata());
        metadata.put("sourceUri", document.sourceUri());
        metadata.put("title", document.title());
        metadata.put("contentFamily", document.contentFamily().name());
        metadata.put("indexable", false);
        metadata.put("chunkRole", "parent");
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
            metadata.put("parserCode", documentProfile.parserCode());
            metadata.put("chunkingStrategy", documentProfile.chunkingStrategy());
        }
        return Map.copyOf(metadata);
    }

    private static Map<String, Object> childMetadata(
            ParsedDocument document,
            DocumentProfile documentProfile,
            StructuralSegment structuralSegment,
            TokenChunk tokenChunk
    ) {
        Map<String, Object> metadata = new HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.putAll(structuralSegment.metadata());
        metadata.put("sourceUri", document.sourceUri());
        metadata.put("title", document.title());
        metadata.put("ordinal", tokenChunk.ordinal());
        metadata.put("structureOrdinal", structuralSegment.ordinal());
        metadata.put("contentFamily", document.contentFamily().name());
        metadata.put("indexable", true);
        metadata.put("chunkRole", "child");
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
            metadata.put("parserCode", documentProfile.parserCode());
            metadata.put("chunkingStrategy", documentProfile.chunkingStrategy());
        }
        return OcrChunkMetadataSupport.mergeBlockOcrFields(metadata, structuralSegment.metadata());
    }
}
