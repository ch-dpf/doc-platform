package com.knowbase.domain.status;

public enum IngestionRunStatus {
    CREATED,
    VALIDATING,
    RUNNING,
    PARTIAL_FAILED,
    FAILED,
    SUCCEEDED,
    CANCELLED
}
