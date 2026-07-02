package com.knowbase.application.service;

import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.retrieval.RetrievalCandidate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Hit@K 判定（对齐主流 RAG 评测惯例，如 RAGAS Context Recall / 检索 Hit@K）：
 * <ul>
 *   <li>在融合+重排后的 Top-K 检索块中，任意一块命中即视为该样本 Hit</li>
 *   <li>命中条件（满足其一即可）：期望 documentId、期望 sourceUri（文件名/路径归一化）、ground truth 文本片段包含于块内容</li>
 * </ul>
 */
public final class RetrievalHitEvaluator {

    private static final int MIN_GROUND_TRUTH_LENGTH = 8;

    private final KnowbaseRepository repository;

    public RetrievalHitEvaluator(KnowbaseRepository repository) {
        this.repository = repository;
    }

    public double contextPrecisionAtK(RetrievalEvalSample sample, List<RetrievalCandidate> rankedCandidates) {
        int hitK = Math.max(1, sample.hitRank());
        List<RetrievalCandidate> topK = rankedCandidates.stream().limit(hitK).toList();
        if (topK.isEmpty()) {
            return 0.0;
        }
        int relevant = 0;
        for (RetrievalCandidate candidate : topK) {
            if (isRelevant(sample, candidate)) {
                relevant++;
            }
        }
        return (double) relevant / topK.size();
    }

    public boolean isRelevant(RetrievalEvalSample sample, RetrievalCandidate candidate) {
        Set<UUID> expectedDocuments = new HashSet<>(sample.expectedDocumentIds() == null ? List.of() : sample.expectedDocumentIds());
        List<String> expectedSources = normalizeSourceUris(sample.expectedSourceUris());
        List<String> groundTruths = normalizeGroundTruths(sample.groundTruthContexts());
        Optional<KnowledgeDocument> document = repository.findDocument(candidate.documentId());
        String sourceUri = document.map(KnowledgeDocument::sourceUri).orElse(null);
        if (expectedDocuments.contains(candidate.documentId())) {
            return true;
        }
        if (matchesSourceUri(sourceUri, expectedSources)) {
            return true;
        }
        return matchesGroundTruth(candidate.content(), groundTruths);
    }

    public HitEvaluation evaluate(RetrievalEvalSample sample, List<RetrievalCandidate> rankedCandidates) {
        int hitK = Math.max(1, sample.hitRank());
        List<RetrievalCandidate> topK = rankedCandidates.stream().limit(hitK).toList();
        if (topK.isEmpty()) {
            return new HitEvaluation(false, hitK, null, null, null, null, rankedCandidates.size(), "Top-K 内无检索结果");
        }
        Set<UUID> expectedDocuments = new HashSet<>(sample.expectedDocumentIds() == null ? List.of() : sample.expectedDocumentIds());
        List<String> expectedSources = normalizeSourceUris(sample.expectedSourceUris());
        List<String> groundTruths = normalizeGroundTruths(sample.groundTruthContexts());

        for (int index = 0; index < topK.size(); index++) {
            RetrievalCandidate candidate = topK.get(index);
            int rank = index + 1;
            Optional<KnowledgeDocument> document = repository.findDocument(candidate.documentId());
            String sourceUri = document.map(KnowledgeDocument::sourceUri).orElse(null);

            if (expectedDocuments.contains(candidate.documentId())) {
                return hit(rank, hitK, candidate, "DOCUMENT_ID", rankedCandidates.size());
            }
            if (matchesSourceUri(sourceUri, expectedSources)) {
                return hit(rank, hitK, candidate, "SOURCE_URI", rankedCandidates.size());
            }
            if (matchesGroundTruth(candidate.content(), groundTruths)) {
                return hit(rank, hitK, candidate, "GROUND_TRUTH_CONTEXT", rankedCandidates.size());
            }
        }
        return new HitEvaluation(
                false,
                hitK,
                null,
                null,
                null,
                null,
                rankedCandidates.size(),
                "Top-" + hitK + " 内未命中期望文档、来源或 ground truth 片段"
        );
    }

    private static HitEvaluation hit(
            int rank,
            int hitK,
            RetrievalCandidate candidate,
            String matchType,
            int retrievedCount
    ) {
        return new HitEvaluation(
                true,
                hitK,
                rank,
                candidate.documentId(),
                candidate.chunkId(),
                matchType,
                retrievedCount,
                null
        );
    }

    static boolean matchesSourceUri(String actualSourceUri, List<String> expectedSources) {
        if (actualSourceUri == null || actualSourceUri.isBlank() || expectedSources.isEmpty()) {
            return false;
        }
        String normalizedActual = normalizeSourceUri(actualSourceUri);
        for (String expected : expectedSources) {
            if (normalizedActual.equals(expected)) {
                return true;
            }
            if (normalizedActual.endsWith("/" + expected) || normalizedActual.endsWith("\\" + expected)) {
                return true;
            }
        }
        return false;
    }

    static boolean matchesGroundTruth(String chunkContent, List<String> groundTruths) {
        if (chunkContent == null || chunkContent.isBlank() || groundTruths.isEmpty()) {
            return false;
        }
        String normalizedChunk = normalizeText(chunkContent);
        for (String groundTruth : groundTruths) {
            if (groundTruth.length() < MIN_GROUND_TRUTH_LENGTH) {
                continue;
            }
            if (normalizedChunk.contains(groundTruth)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> normalizeSourceUris(List<String> sourceUris) {
        if (sourceUris == null || sourceUris.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String sourceUri : sourceUris) {
            if (sourceUri == null || sourceUri.isBlank()) {
                continue;
            }
            normalized.add(normalizeSourceUri(sourceUri));
        }
        return List.copyOf(normalized);
    }

    private static List<String> normalizeGroundTruths(List<String> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String context : contexts) {
            if (context == null || context.isBlank()) {
                continue;
            }
            String value = normalizeText(context);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return List.copyOf(normalized);
    }

    static String normalizeSourceUri(String sourceUri) {
        String normalized = sourceUri.trim().replace('\\', '/').toLowerCase(Locale.ROOT);
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        return normalized;
    }

    static String normalizeText(String text) {
        return text.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public record HitEvaluation(
            boolean hit,
            int hitRankUsed,
            Integer firstHitRank,
            UUID matchedDocumentId,
            UUID matchedChunkId,
            String matchType,
            int retrievedCount,
            String failureReason
    ) {
    }
}
