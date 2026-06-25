package com.knowbase.ingestion.layout;

import com.knowbase.model.vision.VisionDocumentModelClient;

import java.util.List;
import java.util.Map;

/**
 * VLM providers (Ollama / vLLM / PaddleOCR-VL markdown path) via {@link VisionDocumentModelClient}.
 */
public final class VisionMarkdownLayoutProvider implements LayoutAnalysisProvider {

    public static final String PROVIDER_CODE = "vision-markdown";

    private final VisionDocumentModelClient client;

    public VisionMarkdownLayoutProvider(VisionDocumentModelClient client) {
        this.client = client;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean available() {
        return client != null && client.modelName() != null && !client.modelName().isBlank();
    }

    @Override
    public boolean supports(LayoutPageRequest request) {
        return available() && request.imageBytes() != null && request.imageBytes().length > 0;
    }

    @Override
    public LayoutPageResult analyze(LayoutPageRequest request) {
        String markdown = client.recognizePage(
                request.imageBytes(),
                request.mimeType(),
                request.pageNumber(),
                request.effectiveOptions()
        );
        List<com.knowbase.ingestion.StructuralBlock> blocks = LayoutResultMapper.fromVisionMarkdown(
                markdown,
                request,
                PROVIDER_CODE,
                client.modelName()
        );
        return new LayoutPageResult(
                PROVIDER_CODE,
                client.modelName(),
                request.pageNumber(),
                blocks,
                List.of(),
                null,
                null,
                Map.of("visionLanguageModel", client.modelName())
        );
    }
}
