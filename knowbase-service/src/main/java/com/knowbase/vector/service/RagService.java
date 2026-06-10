package com.knowbase.vector.service;



import com.knowbase.vector.client.OllamaChatClient;

import com.knowbase.vector.config.OllamaProperties;

import com.knowbase.vector.config.RagProperties;

import com.knowbase.vector.dto.RagChatMessage;

import com.knowbase.vector.dto.RagChatRequest;

import com.knowbase.vector.dto.RagChatResponse;

import com.knowbase.vector.dto.RagCitation;

import com.knowbase.vector.dto.SearchHit;

import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.service.LibraryNotFoundException;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.vector.rag.RagLibraryStatsSupport;
import com.knowbase.vector.rag.RagWeeklyReportSummarySupport;
import com.knowbase.vector.rag.RagWeeklyReportWeekSupport;
import com.knowbase.vector.rag.RagEmployeeRosterSupport;
import com.knowbase.vector.rag.RagProjectParticipationSupport;
import com.knowbase.vector.mapper.DocumentChunkMapper;

import com.knowbase.vector.rag.RagConversationSupport;

import com.knowbase.vector.rag.RagHitRelevance;

import com.knowbase.vector.rag.RagPromptBuilder;

import com.knowbase.vector.rag.RagAnswerGuard;
import com.knowbase.vector.rag.RagQuestionAnalyzer;
import com.knowbase.vector.rag.RagQueryClassifier;
import com.knowbase.vector.rag.RagTemporalSupport;
import com.knowbase.vector.dto.RagStreamEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.knowbase.vector.rag.RagAnswerTemplates;



@Service

public class RagService {



    private final RagProperties ragProperties;

    private final OllamaProperties ollamaProperties;

    private final RagRetrievalService retrievalService;

    private final OllamaChatClient chatClient;

    private final RagPromptBuilder promptBuilder;

    private final DocMetadataStore docMetadataStore;

    private final VectorLibraryService vectorLibraryService;

    private final DocumentChunkMapper documentChunkMapper;



    public RagService(

            RagProperties ragProperties,

            OllamaProperties ollamaProperties,

            RagRetrievalService retrievalService,

            OllamaChatClient chatClient,

            RagPromptBuilder promptBuilder,

            DocMetadataStore docMetadataStore,

            VectorLibraryService vectorLibraryService,

            DocumentChunkMapper documentChunkMapper) {

        this.ragProperties = ragProperties;

        this.ollamaProperties = ollamaProperties;

        this.retrievalService = retrievalService;

        this.chatClient = chatClient;

        this.promptBuilder = promptBuilder;

        this.docMetadataStore = docMetadataStore;

        this.vectorLibraryService = vectorLibraryService;

        this.documentChunkMapper = documentChunkMapper;

    }



    public RagChatResponse chat(RagChatRequest request) {

        if (!ragProperties.isEnabled()) {

            throw new IllegalStateException("RAG is disabled (rag.enabled=false)");

        }



        List<RagChatMessage> history = RagConversationSupport.sanitizeHistory(

                request.history(),

                ragProperties.getMaxHistoryMessages(),

                ragProperties.getMaxHistoryMessageChars());



        String chatModel = resolveChatModel(request);

        var calendarYearAnswer = RagTemporalSupport.tryCalendarYearAnswer(request.question());
        if (calendarYearAnswer.isPresent()) {
            return metadataRagResponse(calendarYearAnswer.get(), history.size());
        }

        if (RagQueryClassifier.isConversational(request.question())) {

            return conversationalChat(request.question(), history, chatModel);

        }

        if (RagQuestionAnalyzer.isLibraryStatsQuestion(request.question())) {

            return libraryStatsResponse(request, history.size());

        }

        if (RagQuestionAnalyzer.isLibraryPurposeQuestion(request.question())) {

            return libraryPurposeResponse(request, history.size());

        }

        var libraryEmployeeCount = RagEmployeeRosterSupport.tryLibraryWideCountAnswer(
                request.question(), request.libraryId(), request.tenantId(), docMetadataStore);
        if (libraryEmployeeCount.isPresent()) {
            return metadataRagResponse(libraryEmployeeCount.get(), history.size());
        }

        var libraryProjectAnswer = RagProjectParticipationSupport.tryLibraryWideAnswer(
                request.question(),
                request.libraryId(),
                request.tenantId(),
                docMetadataStore,
                documentChunkMapper);
        if (libraryProjectAnswer.isPresent()) {
            return metadataRagResponse(libraryProjectAnswer.get(), history.size());
        }

        var projectRecountAnswer = RagProjectParticipationSupport.tryRecountFollowUp(
                request.question(),
                history,
                request.libraryId(),
                request.tenantId(),
                docMetadataStore,
                documentChunkMapper);
        if (projectRecountAnswer.isPresent()) {
            return metadataRagResponse(projectRecountAnswer.get(), history.size());
        }

        var weeklyWeekAnswer = RagWeeklyReportWeekSupport.tryLibraryWideAnswer(
                request.question(),
                history,
                request.libraryId(),
                request.tenantId(),
                docMetadataStore,
                documentChunkMapper);
        if (weeklyWeekAnswer.isPresent()) {
            return metadataRagResponse(weeklyWeekAnswer.get(), history.size());
        }

        int topK = request.topK() != null ? request.topK() : ragProperties.getDefaultTopK();
        var retrieval = retrievalService.retrieve(request, topK);
        List<SearchHit> hits = retrieval.hits();
        String searchQuery = retrieval.searchQuery();



        if (hits.isEmpty()) {

            return noHitResponse(history.size(), searchQuery);

        }

        if (!RagHitRelevance.hasTermOverlap(request.question(), hits)
                && history.isEmpty()) {
            return weakMatchResponse(history.size(), searchQuery);
        }

        if (RagQuestionAnalyzer.isDeadlineQuestion(request.question())
                && !RagAnswerGuard.sourcesMentionExplicitDeadline(hits)) {
            return new RagChatResponse(
                    RagAnswerTemplates.NO_EXPLICIT_DEADLINE,
                    toCitations(hits),
                    hits.size(),
                    false,
                    false,
                    history.size(),
                    searchQuery,
                    false);
        }

        Map<UUID, String> fileNames = resolveFileNames(hits);

        var ruleAnswer = RagEmployeeRosterSupport.tryRuleBasedAnswer(request.question(), hits, fileNames);
        if (ruleAnswer.isPresent()) {
            return ruleBasedRagResponse(ruleAnswer.get(), hits, history.size(), searchQuery);
        }

        var projectAnswer = RagProjectParticipationSupport.tryRuleBasedAnswer(request.question(), hits, fileNames);
        if (projectAnswer.isPresent()) {
            return ruleBasedRagResponse(projectAnswer.get(), hits, history.size(), searchQuery);
        }

        var weeklySummary = RagWeeklyReportSummarySupport.tryRuleBasedAnswer(
                request.question(), hits, fileNames);
        if (weeklySummary.isPresent()) {
            return ruleBasedRagResponse(weeklySummary.get(), hits, history.size(), searchQuery);
        }

        String userMessage = promptBuilder.buildUserMessage(request.question(), hits, history, fileNames);

        String answer = chatClient.chat(ragProperties.getSystemPrompt(), history, userMessage, chatModel);

        answer = RagAnswerGuard.enforceGrounding(answer, request.question(), hits, fileNames);
        answer = recoverWeeklySummaryIfEchoed(answer, request.question(), hits, fileNames);

        List<RagCitation> citations = toCitations(hits);

        boolean found = !answer.startsWith("未找到");

        return new RagChatResponse(
                answer, citations, hits.size(), true, found, history.size(), searchQuery, false);

    }



    private Map<UUID, String> resolveFileNames(List<SearchHit> hits) {
        List<UUID> docIds = hits.stream().map(SearchHit::docId).distinct().collect(Collectors.toList());
        return docMetadataStore.findFileNamesByDocIds(docIds);
    }

    private RagChatResponse metadataRagResponse(String answer, int historyUsed) {
        return new RagChatResponse(answer, List.of(), 0, false, true, historyUsed, null, false);
    }

    private RagChatResponse libraryStatsResponse(RagChatRequest request, int historyUsed) {
        try {
            VectorLibraryResponse library = requireLibrary(request);
            String answer = RagLibraryStatsSupport.formatAnswer(library, request.question());
            return metadataRagResponse(answer, historyUsed);
        } catch (LibraryNotFoundException ex) {
            return noHitResponse(historyUsed, null);
        }
    }

    private RagChatResponse libraryPurposeResponse(RagChatRequest request, int historyUsed) {
        try {
            VectorLibraryResponse library = requireLibrary(request);
            String answer = RagLibraryStatsSupport.formatPurposeAnswer(library);
            return new RagChatResponse(
                    answer, List.of(), 0, false, true, historyUsed, null, false);
        } catch (LibraryNotFoundException ex) {
            return noHitResponse(historyUsed, null);
        }
    }

    private VectorLibraryResponse requireLibrary(RagChatRequest request) {
        VectorLibraryResponse library = vectorLibraryService.get(request.libraryId());
        if (library.tenantId() != null
                && request.tenantId() != null
                && !library.tenantId().equals(request.tenantId().trim())) {
            throw new LibraryNotFoundException(request.libraryId());
        }
        return library;
    }

    private RagChatResponse conversationalChat(String question, List<RagChatMessage> history, String chatModel) {

        String systemPrompt = buildConversationalSystemPrompt(chatModel);

        String answer = chatClient.chat(systemPrompt, history, question.strip(), chatModel);

        if (answer == null || answer.isBlank()) {

            answer = "你好！我是知识库智能问答助手，请直接提问与已入库文档相关的内容。";

        }

        return new RagChatResponse(

                answer.strip(), List.of(), 0, true, false, history.size(), null, true);

    }



    private String buildConversationalSystemPrompt(String chatModel) {

        String template = ragProperties.getConversationalSystemPrompt();

        if (template == null || template.isBlank()) {

            return "你是知识库智能问答助手，请友好、简洁地用简体中文回答。";

        }

        return template.replace("{{chatModel}}", chatModel);

    }



    private String resolveChatModel(RagChatRequest request) {

        if (request.chatModel() != null && !request.chatModel().isBlank()) {

            return request.chatModel().trim();

        }

        return ollamaProperties.getChatModel();

    }



    private RagChatResponse noHitResponse(int historyUsed, String searchQuery) {

        String answer = ragProperties.getNoHitAnswer() != null && !ragProperties.getNoHitAnswer().isBlank()

                ? ragProperties.getNoHitAnswer().strip()

                : RagAnswerTemplates.NO_HIT;

        return new RagChatResponse(answer, List.of(), 0, false, false, historyUsed, searchQuery, false);

    }



    private List<RagCitation> toCitations(List<SearchHit> hits) {
        return hits.stream()
                .map(hit -> new RagCitation(
                        hit.chunkId(),
                        hit.docId(),
                        hit.chunkIndex(),
                        hit.score(),
                        promptBuilder.excerpt(hit.content())))
                .toList();
    }

    private RagChatResponse weakMatchResponse(int historyUsed, String searchQuery) {

        return new RagChatResponse(

                RagAnswerTemplates.WEAK_MATCH, List.of(), 0, false, false, historyUsed, searchQuery, false);

    }



    public Flux<RagStreamEvent> chatStream(RagChatRequest request) {
        if (!ragProperties.isEnabled()) {
            return Flux.just(RagStreamEvent.error("RAG is disabled (rag.enabled=false)"));
        }

        List<RagChatMessage> history = RagConversationSupport.sanitizeHistory(
                request.history(),
                ragProperties.getMaxHistoryMessages(),
                ragProperties.getMaxHistoryMessageChars());
        String chatModel = resolveChatModel(request);

        var calendarYearAnswer = RagTemporalSupport.tryCalendarYearAnswer(request.question());
        if (calendarYearAnswer.isPresent()) {
            return emitInstant(metadataRagResponse(calendarYearAnswer.get(), history.size()));
        }

        if (RagQueryClassifier.isConversational(request.question())) {
            RagChatResponse response = conversationalChat(request.question(), history, chatModel);
            return emitInstant(response);
        }
        if (RagQuestionAnalyzer.isLibraryStatsQuestion(request.question())) {
            return emitInstant(libraryStatsResponse(request, history.size()));
        }
        if (RagQuestionAnalyzer.isLibraryPurposeQuestion(request.question())) {
            return emitInstant(libraryPurposeResponse(request, history.size()));
        }
        var libraryEmployeeCount = RagEmployeeRosterSupport.tryLibraryWideCountAnswer(
                request.question(), request.libraryId(), request.tenantId(), docMetadataStore);
        if (libraryEmployeeCount.isPresent()) {
            return emitInstant(metadataRagResponse(libraryEmployeeCount.get(), history.size()));
        }
        var libraryProjectAnswer = RagProjectParticipationSupport.tryLibraryWideAnswer(
                request.question(),
                request.libraryId(),
                request.tenantId(),
                docMetadataStore,
                documentChunkMapper);
        if (libraryProjectAnswer.isPresent()) {
            return emitInstant(metadataRagResponse(libraryProjectAnswer.get(), history.size()));
        }
        var projectRecountAnswer = RagProjectParticipationSupport.tryRecountFollowUp(
                request.question(),
                history,
                request.libraryId(),
                request.tenantId(),
                docMetadataStore,
                documentChunkMapper);
        if (projectRecountAnswer.isPresent()) {
            return emitInstant(metadataRagResponse(projectRecountAnswer.get(), history.size()));
        }
        var weeklyWeekAnswer = RagWeeklyReportWeekSupport.tryLibraryWideAnswer(
                request.question(),
                history,
                request.libraryId(),
                request.tenantId(),
                docMetadataStore,
                documentChunkMapper);
        if (weeklyWeekAnswer.isPresent()) {
            return emitInstant(metadataRagResponse(weeklyWeekAnswer.get(), history.size()));
        }

        int topK = request.topK() != null ? request.topK() : ragProperties.getDefaultTopK();
        var retrieval = retrievalService.retrieve(request, topK);
        List<SearchHit> hits = retrieval.hits();
        String searchQuery = retrieval.searchQuery();

        if (hits.isEmpty()) {
            return emitInstant(noHitResponse(history.size(), searchQuery));
        }
        if (!RagHitRelevance.hasTermOverlap(request.question(), hits) && history.isEmpty()) {
            return emitInstant(weakMatchResponse(history.size(), searchQuery));
        }
        if (RagQuestionAnalyzer.isDeadlineQuestion(request.question())
                && !RagAnswerGuard.sourcesMentionExplicitDeadline(hits)) {
            return emitInstant(new RagChatResponse(
                    RagAnswerTemplates.NO_EXPLICIT_DEADLINE,
                    toCitations(hits),
                    hits.size(),
                    false,
                    false,
                    history.size(),
                    searchQuery,
                    false));
        }

        Map<UUID, String> fileNames = resolveFileNames(hits);
        var ruleAnswer = RagEmployeeRosterSupport.tryRuleBasedAnswer(request.question(), hits, fileNames);
        if (ruleAnswer.isPresent()) {
            return emitInstant(ruleBasedRagResponse(ruleAnswer.get(), hits, history.size(), searchQuery));
        }
        var projectAnswer = RagProjectParticipationSupport.tryRuleBasedAnswer(request.question(), hits, fileNames);
        if (projectAnswer.isPresent()) {
            return emitInstant(ruleBasedRagResponse(projectAnswer.get(), hits, history.size(), searchQuery));
        }

        var weeklySummary = RagWeeklyReportSummarySupport.tryRuleBasedAnswer(
                request.question(), hits, fileNames);
        if (weeklySummary.isPresent()) {
            return emitInstant(ruleBasedRagResponse(weeklySummary.get(), hits, history.size(), searchQuery));
        }

        String userMessage = promptBuilder.buildUserMessage(request.question(), hits, history, fileNames);
        List<SearchHit> finalHits = hits;
        StringBuilder answerBuilder = new StringBuilder();

        return chatClient.streamChat(ragProperties.getSystemPrompt(), history, userMessage, chatModel)
                .map(chunk -> {
                    answerBuilder.append(chunk);
                    return RagStreamEvent.chunk(chunk);
                })
                .concatWith(Flux.defer(() -> {
                    String answer = RagAnswerGuard.enforceGrounding(
                            answerBuilder.toString(), request.question(), finalHits, fileNames);
                    answer = recoverWeeklySummaryIfEchoed(answer, request.question(), finalHits, fileNames);
                    List<RagCitation> citations = toCitations(finalHits);
                    boolean found = !answer.startsWith("未找到");
                    RagChatResponse response = new RagChatResponse(
                            answer, citations, finalHits.size(), true, found, history.size(), searchQuery, false);
                    return Flux.just(RagStreamEvent.done(response, null));
                }))
                .onErrorResume(ex -> Flux.just(RagStreamEvent.error(ex.getMessage())));
    }

    private Flux<RagStreamEvent> emitInstant(RagChatResponse response) {
        return Flux.just(RagStreamEvent.chunk(response.answer()), RagStreamEvent.done(response, null));
    }

    private RagChatResponse ruleBasedRagResponse(
            String answer, List<SearchHit> hits, int historyUsed, String searchQuery) {
        return new RagChatResponse(
                answer, toCitations(hits), hits.size(), false, true, historyUsed, searchQuery, false);
    }

    private String recoverWeeklySummaryIfEchoed(
            String answer, String question, List<SearchHit> hits, Map<UUID, String> fileNames) {
        if (!RagWeeklyReportSummarySupport.looksLikeReferenceEcho(answer)) {
            return answer;
        }
        return RagWeeklyReportSummarySupport.tryRuleBasedAnswer(question, hits, fileNames).orElse(answer);
    }

}

