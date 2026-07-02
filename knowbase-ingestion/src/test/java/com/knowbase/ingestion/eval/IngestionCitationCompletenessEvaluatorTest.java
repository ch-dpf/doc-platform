package com.knowbase.ingestion.eval;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.PdfLayoutParser;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionCitationCompletenessEvaluatorTest {

    @Test
    void pdfTableDocumentScoresCitationFields() throws Exception {
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(new PdfLayoutParser().parse(new DocumentSource(
                "memory://metrics.pdf",
                "metrics.pdf",
                "application/pdf",
                new ByteArrayInputStream(buildTablePdf()),
                Map.of()
        )));
        IngestionCitationCompletenessEvaluator.DocumentScore score =
                IngestionCitationCompletenessEvaluator.evaluate(parsed);
        assertTrue(score.overallScore() >= 0.7d, "score=" + score.overallScore());
        assertTrue(score.tableRowCount() >= 1);
    }

    private static byte[] buildTablePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("Name    Age");
                stream.endText();
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 700);
                stream.showText("Alice   30");
                stream.endText();
            }
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
