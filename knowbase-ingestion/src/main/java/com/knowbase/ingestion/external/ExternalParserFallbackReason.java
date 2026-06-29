package com.knowbase.ingestion.external;

/**
 * Normalized fallback reason codes for external parser failures.
 */
public final class ExternalParserFallbackReason {

    public static final String NETWORK = "network";
    public static final String TIMEOUT = "timeout";
    public static final String HTTP_CLIENT = "http_client";
    public static final String HTTP_SERVER = "http_server";
    public static final String EMPTY_RESPONSE = "empty_response";
    public static final String SERVICE_ERROR = "service_error";
    public static final String SCHEMA_UNSUPPORTED = "schema_unsupported";
    public static final String MAPPING_FAILED = "mapping_failed";

    private ExternalParserFallbackReason() {
    }

    public static String classify(Throwable cause, int httpStatus) {
        if (cause instanceof ExternalParserServiceException service) {
            String code = service.errorCode();
            return code == null || code.isBlank() ? SERVICE_ERROR : code;
        }
        if (httpStatus >= 500) {
            return HTTP_SERVER;
        }
        if (httpStatus >= 400) {
            return HTTP_CLIENT;
        }
        if (cause == null) {
            return MAPPING_FAILED;
        }
        if (cause instanceof java.net.http.HttpTimeoutException
                || cause instanceof java.util.concurrent.TimeoutException) {
            return TIMEOUT;
        }
        if (cause instanceof java.io.IOException) {
            return NETWORK;
        }
        String message = cause.getMessage() == null ? "" : cause.getMessage().toLowerCase();
        if (message.contains("timeout")) {
            return TIMEOUT;
        }
        if (message.contains("status码") || message.contains("status code")) {
            if (message.contains("5")) {
                return HTTP_SERVER;
            }
            return HTTP_CLIENT;
        }
        return MAPPING_FAILED;
    }

    public static String classifyHttpStatus(int statusCode) {
        if (statusCode >= 500) {
            return HTTP_SERVER;
        }
        if (statusCode >= 400) {
            return HTTP_CLIENT;
        }
        return MAPPING_FAILED;
    }
}
