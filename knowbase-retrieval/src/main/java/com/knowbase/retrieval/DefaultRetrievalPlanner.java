package com.knowbase.retrieval;

import com.knowbase.agent.QuestionAnalysis;
import com.knowbase.domain.model.AgentVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultRetrievalPlanner implements RetrievalPlanner {

    @Override
    public RetrievalPlan plan(AgentVersion agentVersion, QuestionAnalysis analysis, List<UUID> routedLibraryIds) {
        Map<String, Object> retrievalPolicy = agentVersion.retrievalPolicy() == null
                ? Map.of()
                : new HashMap<>(agentVersion.retrievalPolicy());
        if (analysis != null && analysis.keywords() != null && !analysis.keywords().isEmpty()) {
            retrievalPolicy.putIfAbsent("queryKeywords", analysis.keywords());
        }
        if (analysis != null && analysis.expandedQueries() != null && !analysis.expandedQueries().isEmpty()) {
            retrievalPolicy.putIfAbsent("expandedQueries", analysis.expandedQueries());
        }
        return new RetrievalPlan(
                List.copyOf(routedLibraryIds),
                Map.copyOf(retrievalPolicy),
                readInt(retrievalPolicy, "topKPerLibrary", 8),
                readInt(retrievalPolicy, "maxCandidates", 24),
                readInt(retrievalPolicy, "maxEvidence", 12),
                stringValue(retrievalPolicy, "fusion", "score"),
                stringValue(retrievalPolicy, "rerank", "none")
        );
    }

    private static int readInt(Map<String, Object> policy, String key, int defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        Object value = policy.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private static String stringValue(Map<String, Object> policy, String key, String defaultValue) {
        if (policy == null || policy.get(key) == null) {
            return defaultValue;
        }
        String value = String.valueOf(policy.get(key));
        return value.isBlank() ? defaultValue : value;
    }
}
