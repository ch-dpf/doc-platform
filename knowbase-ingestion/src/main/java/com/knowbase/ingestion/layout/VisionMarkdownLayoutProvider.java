package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.layout.VisionMarkdownLayoutProvider;
import com.knowbase.model.vision.VisionDocumentModelClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * VLM providers (Ollama / vLLM / PaddleOCR-VL markdown path) via {@link VisionDocumentModelClient}.
 */
public final class VisionMarkdownLayoutProvider implements LayoutAnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(VisionMarkdownLayoutProvider.class);

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
        VisionPageImageSupport.PreparedImage prepared = VisionPageImageSupport.prepareForVlm(
                request.imageBytes(),
                request.mimeType()
        );
        String markdown = client.recognizePage(
                prepared.bytes(),
                prepared.mimeType(),
                request.pageNumber(),
                request.effectiveOptions()
        );
        if (markdown == null || markdown.isBlank()) {
            log.warn(
                    "VLM 页面识别为空: provider={}, model={}, page={}, uri={}",
                    PROVIDER_CODE,
                    client.modelName(),
                    request.pageNumber(),
                    request.sourceUri()
            );
        }
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
