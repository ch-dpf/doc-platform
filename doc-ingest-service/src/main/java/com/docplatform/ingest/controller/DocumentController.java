package com.docplatform.ingest.controller;

import com.docplatform.ingest.domain.IndexStatus;
import com.docplatform.ingest.domain.ParseStatus;
import com.docplatform.ingest.domain.SourceType;
import com.docplatform.ingest.dto.CollectRequest;
import com.docplatform.ingest.dto.DocumentListQuery;
import com.docplatform.ingest.dto.DocumentResponse;
import com.docplatform.ingest.dto.PageResponse;
import com.docplatform.ingest.service.CollectionService;
import com.docplatform.ingest.service.DocumentPipelineService;
import com.docplatform.ingest.service.DocumentQueryService;
import com.docplatform.ingest.service.UploadService;
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

@Tag(name = "文档管理", description = "文档上传、采集与元数据查询")
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final UploadService uploadService;
    private final CollectionService collectionService;
    private final DocumentQueryService queryService;
    private final DocumentPipelineService pipelineService;

    public DocumentController(
            UploadService uploadService,
            CollectionService collectionService,
            DocumentQueryService queryService,
            DocumentPipelineService pipelineService) {
        this.uploadService = uploadService;
        this.collectionService = collectionService;
        this.queryService = queryService;
        this.pipelineService = pipelineService;
    }

    @Operation(summary = "上传文档", description = "multipart 上传，可选自动进入向量索引流水线")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @RequestParam String tenantId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean autoIndex) throws Exception {
        return uploadService.upload(tenantId, file, autoIndex);
    }

    @Operation(summary = "URL 采集", description = "从 URL 拉取内容并入库")
    @PostMapping("/collect")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse collect(@Valid @org.springframework.web.bind.annotation.RequestBody CollectRequest request) {
        return collectionService.collect(request);
    }

    @Operation(summary = "分页列出文档", description = "按租户查询未删除文档，支持来源/状态筛选与文件名、URL 关键字")
    @GetMapping
    public PageResponse<DocumentResponse> list(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) ParseStatus parseStatus,
            @RequestParam(required = false) IndexStatus indexStatus,
            @RequestParam(required = false) String keyword) {
        return queryService.list(
                new DocumentListQuery(tenantId, page, size, sourceType, parseStatus, indexStatus, keyword));
    }

    @Operation(summary = "查询文档元数据")
    @GetMapping("/{docId}")
    public DocumentResponse get(@PathVariable UUID docId) {
        return queryService.get(docId);
    }

    @Operation(summary = "删除文档", description = "软删除：标记 deleted，保留 MinIO 与元数据行，发布 DOCUMENT_DELETED")
    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID docId) {
        pipelineService.deleteDocument(docId);
    }

    @Operation(summary = "彻底删除文档", description = "物理删除：清理 MinIO 对象、删除元数据行，并发布 DOCUMENT_DELETED 清理向量")
    @DeleteMapping("/{docId}/purge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable UUID docId) {
        pipelineService.purgeDocument(docId);
    }
}
