package com.knowbase.ingestion.layout;

import com.knowbase.ingestion.DocumentSource;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.ingestion.StructureParsingSupport;
import com.knowbase.ingestion.pdf.PdfPageImageRenderer;
import com.knowbase.ingestion.parse.IngestionParseOptionsSupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Unified raster-page layout analysis with provider selection and fallback.
 */
public final class LayoutAnalysisService {

    private final List<LayoutAnalysisProvider> providers;
    private final boolean fallbackEnabled;

    public LayoutAnalysisService(List<LayoutAnalysisProvider> providers, boolean fallbackEnabled) {
        this.providers = List.copyOf(providers);
        this.fallbackEnabled = fallbackEnabled;
    }

    public boolean hasAvailableProvider() {
        return providers.stream().anyMatch(LayoutAnalysisProvider::available);
    }

    public LayoutPageResult analyzePage(LayoutPageRequest request) {
        List<LayoutAnalysisProvider> chain = resolveProviderChain(request.effectiveOptions());
        RuntimeException lastFailure = null;
        for (LayoutAnalysisProvider provider : chain) {
            if (!provider.available() || !provider.supports(request)) {
                continue;
            }
            try {
                LayoutPageResult result = provider.analyze(request);
                if (result != null && !result.blocks().isEmpty()) {
                    return applyTableRegions(result);
                }
                if (result != null && lastFailure == null) {
                    return applyTableRegions(result);
                }
            } catch (RuntimeException exception) {
                lastFailure = exception;
                if (!fallbackEnabled) {
                    throw exception;
                }
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new IllegalStateException("无可用的 layout provider");
    }

    public List<StructuralBlock> analyzePdfPages(
            DocumentSource source,
            byte[] pdfBytes,
            int maxPages,
            Map<String, Object> options
    ) {
        List<PdfPageImageRenderer.PageImage> pages = PdfPageImageRenderer.render(pdfBytes, maxPages);
        if (pages.isEmpty()) {
            throw new IllegalStateException("PDF 无可用页面: " + source.sourceUri());
        }
        Map<String, Object> mergedOptions = options == null ? new HashMap<>() : new HashMap<>(options);
        mergedOptions.put(LayoutAnalysisOptions.PDF_BYTES, pdfBytes);
        List<StructuralBlock> blocks = new ArrayList<>();
        int globalOrdinal = 0;
        String detectedLanguage = null;
        Double rotation = null;
        for (PdfPageImageRenderer.PageImage page : pages) {
            LayoutPageRequest request = new LayoutPageRequest(
                    page.pngBytes(),
                    "image/png",
                    page.pageNumber(),
                    page.pageWidth(),
                    page.pageHeight(),
                    source.sourceUri(),
                    mergedOptions
            );
            LayoutPageResult pageResult = analyzePage(request);
            if (pageResult.detectedLanguage() != null) {
                detectedLanguage = pageResult.detectedLanguage();
            }
            if (pageResult.rotationDegrees() != null) {
                rotation = pageResult.rotationDegrees();
            }
            for (StructuralBlock block : pageResult.blocks()) {
                Map<String, Object> metadata = new HashMap<>(block.metadata());
                metadata.put("readingOrder", globalOrdinal);
                metadata.put("layoutProvider", pageResult.providerCode());
                metadata.put("layoutModel", pageResult.modelName());
                blocks.add(new StructuralBlock(
                        block.blockType(),
                        block.level(),
                        block.content(),
                        globalOrdinal++,
                        Map.copyOf(metadata)
                ));
            }
        }
        blocks = StructureParsingSupport.enrichHeadingPathsPublic(blocks);
        if (detectedLanguage != null) {
            mergedOptions.putIfAbsent("ocrLanguage", detectedLanguage);
        }
        if (rotation != null) {
            mergedOptions.putIfAbsent("pageRotation", rotation);
        }
        mergedOptions.put("layoutProvider", resolvePrimaryProvider(options));
        return blocks;
    }

    public Map<String, Object> buildDocumentMetadata(
            List<StructuralBlock> blocks,
            int pageCount,
            String primaryProvider,
            String modelName
    ) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("layoutParsing", true);
        metadata.put("layoutProvider", primaryProvider);
        metadata.put("layoutAnalysisApplied", true);
        if (modelName != null) {
            metadata.put("layoutModel", modelName);
        }
        metadata.put("blockCount", blocks.size());
        metadata.put("pageCount", pageCount);
        return Map.copyOf(metadata);
    }

    private LayoutPageResult applyTableRegions(LayoutPageResult result) {
        if (result.tableRegions().isEmpty()) {
            return result;
        }
        List<StructuralBlock> enrichedBlocks = new ArrayList<>(result.blocks().size());
        for (StructuralBlock block : result.blocks()) {
            if (!"table_row".equals(block.blockType())) {
                enrichedBlocks.add(block);
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            if (!metadata.containsKey("tableRegionId")) {
                Object regionId = result.tableRegions().getFirst().tableRegionId();
                metadata.put("tableRegionId", regionId);
                metadata.put("tableRegionLabel", result.tableRegions().getFirst().label());
                metadata.put("tableDetectionSource", result.tableRegions().getFirst().detectionSource());
            }
            enrichedBlocks.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    Map.copyOf(metadata)
            ));
        }
        return new LayoutPageResult(
                result.providerCode(),
                result.modelName(),
                result.pageNumber(),
                enrichedBlocks,
                result.tableRegions(),
                result.detectedLanguage(),
                result.rotationDegrees(),
                result.metadata()
        );
    }

    private List<LayoutAnalysisProvider> resolveProviderChain(Map<String, Object> options) {
        String requested = readProvider(options);
        List<LayoutAnalysisProvider> chain = new ArrayList<>();
        if (requested != null) {
            providers.stream()
                    .filter(provider -> provider.providerCode().equalsIgnoreCase(requested))
                    .findFirst()
                    .ifPresent(chain::add);
        }
        for (LayoutAnalysisProvider provider : providers) {
            if (chain.stream().noneMatch(existing -> existing.providerCode().equals(provider.providerCode()))) {
                chain.add(provider);
            }
        }
        return chain;
    }

    private String resolvePrimaryProvider(Map<String, Object> options) {
        String requested = readProvider(options);
        if (requested != null) {
            return requested;
        }
        return providers.stream()
                .filter(LayoutAnalysisProvider::available)
                .map(LayoutAnalysisProvider::providerCode)
                .findFirst()
                .orElse("unknown");
    }

    private static String readProvider(Map<String, Object> options) {
        IngestionParseOptionsSupport.IngestionParseOptions parsed = IngestionParseOptionsSupport.resolve(options);
        String layoutProvider = parsed.layoutProvider();
        if (layoutProvider != null && !layoutProvider.isBlank()) {
            return layoutProvider.trim().toLowerCase(Locale.ROOT);
        }
        return null;
    }
}
