package com.knowbase.ingestion.pdf;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps vision-language model markdown output into {@link StructuralBlock}s.
 */
public final class VisionDocumentBlockMapper {

    private static final double DEFAULT_VL_CONFIDENCE = 0.85d;

    private VisionDocumentBlockMapper() {
    }

    public static List<StructuralBlock> fromPageText(
            String pageText,
            int pageNumber,
            double pageWidth,
            double pageHeight,
            Map<String, Object> baseMetadata
    ) {
        if (pageText == null || pageText.isBlank()) {
            return List.of();
        }
        Map<String, Object> pageMetadata = new HashMap<>(baseMetadata == null ? Map.of() : baseMetadata);
        pageMetadata.put("pageNumber", pageNumber);
        pageMetadata.put("pageWidth", pageWidth);
        pageMetadata.put("pageHeight", pageHeight);
        pageMetadata.put("vlApplied", true);
        pageMetadata.put("layoutParsing", true);
        pageMetadata.put("ocrApplied", true);
        pageMetadata.put("ocrEngine", "vision-vl");
        pageMetadata.put("ocrConfidence", DEFAULT_VL_CONFIDENCE);
        pageMetadata.put("ocrConfidenceSource", "vision-vl");
        pageMetadata.put("bboxSource", "unavailable");

        List<StructuralBlock> blocks = StructureParsingSupport.parseMarkdownPublic(pageText.trim());
        if (blocks.isEmpty()) {
            blocks = StructureParsingSupport.parseOcrLayout(pageText, pageMetadata);
        }
        return annotateBlocks(blocks, pageMetadata);
    }

    private static List<StructuralBlock> annotateBlocks(List<StructuralBlock> blocks, Map<String, Object> pageMetadata) {
        List<StructuralBlock> annotated = new ArrayList<>(blocks.size());
        for (StructuralBlock block : blocks) {
            Map<String, Object> metadata = new HashMap<>(pageMetadata);
            if (block.metadata() != null) {
                metadata.putAll(block.metadata());
            }
            metadata.putIfAbsent("vlApplied", true);
            metadata.putIfAbsent("ocrEngine", "vision-vl");
            annotated.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return annotated;
    }
}
