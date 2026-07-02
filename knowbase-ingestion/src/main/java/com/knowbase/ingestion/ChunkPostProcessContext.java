package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.Map;

public record ChunkPostProcessContext(
        ParsedDocument document,
        LibraryProfile libraryProfile,
        DocumentProfile documentProfile,
        ModelTokenizer tokenizer,
        Map<String, Object> sourceOptions,
        DocumentSummaryStageOutcome documentSummary
) {

    public ChunkPostProcessContext(
            ParsedDocument document,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            Map<String, Object> sourceOptions
    ) {
        this(document, libraryProfile, documentProfile, tokenizer, sourceOptions, DocumentSummaryStageOutcome.disabled());
    }
}
