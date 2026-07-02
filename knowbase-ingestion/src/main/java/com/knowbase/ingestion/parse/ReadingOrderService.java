package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.model.ollama.OllamaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reading order: dedicated HTTP endpoint → Ollama ML model → heuristic bbox fallback.
 */
public final class ReadingOrderService {

    private static final Logger log = LoggerFactory.getLogger(ReadingOrderService.class);

    private ReadingOrderService() {
    }

    public static List<StructuralBlock> apply(List<StructuralBlock> blocks, Map<String, Object> documentMetadata) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        IngestionParseOptionsSupport.IngestionParseOptions options =
                IngestionParseOptionsSupport.resolve(documentMetadata);

        List<StructuralBlock> ordered = tryMlOrdering(options, blocks);
        if (ordered == null) {
            ordered = applyHeuristic(blocks);
            log.debug("阅读顺序回退: source=heuristic-bbox, blocks={}", blocks.size());
        }
        return CrossPageReadingOrderAdjuster.adjust(ordered);
    }

    /**
     * HTTP endpoint first (when configured), then Ollama, unless provider restricts the chain.
     */
    private static List<StructuralBlock> tryMlOrdering(
            IngestionParseOptionsSupport.IngestionParseOptions options,
            List<StructuralBlock> blocks
    ) {
        String provider = normalizeProvider(options.readingOrderProvider());
        if ("heuristic".equals(provider)) {
            return null;
        }

        if (!"ollama".equals(provider)) {
            String endpoint = options.readingOrderEndpoint();
            if (endpoint != null && !endpoint.isBlank()) {
                List<StructuralBlock> remote = tryRemoteOrdering(endpoint, options, blocks);
                if (remote != null) {
                    log.info("阅读顺序完成: source=remote-http, blocks={}", blocks.size());
                    return remote;
                }
                log.debug("阅读顺序: remote-http 不可用，尝试 Ollama/heuristic");
            }
        }

        if (!"http".equals(provider)) {
            List<StructuralBlock> ollamaOrdered = tryOllamaOrdering(options, blocks);
            if (ollamaOrdered != null) {
                log.info("阅读顺序完成: source=ollama-reading-order, blocks={}", blocks.size());
                return ollamaOrdered;
            }
            if ("ollama".equals(provider)) {
                log.debug("阅读顺序: ollama 不可用，回退 heuristic-bbox");
            }
        }

        return null;
    }

    private static String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "auto";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private static List<StructuralBlock> tryOllamaOrdering(
            IngestionParseOptionsSupport.IngestionParseOptions options,
            List<StructuralBlock> blocks
    ) {
        String model = options.readingOrderOllamaModel();
        String baseUrl = options.readingOrderOllamaBaseUrl();
        if (model == null || model.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        Duration timeout = options.readingOrderTimeout() == null
                ? Duration.ofSeconds(30)
                : options.readingOrderTimeout();
        OllamaReadingOrderClient client = new OllamaReadingOrderClient(
                new OllamaClient(baseUrl, timeout),
                timeout
        );
        return client.order(model, blocks);
    }

    private static List<StructuralBlock> tryRemoteOrdering(
            String endpoint,
            IngestionParseOptionsSupport.IngestionParseOptions options,
            List<StructuralBlock> blocks
    ) {
        Duration timeout = options.readingOrderTimeout() == null
                ? Duration.ofSeconds(30)
                : options.readingOrderTimeout();
        return new ReadingOrderHttpClient(timeout).order(endpoint, blocks);
    }

    static List<StructuralBlock> applyHeuristic(List<StructuralBlock> blocks) {
        List<IndexedBlock> indexed = new ArrayList<>(blocks.size());
        for (int index = 0; index < blocks.size(); index++) {
            indexed.add(new IndexedBlock(index, blocks.get(index)));
        }
        indexed.sort(ReadingOrderService::compareBlocks);
        List<StructuralBlock> ordered = new ArrayList<>(blocks.size());
        for (int order = 0; order < indexed.size(); order++) {
            StructuralBlock block = indexed.get(order).block();
            Map<String, Object> metadata = new HashMap<>(block.metadata());
            metadata.put("readingOrder", order);
            metadata.put("readingOrderSource", "heuristic-bbox");
            ordered.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    order,
                    Map.copyOf(metadata)
            ));
        }
        return ordered;
    }

    static int compareBlocks(IndexedBlock left, IndexedBlock right) {
        int pageCompare = Integer.compare(pageNumber(left.block()), pageNumber(right.block()));
        if (pageCompare != 0) {
            return pageCompare;
        }
        int roleCompare = Integer.compare(layoutRoleRank(left.block()), layoutRoleRank(right.block()));
        if (roleCompare != 0) {
            return roleCompare;
        }
        int leftColumns = columnCount(left.block());
        int rightColumns = columnCount(right.block());
        if (leftColumns > 1 || rightColumns > 1) {
            int columnCompare = Integer.compare(columnIndex(left.block()), columnIndex(right.block()));
            if (columnCompare != 0) {
                return columnCompare;
            }
        }
        double topLeft = bboxTop(left.block());
        double topRight = bboxTop(right.block());
        int topCompare = Double.compare(topLeft, topRight);
        if (topCompare != 0) {
            return topCompare;
        }
        double leftX = bboxLeft(left.block());
        double rightX = bboxLeft(right.block());
        int xCompare = Double.compare(leftX, rightX);
        if (xCompare != 0) {
            return xCompare;
        }
        return Integer.compare(left.originalIndex(), right.originalIndex());
    }

    private static int layoutRoleRank(StructuralBlock block) {
        Object role = block.metadata().get("layoutRole");
        if (role == null) {
            role = block.metadata().get("boundaryType");
        }
        if ("header".equals(role) || "title".equals(role)) {
            return 0;
        }
        if ("footer".equals(role)) {
            return 2;
        }
        return 1;
    }

    private static int columnIndex(StructuralBlock block) {
        Object raw = block.metadata().get("columnIndex");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static int columnCount(StructuralBlock block) {
        Object raw = block.metadata().get("columnCount");
        if (raw instanceof Number number) {
            return number.intValue();
        }
        Object multiColumn = block.metadata().get("multiColumn");
        if (Boolean.TRUE.equals(multiColumn)) {
            return 2;
        }
        return 1;
    }

    private static int pageNumber(StructuralBlock block) {
        Object page = block.metadata().get("pageNumber");
        if (page instanceof Number number) {
            return number.intValue();
        }
        Object slide = block.metadata().get("slideNumber");
        if (slide instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private static double bboxTop(StructuralBlock block) {
        return bboxComponent(block, 1, block.ordinal());
    }

    private static double bboxLeft(StructuralBlock block) {
        return bboxComponent(block, 0, 0d);
    }

    private static double bboxComponent(StructuralBlock block, int index, double fallback) {
        Object bbox = block.metadata().get("bbox");
        if (bbox instanceof List<?> list && list.size() > index && list.get(index) instanceof Number number) {
            return number.doubleValue();
        }
        return fallback;
    }

    record IndexedBlock(int originalIndex, StructuralBlock block) {
    }
}
