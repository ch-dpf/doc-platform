package com.knowbase.application.service;

import com.knowbase.api.command.CreateChatSessionCommand;
import com.knowbase.api.command.SendChatMessageCommand;
import com.knowbase.api.result.ChatMessageResult;
import com.knowbase.api.result.ChatSessionResult;
import com.knowbase.api.result.QueryRunResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.application.pipeline.DefaultQueryPipeline;
import com.knowbase.domain.model.ChatMessage;
import com.knowbase.domain.model.ChatSession;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.repository.KnowbaseRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DefaultChatService {

    private final KnowbaseRepository repository;
    private final DefaultQueryPipeline queryPipeline;

    public DefaultChatService(KnowbaseRepository repository, DefaultQueryPipeline queryPipeline) {
        this.repository = repository;
        this.queryPipeline = queryPipeline;
    }

    public ChatSessionResult createSession(CreateChatSessionCommand command) {
        repository.findAgent(command.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体不存在: " + command.agentId()));
        Instant now = Instant.now();
        ChatSession session = new ChatSession(
                UUID.randomUUID(),
                command.tenantId(),
                command.agentId(),
                command.agentVersionId(),
                command.title() == null || command.title().isBlank() ? "新会话" : command.title(),
                "ACTIVE",
                now,
                now
        );
        return toSessionResult(repository.saveChatSession(session));
    }

    public ChatSessionResult getSession(UUID sessionId) {
        return repository.findChatSession(sessionId)
                .map(DefaultChatService::toSessionResult)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在: " + sessionId));
    }

    public List<ChatSessionResult> listSessions(String tenantId, UUID agentId) {
        return repository.listChatSessions(tenantId, agentId).stream()
                .map(DefaultChatService::toSessionResult)
                .toList();
    }

    public ChatMessageResult sendMessage(UUID sessionId, SendChatMessageCommand command) {
        ChatSession session = repository.findChatSession(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在: " + sessionId));
        KnowledgeAgent agent = repository.findAgent(session.agentId())
                .orElseThrow(() -> new ResourceNotFoundException("知识智能体不存在: " + session.agentId()));
        Instant now = Instant.now();
        repository.saveChatMessage(new ChatMessage(
                UUID.randomUUID(),
                sessionId,
                "user",
                command.content(),
                null,
                now
        ));
        UUID queryRunId = UUID.randomUUID();
        QueryRun queryRun = queryPipeline.run(
                queryRunId,
                session.agentId(),
                command.agentVersionId() != null ? command.agentVersionId() : session.agentVersionId(),
                command.content(),
                command.debugLibraryIds()
        );
        ChatMessage assistantMessage = repository.saveChatMessage(new ChatMessage(
                UUID.randomUUID(),
                sessionId,
                "assistant",
                queryRun.answer(),
                queryRun.queryRunId(),
                Instant.now()
        ));
        repository.saveChatSession(new ChatSession(
                session.sessionId(),
                session.tenantId(),
                session.agentId(),
                queryRun.agentVersionId(),
                session.title(),
                session.status(),
                session.createdAt(),
                Instant.now()
        ));
        return toMessageResult(assistantMessage);
    }

    public List<ChatMessageResult> listMessages(UUID sessionId) {
        repository.findChatSession(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("会话不存在: " + sessionId));
        return repository.listChatMessages(sessionId).stream()
                .map(DefaultChatService::toMessageResult)
                .toList();
    }

    public QueryRunResult getMessageQueryRun(UUID sessionId, UUID messageId) {
        ChatMessage message = repository.listChatMessages(sessionId).stream()
                .filter(item -> item.messageId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("消息不存在: " + messageId));
        if (message.queryRunId() == null) {
            throw new ResourceNotFoundException("消息未关联问答运行: " + messageId);
        }
        return repository.findQueryRun(message.queryRunId())
                .map(ResultMapper::toQueryRunResult)
                .orElseThrow(() -> new ResourceNotFoundException("问答运行不存在: " + message.queryRunId()));
    }

    private static ChatSessionResult toSessionResult(ChatSession session) {
        return new ChatSessionResult(
                session.sessionId(),
                session.tenantId(),
                session.agentId(),
                session.agentVersionId(),
                session.title(),
                session.status(),
                session.createdAt(),
                session.updatedAt()
        );
    }

    private static ChatMessageResult toMessageResult(ChatMessage message) {
        return new ChatMessageResult(
                message.messageId(),
                message.sessionId(),
                message.role(),
                message.content(),
                message.queryRunId(),
                message.createdAt()
        );
    }
}
