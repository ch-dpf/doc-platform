package com.knowbase.domain.status;

public enum DocumentStatus {
    UPLOADED,
    PARSING,
    NORMALIZING,
    CHUNKING,
    EMBEDDING,
    INDEXED,
    FAILED
}