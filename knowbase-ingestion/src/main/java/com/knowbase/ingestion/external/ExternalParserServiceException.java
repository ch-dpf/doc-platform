package com.knowbase.ingestion.external;

/**
 * External parser returned HTTP 200 with a structured error payload.
 */
public final class ExternalParserServiceException extends RuntimeException {

    private final String errorCode;

    public ExternalParserServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
