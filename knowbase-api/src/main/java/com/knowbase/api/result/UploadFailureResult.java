package com.knowbase.api.result;

public record UploadFailureResult(
        String filename,
        String message
) {
}
