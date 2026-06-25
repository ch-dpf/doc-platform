package com.knowbase.ingestion.ocr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses plain OCR text into line results when structured engine output is unavailable.
 */
public final class OcrPlainTextParser {

    private OcrPlainTextParser() {
    }

    public static List<OcrLineResult> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<OcrLineResult> lines = new ArrayList<>();
        String[] rawLines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String rawLine : rawLines) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            lines.add(new OcrLineResult(rawLine.trim(), List.of(), null, List.of(), "line"));
        }
        return lines;
    }
}
