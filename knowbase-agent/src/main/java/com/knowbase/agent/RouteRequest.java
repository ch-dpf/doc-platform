package com.knowbase.agent;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RouteRequest(
        UUID agentVersionId,
        String question,
        List<UUID> candidateLibraryIds,
        Map<String, Object> routingPolicy
) {
}
