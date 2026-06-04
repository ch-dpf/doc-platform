package com.docplatform.vector.service;

import com.docplatform.vector.client.OllamaChatClient;
import com.docplatform.vector.config.RagProperties;
import com.docplatform.vector.dto.RagChatRequest;
import com.docplatform.vector.dto.RagChatResponse;
import com.docplatform.vector.dto.RagCitation;
import com.docplatform.vector.dto.SearchHit;
import com.docplatform.vector.dto.SearchRequest;
import com.docplatform.vector.dto.SearchResponse;
import com.docplatform.vector.rag.RagAnswerTemplates;
import com.docplatform.vector.rag.RagPromptBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {

    private final RagProperties ragProperties;
    private final VectorSearchService searchService;
    private final OllamaChatClient chatClient;
    private final RagPromptBuilder promptBuilder;

    public RagService(
            RagProperties ragProperties,
            VectorSearchService searchService,
            OllamaChatClient chatClient,
            RagPromptBuilder promptBuilder) {
        this.ragProperties = ragProperties;
        this.searchService = searchService;
        this.chatClient = chatClient;
        this.promptBuilder = promptBuilder;
    }

    public RagChatResponse chat(RagChatRequest request) {
        if (!ragProperties.isEnabled()) {
            throw new IllegalStateException("RAG is disabled (rag.enabled=false)");
        }

        int topK = request.topK() != null ? request.topK() : ragProperties.getDefaultTopK();
        double minScore = request.minScore() != null ? request.minScore() : ragProperties.getMinScore();

        SearchRequest searchRequest = toSearchRequest(request, topK);
        SearchResponse searchResponse = searchService.search(searchRequest);
        List<SearchHit> hits = filterByMinScore(searchResponse.hits(), minScore);

        if (hits.isEmpty()) {
            return noHitResponse();
        }

        String userMessage = promptBuilder.buildUserMessage(request.question(), hits);
        String answer = chatClient.chat(ragProperties.getSystemPrompt(), userMessage);
        answer = normalizeGroundedAnswer(answer);
        List<RagCitation> citations = hits.stream()
                .map(hit -> new RagCitation(
                        hit.chunkId(),
                        hit.docId(),
                        hit.chunkIndex(),
                        hit.score(),
                        promptBuilder.excerpt(hit.content())))
                .toList();

        return new RagChatResponse(answer, citations, hits.size(), true, true);
    }

    private RagChatResponse noHitResponse() {
        String answer = ragProperties.getNoHitAnswer() != null && !ragProperties.getNoHitAnswer().isBlank()
                ? ragProperties.getNoHitAnswer().strip()
                : RagAnswerTemplates.NO_HIT;
        return new RagChatResponse(answer, List.of(), 0, false, false);
    }

    /**
     * 若模型仍输出“无法确定”等含糊话术，统一为固定的「未找到」句式。
     */
    private static String normalizeGroundedAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
        }
        String trimmed = answer.strip();
        if (trimmed.startsWith("未找到")) {
            return trimmed;
        }
        String lower = trimmed.toLowerCase();
        if (lower.contains("无法确定")
                || lower.contains("无法回答")
                || lower.contains("不足以回答")
                || lower.contains("没有相关")
                || lower.contains("未检索到")) {
            return RagAnswerTemplates.INSUFFICIENT_IN_PROMPT;
        }
        return trimmed;
    }

    private static SearchRequest toSearchRequest(RagChatRequest request, int topK) {
        SearchRequest.SearchFilter filter = null;
        if (request.filter() != null && request.filter().docIds() != null) {
            filter = new SearchRequest.SearchFilter(request.filter().docIds());
        }
        return new SearchRequest(request.tenantId(), request.question(), topK, filter);
    }

    private List<SearchHit> filterByMinScore(List<SearchHit> hits, double minScore) {
        if (minScore <= 0 || hits == null || hits.isEmpty()) {
            return hits == null ? List.of() : hits;
        }
        List<SearchHit> filtered = new ArrayList<>();
        for (SearchHit hit : hits) {
            if (hit.score() >= minScore) {
                filtered.add(hit);
            }
        }
        return filtered;
    }
}
