package com.knowbase.domain.status;

public enum QueryRunStatus {
    CREATED,
    ROUTING,
    RETRIEVING,
    GENERATING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
