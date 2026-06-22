package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateKnowledgeAgentCommand;
import com.knowbase.api.result.KnowledgeAgentResult;

import java.util.List;
import java.util.UUID;

public interface CreateKnowledgeAgentUseCase {

    KnowledgeAgentResult create(CreateKnowledgeAgentCommand command);

    KnowledgeAgentResult get(UUID agentId);

    List<KnowledgeAgentResult> list(String tenantId);
}
