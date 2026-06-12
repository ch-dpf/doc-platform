package com.knowbase.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowbase.chat.config.ChatProperties;
import com.knowbase.chat.domain.ChatConversation;
import com.knowbase.chat.domain.ChatMessage;
import com.knowbase.chat.domain.MessageRole;
import com.knowbase.chat.dto.MessageResponse;
import com.knowbase.chat.mapper.ChatMessageMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.client.OllamaChatClient;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.dto.RagCitation;
import com.knowbase.vector.rag.RagConversationSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ChatMemoryService {

    private final ChatMessageMapper messageMapper;
    private final ChatConversationService conversationService;
    private final ChatProperties chatProperties;
    private final OllamaChatClient chatClient;

    public ChatMemoryService(
            ChatMessageMapper messageMapper,
            ChatConversationService conversationService,
            ChatProperties chatProperties,
            OllamaChatClient chatClient) {
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
        this.chatProperties = chatProperties;
        this.chatClient = chatClient;
    }

    public List<RagChatMessage> loadHistory(ChatConversation conversation) {
        int limit = Math.max(chatProperties.getMaxHistoryMessages(), 1);
        List<ChatMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getConversationId())
                .in(ChatMessage::getRole, MessageRole.user, MessageRole.assistant)
                .orderByAsc(ChatMessage::getCreatedAt)
                .last("LIMIT " + (limit * 2)));
        if (rows.size() > limit) {
            rows = rows.subList(rows.size() - limit, rows.size());
        }
        List<RagChatMessage> history = rows.stream()
                .map(m -> new RagChatMessage(m.getRole().name(), m.getContent()))
                .toList();
        return RagConversationSupport.sanitizeHistory(
                history,
                chatProperties.getMaxHistoryMessages(),
                Integer.MAX_VALUE);
    }

    public List<MessageResponse> listMessages(UUID conversationId, String tenantId) {
        conversationService.require(conversationId, tenantId);
        List<ChatMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId)
                .orderByAsc(ChatMessage::getCreatedAt));
        return rows.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ChatMessage saveUserMessage(ChatConversation conversation, String content) {
        ChatMessage msg = newMessage(conversation.getConversationId(), MessageRole.user, content, null, null);
        messageMapper.insert(msg);
        conversationService.touchConversation(conversation, content);
        conversationService.incrementMessageCount(conversation, 1);
        return msg;
    }

    @Transactional
    public ChatMessage saveAssistantMessage(
            ChatConversation conversation,
            String content,
            List<RagCitation> citations,
            String searchQuery) {
        String chunkRefsJson = citations == null || citations.isEmpty()
                ? null
                : JsonSupport.toJson(citations);
        ChatMessage msg = newMessage(
                conversation.getConversationId(),
                MessageRole.assistant,
                content,
                chunkRefsJson,
                searchQuery);
        messageMapper.insert(msg);
        conversationService.incrementMessageCount(conversation, 1);
        maybeSummarize(conversation);
        return msg;
    }

    private void maybeSummarize(ChatConversation conversation) {
        if (conversation.getMessageCount() < chatProperties.getSummaryTriggerMessages()) {
            return;
        }
        List<ChatMessage> recent = messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversation.getConversationId())
                .in(ChatMessage::getRole, MessageRole.user, MessageRole.assistant)
                .orderByAsc(ChatMessage::getCreatedAt));
        if (recent.isEmpty()) {
            return;
        }
        StringBuilder transcript = new StringBuilder();
        if (conversation.getSummary() != null && !conversation.getSummary().isBlank()) {
            transcript.append("已有摘要：").append(conversation.getSummary()).append("\n\n");
        }
        for (ChatMessage m : recent) {
            transcript.append(m.getRole().name()).append(": ").append(m.getContent()).append("\n");
        }
        String system = "请将以下对话压缩为简洁的中文摘要（200字以内），保留关键事实与结论，不要编造。";
        try {
            String summary = chatClient.chat(system, transcript.toString());
            if (summary != null && !summary.isBlank()) {
                conversationService.updateSummary(conversation, summary.strip());
            }
        } catch (Exception ignored) {
            // 摘要失败不影响主流程
        }
    }

    private static ChatMessage newMessage(
            UUID conversationId,
            MessageRole role,
            String content,
            String chunkRefs,
            String searchQuery) {
        ChatMessage msg = new ChatMessage();
        msg.setMessageId(UUID.randomUUID());
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setChunkRefs(chunkRefs);
        msg.setSearchQuery(searchQuery);
        msg.setCreatedAt(Instant.now());
        return msg;
    }

    private MessageResponse toResponse(ChatMessage msg) {
        return new MessageResponse(
                msg.getMessageId(),
                msg.getRole().name(),
                msg.getContent(),
                parseCitations(msg.getChunkRefs()),
                msg.getSearchQuery(),
                msg.getCreatedAt());
    }

    private static List<RagCitation> parseCitations(String chunkRefs) {
        if (chunkRefs == null || chunkRefs.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JsonSupport.mapper().readValue(chunkRefs, new TypeReference<List<RagCitation>>() {});
        } catch (Exception ignored) {
            // 兼容旧版仅存 chunkId 列表
        }
        try {
            List<UUID> chunkIds = JsonSupport.mapper().readValue(chunkRefs, new TypeReference<List<UUID>>() {});
            return chunkIds.stream()
                    .map(id -> new RagCitation(id, null, 0, 0.0, "", ""))
                    .toList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}
