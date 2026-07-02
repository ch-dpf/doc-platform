package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecursiveCharacterSplitterTest {

    private final RecursiveCharacterSplitter splitter = new RecursiveCharacterSplitter();

    @Test
    void splitsByParagraphBeforeSentence() {
        String text = "Paragraph one line.\n\nParagraph two has more content.";
        List<String> parts = splitter.split(text, RecursiveCharacterSplitter.defaultSeparators(), 30);
        assertTrue(parts.size() >= 2);
        assertTrue(parts.getFirst().contains("Paragraph one"));
    }

    @Test
    void fallsBackToFinerSeparatorsForLongParagraph() {
        StringBuilder builder = new StringBuilder("Intro.");
        builder.append(" ".repeat(400));
        builder.append("Tail.");
        List<String> parts = splitter.split(builder.toString(), RecursiveCharacterSplitter.defaultSeparators(), 80);
        assertTrue(parts.size() >= 2);
    }
}
