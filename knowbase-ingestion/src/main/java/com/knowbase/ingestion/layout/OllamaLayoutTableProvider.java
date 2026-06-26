package com.knowbase.ingestion.layout;

import com.knowbase.model.ollama.OllamaClient;
import com.knowbase.model.ollama.OllamaMessage;

import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ollama vision layout provider: ML table detection (ruled/borderless/nested) with heuristic fallback via
 * {@link LayoutAnalysisService} provider chain.
 */
public final class OllamaLayoutTableProvider implements LayoutAnalysisProvider {

    public static final String PROVIDER_CODE = "ollama-layout";
    public static final String MODEL_NAME_PREFIX = "ollama-vision-layout";

    private final OllamaClient ollamaClient;
    private final String modelName;
    private final Duration timeout;

    public OllamaLayoutTableProvider(OllamaClient ollamaClient, String modelName, Duration timeout) {
        this.ollamaClient = ollamaClient;
        this.modelName = modelName == null ? "" : modelName.trim();
        this.timeout = timeout == null ? Duration.ofSeconds(120) : timeout;
    }

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public boolean available() {
        return ollamaClient != null && !modelName.isBlank();
    }

    @Override
    public boolean supports(LayoutPageRequest request) {
        return available()
                && request.imageBytes() != null
                && request.imageBytes().length > 0;
    }

    @Override
    public LayoutPageResult analyze(LayoutPageRequest request) {
        if (!supports(request)) {
            throw new IllegalStateException("Ollama layout provider unavailable for page " + request.pageNumber());
        }
        String base64 = Base64.getEncoder().encodeToString(request.imageBytes());
        String userPrompt = OllamaLayoutPrompts.layoutTableUserPrompt(
                request.pageNumber(),
                request.pageWidth(),
                request.pageHeight()
        );
        String answer = ollamaClient.chat(
                modelName,
                List.of(
                        OllamaMessage.system(OllamaLayoutPrompts.LAYOUT_TABLE_SYSTEM),
                        OllamaMessage.userWithImages(userPrompt, List.of(base64))
                ),
                Map.of("temperature", 0.1d)
        ).answer();
        OllamaLayoutResponseMapper.MappedPage mapped = OllamaLayoutResponseMapper.fromJson(
                answer,
                request,
                PROVIDER_CODE,
                modelName
        );
        if (mapped.isEmpty()) {
            throw new IllegalStateException(
                    "Ollama layout returned no blocks for page " + request.pageNumber() + " uri=" + request.sourceUri()
            );
        }
        List<com.knowbase.ingestion.StructuralBlock> stamped = LayoutResultMapper.stampProvider(
                mapped.blocks(),
                PROVIDER_CODE,
                modelName,
                "ollama-vision-layout"
        );
        Map<String, Object> metadata = new HashMap<>(request.effectiveOptions());
        metadata.put("layoutProvider", PROVIDER_CODE);
        metadata.put("layoutModel", modelName);
        metadata.put("layoutAnalysisRoute", "ollama-vision-layout");
        metadata.put("tableDetectionSource", "ollama-layout");
        return new LayoutPageResult(
                PROVIDER_CODE,
                modelName,
                request.pageNumber(),
                stamped,
                mapped.tableRegions(),
                null,
                null,
                Map.copyOf(metadata)
        );
    }
}
