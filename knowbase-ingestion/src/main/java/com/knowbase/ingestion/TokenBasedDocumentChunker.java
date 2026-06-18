package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.tokenizer.ChunkingOptions;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.TokenChunk;
import com.knowbase.tokenizer.TokenWindowChunker;
import com.knowbase.tokenizer.TokenizerGuard;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TokenBasedDocumentChunker implements DocumentChunker {

    private final TokenizerRegistry tokenizerRegistry;
    private final TokenWindowChunker tokenWindowChunker;
    private final TokenizerGuard tokenizerGuard;

    public TokenBasedDocumentChunker(TokenizerRegistry tokenizerRegistry, TokenWindowChunker tokenWindowChunker) {
        this(tokenizerRegistry, tokenWindowChunker, new TokenizerGuard(true));
    }

    public TokenBasedDocumentChunker(
            TokenizerRegistry tokenizerRegistry,
            TokenWindowChunker tokenWindowChunker,
            TokenizerGuard tokenizerGuard
    ) {
        this.tokenizerRegistry = tokenizerRegistry;
        this.tokenWindowChunker = tokenWindowChunker;
        this.tokenizerGuard = tokenizerGuard;
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
            LibraryProfile profile
    ) {
        return chunk(libraryId, documentId, indexVersionId, document, profile, null);
    }

    public List<DocumentChunk> chunk(
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            ParsedDocument document,
            LibraryProfile profile,
            DocumentProfile documentProfile
    ) {
        ModelTokenizer tokenizer = tokenizerRegistry.getTokenizer(profile.embeddingProvider(), profile.embeddingModel());
        return chunk(libraryId, documentId, indexVersionId, document, profile, documentProfile, tokenizer);
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
        tokenizerGuard.validateForIndexing(tokenizer, profile.embeddingProvider(), profile.embeddingModel());
        List<String> segments = splitStructuralSegments(document, documentProfile);
        List<TokenChunk> tokenChunks = tokenWindowChunker.chunk(
                segments,
                tokenizer,
                new ChunkingOptions(
                        profile.chunkMaxTokens(),
                        profile.chunkOverlapTokens(),
                        1,
                        true
                )
        );
        List<DocumentChunk> chunks = new ArrayList<>(tokenChunks.size());
        for (TokenChunk tokenChunk : tokenChunks) {
            chunks.add(new DocumentChunk(
                    UUID.randomUUID(),
                    documentId,
                    libraryId,
                    indexVersionId,
                    tokenChunk.content(),
                    tokenChunk.tokenCount(),
                    tokenizer.tokenizerId(),
                    tokenizer.tokenizerVersion(),
                    profile.embeddingModel(),
                    tokenChunk.boundaryType(),
                    null,
                    chunkMetadata(document, documentProfile, tokenChunk)
            ));
        }
        return chunks;
    }

    private static List<String> splitStructuralSegments(ParsedDocument document, DocumentProfile documentProfile) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        ContentFamily family = documentProfile == null ? document.contentFamily() : documentProfile.contentFamily();
        String pattern = switch (family) {
            case STRUCTURED_TABLE -> "\\R(?=\\S)";
            case CODE_OR_CONFIG -> "(?m)^\\s*(?:class|interface|record|enum|def|function|export|public|private|protected)\\b|\\R{2,}";
            case PRESENTATION, SCANNED_DOCUMENT, IMAGE_TEXT -> "\\R{2,}|(?m)^\\s*(?:Slide|Page|幻灯片|第\\s*\\d+\\s*页)\\b.*$";
            case WEB_PAGE, RICH_TEXT, PLAIN_TEXT -> "(?m)^#{1,6}\\s+.+$|\\R{2,}";
        };
        String[] parts = text.split(pattern);
        List<String> segments = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                segments.add(part.trim());
            }
        }
        if (segments.isEmpty()) {
            segments.add(text.trim());
        }
        return segments;
    }

    private static Map<String, Object> chunkMetadata(
            ParsedDocument document,
            DocumentProfile documentProfile,
            TokenChunk tokenChunk
    ) {
        java.util.HashMap<String, Object> metadata = new java.util.HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.put("sourceUri", document.sourceUri());
        metadata.put("title", document.title());
        metadata.put("ordinal", tokenChunk.ordinal());
        metadata.put("contentFamily", document.contentFamily().name());
        if (documentProfile != null) {
            metadata.put("documentProfileCode", documentProfile.code());
            metadata.put("parserCode", documentProfile.parserCode());
            metadata.put("chunkingStrategy", documentProfile.chunkingStrategy());
        }
        return Map.copyOf(metadata);
    }
}
