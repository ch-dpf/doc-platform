package com.knowbase.ingestion.layout;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.knowbase.ingestion.StructuralBlock;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Official PaddleOCR-VL HTTP layout pipeline ({@code POST /layout-parsing}).
 */
public final class PaddleOcrVlLayoutProvider implements LayoutAnalysisProvider {

    public static final String PROVIDER_CODE = "paddleocr-vl";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String layoutParsingPath;
    private final Duration timeout;
    private final String pipelineName;
    private final boolean prettifyMarkdown;
    private final boolean returnMarkdownImages;
    private final Boolean visualize;

    public PaddleOcrVlLayoutProvider(
            String baseUrl,
            String layoutParsingPath,
            Duration timeout,
            String pipelineName,
            boolean prettifyMarkdown,
            boolean returnMarkdownImages,
            Boolean visualize
    ) {
        this(
                HttpClient.newBuilder().connectTimeout(timeout).build(),
                new ObjectMapper(),
                baseUrl,
                layoutParsingPath,
                timeout,
                pipelineName,
                prettifyMarkdown,
                returnMarkdownImages,
                visualize
        );
    }

    PaddleOcrVlLayoutProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String baseUrl,
            String layoutParsingPath,
            Duration timeout,
            String pipelineName,
            boolean prettifyMarkdown,
            boolean returnMarkdownImages,
            Boolean visualize
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.layoutParsingPath = normalizePath(layoutParsingPath);
        this.timeout = timeout;
        this.pipelineName = pipelineName == null || pipelineName.isBlank() ? "PaddleOCR-VL" : pipelineName.trim();
        this.prettifyMarkdown = prettifyMarkdown;
        this.returnMarkdownImages = returnMarkdownImages;
        this.visualize = visualize;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean available() {
        return baseUrl != null && !baseUrl.isBlank();
    }

    @Override
    public boolean supports(LayoutPageRequest request) {
        return available() && request.imageBytes() != null && request.imageBytes().length > 0;
    }

    @Override
    public LayoutPageResult analyze(LayoutPageRequest request) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("file", Base64.getEncoder().encodeToString(request.imageBytes()));
            payload.put("fileType", 1);
            payload.put("prettifyMarkdown", prettifyMarkdown);
            payload.put("returnMarkdownImages", returnMarkdownImages);
            payload.put("restructurePages", false);
            if (visualize != null) {
                payload.put("visualize", visualize);
            }
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + layoutParsingPath))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("PaddleOCR-VL HTTP " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (root.path("errorCode").asInt(-1) != 0) {
                throw new IllegalStateException("PaddleOCR-VL: " + root.path("errorMsg").asText("error"));
            }
            JsonNode results = root.path("result").path("layoutParsingResults");
            if (!results.isArray() || results.isEmpty()) {
                return LayoutPageResult.empty(PROVIDER_CODE, request.pageNumber());
            }
            JsonNode pageResult = results.get(0);
            JsonNode prunedResult = pageResult.path("prunedResult");
            List<StructuralBlock> blocks = PaddleOcrVlPrunedResultMapper.fromPrunedResult(
                    prunedResult,
                    request,
                    PROVIDER_CODE,
                    pipelineName
            );
            if (blocks.isEmpty()) {
                String markdown = pageResult.path("markdown").path("text").asText("").trim();
                blocks = LayoutResultMapper.fromVisionMarkdown(
                        markdown,
                        request,
                        PROVIDER_CODE,
                        pipelineName
                );
                blocks = PaddleOcrVlPrunedResultMapper.mergeBboxesOntoMarkdownBlocks(
                        blocks,
                        prunedResult,
                        request.pageHeight()
                );
            }
            List<LayoutTableRegion> tableRegions = extractTableRegions(pageResult, blocks, request.pageHeight());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("visionLanguageModel", pipelineName);
            metadata.put("layoutAnalysisRoute", "paddleocr-vl-http");
            JsonNode dataInfo = root.path("result").path("dataInfo");
            if (!dataInfo.isMissingNode()) {
                metadata.put("paddleOcrVlDataInfo", objectMapper.convertValue(dataInfo, Map.class));
            }
            return new LayoutPageResult(
                    PROVIDER_CODE,
                    pipelineName,
                    request.pageNumber(),
                    blocks,
                    tableRegions,
                    null,
                    null,
                    Map.copyOf(metadata)
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("PaddleOCR-VL 调用被中断", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("PaddleOCR-VL 调用失败", exception);
        }
    }

    private List<LayoutTableRegion> extractTableRegions(JsonNode pageResult, List<StructuralBlock> blocks, double pageHeight) {
        List<LayoutTableRegion> regions = new ArrayList<>();
        JsonNode pruned = pageResult.path("prunedResult");
        if (pruned.isObject() && pruned.has("parsing_res_list")) {
            int index = 0;
            for (JsonNode item : pruned.path("parsing_res_list")) {
                String label = item.path("block_label").asText("");
                if (!label.toLowerCase().contains("table")) {
                    continue;
                }
                List<Double> bbox = LayoutBboxSupport.toPdfPoints(
                        LayoutBboxSupport.readBbox(item.path("block_bbox")),
                        pageHeight
                );
                regions.add(new LayoutTableRegion(index, "paddleocr-vl-table-" + index, bbox, "paddle-layout"));
                index++;
            }
        }
        if (regions.isEmpty()) {
            int fallback = 0;
            for (StructuralBlock block : blocks) {
                if (!"table_row".equals(block.blockType())) {
                    continue;
                }
                Object regionId = block.metadata().get("tableRegionId");
                int id = regionId instanceof Number number ? number.intValue() : fallback++;
                Object bbox = block.metadata().get("bbox");
                @SuppressWarnings("unchecked")
                List<Double> bboxList = bbox instanceof List<?> list ? (List<Double>) list : null;
                regions.add(new LayoutTableRegion(id, "paddleocr-vl-table-" + id, bboxList, "markdown-table"));
            }
        }
        return List.copyOf(regions);
    }

    private static String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/layout-parsing";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
