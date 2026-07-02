package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Optional evidence asset hints for citation / preview (page, bbox, sheet row).
 */
public final class EvidenceAssetHintEnricher {

    private EvidenceAssetHintEnricher() {
    }

    public static StructuralBlock apply(StructuralBlock block) {
        return apply(block, null);
    }

    public static StructuralBlock apply(StructuralBlock block, String sourceUri) {
        return apply(block, sourceUri, null);
    }

    public static StructuralBlock apply(StructuralBlock block, String sourceUri, Map<String, Object> documentMetadata) {
        if (block == null) {
            return block;
        }
        Map<String, Object> metadata = block.metadata();
        if (metadata.containsKey("evidenceAssetHint")) {
            return block;
        }
        Map<String, Object> hint = new HashMap<>(buildHint(metadata));
        Map<String, Object> artifact = EvidenceArtifactUriBuilder.buildFromBlockMetadata(sourceUri, metadata);
        if (!artifact.isEmpty()) {
            hint.putAll(artifact);
        }
        attachStoredPageArtifact(hint, metadata, documentMetadata);
        if (hint.isEmpty()) {
            return block;
        }
        Map<String, Object> merged = new HashMap<>(metadata);
        merged.put("evidenceAssetHint", Map.copyOf(hint));
        return new StructuralBlock(block.blockType(), block.level(), block.content(), block.ordinal(), Map.copyOf(merged));
    }

    private static Map<String, Object> buildHint(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> hint = new HashMap<>();
        if (metadata.get("tableRegionBbox") != null && metadata.get("pageNumber") != null) {
            hint.put("kind", "table_region_bbox");
            hint.put("pageNumber", metadata.get("pageNumber"));
            hint.put("bbox", metadata.get("tableRegionBbox"));
            if (metadata.get("tableRegionId") != null) {
                hint.put("tableRegionId", metadata.get("tableRegionId"));
            }
            return hint;
        }
        if (Boolean.TRUE.equals(metadata.get("ocrApplied")) && metadata.get("bbox") != null) {
            hint.put("kind", "ocr_bbox");
            if (metadata.get("pageNumber") != null) {
                hint.put("pageNumber", metadata.get("pageNumber"));
            }
            hint.put("bbox", metadata.get("bbox"));
            if (metadata.get("ocrConfidence") != null) {
                hint.put("ocrConfidence", metadata.get("ocrConfidence"));
            }
            return hint;
        }
        if (metadata.get("pageNumber") != null && metadata.get("bbox") != null) {
            hint.put("kind", "pdf_bbox");
            hint.put("pageNumber", metadata.get("pageNumber"));
            hint.put("bbox", metadata.get("bbox"));
            return hint;
        }
        if (metadata.get("slideNumber") != null) {
            hint.put("kind", "slide");
            hint.put("slideNumber", metadata.get("slideNumber"));
            if (metadata.get("pageNumber") != null) {
                hint.put("pageNumber", metadata.get("pageNumber"));
            }
            if (metadata.get("slideTitle") != null) {
                hint.put("slideTitle", metadata.get("slideTitle"));
            }
            return hint;
        }
        if (metadata.get("pageNumber") != null) {
            hint.put("kind", "pdf_page");
            hint.put("pageNumber", metadata.get("pageNumber"));
            return hint;
        }
        if (metadata.get("sheetName") != null) {
            hint.put("kind", "sheet_row");
            hint.put("sheetName", metadata.get("sheetName"));
            if (metadata.get("rowIndex") != null) {
                hint.put("rowIndex", metadata.get("rowIndex"));
            }
            if (metadata.get("primaryCellRef") != null) {
                hint.put("primaryCellRef", metadata.get("primaryCellRef"));
            }
            if (metadata.get("cellRefs") != null) {
                hint.put("cellRefs", metadata.get("cellRefs"));
            }
            if (metadata.get("columnIndex") != null) {
                hint.put("columnIndex", metadata.get("columnIndex"));
            }
            if (metadata.get("tableRegionLabel") != null) {
                hint.put("tableRegionLabel", metadata.get("tableRegionLabel"));
            }
            return hint;
        }
        if (metadata.get("wordSectionPath") != null) {
            hint.put("kind", "word_section");
            hint.put("wordSectionPath", metadata.get("wordSectionPath"));
            if (metadata.get("tableRegionId") != null) {
                hint.put("tableRegionId", metadata.get("tableRegionId"));
            }
            if (metadata.get("rowIndex") != null) {
                hint.put("rowIndex", metadata.get("rowIndex"));
            }
            if (metadata.get("primaryCellRef") != null) {
                hint.put("primaryCellRef", metadata.get("primaryCellRef"));
            }
            return hint;
        }
        if (metadata.get("tableRegionLabel") != null) {
            hint.put("kind", "table_region");
            hint.put("tableRegionLabel", metadata.get("tableRegionLabel"));
            if (metadata.get("tableRegionId") != null) {
                hint.put("tableRegionId", metadata.get("tableRegionId"));
            }
            return hint;
        }
        if (metadata.get("configKey") != null) {
            hint.put("kind", "config_section");
            hint.put("configKey", metadata.get("configKey"));
            if (metadata.get("configFormat") != null) {
                hint.put("configFormat", metadata.get("configFormat"));
            }
            return hint;
        }
        if (metadata.get("sectionPath") != null && !String.valueOf(metadata.get("sectionPath")).isBlank()) {
            hint.put("kind", "heading_section");
            hint.put("sectionPath", metadata.get("sectionPath"));
            return hint;
        }
        if (metadata.get("headingPath") instanceof List<?> path && !path.isEmpty()) {
            hint.put("kind", "heading_section");
            hint.put("headingPath", path);
            return hint;
        }
        return Map.of();
    }

    private static void attachStoredPageArtifact(
            Map<String, Object> hint,
            Map<String, Object> blockMetadata,
            Map<String, Object> documentMetadata
    ) {
        if (documentMetadata == null || blockMetadata == null) {
            return;
        }
        Object pageNumber = blockMetadata.get("pageNumber");
        if (!(pageNumber instanceof Number page)) {
            return;
        }
        String pageImageUri = EvidenceArtifactGenerator.resolvePageArtifactUri(documentMetadata, page.intValue());
        if (pageImageUri == null) {
            return;
        }
        hint.put("pageImageUri", pageImageUri);
        hint.putIfAbsent("assetUri", pageImageUri);
        hint.putIfAbsent("assetKind", "pdf_page_image");
    }
}
