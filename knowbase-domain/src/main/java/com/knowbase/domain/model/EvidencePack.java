package com.knowbase.domain.model;

import java.util.List;
import java.util.UUID;

public record EvidencePack(
        UUID evidencePackId,
        List<EvidenceSegment> segments,
        List<Citation> citations,
        int contextTokens,
        String tokenizerId,
        String tokenizerVersion
) {
}
