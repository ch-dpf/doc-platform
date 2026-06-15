package com.knowbase.library.dto;

import java.util.UUID;

public record RetryBatchJobResponse(
        UUID sourceJobId,
        UUID jobId,
        int retriedCount,
        String message) {}
