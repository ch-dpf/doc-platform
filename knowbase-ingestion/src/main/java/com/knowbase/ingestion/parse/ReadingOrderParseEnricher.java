package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.List;
import java.util.Map;

public final class ReadingOrderParseEnricher {

    private ReadingOrderParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        return enrich(blocks, null);
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks, Map<String, Object> documentMetadata) {
        return ReadingOrderService.apply(blocks, documentMetadata);
    }
}
