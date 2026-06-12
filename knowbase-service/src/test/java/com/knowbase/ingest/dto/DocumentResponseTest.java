package com.knowbase.ingest.dto;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.domain.ParseStatus;
import com.knowbase.ingest.domain.SourceType;
import com.knowbase.pipeline.config.IngestReport;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentResponseTest {

    @Test
    void fromParsesIngestReportJson() {
        DocMetadata doc = baseDoc();
        doc.setIngestReportJson("""
                {
                  "rawChunkCount": 10,
                  "filteredOutCount": 6,
                  "finalChunkCount": 4,
                  "avgChunkLength": 128.5,
                  "headerOnlyRatioWarning": true,
                  "pipelineConfigVersion": 2
                }
                """);

        DocumentResponse response = DocumentResponse.from(doc);

        assertNotNull(response.ingestReport());
        assertEquals(10, response.ingestReport().getRawChunkCount());
        assertEquals(6, response.ingestReport().getFilteredOutCount());
        assertEquals(4, response.ingestReport().getFinalChunkCount());
        assertEquals(128.5, response.ingestReport().getAvgChunkLength(), 0.001);
        assertTrue(response.ingestReport().isHeaderOnlyRatioWarning());
        assertEquals(2, response.ingestReport().getPipelineConfigVersion());
    }

    @Test
    void fromParsesIngestProfileSummary() {
        DocMetadata doc = baseDoc();
        doc.setIngestProfileJson("{\"chunkSize\":600,\"chunkOverlap\":80}");

        DocumentResponse response = DocumentResponse.from(doc);

        assertEquals(600, response.ingestProfile().chunkSize());
        assertEquals(80, response.ingestProfile().chunkOverlap());
    }

    @Test
    void fromParsesContentSignalsJson() {
        DocMetadata doc = baseDoc();
        doc.setContentSignalsJson("""
                {
                  "contentFamily": "document",
                  "textLength": 4200,
                  "shortDocument": false,
                  "markdownHeadings": false,
                  "headingLineRatio": 0.12
                }
                """);

        DocumentResponse response = DocumentResponse.from(doc);

        assertNotNull(response.contentSignals());
        assertEquals(4200, response.contentSignals().getTextLength());
        assertEquals(0.12, response.contentSignals().getHeadingLineRatio(), 0.001);
    }

    @Test
    void fromReturnsNullIngestReportWhenJsonMissingOrInvalid() {
        DocMetadata doc = baseDoc();
        assertNull(DocumentResponse.from(doc).ingestReport());

        doc.setIngestReportJson("{not-json");
        assertNull(DocumentResponse.from(doc).ingestReport());
    }

    private static DocMetadata baseDoc() {
        DocMetadata doc = new DocMetadata();
        doc.setDocId(UUID.randomUUID());
        doc.setLibraryId(UUID.randomUUID());
        doc.setTenantId("demo");
        doc.setSourceType(SourceType.UPLOAD);
        doc.setFileName("sample.pdf");
        doc.setMimeType("application/pdf");
        doc.setSizeBytes(1024);
        doc.setParseStatus(ParseStatus.PARSED);
        doc.setVersion(1);
        doc.setIndexRequested(true);
        return doc;
    }
}
