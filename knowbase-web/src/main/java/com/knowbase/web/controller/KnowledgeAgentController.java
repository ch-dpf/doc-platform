package com.knowbase.web.controller;

import com.knowbase.api.command.CreateKnowledgeAgentCommand;
import com.knowbase.api.command.CreateRetrievalTestCommand;
import com.knowbase.api.result.KnowledgeAgentResult;
import com.knowbase.api.result.RetrievalTestResult;
import com.knowbase.application.usecase.CreateKnowledgeAgentUseCase;
import com.knowbase.application.usecase.RunRetrievalTestUseCase;
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

@Tag(name = "知识智能体", description = "知识智能体的创建与查询接口")
@RestController
@RequestMapping("/api/v1/agents")
public class KnowledgeAgentController {

    private final CreateKnowledgeAgentUseCase createKnowledgeAgentUseCase;
    private final RunRetrievalTestUseCase runRetrievalTestUseCase;

    public KnowledgeAgentController(
            CreateKnowledgeAgentUseCase createKnowledgeAgentUseCase,
            RunRetrievalTestUseCase runRetrievalTestUseCase
    ) {
        this.createKnowledgeAgentUseCase = createKnowledgeAgentUseCase;
        this.runRetrievalTestUseCase = runRetrievalTestUseCase;
    }

    @Operation(summary = "创建知识智能体", description = "创建一个新的知识智能体，关联知识库并配置检索与回答策略")
    @PostMapping
    public ApiResponse<KnowledgeAgentResult> create(@Valid @RequestBody CreateKnowledgeAgentCommand command) {
        return ApiResponse.ok(createKnowledgeAgentUseCase.create(command));
    }

    @Operation(summary = "获取智能体详情", description = "根据智能体 ID 查询详细信息")
    @GetMapping("/{agentId}")
    public ApiResponse<KnowledgeAgentResult> get(
            @Parameter(description = "智能体 ID") @PathVariable UUID agentId
    ) {
        return ApiResponse.ok(createKnowledgeAgentUseCase.get(agentId));
    }

    @Operation(summary = "查询智能体列表", description = "按租户 ID 过滤查询智能体列表，不传 tenantId 则返回全部")
    @GetMapping
    public ApiResponse<List<KnowledgeAgentResult>> list(
            @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId
    ) {
        return ApiResponse.ok(createKnowledgeAgentUseCase.list(tenantId));
    }

    @Operation(summary = "执行智能体检索测试", description = "不生成最终回答，仅执行多库路由、检索、证据构建与上下文 token 拼装")
    @PostMapping("/{agentId}/retrieval-tests")
    public ApiResponse<RetrievalTestResult> retrievalTest(
            @Parameter(description = "智能体 ID") @PathVariable UUID agentId,
            @Valid @RequestBody CreateRetrievalTestCommand command
    ) {
        return ApiResponse.ok(runRetrievalTestUseCase.run(agentId, command));
    }
}
