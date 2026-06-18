package com.knowbase.ingestion;

public interface DocumentParser {

    boolean supports(String sourceUri, String mimeType);

    ParsedDocument parse(DocumentSource source);
}
