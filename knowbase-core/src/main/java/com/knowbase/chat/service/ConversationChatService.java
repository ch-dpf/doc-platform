package com.knowbase.chat.service;

import com.knowbase.chat.domain.ChatConversation;
import com.knowbase.chat.domain.ChatMessage;
import com.knowbase.chat.dto.ConversationChatRequest;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.dto.RagChatRequest;
import com.knowbase.vector.dto.RagChatResponse;
import com.knowbase.vector.dto.RagStreamEvent;
import com.knowbase.vector.dto.SearchRequest;
import com.knowbase.vector.service.RagService;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationChatService {

    private final ChatConversationService conversationService;
    private final ChatMemoryService memoryService;
    private final RagService ragService;

    public ConversationChatService(
            ChatConversationService conversationService,
            ChatMemoryService memoryService,
            RagService ragService) {
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.ragService = ragService;
    }

    public RagChatResponse chat(UUID conversationId, ConversationChatRequest request) {
        ChatConversation conversation = conversationService.require(conversationId, request.tenantId());
        List<RagChatMessage> history = memoryService.loadHistory(conversation);
        memoryService.saveUserMessage(conversation, request.question().strip());
        RagChatRequest ragRequest = toRagRequest(conversation, request, history);
        RagChatResponse response = ragService.chat(ragRequest);
        memoryService.saveAssistantMessage(
                conversation,
                response.answer(),
                response.citations(),
                response.searchQuery());
        return response;
    }

    public Flux<ServerSentEvent<String>> chatStream(UUID conversationId, ConversationChatRequest request) {
        ChatConversation conversation = conversationService.require(conversationId, request.tenantId());
        List<RagChatMessage> history = memoryService.loadHistory(conversation);
        memoryService.saveUserMessage(conversation, request.question().strip());
        RagChatRequest ragRequest = toRagRequest(conversation, request, history);

        return ragService.chatStream(ragRequest)
                .concatMap(event -> {
                    if ("done".equals(event.type()) && event.content() != null) {
                        ChatMessage saved = memoryService.saveAssistantMessage(
                                conversation,
                                event.content(),
                                event.citations(),
                                event.searchQuery());
                        RagStreamEvent done = RagStreamEvent.done(
                                new RagChatResponse(
                                        event.content(),
                                        event.citations(),
                                        event.retrievedCount() != null ? event.retrievedCount() : 0,
                                        event.usedLlm() != null && event.usedLlm(),
                                        event.found() != null && event.found(),
                                        event.historyUsed() != null ? event.historyUsed() : 0,
                                        event.searchQuery(),
                                        event.conversational() != null && event.conversational(),
                                        event.retrievalTrace()),
                                saved.getMessageId());
                        return Flux.just(toSse(done));
                    }
                    return Flux.just(toSse(event));
                });
    }

    private static RagChatRequest toRagRequest(
            ChatConversation conversation,
            ConversationChatRequest request,
            List<RagChatMessage> history) {
        SearchRequest.SearchFilter filter = request.filter();
        return new RagChatRequest(
                conversation.getLibraryId(),
                request.tenantId().trim(),
                request.question().strip(),
                request.topK(),
                request.minScore(),
                filter,
                request.chatModel(),
                history,
                request.includeAllChunkProfiles(),
                request.chunkProfileIds());
    }

    private static ServerSentEvent<String> toSse(RagStreamEvent event) {
        return ServerSentEvent.<String>builder()
                .event("message")
                .data(JsonSupport.toJson(event))
                .build();
    }
}
