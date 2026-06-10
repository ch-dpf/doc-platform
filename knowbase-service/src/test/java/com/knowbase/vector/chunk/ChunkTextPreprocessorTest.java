package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkTextPreprocessorTest {

    @Test
    void collapsesExcessiveBlankLines() {
        String out = ChunkTextPreprocessor.prepare("a\n\n\n\nb");
        assertEquals("a\n\nb", out);
    }
}
