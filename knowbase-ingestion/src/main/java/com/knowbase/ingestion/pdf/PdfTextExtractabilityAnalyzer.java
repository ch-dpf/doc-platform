package com.knowbase.ingestion.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/**
 * Detects image-only / scanned PDFs with little extractable text.
 */
public final class PdfTextExtractabilityAnalyzer {

    private static final int LOW_TEXT_CHARS_PER_PAGE = 12;
    private static final int LOW_TEXT_TOTAL_CHARS = 24;

    private PdfTextExtractabilityAnalyzer() {
    }

    public record Analysis(
            int pageCount,
            int totalChars,
            double charsPerPage,
            boolean scannedLikely,
            boolean lowTextDensity
    ) {
    }

    public static Analysis analyze(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            int pageCount = Math.max(1, document.getNumberOfPages());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            int totalChars = countMeaningfulChars(text);
            double charsPerPage = totalChars / (double) pageCount;
            boolean scannedLikely = totalChars == 0;
            boolean lowTextDensity = totalChars > 0
                    && charsPerPage < LOW_TEXT_CHARS_PER_PAGE
                    && totalChars < LOW_TEXT_TOTAL_CHARS;
            return new Analysis(pageCount, totalChars, charsPerPage, scannedLikely, lowTextDensity);
        }
    }

    private static int countMeaningfulChars(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                count++;
            }
        }
        return count;
    }
}
