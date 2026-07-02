package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propagates detected page/document language to OCR blocks when missing.
 */
public final class OcrLanguageEnricher {

    private OcrLanguageEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks, Map<String, Object> documentMetadata) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        String documentLanguage = resolveDocumentLanguage(documentMetadata, blocks);
        if (documentLanguage == null) {
            return blocks;
        }
        List<StructuralBlock> enriched = new ArrayList<>(blocks.size());
        for (StructuralBlock block : blocks) {
            Map<String, Object> metadata = block.metadata();
            if (metadata == null || !Boolean.TRUE.equals(metadata.get("ocrApplied"))) {
                enriched.add(block);
                continue;
            }
            if (metadata.containsKey("ocrLanguage") && !"auto".equalsIgnoreCase(String.valueOf(metadata.get("ocrLanguage")))) {
                enriched.add(block);
                continue;
            }
            Map<String, Object> merged = new HashMap<>(metadata);
            merged.put("ocrLanguage", documentLanguage);
            merged.putIfAbsent("ocrLanguageSource", "document-inferred");
            enriched.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(merged)
            ));
        }
        return List.copyOf(enriched);
    }

    private static String resolveDocumentLanguage(Map<String, Object> documentMetadata, List<StructuralBlock> blocks) {
        if (documentMetadata != null) {
            Object configured = documentMetadata.get("ocrLanguage");
            if (configured != null && !String.valueOf(configured).isBlank() && !"auto".equalsIgnoreCase(String.valueOf(configured))) {
                return String.valueOf(configured).trim();
            }
            Object detected = documentMetadata.get("detectedLanguage");
            if (detected != null && !String.valueOf(detected).isBlank()) {
                return String.valueOf(detected).trim();
            }
        }
        for (StructuralBlock block : blocks) {
            Object language = block.metadata().get("ocrLanguage");
            if (language != null && !String.valueOf(language).isBlank() && !"auto".equalsIgnoreCase(String.valueOf(language))) {
                return String.valueOf(language).trim();
            }
        }
        return null;
    }
}
