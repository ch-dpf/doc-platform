package com.knowbase.api.result;

import java.util.List;
import java.util.Map;

public record ParseStageResult(
        String parserCode,
        boolean structureAware,
        int blockCount,
        int textCharCount,
        String textPreview,
        List<StructuralBlockResult> blocks,
        Map<String, Object> metadata
) {
}
