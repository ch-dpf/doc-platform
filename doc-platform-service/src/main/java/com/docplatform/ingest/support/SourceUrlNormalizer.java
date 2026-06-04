package com.docplatform.ingest.support;

import java.net.URI;

/**
 * Canonical form for crawl source URLs so the same logical page maps to one document key.
 */
public final class SourceUrlNormalizer {

    private SourceUrlNormalizer() {}

    public static String normalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        URI uri = URI.create(rawUrl.trim());
        if (uri.getScheme() == null || uri.getHost() == null) {
            throw new IllegalArgumentException("URL must include scheme and host, e.g. https://example.com/path");
        }
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();
        int port = uri.getPort();
        String path = uri.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        } else if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String query = uri.getRawQuery();
        StringBuilder canonical = new StringBuilder();
        canonical.append(scheme).append("://").append(host);
        if (port != -1 && port != defaultPort(scheme)) {
            canonical.append(':').append(port);
        }
        canonical.append(path);
        if (query != null && !query.isBlank()) {
            canonical.append('?').append(query);
        }
        return canonical.toString();
    }

    private static int defaultPort(String scheme) {
        return switch (scheme) {
            case "https" -> 443;
            case "http" -> 80;
            default -> -1;
        };
    }
}
