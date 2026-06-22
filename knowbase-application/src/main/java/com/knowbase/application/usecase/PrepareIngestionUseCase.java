package com.knowbase.application.usecase;

import com.knowbase.api.command.PrepareIngestionCommand;
import com.knowbase.api.result.IngestionPrepareResult;

public interface PrepareIngestionUseCase {

    IngestionPrepareResult prepare(PrepareIngestionCommand command);
}
