package com.knowbase.pipeline.content;

import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentSignalsChunkingAdjusterTest {

    private final ContentSignalsChunkingAdjuster adjuster = new ContentSignalsChunkingAdjuster();

    @Test
    void downgradesHeadingForShortWordDocument() {
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategy.HEADING_LEVEL);

        ContentSignals signals = new ContentSignals();
        signals.setShortDocument(true);
        signals.setTextLength(500);

        adjuster.apply(ContentFamily.DOCUMENT, signals, chunking);

        assertEquals(ChunkingStrategy.PARAGRAPH_FIRST, chunking.getStrategy());
        assertEquals("short-document-downgrade-heading", signals.getChunkingAdjustmentReason());
    }

    @Test
    void upgradesParagraphFirstForLongStructuredPdf() {
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategy.PARAGRAPH_FIRST);

        ContentSignals signals = new ContentSignals();
        signals.setShortDocument(false);
        signals.setTextLength(5000);
        signals.setHeadingLineRatio(0.12);
        signals.setHeadingLineCount(6);

        adjuster.apply(ContentFamily.DOCUMENT, signals, chunking);

        assertEquals(ChunkingStrategy.HEADING_LEVEL, chunking.getStrategy());
        assertEquals("document-heading-density-upgrade", signals.getChunkingAdjustmentReason());
    }
}
