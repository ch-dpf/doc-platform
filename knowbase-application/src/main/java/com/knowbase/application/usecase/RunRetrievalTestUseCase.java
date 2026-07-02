package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateRetrievalTestCommand;
import com.knowbase.api.result.RetrievalTestResult;

import java.util.UUID;

public interface RunRetrievalTestUseCase {

    RetrievalTestResult run(UUID agentId, CreateRetrievalTestCommand command);
}
