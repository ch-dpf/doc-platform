package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.pdf.PdfFormulaDetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Promotes LaTeX-like lines to {@code formula} blocks with preserved latex metadata.
 */
public final class FormulaBlockParseEnricher {

    private FormulaBlockParseEnricher() {
    }

    public static List<StructuralBlock> enrich(List<StructuralBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        List<StructuralBlock> enriched = new ArrayList<>(blocks.size());
        for (StructuralBlock block : blocks) {
            if ("table_row".equals(block.blockType()) || "table_summary".equals(block.blockType())) {
                enriched.add(block);
                continue;
            }
            PdfFormulaDetector.FormulaMatch match = PdfFormulaDetector.detect(block.content());
            if (match == null) {
                enriched.add(block);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.put("boundaryType", "formula");
            metadata.put("layoutRole", "formula");
            metadata.put("formulaBlock", true);
            metadata.put("formulaLatex", match.latex());
            metadata.put("formulaFormat", match.format());
            metadata.put("formulaDisplay", match.display());
            metadata.put("indexableHint", true);
            enriched.add(new StructuralBlock(
                    "formula",
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return List.copyOf(enriched);
    }
}
