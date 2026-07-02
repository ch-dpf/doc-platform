package com.knowbase.api.result;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AgentVersionResult(
        UUID agentVersionId,
        UUID agentId,
        int version,
        String status,
        String scenePresetCode,
        List<UUID> libraryIds,
        Map<String, Object> routingPolicy,
        Map<String, Object> retrievalPolicy,
        Map<String, Object> answerPolicy,
        String systemPrompt,
        UUID chatTokenizerProfileId,
        boolean published,
        Instant createdAt
) {
}
