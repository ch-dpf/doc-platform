package com.knowbase.web.controller;

import com.knowbase.api.command.AskQuestionCommand;
import com.knowbase.api.result.QueryRunResult;
import com.knowbase.application.usecase.AskQuestionUseCase;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "智能问答", description = "基于知识智能体的问答接口")
@RestController
@RequestMapping("/api/v1/query-runs")
public class QueryRunController {

    private final AskQuestionUseCase askQuestionUseCase;

    public QueryRunController(AskQuestionUseCase askQuestionUseCase) {
        this.askQuestionUseCase = askQuestionUseCase;
    }

    @Operation(summary = "提交问答请求", description = "向指定智能体提问，返回检索证据与生成回答")
    @PostMapping
    public ApiResponse<QueryRunResult> ask(@Valid @RequestBody AskQuestionCommand command) {
        return ApiResponse.ok(askQuestionUseCase.ask(command));
    }

    @Operation(summary = "获取问答运行详情", description = "根据问答运行 ID 查询执行结果，包括回答、引用和证据")
    @GetMapping("/{queryRunId}")
    public ApiResponse<QueryRunResult> get(
            @Parameter(description = "问答运行 ID") @PathVariable UUID queryRunId
    ) {
        return ApiResponse.ok(askQuestionUseCase.get(queryRunId));
    }
}
