package com.knowbase.vector.chunk;

import com.knowbase.vector.config.ChunkingProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定长度滑动窗口分块（供多种策略复用）。
 */
public final class FixedLengthChunker {

    private FixedLengthChunker() {}

    public static List<String> chunk(String text, ChunkingProperties props) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        int size = props.getChunkSize();
        int overlap = props.getOverlap();
        int step = Math.max(1, size - overlap);

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + size, text.length());
            String piece = sliceAtBoundary(text, start, end, props).strip();
            addIfValid(chunks, piece, props);
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }

    private static String sliceAtBoundary(String text, int start, int end, ChunkingProperties props) {
        if (end >= text.length()) {
            return text.substring(start);
        }
        int windowStart = Math.max(start, end - 80);
        String window = text.substring(windowStart, end);
        int[] breakOffsets = {
                window.lastIndexOf("\n\n"),
                window.lastIndexOf('\n'),
                window.lastIndexOf('。'),
                window.lastIndexOf('！'),
                window.lastIndexOf('？'),
                window.lastIndexOf('.'),
                window.lastIndexOf('!'),
                window.lastIndexOf('?')
        };
        for (int offset : breakOffsets) {
            if (offset > 20) {
                int breakAt = windowStart + offset + 1;
                if (breakAt > start + props.getMinChunkSize() / 2) {
                    return text.substring(start, breakAt);
                }
            }
        }
        return text.substring(start, end);
    }

    private static void addIfValid(List<String> chunks, String piece, ChunkingProperties props) {
        if (piece.length() >= props.getMinChunkSize() || chunks.isEmpty()) {
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
        } else if (!chunks.isEmpty()) {
            int last = chunks.size() - 1;
            chunks.set(last, chunks.get(last) + piece);
        }
    }
}
