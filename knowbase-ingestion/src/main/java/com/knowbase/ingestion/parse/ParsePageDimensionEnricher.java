package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies page dimension hints onto blocks when available at document or block level.
 */
public final class ParsePageDimensionEnricher {

    private ParsePageDimensionEnricher() {
    }

    public static StructuralBlock apply(StructuralBlock block, Map<String, Object> documentMetadata) {
        if (block == null) {
            return block;
        }
        Map<String, Object> metadata = block.metadata();
        if (metadata.containsKey("pageWidth") && metadata.containsKey("pageHeight")) {
            return block;
        }
        Object pageNumber = metadata.get("pageNumber");
        if (!(pageNumber instanceof Number page)) {
            return block;
        }
        Map<String, Object> merged = new HashMap<>(metadata);
        copyPageDimension(documentMetadata, merged, page.intValue(), "pageWidths", "pageWidth");
        copyPageDimension(documentMetadata, merged, page.intValue(), "pageHeights", "pageHeight");
        if (merged.equals(metadata)) {
            return block;
        }
        return new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(merged));
    }

    @SuppressWarnings("unchecked")
    private static void copyPageDimension(
            Map<String, Object> documentMetadata,
            Map<String, Object> target,
            int pageNumber,
            String mapKey,
            String fieldKey
    ) {
        if (target.containsKey(fieldKey) || documentMetadata == null) {
            return;
        }
        Object raw = documentMetadata.get(mapKey);
        if (raw instanceof Map<?, ?> map) {
            Object value = map.get(pageNumber);
            if (value == null) {
                value = map.get(String.valueOf(pageNumber));
            }
            if (value != null) {
                target.put(fieldKey, value);
            }
            return;
        }
        if (raw instanceof List<?> list && pageNumber > 0 && pageNumber <= list.size()) {
            target.put(fieldKey, list.get(pageNumber - 1));
        }
    }
}
