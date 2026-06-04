package com.docplatform.ingest.controller;



import com.docplatform.ingest.domain.IndexStatus;

import com.docplatform.ingest.domain.ParseStatus;

import com.docplatform.ingest.domain.SourceType;

import com.docplatform.ingest.dto.BatchUploadResponse;

import com.docplatform.ingest.dto.CollectRequest;

import com.docplatform.ingest.dto.DocumentListQuery;

import com.docplatform.ingest.dto.DocumentResponse;

import com.docplatform.ingest.dto.PageResponse;

import com.docplatform.ingest.dto.ParsePreviewResponse;

import com.docplatform.ingest.dto.UploadConstraintsResponse;

import com.docplatform.ingest.service.CollectionService;

import com.docplatform.ingest.service.DocumentPipelineService;

import com.docplatform.ingest.service.DocumentQueryService;

import com.docplatform.ingest.service.ParsePreviewService;

import com.docplatform.ingest.service.UploadService;

import com.docplatform.library.dto.UploadTaskResponse;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;

import org.springframework.web.bind.annotation.DeleteMapping;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.multipart.MultipartFile;



import java.util.UUID;



@Tag(name = "文档管理", description = "建仓入库：文档上传、采集与元数据（需指定向量库）")

@RestController

@RequestMapping("/api/v1/documents")

public class DocumentController {



    private final UploadService uploadService;

    private final CollectionService collectionService;

    private final DocumentQueryService queryService;

    private final DocumentPipelineService pipelineService;

    private final ParsePreviewService parsePreviewService;



    public DocumentController(

            UploadService uploadService,

            CollectionService collectionService,

            DocumentQueryService queryService,

            DocumentPipelineService pipelineService,

            ParsePreviewService parsePreviewService) {

        this.uploadService = uploadService;

        this.collectionService = collectionService;

        this.queryService = queryService;

        this.pipelineService = pipelineService;

        this.parsePreviewService = parsePreviewService;

    }



    @Operation(summary = "上传约束")

    @GetMapping("/upload-constraints")

    public UploadConstraintsResponse uploadConstraints(@RequestParam UUID libraryId) {

        return uploadService.uploadConstraints(libraryId);

    }



    @Operation(summary = "文档解析预览", description = "Tika 抽取正文，用于建仓向导预览（不入库，≤5MB）")

    @PostMapping(value = "/parse-preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    public ParsePreviewResponse parsePreview(@RequestParam("file") MultipartFile file) throws java.io.IOException {

        return parsePreviewService.preview(file);

    }



    @Operation(summary = "单文件上传（同步，小文件）")

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    @ResponseStatus(HttpStatus.CREATED)

    public DocumentResponse upload(

            @RequestParam UUID libraryId,

            @RequestParam String tenantId,

            @RequestParam("file") MultipartFile file,

            @RequestParam(defaultValue = "true") boolean autoIndex) throws Exception {

        return uploadService.upload(libraryId, tenantId, file, autoIndex);

    }



    @Operation(summary = "大文件异步上传", description = "返回任务 ID，可轮询 /api/v1/upload-tasks/{taskId}")

    @PostMapping(value = "/upload/async", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    @ResponseStatus(HttpStatus.ACCEPTED)

    public UploadTaskResponse uploadAsync(

            @RequestParam UUID libraryId,

            @RequestParam String tenantId,

            @RequestParam("file") MultipartFile file,

            @RequestParam(defaultValue = "true") boolean autoIndex) throws Exception {

        return uploadService.uploadAsync(libraryId, tenantId, file, autoIndex);

    }



    @Operation(summary = "批量上传")

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    @ResponseStatus(HttpStatus.CREATED)

    public BatchUploadResponse uploadBatch(

            @RequestParam UUID libraryId,

            @RequestParam String tenantId,

            @RequestParam("files") MultipartFile[] files,

            @RequestParam(defaultValue = "true") boolean autoIndex) {

        return uploadService.uploadBatch(libraryId, tenantId, files, autoIndex);

    }



    @Operation(summary = "URL 采集")

    @PostMapping("/collect")

    @ResponseStatus(HttpStatus.CREATED)

    public DocumentResponse collect(@Valid @org.springframework.web.bind.annotation.RequestBody CollectRequest request) {

        return collectionService.collect(request);

    }



    @Operation(summary = "分页列出文档（按向量库）")

    @GetMapping

    public PageResponse<DocumentResponse> list(

            @RequestParam UUID libraryId,

            @RequestParam String tenantId,

            @RequestParam(defaultValue = "1") int page,

            @RequestParam(defaultValue = "20") int size,

            @RequestParam(required = false) SourceType sourceType,

            @RequestParam(required = false) ParseStatus parseStatus,

            @RequestParam(required = false) IndexStatus indexStatus,

            @RequestParam(required = false) String keyword) {

        return queryService.list(new DocumentListQuery(

                libraryId, tenantId, page, size, sourceType, parseStatus, indexStatus, keyword));

    }



    @Operation(summary = "查询文档元数据")

    @GetMapping("/{docId}")

    public DocumentResponse get(@PathVariable UUID docId) {

        return queryService.get(docId);

    }



    @Operation(summary = "软删除文档")

    @DeleteMapping("/{docId}")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void delete(@PathVariable UUID docId) {

        pipelineService.deleteDocument(docId);

    }



    @Operation(summary = "彻底删除文档")

    @DeleteMapping("/{docId}/purge")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void purge(@PathVariable UUID docId) {

        pipelineService.purgeDocument(docId);

    }

}


