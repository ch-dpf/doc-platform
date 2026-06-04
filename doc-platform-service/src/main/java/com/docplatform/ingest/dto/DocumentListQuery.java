package com.docplatform.ingest.dto;

import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.domain.ParseStatus;
import com.docplatform.ingest.domain.SourceType;

import java.util.UUID;

public record DocumentListQuery(
        UUID libraryId,
        String tenantId,
        int page,
        int size,
        SourceType sourceType,
        ParseStatus parseStatus,
        IndexStatus indexStatus,
        String keyword
) {
    public DocumentListQuery {
        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }
    }
}
