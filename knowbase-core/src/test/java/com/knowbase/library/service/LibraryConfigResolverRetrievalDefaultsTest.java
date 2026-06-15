package com.knowbase.library.service;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.retrieval.TemporalMetadataFields;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryConfigResolverRetrievalDefaultsTest {

    @Test
    void mergesTemporalFilterWhitelistWhenEmpty() {
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setMetadataFilterFields(java.util.List.of());

        RetrievalRulesSettings merged = invokeWithDefaults(retrieval);

        assertTrue(merged.getMetadataFilterFields().contains(TemporalMetadataFields.PERIOD_YEAR));
        assertTrue(merged.getMetadataFilterFields().contains(TemporalMetadataFields.SUBMITTER));
        assertEquals(TemporalMetadataFields.defaultFilterWhitelist().size(), merged.getMetadataFilterFields().size());
    }

    @Test
    void preservesUserConfiguredFilterFields() {
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setMetadataFilterFields(java.util.List.of("docType"));

        RetrievalRulesSettings merged = invokeWithDefaults(retrieval);

        assertEquals(1, merged.getMetadataFilterFields().size());
        assertEquals("docType", merged.getMetadataFilterFields().getFirst());
    }

    private static RetrievalRulesSettings invokeWithDefaults(RetrievalRulesSettings retrieval) {
        try {
            var method = LibraryConfigResolver.class.getDeclaredMethod(
                    "withDefaultMetadataFilterFields", RetrievalRulesSettings.class);
            method.setAccessible(true);
            return (RetrievalRulesSettings) method.invoke(null, retrieval);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
