package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataFilterResolverTest {

    @Test
    void resolvesAllowedFields() {
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setMetadataFilterFields(List.of("docType", "department"));

        List<MetadataFilterClause> clauses = MetadataFilterResolver.resolve(
                Map.of("docType", "pdf", "department", "sales"),
                retrieval);

        assertEquals(2, clauses.size());
        assertEquals(
                "pdf",
                clauses.stream().filter(c -> "docType".equals(c.field())).findFirst().orElseThrow().value());
        assertEquals(
                "sales",
                clauses.stream().filter(c -> "department".equals(c.field())).findFirst().orElseThrow().value());
    }

    @Test
    void rejectsUnknownField() {
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setMetadataFilterFields(List.of("docType"));

        assertThrows(
                InvalidMetadataFilterException.class,
                () -> MetadataFilterResolver.resolve(Map.of("department", "sales"), retrieval));
    }

    @Test
    void rejectsWhenLibraryHasNoWhitelist() {
        assertThrows(
                InvalidMetadataFilterException.class,
                () -> MetadataFilterResolver.resolve(Map.of("docType", "pdf"), new RetrievalRulesSettings()));
    }

    @Test
    void emptyRequestReturnsEmptyClauses() {
        RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
        retrieval.setMetadataFilterFields(List.of("docType"));

        assertTrue(MetadataFilterResolver.resolve(Map.of(), retrieval).isEmpty());
        assertTrue(MetadataFilterResolver.resolve(null, retrieval).isEmpty());
    }
}
