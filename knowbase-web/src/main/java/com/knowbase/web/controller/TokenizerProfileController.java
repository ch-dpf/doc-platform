package com.knowbase.web.controller;

import com.knowbase.api.command.CreateTokenizerProfileCommand;
import com.knowbase.api.result.TokenizerProfileResult;
import com.knowbase.application.usecase.ManageTokenizerProfileUseCase;
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

@Tag(name = "Tokenizer Profile", description = "模型 tokenizer 配置与治理接口")
@RestController
@RequestMapping("/api/v1/tokenizer-profiles")
public class TokenizerProfileController {

    private final ManageTokenizerProfileUseCase useCase;

    public TokenizerProfileController(ManageTokenizerProfileUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(summary = "创建或更新 Tokenizer Profile", description = "为 Embedding 或 Chat 模型声明 tokenizer 标识、版本与是否近似")
    @PostMapping
    public ApiResponse<TokenizerProfileResult> create(@Valid @RequestBody CreateTokenizerProfileCommand command) {
        return ApiResponse.ok(useCase.create(command));
    }

    @Operation(summary = "查询 Tokenizer Profile 详情")
    @GetMapping("/{tokenizerProfileId}")
    public ApiResponse<TokenizerProfileResult> get(
            @Parameter(description = "Tokenizer Profile ID") @PathVariable UUID tokenizerProfileId
    ) {
        return ApiResponse.ok(useCase.get(tokenizerProfileId));
    }

    @Operation(summary = "查询 Tokenizer Profile 列表", description = "可按 provider 过滤，默认仅返回启用配置")
    @GetMapping
    public ApiResponse<List<TokenizerProfileResult>> list(
            @Parameter(description = "模型提供方") @RequestParam(required = false) String provider,
            @Parameter(description = "是否包含停用配置") @RequestParam(defaultValue = "false") boolean includeDisabled
    ) {
        return ApiResponse.ok(useCase.list(provider, includeDisabled));
    }
}
