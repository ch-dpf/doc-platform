package com.knowbase.web.controller;

import com.knowbase.api.result.BatchObjectUploadResult;
import com.knowbase.api.result.ObjectUploadResult;
import com.knowbase.application.service.DefaultObjectUploadService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "对象存储", description = "文件上传至 MinIO/本地对象存储")
@RestController
@RequestMapping("/api/v1/storage")
public class StorageController {

    private final DefaultObjectUploadService uploadService;

    public StorageController(DefaultObjectUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Operation(summary = "上传文件")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ObjectUploadResult> upload(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        uploadService.validateUpload(file.getOriginalFilename(), file.getSize());
        return ApiResponse.ok(uploadService.upload(
                file.getOriginalFilename(),
                file.getInputStream(),
                file.getContentType(),
                file.getSize()
        ));
    }

    @Operation(summary = "批量上传文件", description = "单次最多上传 50 个文件，单个文件不超过 100MB")
    @PostMapping(value = "/upload-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BatchObjectUploadResult> uploadBatch(
            @RequestParam("files") List<MultipartFile> files
    ) throws Exception {
        List<DefaultObjectUploadService.UploadCandidate> candidates = new ArrayList<>();
        for (MultipartFile file : files) {
            candidates.add(new DefaultObjectUploadService.UploadCandidate(
                    file.getOriginalFilename(),
                    file.getInputStream(),
                    file.getContentType(),
                    file.getSize()
            ));
        }
        return ApiResponse.ok(uploadService.uploadBatch(candidates));
    }
}
