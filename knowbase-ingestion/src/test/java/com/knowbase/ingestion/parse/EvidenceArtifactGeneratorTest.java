package com.knowbase.ingestion.parse;

import com.knowbase.storage.LocalFilesystemObjectStorage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceArtifactGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void storesPageArtifactsWhenEnabled() throws Exception {
        LocalFilesystemObjectStorage storage = new LocalFilesystemObjectStorage(tempDir);
        EvidenceArtifactGenerator generator = new EvidenceArtifactGenerator(storage, "knowbase-evidence", true, 2);
        byte[] pdfBytes = buildPdf();
        Map<String, Object> metadata = generator.generateForPdf(pdfBytes, "file://sample.pdf");
        assertTrue((Boolean) metadata.get("evidenceArtifactsGenerated"));
        @SuppressWarnings("unchecked")
        Map<String, String> pageArtifacts = (Map<String, String>) metadata.get("pageArtifacts");
        assertEquals(1, pageArtifacts.size());
        Path stored = Path.of(URI.create(pageArtifacts.get("1")));
        assertTrue(Files.exists(stored));
        assertTrue(Files.size(stored) > 0);
    }

    private static byte[] buildPdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("Evidence artifact page");
                stream.endText();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
