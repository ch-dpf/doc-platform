package com.knowbase.vector.retrieval;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.SourceType;
import com.knowbase.platform.JsonSupport;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChunkMetadataBuilderTest {

    @Test
    void buildsStandardFieldsAndCustomMetadata() {
        DocMetadata doc = new DocMetadata();
        doc.setFileName("report.pdf");
        doc.setMimeType("application/pdf");
        doc.setSourceType(SourceType.UPLOAD);
        doc.setCustomMetadataJson("{\"department\":\"sales\",\"docType\":\"policy\"}");

        String json = ChunkMetadataBuilder.buildJson(doc);
        Map<?, ?> metadata = JsonSupport.fromJson(json, Map.class);

        assertEquals("application/pdf", metadata.get("mimeType"));
        assertEquals("UPLOAD", metadata.get("sourceType"));
        assertEquals("report.pdf", metadata.get("fileName"));
        assertEquals("policy", metadata.get("docType"));
        assertEquals("sales", metadata.get("department"));
    }

    @Test
    void returnsNullWhenNoMetadataAvailable() {
        assertNull(ChunkMetadataBuilder.buildJson(null));
        assertNull(ChunkMetadataBuilder.buildJson(new DocMetadata()));
    }

    @Test
    void resolveDocTypeFromExtension() {
        assertEquals("word", ChunkMetadataBuilder.resolveDocType("a.docx", null));
        assertEquals("markdown", ChunkMetadataBuilder.resolveDocType("readme.md", null));
    }
}
