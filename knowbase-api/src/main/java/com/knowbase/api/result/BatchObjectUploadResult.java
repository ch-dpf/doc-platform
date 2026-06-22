package com.knowbase.api.result;

import java.util.List;

public record BatchObjectUploadResult(
        List<ObjectUploadResult> uploaded,
        List<UploadFailureResult> failures
) {
}
