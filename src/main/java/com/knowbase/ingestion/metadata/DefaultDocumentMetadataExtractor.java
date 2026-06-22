package com.knowbase.ingestion.metadata;

import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.document.ParsedDocument.BlockType;
import com.knowbase.ingestion.document.ParsedDocument.DocumentBlock;
import com.knowbase.ingestion.metadata.DocumentMetadataExtractor.MetadataExtractionOptions;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic metadata extractor for ingestion pipeline diagnostics and chunk filtering.
 */
public final class DefaultDocumentMetadataExtractor implements DocumentMetadataExtractor {

    @Override
    public ParsedDocument extract(ParsedDocument document, MetadataExtractionOptions options) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(options, "options");
        Map<String, String> metadata = new LinkedHashMap<>(document.metadata());
        metadata.put("metadataExtractor", "default");
        metadata.put("contentFamily", document.contentFamily().name());
        if (options.includeBlockCounts()) {
            addBlockCounts(document, metadata);
        }
        if (options.includeTextStats()) {
            String plainText = document.plainText();
            metadata.put("text.characterCount", String.valueOf(plainText.length()));
            metadata.put("text.tokenEstimate", String.valueOf(com.knowbase.ingestion.chunking.SmartDocumentChunker.TokenEstimator.estimate(plainText)));
        }
        if (options.includeFirstHeading()) {
            document.blocks().stream()
                    .filter(block -> block.type() == BlockType.HEADING)
                    .findFirst()
                    .map(DocumentBlock::asText)
                    .ifPresent(value -> metadata.put("firstHeading", value));
        }
        return document.withMetadata(metadata);
    }

    private static void addBlockCounts(ParsedDocument document, Map<String, String> metadata) {
        EnumMap<BlockType, Integer> counts = new EnumMap<>(BlockType.class);
        for (DocumentBlock block : document.blocks()) {
            counts.merge(block.type(), 1, Integer::sum);
        }
        metadata.put("blockCount", String.valueOf(document.blocks().size()));
        for (BlockType type : BlockType.values()) {
            metadata.put("blockCount." + type.name().toLowerCase(), String.valueOf(counts.getOrDefault(type, 0)));
        }
    }
}
