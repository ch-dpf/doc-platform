package com.knowbase.ingest.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestPropertiesTest {

    @Test
    void resolvesVersionStrategyWhenPolicyDisabled() {
        IngestProperties props = new IngestProperties();
        props.getVersionPolicy().setEnabled(false);
        props.getVersionPolicy().setUpdateStrategy("keep-history");

        assertEquals("overwrite", props.resolvedVersionUpdateStrategy());
    }

    @Test
    void resolvesVersionStrategyWhenPolicyEnabled() {
        IngestProperties props = new IngestProperties();
        props.getVersionPolicy().setEnabled(true);
        props.getVersionPolicy().setUpdateStrategy("incremental");

        assertEquals("incremental", props.resolvedVersionUpdateStrategy());
    }

    @Test
    void detectsManualReviewMode() {
        IngestProperties props = new IngestProperties();
        props.setIngestReviewMode("manual-review");

        assertTrue(props.requiresManualReview());
    }

    @Test
    void autoReviewByDefault() {
        IngestProperties props = new IngestProperties();

        assertFalse(props.requiresManualReview());
        assertEquals("auto", props.getIngestReviewMode());
    }
}
