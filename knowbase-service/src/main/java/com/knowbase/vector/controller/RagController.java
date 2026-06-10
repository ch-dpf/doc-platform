package com.knowbase.vector.controller;

import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.dto.RagChatRequest;
import com.knowbase.vector.dto.RagChatResponse;
import com.knowbase.vector.dto.RagStreamEvent;
import com.knowbase.vector.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@Tag(name = "RAG 问答", description = "检索增强生成：向量检索 + LLM 生成回答")
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @Operation(summary = "RAG 问答", description = "基于选定知识库向量检索 + 多轮上下文 + LLM 生成带引用的回答")
    @PostMapping("/chat")
    public RagChatResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragService.chat(request);
    }

    @Operation(summary = "RAG 问答（SSE 流式）", description = "无会话持久化时的流式问答，客户端自行维护 history")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatStream(@Valid @RequestBody RagChatRequest request) {
        return ragService.chatStream(request)
                .map(event -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(JsonSupport.toJson(event))
                        .build());
    }
}
