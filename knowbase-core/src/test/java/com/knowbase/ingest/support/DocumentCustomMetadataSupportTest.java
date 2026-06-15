package com.knowbase.ingest.support;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentCustomMetadataSupportTest {

    @Test
    void normalizesValidJsonObject() {
        String json = DocumentCustomMetadataSupport.normalizeJson("{\"department\":\"sales\",\"docType\":\"policy\"}");
        Map<?, ?> parsed = com.knowbase.platform.JsonSupport.fromJson(json, Map.class);
        assertEquals("sales", parsed.get("department"));
        assertEquals("policy", parsed.get("docType"));
    }

    @Test
    void blankReturnsNull() {
        assertNull(DocumentCustomMetadataSupport.normalizeJson(null));
        assertNull(DocumentCustomMetadataSupport.normalizeJson("  "));
    }

    @Test
    void rejectsBlankValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> DocumentCustomMetadataSupport.normalizeJson("{\"department\":\"\"}"));
    }
}
