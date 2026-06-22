package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record DocumentPreparationResult(
        String sourceUri,
        ParsedDocument parsed,
        NormalizationResult normalization,
        List<DocumentChunk> chunks,
        DocumentProfile documentProfile,
        Map<String, Object> options
) {

    public boolean structureAware() {
        return parsed != null && parsed.structureAware();
    }
}
