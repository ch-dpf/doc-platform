package com.knowbase.web.controller;

import com.knowbase.api.command.CreateLibraryCommand;
import com.knowbase.api.result.LibraryResult;
import com.knowbase.application.usecase.CreateLibraryUseCase;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "知识库管理", description = "知识库的创建、查询接口")
@RestController
@RequestMapping("/api/v1/libraries")
public class LibraryController {

    private final CreateLibraryUseCase createLibraryUseCase;

    public LibraryController(CreateLibraryUseCase createLibraryUseCase) {
        this.createLibraryUseCase = createLibraryUseCase;
    }

    @Operation(summary = "创建知识库", description = "创建一个新的知识库，可同时配置 Profile 和文档 Profile")
    @PostMapping
    public ApiResponse<LibraryResult> create(@Valid @RequestBody CreateLibraryCommand command) {
        return ApiResponse.ok(createLibraryUseCase.create(command));
    }

    @Operation(summary = "获取知识库详情", description = "根据知识库 ID 查询详细信息")
    @GetMapping("/{libraryId}")
    public ApiResponse<LibraryResult> get(
            @Parameter(description = "知识库 ID") @PathVariable UUID libraryId
    ) {
        return ApiResponse.ok(createLibraryUseCase.get(libraryId));
    }

    @Operation(summary = "查询知识库列表", description = "按租户 ID 过滤查询知识库列表，不传 tenantId 则返回全部")
    @GetMapping
    public ApiResponse<List<LibraryResult>> list(
            @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId
    ) {
        return ApiResponse.ok(createLibraryUseCase.list(tenantId));
    }
}
