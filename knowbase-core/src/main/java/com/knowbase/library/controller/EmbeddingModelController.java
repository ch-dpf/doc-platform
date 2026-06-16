package com.knowbase.library.controller;

import com.knowbase.library.dto.EmbeddingCatalogResponse;
import com.knowbase.vector.support.OllamaEmbeddingCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Embedding 模型", description = "本地 Ollama Embedding 模型目录")
@RestController
@RequestMapping("/api/v1/embedding-models")
public class EmbeddingModelController {

    private final OllamaEmbeddingCatalogService catalogService;

    public EmbeddingModelController(OllamaEmbeddingCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(
            summary = "Embedding 模型目录",
            description = "探测本地 Ollama 已拉取模型，返回实测支持 Embedding 的模型及维度；当前版本提供方固定 ollama，向量库固定 pgvector。")
    @GetMapping
    public EmbeddingCatalogResponse list() {
        return catalogService.catalog();
    }
}
