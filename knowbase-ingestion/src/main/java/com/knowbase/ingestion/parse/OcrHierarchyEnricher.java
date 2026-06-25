package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds paragraph → line → word hierarchy metadata from OCR blocks.
 */
public final class OcrHierarchyEnricher {

    private OcrHierarchyEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        List<StructuralBlock> enriched = new ArrayList<>(blocks.size());
        int paragraphIndex = 0;
        for (StructuralBlock block : blocks) {
            if (block.metadata() == null || !Boolean.TRUE.equals(block.metadata().get("ocrApplied"))) {
                enriched.add(block);
                continue;
            }
            if (block.metadata().containsKey("ocrHierarchy")) {
                enriched.add(block);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.put("ocrLevel", metadata.getOrDefault("ocrLevel", "line"));
            Map<String, Object> hierarchy = new HashMap<>();
            hierarchy.put("paragraphIndex", paragraphIndex++);
            hierarchy.put("lineText", block.content());
            Object words = metadata.get("ocrWords");
            if (words instanceof List<?> wordList && !wordList.isEmpty()) {
                hierarchy.put("words", wordList);
                metadata.put("ocrWordCount", wordList.size());
            }
            metadata.put("ocrHierarchy", Map.copyOf(hierarchy));
            enriched.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(enriched);
    }
}
