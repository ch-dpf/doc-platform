package com.knowbase.api.facade;

import com.knowbase.api.command.CreateIngestionRunCommand;
import com.knowbase.api.result.IngestionRunResult;

import java.util.UUID;

public interface KnowbaseIngestionFacade {

    IngestionRunResult createIngestionRun(CreateIngestionRunCommand command);

    IngestionRunResult getIngestionRun(UUID runId);
}
