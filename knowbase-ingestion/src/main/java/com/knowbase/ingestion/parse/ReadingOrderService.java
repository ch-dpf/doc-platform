package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.model.ollama.OllamaClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reading order: Ollama ML model → dedicated HTTP endpoint → heuristic bbox fallback.
 */
public final class ReadingOrderService {

    private static final ReadingOrderHttpClient REMOTE_CLIENT = new ReadingOrderHttpClient();

    private ReadingOrderService() {
    }

    public static List<StructuralBlock> apply(List<StructuralBlock> blocks, Map<String, Object> documentMetadata) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        if (blocks.stream().allMatch(ReadingOrderService::hasReadingOrder)) {
            return CrossPageReadingOrderAdjuster.adjust(blocks);
        }
        IngestionParseOptionsSupport.IngestionParseOptions options =
                IngestionParseOptionsSupport.resolve(documentMetadata);
        String provider = options.readingOrderProvider() == null
                ? "heuristic"
                : options.readingOrderProvider().toLowerCase(Locale.ROOT);

        if ("ollama".equals(provider)) {
            List<StructuralBlock> ollamaOrdered = tryOllamaOrdering(options, blocks);
            if (ollamaOrdered != null) {
                return CrossPageReadingOrderAdjuster.adjust(ollamaOrdered);
            }
        } else if ("http".equals(provider)) {
            String endpoint = options.readingOrderEndpoint();
            if (endpoint != null && !endpoint.isBlank()) {
                List<StructuralBlock> remote = tryRemoteOrdering(endpoint, blocks);
                if (remote != null) {
                    return CrossPageReadingOrderAdjuster.adjust(remote);
                }
            }
        }

        return CrossPageReadingOrderAdjuster.adjust(applyHeuristic(blocks));
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

    private static List<StructuralBlock> tryRemoteOrdering(String endpoint, List<StructuralBlock> blocks) {
        return REMOTE_CLIENT.order(endpoint, blocks);
    }

    private static List<StructuralBlock> applyHeuristic(List<StructuralBlock> blocks) {
        List<IndexedBlock> indexed = new ArrayList<>(blocks.size());
        for (int index = 0; index < blocks.size(); index++) {
            indexed.add(new IndexedBlock(index, blocks.get(index)));
        }
        indexed.sort(ReadingOrderService::compareBlocks);
        List<StructuralBlock> ordered = new ArrayList<>(blocks.size());
        for (int order = 0; order < indexed.size(); order++) {
            StructuralBlock block = indexed.get(order).block();
            if (hasReadingOrder(block)) {
                ordered.add(block);
                continue;
            }
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

    private static int compareBlocks(IndexedBlock left, IndexedBlock right) {
        int pageCompare = Integer.compare(pageNumber(left.block()), pageNumber(right.block()));
        if (pageCompare != 0) {
            return pageCompare;
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

    private static boolean hasReadingOrder(StructuralBlock block) {
        return block.metadata() != null && block.metadata().get("readingOrder") instanceof Number;
    }

    private record IndexedBlock(int originalIndex, StructuralBlock block) {
    }
}
