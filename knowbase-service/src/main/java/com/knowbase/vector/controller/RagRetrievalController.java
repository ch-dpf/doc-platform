package com.knowbase.vector.controller;

import com.knowbase.vector.dto.RagRetrievalPreviewRequest;
import com.knowbase.vector.dto.RagRetrievalPreviewResponse;
import com.knowbase.vector.service.RagRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "RAG 检索", description = "与问答同路的混合检索预览与缓存诊断")
@RestController
@RequestMapping("/api/v1/rag")
public class RagRetrievalController {

    private final RagRetrievalService retrievalService;

    public RagRetrievalController(RagRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Operation(
            summary = "RAG 检索预览",
            description = "与智能问答相同的向量化 + 混合检索 + 双路合并逻辑，返回 Top-K 片段及是否命中缓存")
    @PostMapping("/retrieval-preview")
    public RagRetrievalPreviewResponse preview(@Valid @RequestBody RagRetrievalPreviewRequest request) {
        return retrievalService.preview(request);
    }
}
