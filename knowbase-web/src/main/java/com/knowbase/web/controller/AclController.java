package com.knowbase.web.controller;

import com.knowbase.api.command.GrantAclCommand;
import com.knowbase.api.result.AclEntryResult;
import com.knowbase.application.service.DefaultAclService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "权限 ACL", description = "知识库/智能体/文档 ACL 管理")
@RestController
@RequestMapping("/api/v1/acls")
public class AclController {

    private final DefaultAclService aclService;

    public AclController(DefaultAclService aclService) {
        this.aclService = aclService;
    }

    @Operation(summary = "授予 ACL")
    @PostMapping
    public ApiResponse<AclEntryResult> grant(@Valid @RequestBody GrantAclCommand command) {
        return ApiResponse.ok(aclService.grant(command));
    }

    @Operation(summary = "撤销 ACL")
    @DeleteMapping("/{aclId}")
    public ApiResponse<Void> revoke(@PathVariable UUID aclId) {
        aclService.revoke(aclId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "查询资源 ACL 列表")
    @GetMapping
    public ApiResponse<List<AclEntryResult>> list(
            @RequestParam String tenantId,
            @RequestParam String resourceType,
            @RequestParam UUID resourceId
    ) {
        return ApiResponse.ok(aclService.list(tenantId, resourceType, resourceId));
    }
}
