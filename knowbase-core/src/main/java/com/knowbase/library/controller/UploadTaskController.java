package com.knowbase.library.controller;

import com.knowbase.library.dto.UploadTaskResponse;
import com.knowbase.library.service.UploadTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "上传任务", description = "大文件异步上传进度查询")
@RestController
@RequestMapping("/api/v1/upload-tasks")
public class UploadTaskController {

    private final UploadTaskService uploadTaskService;

    public UploadTaskController(UploadTaskService uploadTaskService) {
        this.uploadTaskService = uploadTaskService;
    }

    @Operation(summary = "查询上传任务状态")
    @GetMapping("/{taskId}")
    public UploadTaskResponse get(@PathVariable UUID taskId) {
        return uploadTaskService.get(taskId);
    }
}
