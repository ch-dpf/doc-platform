package com.knowbase.ingestion.summary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LongContentSamplerTest {

    @Test
    void samplesHeadMiddleAndTailWithOmitMarkers() {
        StringBuilder content = new StringBuilder();
        for (int index = 0; index < 500; index++) {
            content.append("segment-").append(index).append(' ');
        }
        String sampled = LongContentSampler.sample(content.toString(), 400);
        assertTrue(sampled.contains(LongContentSampler.OMIT_MARKER));
        assertTrue(sampled.startsWith("segment-0"));
        assertTrue(sampled.contains("segment-499") || sampled.contains("segment-498"));
        assertTrue(sampled.length() <= 400);
    }
}
