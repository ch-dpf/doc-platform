package com.knowbase.web.controller;

import com.knowbase.api.result.PresetResult;
import com.knowbase.application.usecase.ListPresetUseCase;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "预设管理", description = "库类型预设与场景规则预设查询接口")
@RestController
@RequestMapping("/api/v1/presets")
public class PresetController {

    private final ListPresetUseCase listPresetUseCase;

    public PresetController(ListPresetUseCase listPresetUseCase) {
        this.listPresetUseCase = listPresetUseCase;
    }

    @Operation(summary = "查询库类型预设", description = "返回系统内置且已启用的知识库类型预设，用于建库时选择默认 Profile")
    @GetMapping("/library-types")
    public ApiResponse<List<PresetResult>> listLibraryTypePresets() {
        return ApiResponse.ok(listPresetUseCase.listLibraryTypePresets());
    }

    @Operation(summary = "查询场景规则预设", description = "返回系统内置且已启用的问答场景规则预设，用于创建知识智能体")
    @GetMapping("/scene-rules")
    public ApiResponse<List<PresetResult>> listSceneRulePresets() {
        return ApiResponse.ok(listPresetUseCase.listSceneRulePresets());
    }
}
