package com.docplatform.library.controller;

import com.docplatform.library.dto.CreateVectorLibraryRequest;
import com.docplatform.library.dto.UpdateVectorLibrarySettingsRequest;
import com.docplatform.library.dto.VectorLibraryResponse;
import com.docplatform.library.dto.VectorLibraryUpdateResponse;
import com.docplatform.library.service.VectorLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "向量库管理", description = "向量库新增、列表与详情")
@RestController
@RequestMapping("/api/v1/vector-libraries")
public class VectorLibraryController {

    private final VectorLibraryService libraryService;

    public VectorLibraryController(VectorLibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Operation(summary = "向量库列表")
    @GetMapping
    public List<VectorLibraryResponse> list(@RequestParam String tenantId) {
        return libraryService.list(tenantId);
    }

    @Operation(summary = "向量库详情")
    @GetMapping("/{libraryId}")
    public VectorLibraryResponse get(@PathVariable UUID libraryId) {
        return libraryService.get(libraryId);
    }

    @Operation(summary = "新增向量库", description = "同时创建固定全流程的默认建仓编排")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VectorLibraryResponse create(@Valid @RequestBody CreateVectorLibraryRequest request) {
        return libraryService.create(request);
    }

    @Operation(
            summary = "更新向量库（低风险）",
            description = "可改名称、描述、分块/清洗/向量化配置；不改存储与数据源。流水线步骤固定。仅影响之后新入库或重索引的文档。")
    @PutMapping("/{libraryId}")
    public VectorLibraryUpdateResponse updateSettings(
            @PathVariable UUID libraryId, @Valid @RequestBody UpdateVectorLibrarySettingsRequest request) {
        return libraryService.updateSettings(libraryId, request);
    }
}
