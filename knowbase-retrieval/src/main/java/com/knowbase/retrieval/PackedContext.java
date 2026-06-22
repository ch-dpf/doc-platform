package com.knowbase.retrieval;

import com.knowbase.domain.model.Citation;

import java.util.List;

public record PackedContext(
        String context,
        List<Citation> citations,
        int tokenCount
) {
}
