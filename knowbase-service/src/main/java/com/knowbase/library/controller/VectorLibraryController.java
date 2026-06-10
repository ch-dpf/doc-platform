package com.knowbase.library.controller;

import com.knowbase.ingest.dto.PageResponse;
import com.knowbase.library.dto.CreateVectorLibraryRequest;
import com.knowbase.library.dto.DeleteVectorLibraryResponse;
import com.knowbase.library.dto.UpdateVectorLibrarySettingsRequest;
import com.knowbase.library.dto.VectorLibraryListItemResponse;
import com.knowbase.library.dto.VectorLibraryListQuery;
import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.dto.VectorLibraryUpdateResponse;
import com.knowbase.library.service.VectorLibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

@Tag(name = "知识库管理", description = "知识库新增、列表、详情与配置")
@RestController
@RequestMapping("/api/v1/vector-libraries")
public class VectorLibraryController {

    private final VectorLibraryService libraryService;

    public VectorLibraryController(VectorLibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @Operation(summary = "知识库列表（分页）", description = "按租户查询，支持名称/描述关键字与标签筛选")
    @GetMapping
    public PageResponse<VectorLibraryListItemResponse> list(
            @RequestParam String tenantId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return libraryService.list(new VectorLibraryListQuery(tenantId, keyword, tag, page, size));
    }

    @Operation(summary = "租户下知识库标签索引", description = "汇总当前租户所有知识库已使用的标签，供筛选与管理")
    @GetMapping("/meta/tags")
    public List<String> listTags(@RequestParam String tenantId) {
        return libraryService.listDistinctTags(tenantId);
    }

    @Operation(summary = "知识库详情")
    @GetMapping("/{libraryId}")
    public VectorLibraryResponse get(@PathVariable UUID libraryId) {
        return libraryService.get(libraryId);
    }

    @Operation(summary = "新增知识库", description = "创建知识库并写入默认流水线配置")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VectorLibraryResponse create(@Valid @RequestBody CreateVectorLibraryRequest request) {
        return libraryService.create(request);
    }

    @Operation(
            summary = "更新知识库配置",
            description = "可改名称、描述、检索与治理等；库内已有文档时解析/分块/向量化等流水线配置保持只读。")
    @PutMapping("/{libraryId}")
    public VectorLibraryUpdateResponse updateSettings(
            @PathVariable UUID libraryId, @Valid @RequestBody UpdateVectorLibrarySettingsRequest request) {
        return libraryService.updateSettings(libraryId, request);
    }

    @Operation(summary = "删除知识库", description = "永久删除知识库及其文档、向量分块与存储对象；系统默认库不可删除")
    @DeleteMapping("/{libraryId}")
    public DeleteVectorLibraryResponse delete(
            @PathVariable UUID libraryId, @RequestParam String tenantId) {
        return libraryService.delete(libraryId, tenantId);
    }
}
