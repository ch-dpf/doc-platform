package com.knowbase.application.usecase;

import com.knowbase.api.command.AskQuestionCommand;
import com.knowbase.api.result.QueryRunResult;

import java.util.UUID;

public interface AskQuestionUseCase {

    QueryRunResult ask(AskQuestionCommand command);

    QueryRunResult get(UUID queryRunId);
}
