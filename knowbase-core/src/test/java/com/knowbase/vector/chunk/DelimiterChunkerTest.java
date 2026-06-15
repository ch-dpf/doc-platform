package com.knowbase.vector.chunk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DelimiterChunkerTest {

    @Test
    void splitsByNewlineDelimiterEscape() {
        var segments = DelimiterChunker.splitSegments("a\nb\nc", "\\n");
        assertEquals(3, segments.size());
        assertEquals("a", segments.get(0));
        assertEquals("b", segments.get(1));
        assertEquals("c", segments.get(2));
    }

    @Test
    void splitsByCustomToken() {
        var segments = DelimiterChunker.splitSegments("part1---part2", "---");
        assertEquals(2, segments.size());
        assertEquals("part1", segments.get(0));
        assertEquals("part2", segments.get(1));
    }
}
