package com.knowbase.ingestion;

import com.knowbase.domain.model.IngestionRun;

public interface IngestionPipeline {

    IngestionRun run(IngestionRequest request);
}
