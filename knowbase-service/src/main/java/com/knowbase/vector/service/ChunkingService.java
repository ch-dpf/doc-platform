package com.knowbase.vector.service;

import com.knowbase.vector.chunk.ChunkTextPreprocessor;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.chunk.FixedLengthChunker;
import com.knowbase.vector.chunk.HeadingLevelChunker;
import com.knowbase.vector.chunk.SemanticChunker;
import com.knowbase.vector.config.ChunkingProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChunkingService {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("(?:\\n\\s*){2,}");

    private final ChunkingProperties defaultProperties;
    private final SemanticChunker semanticChunker;

    public ChunkingService(ChunkingProperties defaultProperties, SemanticChunker semanticChunker) {
        this.defaultProperties = defaultProperties;
        this.semanticChunker = semanticChunker;
    }

    public List<String> chunk(String text) {
        return chunk(null, text, defaultProperties);
    }

    public List<String> chunk(String text, ChunkingProperties props) {
        return chunk(null, text, props);
    }

    public List<String> chunk(UUID libraryId, String text, ChunkingProperties props) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        ChunkingProperties effective = props != null ? props : defaultProperties;
        String normalized = effective.isNormalizeBeforeChunk()
                ? ChunkTextPreprocessor.prepare(text)
                : text.replace("\r\n", "\n").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        return switch (effective.getStrategy()) {
            case FIXED_CHAR -> FixedLengthChunker.chunk(normalized, effective);
            case HEADING_LEVEL -> chunkHeadingLevel(normalized, effective);
            case SEMANTIC -> semanticChunker.chunk(libraryId, normalized, effective);
            case PARAGRAPH_FIRST -> chunkParagraphFirst(normalized, effective);
        };
    }

    private List<String> chunkHeadingLevel(String text, ChunkingProperties props) {
        List<String> sections = HeadingLevelChunker.splitSections(text);
        if (sections.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (String section : sections) {
            if (section.length() <= props.getChunkSize()) {
                addIfValid(chunks, section, props);
            } else {
                chunks.addAll(chunkParagraphFirst(section, props));
            }
        }
        return chunks;
    }

    private List<String> chunkParagraphFirst(String text, ChunkingProperties props) {
        String[] rawParagraphs = PARAGRAPH_SPLIT.split(text);
        List<String> merged = mergeParagraphs(rawParagraphs, props);

        List<String> chunks = new ArrayList<>();
        for (String segment : merged) {
            if (segment.length() <= props.getChunkSize()) {
                addIfValid(chunks, segment, props);
            } else {
                chunks.addAll(FixedLengthChunker.chunk(segment, props));
            }
        }
        return chunks;
    }

    private List<String> mergeParagraphs(String[] rawParagraphs, ChunkingProperties props) {
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
            if (shouldMerge(buffer.toString(), paragraph, props)) {
                buffer.append("\n\n").append(paragraph);
            } else {
                merged.add(buffer.toString());
                buffer = new StringBuilder(paragraph);
            }
            while (buffer.length() > props.getMaxChunkSize()) {
                merged.add(takePrefixBySize(buffer, props.getMaxChunkSize()));
            }
        }
        if (!buffer.isEmpty()) {
            merged.add(buffer.toString());
        }
        return merged;
    }

    private boolean shouldMerge(String current, String next, ChunkingProperties props) {
        if (current.length() < props.getMinParagraphLength()) {
            return true;
        }
        if (current.length() < props.getMinChunkSize()
                && current.length() + 2 + next.length() <= props.getChunkSize()) {
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

    private void addIfValid(List<String> chunks, String piece, ChunkingProperties props) {
        if (piece.length() >= props.getMinChunkSize() || chunks.isEmpty()) {
            if (!piece.isEmpty()) {
                chunks.add(piece);
            }
        } else if (!chunks.isEmpty()) {
            int last = chunks.size() - 1;
            chunks.set(last, chunks.get(last) + "\n\n" + piece);
        }
    }
}
