package com.knowbase.library.dto;

import java.util.UUID;

public record DeleteVectorLibraryResponse(
        UUID libraryId,
        String name,
        int deletedDocuments,
        int deletedChunks,
        String message) {}
