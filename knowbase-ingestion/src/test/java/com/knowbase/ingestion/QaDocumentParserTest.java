package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QaDocumentParserTest {

    private final QaDocumentParser parser = new QaDocumentParser();

    @Test
    void parsesCsvQaPairs() {
        String csv = "question,answer\nWhat is KnowBase?,A knowledge platform\nHow to ingest?,Upload then run\n";
        ParsedDocument parsed = parser.parse(source("faq.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8)));

        assertEquals(ContentFamily.PLAIN_TEXT, parsed.contentFamily());
        assertTrue(parsed.text().contains("问：What is KnowBase?"));
        assertTrue(parsed.text().contains("答：A knowledge platform"));
        assertEquals(2, parsed.metadata().get("qaPairCount"));
    }

    @Test
    void supportsExplicitQaFilename() {
        assertTrue(parser.supportsExplicit("minio://knowbase/faq.xlsx", "application/vnd.ms-excel", null));
    }

    private static DocumentSource source(String filename, String mimeType, byte[] content) {
        return new DocumentSource(
                "minio://knowbase/" + filename,
                filename,
                mimeType,
                new ByteArrayInputStream(content),
                Map.of("filename", filename)
        );
    }
}
