package com.knowbase.autoconfigure;

import com.knowbase.api.result.ParserHealthResult;
import com.knowbase.application.service.ParserHealthProbe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class DefaultParserHealthProbe implements ParserHealthProbe {

    private static final Duration PROBE_TIMEOUT = Duration.ofMillis(900);
    private static final Duration CACHE_TTL = Duration.ofSeconds(15);

    private final KnowbaseProperties properties;
    private final HttpClient httpClient;
    private final Map<String, CachedHealth> cache = new ConcurrentHashMap<>();

    DefaultParserHealthProbe(KnowbaseProperties properties) {
        this(properties, HttpClient.newBuilder().connectTimeout(PROBE_TIMEOUT).build());
    }

    DefaultParserHealthProbe(KnowbaseProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public ParserHealthResult check(String parserCode) {
        String code = parserCode == null ? "" : parserCode.trim().toLowerCase(Locale.ROOT);
        CachedHealth cached = cache.get(code);
        Instant now = Instant.now();
        if (cached != null && Duration.between(cached.cachedAt(), now).compareTo(CACHE_TTL) < 0) {
            return cached.result();
        }
        ParserHealthResult result = switch (code) {
            case "pdf-layout" -> pdfLayoutHealth();
            case "ocr-layout" -> ocrLayoutHealth();
            case "docling", "unstructured", "external" -> externalParserHealth(code);
            default -> ready("内置解析器可用", true, null, code, Map.of("dependency", "in-process"));
        };
        cache.put(code, new CachedHealth(result, now));
        return result;
    }

    private ParserHealthResult pdfLayoutHealth() {
        KnowbaseProperties.VisionDocument vision = properties.getVisionDocument();
        KnowbaseProperties.Pdf pdf = properties.getIngestion().getPdf();
        Map<String, Object> details = new HashMap<>();
        details.put("vlOnScanned", pdf.isVlOnScanned());
        details.put("vlOnLowConfidence", pdf.isVlOnLowConfidence());
        details.put("fallbackToHeuristic", pdf.isVlFallbackToHeuristic());
        if (vision == null || !vision.isEnabled()) {
            details.put("layoutFallback", "local-pdf-layout");
            return ready("本地 PDF layout 可用，VLM 未启用", true, "local-pdf-layout", "local-pdf-layout", details);
        }
        String provider = normalize(vision.getProvider(), "paddleocr-vl");
        details.put("visionProvider", provider);
        if ("paddleocr-vl".equals(provider)) {
            String endpoint = joinUrl(vision.getPaddleocrVl().getBaseUrl(), vision.getPaddleocrVl().getLayoutParsingPath());
            return endpointHealth(endpoint, provider, "PaddleOCR-VL 已配置", details, true);
        }
        if ("vllm".equals(provider)) {
            String endpoint = joinUrl(vision.getVllm().getBaseUrl(), vision.getVllm().getChatCompletionsPath());
            details.put("model", vision.getVllm().getModel());
            return endpointHealth(endpoint, provider, "vLLM 视觉解析已配置", details, true);
        }
        if ("ollama".equals(provider)) {
            String endpoint = properties.getOllama().getBaseUrl();
            details.put("model", properties.getOllama().getVisionLanguageModel());
            return endpointHealth(endpoint, provider, "Ollama VLM 已配置", details, true);
        }
        details.put("layoutFallback", "local-pdf-layout");
        return degraded("未知 VLM provider，已回退本地 layout", true, null, provider, details);
    }

    private ParserHealthResult ocrLayoutHealth() {
        KnowbaseProperties.Ocr ocr = properties.getIngestion().getOcr();
        String engine = normalize(ocr.getDefaultEngine(), "tesseract");
        Map<String, Object> details = new HashMap<>();
        details.put("defaultEngine", engine);
        details.put("language", ocr.getLanguage());
        details.put("confidenceThreshold", ocr.getConfidenceThreshold());
        details.put("downweightMode", ocr.getDownweightMode());
        if ("paddle".equals(engine)) {
            String endpoint = firstNonBlank(System.getenv("KNOWBASE_PADDLE_OCR_ENDPOINT"), "");
            if (endpoint.isBlank()) {
                return unconfigured("Paddle OCR endpoint 未配置，OCR 会失败或需请求级传入 endpoint", null, engine, details);
            }
            return endpointHealth(endpoint, engine, "Paddle OCR 已配置", details, true);
        }
        return unknown("Tesseract OCR 配置可用，二进制能力将在首次 OCR 时验证", true, "tesseract", engine, details);
    }

    private ParserHealthResult externalParserHealth(String parserCode) {
        String endpoint = firstNonBlank(System.getenv("KNOWBASE_EXTERNAL_PARSER_ENDPOINT"), "");
        Map<String, Object> details = new HashMap<>();
        details.put("parserCode", parserCode);
        details.put("endpointSource", endpoint.isBlank() ? "documentProfile/options" : "environment");
        if (endpoint.isBlank()) {
            return unconfigured("外接解析器未全局配置 endpoint，可在 DocumentProfile 或入库 options 中传入", null, parserCode, details);
        }
        return endpointHealth(endpoint, parserCode, "外接解析器 endpoint 已配置", details, true);
    }

    private ParserHealthResult endpointHealth(
            String endpoint,
            String provider,
            String readyMessage,
            Map<String, Object> details,
            boolean configured
    ) {
        if (endpoint == null || endpoint.isBlank()) {
            return unconfigured(provider + " endpoint 未配置", null, provider, details);
        }
        Map<String, Object> enriched = new HashMap<>(details);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(PROBE_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            enriched.put("httpStatus", statusCode);
            boolean reachable = statusCode < 500 || statusCode == 501;
            if (reachable) {
                return ready(readyMessage + "，端点可达", configured, endpoint, provider, enriched);
            }
            return degraded(provider + " endpoint 返回状态码 " + statusCode, configured, endpoint, provider, enriched);
        } catch (Exception exception) {
            enriched.put("error", exception.getClass().getSimpleName());
            enriched.put("errorMessage", exception.getMessage());
            return degraded(provider + " endpoint 暂不可达，复杂文档将回退或失败", configured, endpoint, provider, enriched);
        }
    }

    private static ParserHealthResult ready(
            String message,
            boolean configured,
            String endpoint,
            String provider,
            Map<String, Object> details
    ) {
        return result("READY", message, configured, endpoint, provider, details);
    }

    private static ParserHealthResult degraded(
            String message,
            boolean configured,
            String endpoint,
            String provider,
            Map<String, Object> details
    ) {
        return result("DEGRADED", message, configured, endpoint, provider, details);
    }

    private static ParserHealthResult unconfigured(
            String message,
            String endpoint,
            String provider,
            Map<String, Object> details
    ) {
        return result("UNCONFIGURED", message, false, endpoint, provider, details);
    }

    private static ParserHealthResult unknown(
            String message,
            boolean configured,
            String endpoint,
            String provider,
            Map<String, Object> details
    ) {
        return result("UNKNOWN", message, configured, endpoint, provider, details);
    }

    private static ParserHealthResult result(
            String status,
            String message,
            boolean configured,
            String endpoint,
            String provider,
            Map<String, Object> details
    ) {
        return new ParserHealthResult(
                status,
                message,
                configured,
                endpoint == null ? "" : endpoint,
                provider == null ? "" : provider,
                Instant.now(),
                Map.copyOf(details == null ? Map.of() : details)
        );
    }

    private static String joinUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (path == null || path.isBlank()) {
            return base;
        }
        String suffix = path.startsWith("/") ? path : "/" + path;
        return base + suffix;
    }

    private static String normalize(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private record CachedHealth(ParserHealthResult result, Instant cachedAt) {
    }
}
