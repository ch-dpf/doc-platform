package com.knowbase.chat.controller;

import com.knowbase.chat.dto.ConversationChatRequest;
import com.knowbase.chat.dto.ConversationResponse;
import com.knowbase.chat.dto.CreateConversationRequest;
import com.knowbase.chat.dto.MessageResponse;
import com.knowbase.chat.service.ChatConversationService;
import com.knowbase.chat.service.ChatMemoryService;
import com.knowbase.chat.service.ConversationChatService;
import com.knowbase.ingest.dto.PageResponse;
import com.knowbase.vector.dto.RagChatResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Tag(name = "对话会话", description = "持久化上下文记忆的 RAG 多轮对话")
@RestController
@RequestMapping("/api/v1")
public class ConversationController {

    private final ChatConversationService conversationService;
    private final ChatMemoryService memoryService;
    private final ConversationChatService chatService;

    public ConversationController(
            ChatConversationService conversationService,
            ChatMemoryService memoryService,
            ConversationChatService chatService) {
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.chatService = chatService;
    }

    @Operation(summary = "创建会话")
    @PostMapping("/vector-libraries/{libraryId}/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationResponse create(
            @PathVariable UUID libraryId,
            @Valid @RequestBody CreateConversationRequest request) {
        return conversationService.create(libraryId, request);
    }

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public PageResponse<ConversationResponse> list(
            @RequestParam UUID libraryId,
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return conversationService.list(libraryId, tenantId, page, size);
    }

    @Operation(summary = "会话详情")
    @GetMapping("/conversations/{conversationId}")
    public ConversationResponse get(
            @PathVariable UUID conversationId,
            @RequestParam String tenantId) {
        return conversationService.get(conversationId, tenantId);
    }

    @Operation(summary = "会话消息历史")
    @GetMapping("/conversations/{conversationId}/messages")
    public java.util.List<MessageResponse> messages(
            @PathVariable UUID conversationId,
            @RequestParam String tenantId) {
        return memoryService.listMessages(conversationId, tenantId);
    }

    @Operation(summary = "发送消息（同步）", description = "基于服务端持久化的上下文记忆进行 RAG 问答")
    @PostMapping("/conversations/{conversationId}/chat")
    public RagChatResponse chat(
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationChatRequest request) {
        return chatService.chat(conversationId, request);
    }

    @Operation(summary = "发送消息（SSE 流式）")
    @PostMapping(value = "/conversations/{conversationId}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(
            @PathVariable UUID conversationId,
            @Valid @RequestBody ConversationChatRequest request) {
        return chatService.chatStream(conversationId, request);
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID conversationId,
            @RequestParam String tenantId) {
        conversationService.delete(conversationId, tenantId);
    }
}
