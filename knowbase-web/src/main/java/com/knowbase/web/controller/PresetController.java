package com.knowbase.web.controller;

import com.knowbase.api.command.CreateLibraryTypePresetCommand;
import com.knowbase.api.command.CreateSceneRulePresetCommand;
import com.knowbase.api.result.PresetResult;
import com.knowbase.application.service.DefaultPresetService;
import com.knowbase.application.usecase.ListPresetUseCase;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "预设管理", description = "库类型预设与场景规则预设查询接口")
@RestController
@RequestMapping("/api/v1/presets")
public class PresetController {

    private final DefaultPresetService presetService;

    public PresetController(DefaultPresetService presetService) {
        this.presetService = presetService;
    }

    @Operation(summary = "查询库类型预设", description = "返回系统内置与租户自定义的知识库类型预设")
    @GetMapping("/library-types")
    public ApiResponse<List<PresetResult>> listLibraryTypePresets(
            @RequestParam(required = false) String tenantId
    ) {
        if (tenantId == null || tenantId.isBlank()) {
            return ApiResponse.ok(presetService.listLibraryTypePresets());
        }
        return ApiResponse.ok(presetService.listLibraryTypePresets(tenantId));
    }

    @Operation(summary = "创建库类型预设")
    @PostMapping("/library-types")
    public ApiResponse<PresetResult> createLibraryTypePreset(
            @Valid @RequestBody CreateLibraryTypePresetCommand command
    ) {
        return ApiResponse.ok(presetService.createLibraryTypePreset(command));
    }

    @Operation(summary = "查询场景规则预设", description = "返回系统内置与租户自定义的问答场景规则预设")
    @GetMapping("/scene-rules")
    public ApiResponse<List<PresetResult>> listSceneRulePresets(
            @RequestParam(required = false) String tenantId
    ) {
        if (tenantId == null || tenantId.isBlank()) {
            return ApiResponse.ok(presetService.listSceneRulePresets());
        }
        return ApiResponse.ok(presetService.listSceneRulePresets(tenantId));
    }

    @Operation(summary = "创建场景规则预设")
    @PostMapping("/scene-rules")
    public ApiResponse<PresetResult> createSceneRulePreset(
            @Valid @RequestBody CreateSceneRulePresetCommand command
    ) {
        return ApiResponse.ok(presetService.createSceneRulePreset(command));
    }
}
