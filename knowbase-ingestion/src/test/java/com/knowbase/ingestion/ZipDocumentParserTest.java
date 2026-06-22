package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipDocumentParserTest {

    private final ZipDocumentParser parser = new ZipDocumentParser();

    @Test
    void parsesMarkdownBundleWithImageReference() throws Exception {
        Map<String, byte[]> entries = new java.util.LinkedHashMap<>();
        entries.put("guide/readme.md", "# Title\nSee ![diagram](diagram.png)\n".getBytes(StandardCharsets.UTF_8));
        entries.put("guide/diagram.png", "fake-image".getBytes(StandardCharsets.UTF_8));
        byte[] zipBytes = buildZip(entries);
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "minio://knowbase/docs.zip",
                "docs.zip",
                "application/zip",
                new ByteArrayInputStream(zipBytes),
                Map.of("filename", "docs.zip")
        ));

        assertEquals(ContentFamily.RICH_TEXT, parsed.contentFamily());
        assertTrue(parsed.text().contains("guide/readme.md"));
        assertTrue(parsed.text().contains("[image:guide/diagram.png]"));
        assertEquals(1, parsed.metadata().get("markdownEntryCount"));
    }

    private static byte[] buildZip(Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.getKey()));
                zipOutputStream.write(entry.getValue());
                zipOutputStream.closeEntry();
            }
        }
        return outputStream.toByteArray();
    }
}
