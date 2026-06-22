package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.ingestion.DocumentMetadataEnricher.MetadataContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class DefaultDocumentMetadataEnricher implements DocumentMetadataEnricher {

    @Override
    public ParsedDocument enrich(ParsedDocument document, MetadataContext context) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(context, "context");
        Map<String, Object> metadata = new HashMap<>();
        if (document.metadata() != null) {
            metadata.putAll(document.metadata());
        }
        metadata.put("metadataEnricher", "default");
        putIfNotNull(metadata, "sourceUri", valueOrDefault(context.sourceUri(), document.sourceUri()));
        metadata.put("contentFamily", document.contentFamily() == null ? "" : document.contentFamily().name());
        metadata.put("structureAware", document.structureAware());
        metadata.put("blockCount", document.blocks().size());
        metadata.put("textLength", document.text() == null ? 0 : document.text().length());
        firstHeading(document).ifPresent(value -> metadata.put("firstHeading", value));
        addProfileMetadata(metadata, context.libraryProfile(), context.documentProfile());
        return new ParsedDocument(
                document.sourceUri(),
                document.title(),
                document.text(),
                document.contentFamily(),
                Map.copyOf(metadata),
                document.blocks()
        );
    }

    private static java.util.Optional<String> firstHeading(ParsedDocument document) {
        return document.blocks().stream()
                .filter(block -> "heading".equals(block.blockType()))
                .map(StructuralBlock::content)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private static void addProfileMetadata(
            Map<String, Object> metadata,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile
    ) {
        if (libraryProfile != null) {
            putIfNotNull(metadata, "libraryProfileId", libraryProfile.profileId());
            putIfNotNull(metadata, "embeddingModel", libraryProfile.embeddingModel());
            metadata.put("chunkMaxTokens", libraryProfile.chunkMaxTokens());
            metadata.put("chunkOverlapTokens", libraryProfile.chunkOverlapTokens());
        }
        if (documentProfile != null) {
            putIfNotNull(metadata, "documentProfileId", documentProfile.documentProfileId());
            putIfNotNull(metadata, "documentProfileCode", documentProfile.code());
            putIfNotNull(metadata, "parserCode", documentProfile.parserCode());
            putIfNotNull(metadata, "chunkingStrategy", documentProfile.chunkingStrategy());
            putIfNotNull(metadata, "documentTokenizerProfileId", documentProfile.tokenizerProfileId());
        }
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void putIfNotNull(Map<String, Object> metadata, String key, Object value) {
        if (value != null) {
            metadata.put(key, value);
        }
    }
}
