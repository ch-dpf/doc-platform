package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;

public interface DocumentNormalizer {

    NormalizationResult normalize(ParsedDocument parsed, DocumentProfile documentProfile);

    String normalizeText(String text);
}
