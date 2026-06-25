package com.knowbase.preset;

import com.knowbase.domain.model.LibraryTypePreset;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionProductCatalogTest {

    @Test
    void listsBuiltInParsersAndProfiles() {
        assertFalse(IngestionProductCatalog.parsers().isEmpty());
        assertFalse(IngestionProductCatalog.profileTemplates().isEmpty());
        assertTrue(IngestionProductCatalog.findParser("table-deep").orElseThrow().builtIn());
        assertTrue(IngestionProductCatalog.findParser("docling").orElseThrow().external());
    }

    @Test
    void buildsTechnicalDocsGuide() {
        LibraryTypePreset preset = new BuiltinPresetCatalog().findLibraryTypePreset("technical_docs").orElseThrow();
        Map<String, Object> guide = IngestionProductCatalog.buildPresetGuidePayload(preset);
        assertEquals("technical_docs", guide.get("code"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> profiles = (List<Map<String, Object>>) guide.get("documentProfiles");
        assertTrue(profiles.stream().anyMatch(item -> "default_docx".equals(item.get("code"))));
        assertTrue(profiles.stream().anyMatch(item -> "default_table".equals(item.get("code"))));
    }
}
