package com.knowbase.ingestion.ocr;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OcrTsvParserTest {

    @Test
    void parsesTesseractLineRows() {
        String tsv = """
                level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext
                5\t1\t0\t0\t0\t0\t10\t20\t100\t20\t93\tInvoice
                """;
        List<OcrLineResult> lines = OcrTsvParser.parse(tsv);
        assertEquals(1, lines.size());
        assertEquals("Invoice", lines.get(0).text());
        assertEquals(0.93, lines.get(0).confidence(), 0.001);
        assertEquals(1, lines.get(0).words().size());
    }

    @Test
    void groupsWordsIntoLines() {
        String tsv = """
                level\tpage_num\tblock_num\tpar_num\tline_num\tword_num\tleft\ttop\twidth\theight\tconf\ttext
                4\t1\t0\t0\t0\t0\t10\t20\t120\t20\t90\t
                5\t1\t0\t0\t0\t0\t10\t20\t50\t20\t92\tHello
                5\t1\t0\t0\t0\t1\t70\t20\t60\t20\t88\tWorld
                """;
        List<OcrLineResult> lines = OcrTsvParser.parse(tsv);
        assertEquals(1, lines.size());
        assertEquals("Hello World", lines.get(0).text());
        assertEquals(2, lines.get(0).words().size());
        assertEquals(0.90, lines.get(0).confidence(), 0.01);
    }
}
