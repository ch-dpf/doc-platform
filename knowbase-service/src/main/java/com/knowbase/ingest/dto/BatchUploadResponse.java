package com.knowbase.ingest.dto;

import java.util.List;

public record BatchUploadResponse(int total, int succeeded, int failed, List<BatchUploadItemResult> items) {}
