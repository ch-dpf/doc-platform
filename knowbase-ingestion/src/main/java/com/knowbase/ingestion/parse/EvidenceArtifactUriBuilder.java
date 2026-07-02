package com.knowbase.ingestion.parse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds logical artifact URIs for multimodal evidence (page / bbox / table region).
 */
public final class EvidenceArtifactUriBuilder {

    public static final String SCHEME = "knowbase-artifact";

    private EvidenceArtifactUriBuilder() {
    }

    public static String pageUri(String sourceUri, int pageNumber) {
        return SCHEME + "://page?source=" + encode(sourceUri) + "&page=" + pageNumber;
    }

    public static String bboxUri(String sourceUri, int pageNumber, List<?> bbox) {
        return pageUri(sourceUri, pageNumber) + "&bbox=" + encode(bbox.stream().map(String::valueOf).collect(Collectors.joining(",")));
    }

    public static String tableRegionUri(String sourceUri, int tableRegionId, Integer pageNumber) {
        StringBuilder builder = new StringBuilder(SCHEME)
                .append("://table-region?source=")
                .append(encode(sourceUri))
                .append("&tableRegionId=")
                .append(tableRegionId);
        if (pageNumber != null) {
            builder.append("&page=").append(pageNumber);
        }
        return builder.toString();
    }

    public static String sheetRowUri(String sourceUri, String sheetName, int rowIndex) {
        return SCHEME + "://sheet-row?source=" + encode(sourceUri)
                + "&sheet=" + encode(sheetName)
                + "&row=" + rowIndex;
    }

    public static Map<String, Object> buildFromBlockMetadata(String sourceUri, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty() || sourceUri == null || sourceUri.isBlank()) {
            return Map.of();
        }
        Object pageNumber = metadata.get("pageNumber");
        Object bbox = metadata.get("bbox");
        Object tableRegionBbox = metadata.get("tableRegionBbox");
        Object tableRegionId = metadata.get("tableRegionId");
        Object sheetName = metadata.get("sheetName");
        Object rowIndex = metadata.get("rowIndex");

        if (pageNumber instanceof Number page && tableRegionBbox instanceof List<?> regionBbox && !regionBbox.isEmpty()) {
            return Map.of(
                    "assetUri", bboxUri(sourceUri, page.intValue(), regionBbox),
                    "assetKind", "table_region_bbox"
            );
        }
        if (pageNumber instanceof Number page && bbox instanceof List<?> bboxList && bboxList.size() >= 4) {
            return Map.of(
                    "assetUri", bboxUri(sourceUri, page.intValue(), bboxList),
                    "assetKind", Boolean.TRUE.equals(metadata.get("ocrApplied")) ? "ocr_bbox" : "pdf_bbox"
            );
        }
        if (pageNumber instanceof Number page) {
            return Map.of(
                    "assetUri", pageUri(sourceUri, page.intValue()),
                    "assetKind", "pdf_page"
            );
        }
        if (sheetName != null && rowIndex instanceof Number row) {
            return Map.of(
                    "assetUri", sheetRowUri(sourceUri, String.valueOf(sheetName), row.intValue()),
                    "assetKind", "sheet_row"
            );
        }
        if (tableRegionId instanceof Number region) {
            Integer page = pageNumber instanceof Number number ? number.intValue() : null;
            return Map.of(
                    "assetUri", tableRegionUri(sourceUri, region.intValue(), page),
                    "assetKind", "table_region"
            );
        }
        return Map.of();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
