package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.pdf.PdfPageImageRenderer;
import com.knowbase.storage.ObjectStorage;
import com.knowbase.storage.StoredObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stores optional PDF page PNG artifacts for citation preview.
 */
public final class EvidenceArtifactGenerator {

    private final ObjectStorage objectStorage;
    private final String bucket;
    private final boolean enabled;
    private final int maxPages;

    public EvidenceArtifactGenerator(ObjectStorage objectStorage, String bucket, boolean enabled, int maxPages) {
        this.objectStorage = objectStorage;
        this.bucket = bucket == null || bucket.isBlank() ? "knowbase-evidence" : bucket.trim();
        this.enabled = enabled;
        this.maxPages = Math.max(0, maxPages);
    }

    public static EvidenceArtifactGenerator disabled() {
        return new EvidenceArtifactGenerator(null, "knowbase-evidence", false, 0);
    }

    public boolean enabled() {
        return enabled && objectStorage != null;
    }

    public Map<String, Object> generateForPdf(byte[] pdfBytes, String sourceUri) {
        if (!enabled() || pdfBytes == null || pdfBytes.length == 0 || sourceUri == null || sourceUri.isBlank()) {
            return Map.of();
        }
        List<PdfPageImageRenderer.PageImage> pages = PdfPageImageRenderer.render(pdfBytes, maxPages > 0 ? maxPages : 0);
        if (pages.isEmpty()) {
            return Map.of();
        }
        Map<String, String> pageArtifacts = new HashMap<>();
        String prefix = objectPrefix(sourceUri);
        for (PdfPageImageRenderer.PageImage page : pages) {
            String objectKey = prefix + "/page-" + page.pageNumber() + ".png";
            StoredObject stored = objectStorage.put(
                    bucket,
                    objectKey,
                    new ByteArrayInputStream(page.pngBytes()),
                    "image/png"
            );
            pageArtifacts.put(String.valueOf(page.pageNumber()), stored.uri());
        }
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("evidenceArtifactsGenerated", true);
        metadata.put("evidenceArtifactBucket", bucket);
        metadata.put("pageArtifacts", Map.copyOf(pageArtifacts));
        return Map.copyOf(metadata);
    }

    public static String resolvePageArtifactUri(Map<String, Object> documentMetadata, int pageNumber) {
        if (documentMetadata == null) {
            return null;
        }
        Object raw = documentMetadata.get("pageArtifacts");
        if (!(raw instanceof Map<?, ?> artifacts)) {
            return null;
        }
        Object uri = artifacts.get(String.valueOf(pageNumber));
        return uri == null ? null : String.valueOf(uri);
    }

    private static String objectPrefix(String sourceUri) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sourceUri.getBytes(StandardCharsets.UTF_8));
            return "evidence/" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException exception) {
            return "evidence/" + Integer.toHexString(sourceUri.hashCode()).toLowerCase(Locale.ROOT);
        }
    }
}
