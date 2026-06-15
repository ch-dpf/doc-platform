package com.knowbase.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowbase.chat.config.ChatProperties;
import com.knowbase.chat.domain.ChatConversation;
import com.knowbase.chat.dto.ConversationResponse;
import com.knowbase.chat.dto.CreateConversationRequest;
import com.knowbase.chat.mapper.ChatConversationMapper;
import com.knowbase.chat.mapper.ChatMessageMapper;
import com.knowbase.chat.domain.ChatMessage;
import com.knowbase.ingest.dto.PageResponse;
import com.knowbase.library.service.LibraryNotFoundException;
import com.knowbase.library.service.VectorLibraryService;
import org.springframework.stereotype.Service;
import com.knowbase.tx.KnowbaseTransactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ChatConversationService {

    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final VectorLibraryService libraryService;
    private final ChatProperties chatProperties;

    public ChatConversationService(
            ChatConversationMapper conversationMapper,
            ChatMessageMapper messageMapper,
            VectorLibraryService libraryService,
            ChatProperties chatProperties) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.libraryService = libraryService;
        this.chatProperties = chatProperties;
    }

    public ConversationResponse create(UUID libraryId, CreateConversationRequest request) {
        try {
            libraryService.get(libraryId);
        } catch (LibraryNotFoundException e) {
            throw e;
        }
        Instant now = Instant.now();
        ChatConversation conv = new ChatConversation();
        conv.setConversationId(UUID.randomUUID());
        conv.setLibraryId(libraryId);
        conv.setTenantId(request.tenantId().trim());
        conv.setTitle(request.title() != null && !request.title().isBlank()
                ? request.title().trim()
                : "新对话");
        conv.setMessageCount(0);
        conv.setCreatedAt(now);
        conv.setUpdatedAt(now);
        conv.setDeleted(false);
        conversationMapper.insert(conv);
        return toResponse(conv);
    }

    public PageResponse<ConversationResponse> list(UUID libraryId, String tenantId, int page, int size) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<ChatConversation>()
                .eq(ChatConversation::getLibraryId, libraryId)
                .eq(ChatConversation::getTenantId, tenantId)
                .eq(ChatConversation::isDeleted, false)
                .orderByDesc(ChatConversation::getUpdatedAt);
        Page<ChatConversation> result = conversationMapper.selectPage(new Page<>(page, size), wrapper);
        List<ConversationResponse> items = result.getRecords().stream().map(this::toResponse).toList();
        return new PageResponse<>(items, result.getTotal(), page, size);
    }

    public ChatConversation require(UUID conversationId, String tenantId) {
        ChatConversation conv = conversationMapper.selectById(conversationId);
        if (conv == null || conv.isDeleted()) {
            throw new ConversationNotFoundException(conversationId);
        }
        if (tenantId != null && !tenantId.isBlank() && !conv.getTenantId().equals(tenantId.trim())) {
            throw new ConversationNotFoundException(conversationId);
        }
        return conv;
    }

    public ConversationResponse get(UUID conversationId, String tenantId) {
        return toResponse(require(conversationId, tenantId));
    }

    @KnowbaseTransactional
    public void delete(UUID conversationId, String tenantId) {
        ChatConversation conv = require(conversationId, tenantId);
        conv.setDeleted(true);
        conv.setUpdatedAt(Instant.now());
        conversationMapper.updateById(conv);
        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getConversationId, conversationId));
    }

    public void touchConversation(ChatConversation conv, String titleCandidate) {
        conv.setUpdatedAt(Instant.now());
        if ((conv.getTitle() == null || conv.getTitle().isBlank() || "新对话".equals(conv.getTitle()))
                && titleCandidate != null
                && !titleCandidate.isBlank()) {
            int max = chatProperties.getMaxTitleChars();
            String title = titleCandidate.strip();
            if (title.length() > max) {
                title = title.substring(0, max) + "…";
            }
            conv.setTitle(title);
        }
        conversationMapper.updateById(conv);
    }

    public void incrementMessageCount(ChatConversation conv, int delta) {
        conv.setMessageCount(conv.getMessageCount() + delta);
        conv.setUpdatedAt(Instant.now());
        conversationMapper.updateById(conv);
    }

    public void updateSummary(ChatConversation conv, String summary) {
        conv.setSummary(summary);
        conv.setUpdatedAt(Instant.now());
        conversationMapper.updateById(conv);
    }

    private ConversationResponse toResponse(ChatConversation conv) {
        return new ConversationResponse(
                conv.getConversationId(),
                conv.getLibraryId(),
                conv.getTenantId(),
                conv.getTitle(),
                conv.getSummary(),
                conv.getMessageCount(),
                conv.getCreatedAt(),
                conv.getUpdatedAt());
    }
}
