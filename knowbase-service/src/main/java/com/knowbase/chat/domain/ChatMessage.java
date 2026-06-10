package com.knowbase.chat.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.knowbase.platform.mybatis.PostgresJsonbTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.Instant;
import java.util.UUID;

@TableName("chat_message")
public class ChatMessage {

    @TableId(value = "message_id", type = IdType.INPUT)
    private UUID messageId;

    @TableField("conversation_id")
    private UUID conversationId;

    private MessageRole role;
    private String content;

    @TableField(value = "chunk_refs", jdbcType = JdbcType.OTHER, typeHandler = PostgresJsonbTypeHandler.class)
    private String chunkRefs;

    @TableField("search_query")
    private String searchQuery;

    @TableField("token_count")
    private Integer tokenCount;

    @TableField("created_at")
    private Instant createdAt;

    public UUID getMessageId() {
        return messageId;
    }

    public void setMessageId(UUID messageId) {
        this.messageId = messageId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChunkRefs() {
        return chunkRefs;
    }

    public void setChunkRefs(String chunkRefs) {
        this.chunkRefs = chunkRefs;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
