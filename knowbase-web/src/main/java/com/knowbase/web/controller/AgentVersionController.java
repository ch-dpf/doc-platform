package com.knowbase.web.controller;

import com.knowbase.api.command.CreateAgentVersionCommand;
import com.knowbase.api.result.AgentVersionResult;
import com.knowbase.application.service.DefaultAgentVersionService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "智能体版本", description = "智能体版本生命周期管理")
@RestController
@RequestMapping("/api/v1/agents/{agentId}/versions")
public class AgentVersionController {

    private final DefaultAgentVersionService agentVersionService;

    public AgentVersionController(DefaultAgentVersionService agentVersionService) {
        this.agentVersionService = agentVersionService;
    }

    @Operation(summary = "查询智能体版本列表")
    @GetMapping
    public ApiResponse<List<AgentVersionResult>> list(@PathVariable UUID agentId) {
        return ApiResponse.ok(agentVersionService.list(agentId));
    }

    @Operation(summary = "查询智能体版本详情")
    @GetMapping("/{agentVersionId}")
    public ApiResponse<AgentVersionResult> get(
            @PathVariable UUID agentId,
            @PathVariable UUID agentVersionId
    ) {
        return ApiResponse.ok(agentVersionService.get(agentId, agentVersionId));
    }

    @Operation(summary = "创建智能体版本")
    @PostMapping
    public ApiResponse<AgentVersionResult> create(
            @PathVariable UUID agentId,
            @Valid @RequestBody CreateAgentVersionCommand command
    ) {
        return ApiResponse.ok(agentVersionService.create(agentId, command));
    }

    @Operation(summary = "发布智能体版本")
    @PostMapping("/{agentVersionId}/publish")
    public ApiResponse<AgentVersionResult> publish(
            @PathVariable UUID agentId,
            @PathVariable UUID agentVersionId
    ) {
        return ApiResponse.ok(agentVersionService.publish(agentId, agentVersionId));
    }

    @Operation(summary = "禁用智能体版本")
    @PostMapping("/{agentVersionId}/disable")
    public ApiResponse<AgentVersionResult> disable(
            @PathVariable UUID agentId,
            @PathVariable UUID agentVersionId
    ) {
        return ApiResponse.ok(agentVersionService.disable(agentId, agentVersionId));
    }
}
