package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.StructuralBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Final parse-stage enrichment shared by all built-in parsers.
 */
public final class ParsedDocumentParseEnricher {

    private static final Logger log = LoggerFactory.getLogger(ParsedDocumentParseEnricher.class);

    private ParsedDocumentParseEnricher() {
    }

    public static ParsedDocument enrich(ParsedDocument parsed) {
        if (parsed == null || !parsed.structureAware() || parsed.blocks().isEmpty()) {
            return parsed;
        }
        List<StructuralBlock> blocks = new ArrayList<>(parsed.blocks().size());
        Map<String, Object> documentMetadata = parsed.metadata() == null ? Map.of() : parsed.metadata();
        for (StructuralBlock block : parsed.blocks()) {
            StructuralBlock step = StructuralBlockIndexabilityPolicy.applyIndexableHint(block);
            step = ParsePageDimensionEnricher.apply(step, documentMetadata);
            step = EvidenceAssetHintEnricher.apply(step, parsed.sourceUri(), documentMetadata);
            blocks.add(step);
        }
        blocks = OcrParseEnricher.enrich(blocks, documentMetadata);
        blocks = OcrHierarchyEnricher.enrich(blocks);
        blocks = OcrLanguageEnricher.enrich(blocks, documentMetadata);
        blocks = TableRegionIdParseEnricher.enrich(blocks);
        blocks = TableGridParseEnricher.enrich(blocks);
        blocks = TableSemanticParseEnricher.enrich(blocks);
        blocks = TableRegionSummaryParseEnricher.enrich(blocks);
        blocks = FormulaBlockParseEnricher.enrich(blocks);
        blocks = ReadingOrderParseEnricher.enrich(blocks, documentMetadata);
        Map<String, Object> metadata = new HashMap<>();
        if (parsed.metadata() != null) {
            metadata.putAll(parsed.metadata());
        }
        String parserCode = metadata.get("parserCode") == null
                ? stringValue(metadata.get("parser"))
                : stringValue(metadata.get("parserCode"));
        UniversalParseConfidenceAggregator.ParseConfidence confidence =
                UniversalParseConfidenceAggregator.aggregate(parsed, blocks);
        if (!metadata.containsKey("parseConfidence")) {
            metadata.putAll(UniversalParseConfidenceAggregator.toDocumentMetadata(confidence, parserCode));
        } else {
            metadata.putIfAbsent("indexableBlockCount", confidence.indexableBlockCount());
            metadata.putIfAbsent("tableRegionCount", confidence.tableRegionCount());
            metadata.putIfAbsent("parseConfidenceSource", parserCode == null ? "structure-universal" : parserCode);
        }
        metadata.put("blockCount", blocks.size());
        metadata.putIfAbsent("structureAware", true);
        log.info(
                "解析增强完成: sourceUri={}, parserCode={}, blocks={}, indexableBlocks={}, tableRegions={}, parseConfidence={}",
                parsed.sourceUri(),
                parserCode,
                blocks.size(),
                confidence.indexableBlockCount(),
                confidence.tableRegionCount(),
                metadata.get("parseConfidence")
        );
        return new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                parsed.text(),
                parsed.contentFamily(),
                Map.copyOf(metadata),
                List.copyOf(blocks)
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
