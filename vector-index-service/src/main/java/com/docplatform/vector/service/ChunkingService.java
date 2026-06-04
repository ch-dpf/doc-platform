package com.docplatform.vector.service;

import com.docplatform.vector.chunk.ChunkTextPreprocessor;
import com.docplatform.vector.chunk.ChunkingStrategy;
import com.docplatform.vector.config.ChunkingProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("(?:\\n\\s*){2,}");

    private final ChunkingProperties properties;

    public ChunkingService(ChunkingProperties properties) {
        this.properties = properties;
    }

    public List<String> chunk(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = properties.isNormalizeBeforeChunk()
                ? ChunkTextPreprocessor.prepare(text)
                : text.replace("\r\n", "\n").trim();

        if (normalized.isEmpty()) {
            return List.of();
        }

        return properties.getStrategy() == ChunkingStrategy.PARAGRAPH_FIRST
                ? chunkParagraphFirst(normalized)
                : chunkFixedChar(normalized);
    }

    private List<String> chunkParagraphFirst(String text) {
        String[] rawParagraphs = PARAGRAPH_SPLIT.split(text);
        List<String> merged = mergeParagraphs(rawParagraphs);

        List<String> chunks = new ArrayList<>();
        for (String segment : merged) {
            if (segment.length() <= properties.getChunkSize()) {
                addIfValid(chunks, segment);
            } else {
                chunks.addAll(chunkFixedChar(segment));
            }
        }
        return chunks;
    }

    private List<String> mergeParagraphs(String[] rawParagraphs) {
        List<String> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String raw : rawParagraphs) {
            String paragraph = raw.strip();
            if (paragraph.isEmpty()) {
                continue;
            }
            if (buffer.isEmpty()) {
                buffer.append(paragraph);
                continue;
            }
            if (shouldMerge(buffer.toString(), paragraph)) {
                buffer.append("\n\n").append(paragraph);
            } else {
                merged.add(buffer.toString());
                buffer = new StringBuilder(paragraph);
            }
            while (buffer.length() > properties.getMaxChunkSize()) {
                merged.add(takePrefixBySize(buffer, properties.getMaxChunkSize()));
            }
        }
        if (!buffer.isEmpty()) {
            merged.add(buffer.toString());
        }
        return merged;
    }

    private boolean shouldMerge(String current, String next) {
        if (current.length() < properties.getMinParagraphLength()) {
            return true;
        }
        if (current.length() < properties.getMinChunkSize()
                && current.length() + 2 + next.length() <= properties.getChunkSize()) {
            return true;
        }
        return false;
    }

    private static String takePrefixBySize(StringBuilder buffer, int maxSize) {
        String all = buffer.toString();
        String prefix = all.substring(0, Math.min(maxSize, all.length())).strip();
        String remainder = all.substring(Math.min(maxSize, all.length())).strip();
        buffer.setLength(0);
        buffer.append(remainder);
        return prefix;
    }

    private List<String> chunkFixedChar(String text) {
        int size = properties.getChunkSize();
        int overlap = properties.getOverlap();
        int step = Math.max(1, size - overlap);

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + size, text.length());
            String piece = sliceAtBoundary(text, start, end).strip();
            addIfValid(chunks, piece);
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }

    /**
     * 尽量在句读符或换行处截断，避免硬切句子中间。
     */
    private String sliceAtBoundary(String text, int start, int end) {
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
                if (breakAt > start + properties.getMinChunkSize() / 2) {
                    return text.substring(start, breakAt);
                }
            }
        }
        return text.substring(start, end);
    }

    private void addIfValid(List<String> chunks, String piece) {
        if (piece.length() >= properties.getMinChunkSize() || chunks.isEmpty()) {
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
        } else if (!chunks.isEmpty()) {
            int last = chunks.size() - 1;
            chunks.set(last, chunks.get(last) + "\n\n" + piece);
        }
    }
}
