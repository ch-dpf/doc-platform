package com.knowbase.library.dto;

public record VectorLibraryListQuery(
        String tenantId,
        String keyword,
        String tag,
        int page,
        int size) {

    public VectorLibraryListQuery {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
    }
}
