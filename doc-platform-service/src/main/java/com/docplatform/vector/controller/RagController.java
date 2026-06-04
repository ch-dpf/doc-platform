package com.docplatform.vector.controller;

import com.docplatform.vector.dto.RagChatRequest;
import com.docplatform.vector.dto.RagChatResponse;
import com.docplatform.vector.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RAG 问答", description = "检索增强生成：向量检索 + LLM 生成回答")
@RestController
@RequestMapping("/api/v1/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @Operation(summary = "RAG 问答", description = "先按问题向量检索 TopK 片段，再调用 Ollama 对话模型生成带引用的回答")
    @PostMapping("/chat")
    public RagChatResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragService.chat(request);
    }
}
