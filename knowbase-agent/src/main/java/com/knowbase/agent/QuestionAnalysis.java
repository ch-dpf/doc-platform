package com.knowbase.agent;

import java.util.List;
import java.util.Map;

public record QuestionAnalysis(
        String originalQuestion,
        String normalizedQuestion,
        List<String> expandedQueries,
        List<String> keywords,
        Map<String, Object> metadata
) {
}
