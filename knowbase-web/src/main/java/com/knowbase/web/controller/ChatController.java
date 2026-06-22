package com.knowbase.web.controller;

import com.knowbase.api.command.CreateChatSessionCommand;
import com.knowbase.api.command.SendChatMessageCommand;
import com.knowbase.api.result.ChatMessageResult;
import com.knowbase.api.result.ChatSessionResult;
import com.knowbase.api.result.QueryRunResult;
import com.knowbase.application.service.DefaultChatService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "智能问答会话", description = "多轮会话与消息接口")
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final DefaultChatService chatService;

    public ChatController(DefaultChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "创建会话")
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionResult> createSession(@Valid @RequestBody CreateChatSessionCommand command) {
        return ApiResponse.ok(chatService.createSession(command));
    }

    @Operation(summary = "查询会话详情")
    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<ChatSessionResult> getSession(@PathVariable UUID sessionId) {
        return ApiResponse.ok(chatService.getSession(sessionId));
    }

    @Operation(summary = "查询会话列表")
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResult>> listSessions(
            @RequestParam String tenantId,
            @RequestParam(required = false) UUID agentId
    ) {
        return ApiResponse.ok(chatService.listSessions(tenantId, agentId));
    }

    @Operation(summary = "发送会话消息")
    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<ChatMessageResult> sendMessage(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SendChatMessageCommand command
    ) {
        return ApiResponse.ok(chatService.sendMessage(sessionId, command));
    }

    @Operation(summary = "查询会话消息列表")
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResult>> listMessages(@PathVariable UUID sessionId) {
        return ApiResponse.ok(chatService.listMessages(sessionId));
    }

    @Operation(summary = "查询消息关联的问答运行")
    @GetMapping("/sessions/{sessionId}/messages/{messageId}/query-run")
    public ApiResponse<QueryRunResult> getMessageQueryRun(
            @PathVariable UUID sessionId,
            @PathVariable UUID messageId
    ) {
        return ApiResponse.ok(chatService.getMessageQueryRun(sessionId, messageId));
    }
}
