package com.knowbase.api.facade;

import com.knowbase.api.command.AskQuestionCommand;
import com.knowbase.api.result.QueryRunResult;

import java.util.UUID;

public interface KnowbaseQuestionFacade {

    QueryRunResult ask(AskQuestionCommand command);

    QueryRunResult getQueryRun(UUID queryRunId);
}
