package com.knowbase.api.facade;

import com.knowbase.api.command.CreateKnowledgeAgentCommand;
import com.knowbase.api.result.KnowledgeAgentResult;

import java.util.List;
import java.util.UUID;

public interface KnowbaseAgentFacade {

    KnowledgeAgentResult createKnowledgeAgent(CreateKnowledgeAgentCommand command);

    KnowledgeAgentResult getKnowledgeAgent(UUID agentId);

    List<KnowledgeAgentResult> listKnowledgeAgents(String tenantId);
}
