package com.docplatform.library.service;

public class UnsupportedEmbeddingProviderException extends RuntimeException {

    public UnsupportedEmbeddingProviderException(String provider) {
        super("Unsupported embedding provider: " + provider + " (only ollama is supported)");
    }
}
