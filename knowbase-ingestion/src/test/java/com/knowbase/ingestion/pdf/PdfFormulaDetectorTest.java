package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfFormulaDetectorTest {

    @Test
    void detectsInlineDollarLatex() {
        PdfFormulaDetector.FormulaMatch match = PdfFormulaDetector.detect("Energy is $E=mc^2$ in text.");
        assertNotNull(match);
        assertEquals("E=mc^2", match.latex());
        assertTrue(!match.display());
    }

    @Test
    void detectsLatexCommandLine() {
        assertTrue(PdfFormulaDetector.isFormulaLike("\\frac{a}{b} + \\alpha"));
    }
}
