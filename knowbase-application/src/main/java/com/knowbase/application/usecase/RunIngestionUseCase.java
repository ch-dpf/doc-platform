package com.knowbase.application.usecase;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.result.IngestionRunResult;

import java.util.UUID;

public interface RunIngestionUseCase {

    IngestionRunResult create(CreateIngestionRunCommand command);

    IngestionRunResult get(UUID runId);
}
