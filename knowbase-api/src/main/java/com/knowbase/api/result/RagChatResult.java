package com.knowbase.api.result;

import java.util.List;

public record RagChatResult(
        String answer,
        List<RagCitationResult> citations,
        int retrievedCount,
        boolean usedLlm,
        boolean found,
        String searchQuery) {}
