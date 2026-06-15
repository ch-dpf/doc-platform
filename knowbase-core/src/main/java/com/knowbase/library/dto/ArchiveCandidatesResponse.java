package com.knowbase.library.dto;

import java.util.List;

public record ArchiveCandidatesResponse(
        int totalCount,
        List<ArchiveCandidateItem> previewItems) {}
