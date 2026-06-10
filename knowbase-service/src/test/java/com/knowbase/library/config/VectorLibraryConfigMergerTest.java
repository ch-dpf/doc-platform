package com.knowbase.library.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VectorLibraryConfigMergerTest {

    @Test
    void mergesLibraryPresetIdWhenIncomingCustom() {
        VectorLibraryConfig target = new VectorLibraryConfig();
        target.setLibraryPresetId("policy-longform");

        VectorLibraryConfig incoming = new VectorLibraryConfig();
        incoming.setLibraryPresetId("custom");

        VectorLibraryConfigMerger.mergeSafeFields(target, incoming);

        assertEquals("custom", target.getLibraryPresetId());
    }

    @Test
    void incomingNullLibraryPresetIdDoesNotOverwrite() {
        VectorLibraryConfig target = new VectorLibraryConfig();
        target.setLibraryPresetId("weekly-report-excel");

        VectorLibraryConfig incoming = new VectorLibraryConfig();
        incoming.setLibraryPresetId(null);

        VectorLibraryConfigMerger.mergeSafeFields(target, incoming);

        assertEquals("weekly-report-excel", target.getLibraryPresetId());
    }

    @Test
    void lockPipelineStillAllowsMetadataAndPresetIdMerge() {
        VectorLibraryConfig target = new VectorLibraryConfig();
        target.setLibraryPresetId("policy-longform");
        target.setChunkSize(500);

        VectorLibraryConfig incoming = new VectorLibraryConfig();
        incoming.setLibraryPresetId("custom");
        incoming.setTags(java.util.List.of("ops"));
        incoming.setChunkSize(800);

        VectorLibraryConfigMerger.mergeSafeFields(target, incoming, true);

        assertEquals("custom", target.getLibraryPresetId());
        assertEquals(java.util.List.of("ops"), target.getTags());
        assertEquals(500, target.getChunkSize());
    }
}
