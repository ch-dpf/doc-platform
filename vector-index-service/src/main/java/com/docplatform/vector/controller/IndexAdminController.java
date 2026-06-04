package com.docplatform.vector.controller;

import com.docplatform.vector.dto.RebuildRequest;
import com.docplatform.vector.service.IndexingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "索引管理", description = "向量索引补偿与清理")
@RestController
@RequestMapping("/api/v1/index")
public class IndexAdminController {

    private final IndexingService indexingService;

    public IndexAdminController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @Operation(summary = "补偿重索引")
    @PostMapping("/rebuild")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void rebuild(@Valid @RequestBody RebuildRequest request) {
        indexingService.rebuild(
                request.docId(),
                request.tenantId(),
                request.version(),
                request.parsedTextUrl());
    }

    @Operation(summary = "清理文档向量")
    @DeleteMapping("/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void purge(@PathVariable UUID docId) {
        indexingService.delete(com.docplatform.contract.DocumentDeletedEvent.create(docId, "admin", 0));
    }
}
