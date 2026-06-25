package com.knowbase.ingestion.parse;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EvidenceArtifactUriBuilderTest {

    @Test
    void buildsPageAndBboxArtifactUris() {
        String uri = EvidenceArtifactUriBuilder.pageUri("file://report.pdf", 2);
        assertTrue(uri.startsWith(EvidenceArtifactUriBuilder.SCHEME + "://page?"));
        String bboxUri = EvidenceArtifactUriBuilder.bboxUri("file://report.pdf", 2, List.of(72, 100, 200, 24));
        assertTrue(bboxUri.contains("bbox=72"));
        Map<String, Object> artifact = EvidenceArtifactUriBuilder.buildFromBlockMetadata(
                "file://report.pdf",
                Map.of("pageNumber", 2, "bbox", List.of(72, 100, 200, 24))
        );
        assertTrue(artifact.get("assetUri").toString().contains("bbox=72"));
    }
}
