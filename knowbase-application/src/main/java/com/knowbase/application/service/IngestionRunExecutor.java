package com.knowbase.application.service;

import com.knowbase.domain.model.IngestionRun;
import com.knowbase.ingestion.IngestionRequest;

public interface IngestionRunExecutor {

    IngestionRun execute(IngestionRequest request);
}
