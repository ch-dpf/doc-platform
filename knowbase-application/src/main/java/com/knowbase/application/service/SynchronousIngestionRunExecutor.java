package com.knowbase.application.service;

import com.knowbase.domain.model.IngestionRun;
import com.knowbase.ingestion.IngestionPipeline;
import com.knowbase.ingestion.IngestionRequest;

public final class SynchronousIngestionRunExecutor implements IngestionRunExecutor {

    private final IngestionPipeline ingestionPipeline;

    public SynchronousIngestionRunExecutor(IngestionPipeline ingestionPipeline) {
        this.ingestionPipeline = ingestionPipeline;
    }

    @Override
    public IngestionRun execute(IngestionRequest request) {
        return ingestionPipeline.run(request);
    }
}
