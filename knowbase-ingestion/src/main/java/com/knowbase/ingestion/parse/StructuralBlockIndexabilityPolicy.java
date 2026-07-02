package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.adaptive.AdaptiveTableTextSerializer;
import com.knowbase.ingestion.adaptive.TableRowRole;
import com.knowbase.ingestion.pdf.PdfLayoutRoleClassifier;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Default {@code indexableHint} for structure blocks that omit parser-specific hints.
 */
public final class StructuralBlockIndexabilityPolicy {

    private StructuralBlockIndexabilityPolicy() {
    }

    public static boolean resolveIndexableHint(StructuralBlock block) {
        if (block == null) {
            return true;
        }
        Map<String, Object> metadata = block.metadata();
        if (metadata.containsKey("indexableHint")) {
            return asBoolean(metadata.get("indexableHint"), true);
        }
        Object rowRole = metadata.get("rowRole");
        if (rowRole != null) {
            try {
                return AdaptiveTableTextSerializer.defaultIndexable(
                        TableRowRole.valueOf(String.valueOf(rowRole).trim().toUpperCase(Locale.ROOT))
                );
            } catch (IllegalArgumentException ignored) {
                return true;
            }
        }
        Object layoutRole = metadata.get("layoutRole");
        if (layoutRole != null && !PdfLayoutRoleClassifier.isIndexableRole(String.valueOf(layoutRole))) {
            return false;
        }
        Object ocrFilterReason = metadata.get("ocrFilterReason");
        if (ocrFilterReason != null && !String.valueOf(ocrFilterReason).isBlank()) {
            return false;
        }
        return switch (block.blockType()) {
            case "heading", "table_summary" -> false;
            case "table_row" -> true;
            default -> true;
        };
    }

    public static StructuralBlock applyIndexableHint(StructuralBlock block) {
        if (block.metadata().containsKey("indexableHint")) {
            return block;
        }
        Map<String, Object> metadata = new HashMap<>(block.metadata());
        metadata.put("indexableHint", resolveIndexableHint(block));
        return new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(metadata));
    }

    private static boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value == null) {
            return defaultValue;
        }
        return !"false".equalsIgnoreCase(String.valueOf(value));
    }
}
