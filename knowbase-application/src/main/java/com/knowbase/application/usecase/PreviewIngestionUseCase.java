package com.knowbase.application.usecase;

import com.knowbase.api.command.PreviewIngestionCommand;
import com.knowbase.api.result.IngestionPreviewResult;

public interface PreviewIngestionUseCase {

    IngestionPreviewResult preview(PreviewIngestionCommand command);
}
