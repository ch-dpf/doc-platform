package com.knowbase.retrieval;

import com.knowbase.agent.QuestionAnalysis;
import com.knowbase.domain.model.AgentVersion;

import java.util.List;
import java.util.UUID;

public interface RetrievalPlanner {

    RetrievalPlan plan(AgentVersion agentVersion, QuestionAnalysis analysis, List<UUID> routedLibraryIds);
}
