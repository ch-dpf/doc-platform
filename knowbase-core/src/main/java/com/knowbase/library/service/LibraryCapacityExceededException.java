package com.knowbase.library.service;

public class LibraryCapacityExceededException extends RuntimeException {

    public static final String CODE_CHUNK_LIMIT = "LIBRARY_CHUNK_LIMIT_EXCEEDED";

    private final String errorCode;

    public LibraryCapacityExceededException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
