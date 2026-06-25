package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.pdf.VisionDocumentBlockMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LayoutResultMapper {

    private LayoutResultMapper() {
    }

    public static List<StructuralBlock> fromVisionMarkdown(
            String markdown,
            LayoutPageRequest request,
            String providerCode,
            String modelName
    ) {
        List<StructuralBlock> blocks = VisionDocumentBlockMapper.fromPageText(
                markdown,
                request.pageNumber(),
                request.pageWidth(),
                request.pageHeight(),
                request.effectiveOptions()
        );
        return stampProvider(blocks, providerCode, modelName, "vision-markdown");
    }

    public static List<StructuralBlock> stampProvider(
            List<StructuralBlock> blocks,
            String providerCode,
            String modelName,
            String route
    ) {
        List<StructuralBlock> stamped = new ArrayList<>(blocks.size());
        for (StructuralBlock block : blocks) {
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.put("layoutProvider", providerCode);
            metadata.put("layoutAnalysisRoute", route);
            if (modelName != null && !modelName.isBlank()) {
                metadata.put("layoutModel", modelName);
            }
            stamped.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return stamped;
    }

    public static LayoutPageResult mergePageMetadata(LayoutPageResult result, Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return result;
        }
        Map<String, Object> merged = new HashMap<>(result.metadata());
        merged.putAll(extra);
        return new LayoutPageResult(
                result.providerCode(),
                result.modelName(),
                result.pageNumber(),
                result.blocks(),
                result.tableRegions(),
                result.detectedLanguage(),
                result.rotationDegrees(),
                Map.copyOf(merged)
        );
    }
}
