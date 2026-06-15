package com.knowbase.library.dto;

import java.util.UUID;

public record FailedBatchJobItem(UUID docId, String fileName, boolean deleted) {}
