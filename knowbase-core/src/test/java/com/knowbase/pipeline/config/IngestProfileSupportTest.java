package com.knowbase.pipeline.config;

import com.knowbase.ingest.service.InvalidDocumentException;
import com.knowbase.platform.JsonSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestProfileSupportTest {

    @Test
    void ingestProfileJsonRoundTrip() {
        IngestProfile profile = new IngestProfile();
        profile.setChunkSize(600);
        profile.setChunkOverlap(80);
        String json = JsonSupport.toJson(profile);
        assertTrue(json.contains("600"));
        IngestProfile parsed = JsonSupport.fromJson(json, IngestProfile.class);
        assertEquals(600, parsed.getChunkSize());
        assertEquals(80, parsed.getChunkOverlap());
    }

    @Test
    void prepareForUploadAcceptsChunkNumericOverrides() {
        String prepared = IngestProfileSupport.prepareForUpload("{\"chunkSize\":600,\"chunkOverlap\":80}");
        assertNotNull(prepared);
        var summary = IngestProfileSupport.toSummary(prepared);
        assertEquals(600, summary.chunkSize());
        assertEquals(80, summary.chunkOverlap());
    }

    @Test
    void prepareForUploadRejectsParsingAndCleaningFields() {
        InvalidDocumentException ex = assertThrows(
                InvalidDocumentException.class,
                () -> IngestProfileSupport.prepareForUpload("{\"parsing\":{\"ocrEnabled\":true}}"));
        assertEquals(InvalidDocumentException.CODE_INGEST_PROFILE_INVALID, ex.getErrorCode());
    }

    @Test
    void prepareForUploadReturnsNullForBlankInput() {
        assertNull(IngestProfileSupport.prepareForUpload(null));
        assertNull(IngestProfileSupport.prepareForUpload("  "));
    }

    @Test
    void toSummaryExposesChunkOverrides() {
        var summary = IngestProfileSupport.toSummary("{\"chunkSize\":600,\"chunkOverlap\":80}");
        assertEquals(600, summary.chunkSize());
        assertEquals(80, summary.chunkOverlap());
    }

    @Test
    void toSummaryReturnsNullWhenNoChunkOverrides() {
        assertNull(IngestProfileSupport.toSummary("{\"minParagraphLength\":40}"));
        assertNull(IngestProfileSupport.toSummary(null));
    }
}
