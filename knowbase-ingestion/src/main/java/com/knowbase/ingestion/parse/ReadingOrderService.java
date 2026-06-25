package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Heuristic reading order: page → top-to-bottom bbox → ordinal fallback.
 * Optional HTTP provider can be wired later via {@code readingOrderEndpoint}.
 */
public final class ReadingOrderService {

    private ReadingOrderService() {
    }

    public static List<StructuralBlock> apply(List<StructuralBlock> blocks, Map<String, Object> documentMetadata) {
        if (blocks == null || blocks.isEmpty()) {
            return blocks;
        }
        if (blocks.stream().allMatch(ReadingOrderService::hasReadingOrder)) {
            return blocks;
        }
        String endpoint = readEndpoint(documentMetadata);
        if (endpoint != null) {
            List<StructuralBlock> remote = tryRemoteOrdering(endpoint, blocks);
            if (remote != null) {
                return remote;
            }
        }
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
        return List.copyOf(ordered);
    }

    private static List<StructuralBlock> tryRemoteOrdering(String endpoint, List<StructuralBlock> blocks) {
        // Placeholder for dedicated reading-order model HTTP adapter.
        return null;
    }

    private static int compareBlocks(IndexedBlock left, IndexedBlock right) {
        int pageCompare = Integer.compare(pageNumber(left.block()), pageNumber(right.block()));
        if (pageCompare != 0) {
            return pageCompare;
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

    private static String readEndpoint(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object endpoint = metadata.get("readingOrderEndpoint");
        if (endpoint == null) {
            endpoint = metadata.get("readingOrderModelEndpoint");
        }
        if (endpoint == null || String.valueOf(endpoint).isBlank()) {
            return null;
        }
        return String.valueOf(endpoint).trim();
    }

    private record IndexedBlock(int originalIndex, StructuralBlock block) {
    }
}
