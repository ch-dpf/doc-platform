package com.knowbase.ingestion;

import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleConfigParseRegressionTest {

    @Test
    void yamlConfigProducesSectionBlocksWithKeys() throws Exception {
        byte[] bytes = SampleConfigParseRegressionTest.class.getClassLoader()
                .getResourceAsStream("sample-documents/config/application-sample.yml")
                .readAllBytes();

        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new CodeConfigStructureParser().parse(new DocumentSource(
                "memory://application-sample.yml",
                "application-sample.yml",
                "text/yaml",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.structureAware());
        assertEquals("code-config-structure", parsed.metadata().get("parserCode"));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "config_section".equals(block.blockType())));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "knowbase".equals(block.metadata().get("configKey"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.content().contains("ocrConfidenceThreshold")));
        assertTrue(parsed.blocks().stream().anyMatch(block -> block.metadata().containsKey("evidenceAssetHint")));
        assertNotNull(parsed.metadata().get("parseConfidence"));
    }
}
