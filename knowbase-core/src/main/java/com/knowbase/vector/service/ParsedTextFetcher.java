package com.knowbase.vector.service;

import com.knowbase.ingest.config.MinioProperties;
import com.knowbase.vector.storage.ParsedTextObjectStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 优先通过 MinIO SDK 按 object key 读取解析文本（避免预签名 URL 被 HTTP 客户端改坏签名）；
 * 兼容旧事件：从 URL 路径解析 key，或回退 HTTP GET。
 */
@Component
public class ParsedTextFetcher {

    private static final Logger log = LoggerFactory.getLogger(ParsedTextFetcher.class);

    private final ParsedTextObjectStore objectStore;
    private final MinioProperties minioProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public ParsedTextFetcher(ParsedTextObjectStore objectStore, MinioProperties minioProperties) {
        this.objectStore = objectStore;
        this.minioProperties = minioProperties;
    }

    public String fetch(String parsedTextKey, String parsedTextUrl) {
        String objectKey = resolveObjectKey(parsedTextKey, parsedTextUrl);
        if (objectKey != null) {
            log.debug("Fetching parsed text from MinIO key: {}", objectKey);
            return objectStore.readAsString(objectKey);
        }
        return fetchViaHttp(parsedTextUrl);
    }

    private String resolveObjectKey(String parsedTextKey, String parsedTextUrl) {
        if (parsedTextKey != null && !parsedTextKey.isBlank()) {
            return parsedTextKey.trim();
        }
        if (parsedTextUrl == null || parsedTextUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(parsedTextUrl.trim());
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                return null;
            }
            String bucketPrefix = "/" + minioProperties.getBucket() + "/";
            if (path.startsWith(bucketPrefix)) {
                return path.substring(bucketPrefix.length());
            }
        } catch (Exception e) {
            log.debug("Could not parse object key from URL: {}", e.getMessage());
        }
        return null;
    }

    private String fetchViaHttp(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url.trim()))
                    .GET()
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "Failed to fetch parsed text (HTTP " + response.statusCode() + "): " + url);
            }
            return response.body() != null ? response.body() : "";
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch parsed text: " + url + " — " + e.getMessage(), e);
        }
    }
}
