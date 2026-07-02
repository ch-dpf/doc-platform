package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.ocr.OcrConfidencePolicy;

import java.util.List;
import java.util.Map;

public final class OcrParseEnricher {

    private OcrParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks, Map<String, Object> documentMetadata) {
        if (blocks == null || blocks.isEmpty() || !containsOcrBlocks(blocks)) {
            return blocks;
        }
        if (blocks.stream().anyMatch(OcrParseEnricher::alreadyAnnotated)) {
            return blocks;
        }
        IngestionParseOptionsSupport.IngestionParseOptions options = IngestionParseOptionsSupport.resolve(documentMetadata);
        return OcrConfidencePolicy.apply(
                blocks,
                options.ocrConfidenceThreshold(),
                options.ocrDownweightMode()
        );
    }

    private static boolean containsOcrBlocks(List<StructuralBlock> blocks) {
        return blocks.stream().anyMatch(block ->
                block.metadata() != null
                        && (Boolean.TRUE.equals(block.metadata().get("ocrApplied"))
                        || block.metadata().containsKey("ocrConfidence")));
    }

    private static boolean alreadyAnnotated(StructuralBlock block) {
        Map<String, Object> metadata = block.metadata();
        return metadata != null
                && (metadata.containsKey("lowConfidenceOcr") || metadata.containsKey("ocrFilterReason"));
    }
}
