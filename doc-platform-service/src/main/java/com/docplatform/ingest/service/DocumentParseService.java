package com.docplatform.ingest.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.InputStream;

@Service
public class DocumentParseService {

    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();

    public String extractText(InputStream inputStream, String fileName) {
        try {
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            parser.parse(inputStream, handler, metadata, new ParseContext());
            String text = handler.toString();
            if (text == null || text.isBlank()) {
                return "";
            }
            return text.trim();
        } catch (Exception e) {
            throw new ParseException("Failed to parse document: " + fileName, e);
        }
    }

    public String detectMimeType(byte[] sample, String fileName) {
        return tika.detect(sample, fileName);
    }
}
