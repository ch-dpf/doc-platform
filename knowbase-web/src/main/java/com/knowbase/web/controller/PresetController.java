package com.knowbase.web.controller;

import com.knowbase.api.command.CreateLibraryTypePresetCommand;
import com.knowbase.api.command.CreateSceneRulePresetCommand;
import com.knowbase.api.result.PageResult;
import com.knowbase.api.result.PresetResult;
import com.knowbase.application.usecase.ManagePresetUseCase;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "预设管理", description = "库类型预设与场景规则预设的增删查接口")
@RestController
@RequestMapping("/api/v1/presets")
public class PresetController {

    private final ManagePresetUseCase managePresetUseCase;

    public PresetController(ManagePresetUseCase managePresetUseCase) {
        this.managePresetUseCase = managePresetUseCase;
    }

    @Operation(summary = "分页查询库类型预设", description = "返回系统内置与租户自定义的知识库类型预设")
    @GetMapping("/library-types")
    public ApiResponse<PageResult<PresetResult>> pageLibraryTypePresets(
            @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(managePresetUseCase.pageLibraryTypePresets(tenantId, page, size));
    }

    @Operation(summary = "查询库类型预设详情", description = "按编码查询预设完整配置")
    @GetMapping("/library-types/{code}")
    public ApiResponse<PresetResult> getLibraryTypePreset(
            @Parameter(description = "预设编码") @PathVariable String code,
            @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId
    ) {
        return ApiResponse.ok(managePresetUseCase.getLibraryTypePreset(tenantId, code));
    }

    @Operation(summary = "创建库类型预设")
    @PostMapping("/library-types")
    public ApiResponse<PresetResult> createLibraryTypePreset(
            @Valid @RequestBody CreateLibraryTypePresetCommand command
    ) {
        return ApiResponse.ok(managePresetUseCase.createLibraryTypePreset(command));
    }

    @Operation(summary = "删除库类型预设", description = "仅可删除租户自定义预设，系统内置预设不可删除")
    @DeleteMapping("/library-types/{code}")
    public ApiResponse<Void> deleteLibraryTypePreset(
            @Parameter(description = "预设编码") @PathVariable String code,
            @Parameter(description = "租户 ID") @RequestParam String tenantId
    ) {
        managePresetUseCase.deleteLibraryTypePreset(tenantId, code);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "分页查询场景规则预设", description = "返回系统内置与租户自定义的问答场景规则预设")
    @GetMapping("/scene-rules")
    public ApiResponse<PageResult<PresetResult>> pageSceneRulePresets(
            @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId,
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.ok(managePresetUseCase.pageSceneRulePresets(tenantId, page, size));
    }

    @Operation(summary = "查询场景规则预设详情", description = "按编码查询预设完整配置")
    @GetMapping("/scene-rules/{code}")
    public ApiResponse<PresetResult> getSceneRulePreset(
            @Parameter(description = "预设编码") @PathVariable String code,
            @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId
    ) {
        return ApiResponse.ok(managePresetUseCase.getSceneRulePreset(tenantId, code));
    }

    @Operation(summary = "创建场景规则预设")
    @PostMapping("/scene-rules")
    public ApiResponse<PresetResult> createSceneRulePreset(
            @Valid @RequestBody CreateSceneRulePresetCommand command
    ) {
        return ApiResponse.ok(managePresetUseCase.createSceneRulePreset(command));
    }

    @Operation(summary = "删除场景规则预设", description = "仅可删除租户自定义预设，系统内置预设不可删除")
    @DeleteMapping("/scene-rules/{code}")
    public ApiResponse<Void> deleteSceneRulePreset(
            @Parameter(description = "预设编码") @PathVariable String code,
            @Parameter(description = "租户 ID") @RequestParam String tenantId
    ) {
        managePresetUseCase.deleteSceneRulePreset(tenantId, code);
        return ApiResponse.ok(null);
    }
}
