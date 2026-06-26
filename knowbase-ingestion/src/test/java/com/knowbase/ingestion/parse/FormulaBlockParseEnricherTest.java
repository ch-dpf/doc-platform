package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormulaBlockParseEnricherTest {

    @Test
    void promotesLatexParagraphToFormulaBlock() {
        List<StructuralBlock> blocks = FormulaBlockParseEnricher.enrich(List.of(
                StructuralBlock.paragraph("Equation $a^2+b^2=c^2$ here.", 0)
        ));
        assertEquals("formula", blocks.getFirst().blockType());
        assertTrue(blocks.getFirst().metadata().containsKey("formulaLatex"));
    }
}
