package com.knowbase.vector.chunk;

import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.service.LibraryEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SemanticChunker {

    private static final Logger log = LoggerFactory.getLogger(SemanticChunker.class);

    private final LibraryEmbeddingService embeddingService;

    public SemanticChunker(LibraryEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public List<String> chunk(UUID libraryId, String text, ChunkingProperties props) {
        List<String> topLevelSections = HeadingLevelChunker.splitTopLevelSections(text);
        if (topLevelSections.size() <= 1) {
            return chunkSection(libraryId, text, props);
        }
        List<String> result = new ArrayList<>();
        for (String section : topLevelSections) {
            result.addAll(chunkSection(libraryId, section, props));
        }
        return result;
    }

    private List<String> chunkSection(UUID libraryId, String text, ChunkingProperties props) {
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return List.of();
        }
        // 整节可放入一块时无需再按句相似度切分，避免短节被误碎成多块
        if (normalized.length() <= props.getChunkSize()) {
            return List.of(normalized);
        }

        List<String> sentences = SentenceSplitter.split(normalized);
        if (sentences.isEmpty()) {
            return List.of();
        }
        if (sentences.size() == 1) {
            return List.of(sentences.get(0));
        }

        List<float[]> embeddings = embedSentences(libraryId, sentences);
        if (embeddings.size() != sentences.size()) {
            log.warn("Semantic chunking fallback to single chunk due to embedding mismatch");
            return List.of(String.join("", sentences));
        }

        List<String> groups = groupBySimilarity(sentences, embeddings, props);
        return enforceSizeLimits(groups, props);
    }

    private List<float[]> embedSentences(UUID libraryId, List<String> sentences) {
        if (libraryId != null) {
            return embeddingService.embedBatch(libraryId, sentences);
        }
        return embeddingService.embedBatchWithDefaultModel(sentences);
    }

    static List<String> groupBySimilarity(
            List<String> sentences, List<float[]> embeddings, ChunkingProperties props) {
        double threshold = props.getSemanticSimilarityThreshold();
        List<String> groups = new ArrayList<>();
        StringBuilder current = new StringBuilder(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            double similarity = VectorSimilarity.cosineSimilarity(embeddings.get(i - 1), embeddings.get(i));
            String next = sentences.get(i);
            boolean topicShift = similarity < threshold;
            boolean sizeExceeded = current.length() + next.length() > props.getChunkSize();
            if (topicShift || sizeExceeded) {
                groups.add(current.toString().strip());
                current = new StringBuilder(next);
            } else {
                current.append(next);
            }
        }
        if (!current.isEmpty()) {
            groups.add(current.toString().strip());
        }
        return groups;
    }

    private List<String> enforceSizeLimits(List<String> groups, ChunkingProperties props) {
        List<String> merged = mergeUndersizedGroups(groups, props);
        List<String> result = new ArrayList<>();
        for (String group : merged) {
            if (group.length() <= props.getMaxChunkSize()) {
                result.add(group);
            } else {
                result.addAll(FixedLengthChunker.chunk(group, props));
            }
        }
        return result;
    }

    /** 将低于 minChunkSize 的相邻组合并，避免节内出现 20~30 字的碎片块。 */
    static List<String> mergeUndersizedGroups(List<String> groups, ChunkingProperties props) {
        if (groups.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        StringBuilder pending = new StringBuilder(groups.get(0));
        for (int i = 1; i < groups.size(); i++) {
            String group = groups.get(i);
            if (pending.length() < props.getMinChunkSize()) {
                pending.append(group);
                continue;
            }
            result.add(pending.toString().strip());
            pending = new StringBuilder(group);
        }
        if (pending.length() > 0) {
            if (pending.length() < props.getMinChunkSize() && !result.isEmpty()) {
                int last = result.size() - 1;
                result.set(last, result.get(last) + pending);
            } else {
                result.add(pending.toString().strip());
            }
        }
        return result;
    }
}
