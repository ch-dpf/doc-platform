package com.knowbase.web.controller;

import com.knowbase.api.command.DeleteDocumentsCommand;
import com.knowbase.api.command.CreateLibraryRetrievalTestCommand;
import com.knowbase.api.command.CreateDocumentProfileCommand;
import com.knowbase.api.command.GenerateRetrievalEvalDraftsCommand;
import com.knowbase.api.command.ImportRetrievalEvalSamplesCommand;
import com.knowbase.api.command.LibraryProfileCommand;
import com.knowbase.api.command.UpdateDocumentProfileCommand;
import com.knowbase.api.command.CreateRetrievalEvalRunCommand;
import com.knowbase.api.command.CreateRetrievalEvalSampleCommand;
import com.knowbase.api.command.UpdateDocumentChunkCommand;
import com.knowbase.api.command.UpdateRetrievalEvalSampleCommand;
import com.knowbase.api.result.BatchDeleteDocumentsResult;
import com.knowbase.api.result.BatchReindexResult;
import com.knowbase.api.result.DocumentPipelineTraceResult;
import com.knowbase.api.result.DocumentChunkResult;
import com.knowbase.api.result.DocumentDuplicateGroupResult;
import com.knowbase.api.result.DocumentIndexJobResult;
import com.knowbase.api.result.DocumentUploadResult;
import com.knowbase.api.result.IndexGenerationRebuildResult;
import com.knowbase.api.result.IndexHealthResult;
import com.knowbase.api.result.IndexVersionResult;
import com.knowbase.api.result.IngestionDocumentErrorResult;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.api.result.KnowledgeDocumentResult;
import com.knowbase.api.result.LibraryProfileResult;
import com.knowbase.api.result.LibraryRetrievalTestResult;
import com.knowbase.api.result.PromoteEvalGateResult;
import com.knowbase.api.result.PromoteReadinessResult;
import com.knowbase.api.result.PageResult;
import com.knowbase.api.result.DocumentProfileResult;
import com.knowbase.api.result.LibraryProfileVersionResult;
import com.knowbase.api.result.RetrievalEvalBaselineResult;
import com.knowbase.api.result.RetrievalEvalRunResult;
import com.knowbase.api.result.RetrievalEvalSampleResult;
import com.knowbase.api.result.RetrievalHitCheckResult;
import com.knowbase.application.service.DefaultDocumentChunkService;
import com.knowbase.application.service.DefaultDocumentProfileService;
import com.knowbase.application.service.DefaultDocumentService;
import com.knowbase.application.service.DocumentPreviewContent;
import com.knowbase.application.service.DefaultIndexGenerationRebuildService;
import com.knowbase.application.service.DefaultIndexVersionService;
import com.knowbase.application.service.DefaultLibraryCatalogService;
import com.knowbase.application.service.DefaultLibraryIndexHealthService;
import com.knowbase.application.service.DefaultLibraryProfileService;
import com.knowbase.application.service.DefaultLibraryRetrievalTestService;
import com.knowbase.application.service.DefaultObjectUploadService;
import com.knowbase.application.service.DefaultPromoteEvalGateService;
import com.knowbase.application.service.DefaultRetrievalEvalService;
import com.knowbase.web.support.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Tag(name = "知识库文档与索引代次", description = "文档（主路径）与索引代次运维 API")
@RestController
@RequestMapping("/api/v1/libraries/{libraryId}")
public class LibraryCatalogController {

    private final DefaultLibraryCatalogService catalogService;
    private final DefaultIndexVersionService indexVersionService;
    private final DefaultDocumentService documentService;
    private final DefaultIndexGenerationRebuildService rebuildService;
    private final DefaultLibraryIndexHealthService indexHealthService;
    private final DefaultLibraryRetrievalTestService libraryRetrievalTestService;
    private final DefaultLibraryProfileService libraryProfileService;
    private final DefaultPromoteEvalGateService promoteEvalGateService;
    private final DefaultRetrievalEvalService retrievalEvalService;
    private final DefaultDocumentProfileService documentProfileService;
    private final DefaultDocumentChunkService documentChunkService;

    public LibraryCatalogController(
            DefaultLibraryCatalogService catalogService,
            DefaultIndexVersionService indexVersionService,
            DefaultDocumentService documentService,
            DefaultIndexGenerationRebuildService rebuildService,
            DefaultLibraryIndexHealthService indexHealthService,
            DefaultLibraryRetrievalTestService libraryRetrievalTestService,
            DefaultLibraryProfileService libraryProfileService,
            DefaultPromoteEvalGateService promoteEvalGateService,
            DefaultRetrievalEvalService retrievalEvalService,
            DefaultDocumentProfileService documentProfileService,
            DefaultDocumentChunkService documentChunkService
    ) {
        this.catalogService = catalogService;
        this.indexVersionService = indexVersionService;
        this.documentService = documentService;
        this.rebuildService = rebuildService;
        this.indexHealthService = indexHealthService;
        this.libraryRetrievalTestService = libraryRetrievalTestService;
        this.libraryProfileService = libraryProfileService;
        this.promoteEvalGateService = promoteEvalGateService;
        this.retrievalEvalService = retrievalEvalService;
        this.documentProfileService = documentProfileService;
        this.documentChunkService = documentChunkService;
    }

    @Operation(summary = "查询索引代次列表")
    @GetMapping("/index-generations")
    public ApiResponse<List<IndexVersionResult>> listIndexGenerations(@PathVariable UUID libraryId) {
        return ApiResponse.ok(catalogService.listIndexVersions(libraryId));
    }

    @Operation(summary = "全库重建索引代次", description = "在新建 BUILDING 代次中重建全部文档；可选 autoPromote 切换 active 指针")
    @PostMapping("/index-generations/rebuild")
    public ApiResponse<IndexGenerationRebuildResult> rebuildIndexGeneration(
            @PathVariable UUID libraryId,
            @RequestParam(defaultValue = "false") boolean autoPromote
    ) {
        return ApiResponse.ok(rebuildService.rebuild(libraryId, autoPromote));
    }

    @Operation(summary = "Promote 索引代次")
    @PostMapping("/index-generations/{indexGenerationId}/promote")
    public ApiResponse<IndexVersionResult> promoteIndexGeneration(
            @PathVariable UUID libraryId,
            @PathVariable UUID indexGenerationId,
            @RequestParam(defaultValue = "false") boolean force
    ) {
        return ApiResponse.ok(indexVersionService.publish(libraryId, indexGenerationId, force));
    }

    @Operation(summary = "索引健康检查", description = "检测 active 代次与当前 Library Profile 的 L1 漂移，提示是否需要 rebuild")
    @GetMapping("/index-health")
    public ApiResponse<IndexHealthResult> indexHealth(@PathVariable UUID libraryId) {
        return ApiResponse.ok(indexHealthService.assess(libraryId));
    }

    @Operation(summary = "Promote 就绪检查", description = "返回 blockers/warnings，前端可在 promote 前确认")
    @GetMapping("/index-generations/{indexGenerationId}/promote-readiness")
    public ApiResponse<PromoteReadinessResult> promoteReadiness(
            @PathVariable UUID libraryId,
            @PathVariable UUID indexGenerationId
    ) {
        return ApiResponse.ok(indexHealthService.checkPromote(libraryId, indexGenerationId));
    }

    @Operation(summary = "Promote 评测门禁", description = "基于 kb_retrieval_eval_sample 黄金集执行 Recall@K 评测")
    @GetMapping("/index-generations/promote-eval-gate")
    public ApiResponse<PromoteEvalGateResult> promoteEvalGate(@PathVariable UUID libraryId) {
        return ApiResponse.ok(promoteEvalGateService.evaluate(libraryId));
    }

    @Operation(summary = "查询 Library Profile", description = "当前最新 Profile 及与 active 代次的 L1 漂移信息")
    @GetMapping("/profile")
    public ApiResponse<LibraryProfileResult> libraryProfile(@PathVariable UUID libraryId) {
        return ApiResponse.ok(libraryProfileService.getLatest(libraryId));
    }

    @Operation(summary = "创建 Library Profile 新版本", description = "L1 变更需 rebuild；L2 变更建议按 Document Profile 重索引并回归评测")
    @PostMapping("/profiles")
    public ApiResponse<LibraryProfileResult> createLibraryProfileVersion(
            @PathVariable UUID libraryId,
            @Valid @RequestBody LibraryProfileCommand command
    ) {
        return ApiResponse.ok(libraryProfileService.createVersion(libraryId, command));
    }

    @Operation(summary = "查询 Library Profile 版本历史")
    @GetMapping("/profiles")
    public ApiResponse<List<LibraryProfileVersionResult>> listLibraryProfileVersions(@PathVariable UUID libraryId) {
        return ApiResponse.ok(libraryProfileService.listVersions(libraryId));
    }

    @Operation(summary = "查询指定 Library Profile 版本")
    @GetMapping("/profiles/{profileId}")
    public ApiResponse<LibraryProfileResult> getLibraryProfileVersion(
            @PathVariable UUID libraryId,
            @PathVariable UUID profileId
    ) {
        return ApiResponse.ok(libraryProfileService.getVersion(libraryId, profileId));
    }

    @Operation(summary = "查询 Document Profile 列表")
    @GetMapping("/document-profiles")
    public ApiResponse<List<DocumentProfileResult>> listDocumentProfiles(@PathVariable UUID libraryId) {
        return ApiResponse.ok(documentProfileService.list(libraryId));
    }

    @Operation(summary = "创建 Document Profile")
    @PostMapping("/document-profiles")
    public ApiResponse<DocumentProfileResult> createDocumentProfile(
            @PathVariable UUID libraryId,
            @Valid @RequestBody CreateDocumentProfileCommand command
    ) {
        return ApiResponse.ok(documentProfileService.create(libraryId, command));
    }

    @Operation(summary = "查询 Document Profile")
    @GetMapping("/document-profiles/{code}")
    public ApiResponse<DocumentProfileResult> getDocumentProfile(
            @PathVariable UUID libraryId,
            @PathVariable String code
    ) {
        return ApiResponse.ok(documentProfileService.get(libraryId, code));
    }

    @Operation(summary = "更新 Document Profile")
    @PutMapping("/document-profiles/{code}")
    public ApiResponse<DocumentProfileResult> updateDocumentProfile(
            @PathVariable UUID libraryId,
            @PathVariable String code,
            @Valid @RequestBody UpdateDocumentProfileCommand command
    ) {
        return ApiResponse.ok(documentProfileService.update(libraryId, code, command));
    }

    @Operation(summary = "删除 Document Profile")
    @DeleteMapping("/document-profiles/{code}")
    public ApiResponse<Void> deleteDocumentProfile(
            @PathVariable UUID libraryId,
            @PathVariable String code
    ) {
        documentProfileService.delete(libraryId, code);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "库级召回检索测试", description = "无需智能体，直接对当前 active 代次执行向量检索；可选附带 Hit@K 判定字段")
    @PostMapping("/retrieval-tests")
    public ApiResponse<LibraryRetrievalTestResult> libraryRetrievalTest(
            @PathVariable UUID libraryId,
            @Valid @RequestBody CreateLibraryRetrievalTestCommand command
    ) {
        if (!hasEvalCriteria(command)) {
            return ApiResponse.ok(libraryRetrievalTestService.run(libraryId, command));
        }
        var execution = libraryRetrievalTestService.executeRetrieval(libraryId, command);
        RetrievalHitCheckResult hitCheck = retrievalEvalService.evaluateHit(
                libraryId,
                command,
                execution.rankedCandidates()
        );
        return ApiResponse.ok(new LibraryRetrievalTestResult(
                execution.result().retrievalTestId(),
                execution.result().libraryId(),
                execution.result().question(),
                execution.result().candidateCount(),
                execution.result().evidence(),
                execution.result().citations(),
                execution.result().contextTokens(),
                execution.result().tokenizerId(),
                execution.result().tokenizerVersion(),
                execution.result().evidenceLow(),
                execution.result().trace(),
                execution.result().createdAt(),
                hitCheck
        ));
    }

    @Operation(summary = "查询召回评测黄金样本")
    @GetMapping("/retrieval-eval-samples")
    public ApiResponse<List<RetrievalEvalSampleResult>> listRetrievalEvalSamples(
            @PathVariable UUID libraryId,
            @RequestParam(defaultValue = "false") boolean enabledOnly
    ) {
        return ApiResponse.ok(retrievalEvalService.listSamples(libraryId, enabledOnly));
    }

    @Operation(summary = "创建召回评测黄金样本")
    @PostMapping("/retrieval-eval-samples")
    public ApiResponse<RetrievalEvalSampleResult> createRetrievalEvalSample(
            @PathVariable UUID libraryId,
            @Valid @RequestBody CreateRetrievalEvalSampleCommand command
    ) {
        return ApiResponse.ok(retrievalEvalService.createSample(libraryId, command));
    }

    @Operation(summary = "更新召回评测黄金样本")
    @PutMapping("/retrieval-eval-samples/{sampleId}")
    public ApiResponse<RetrievalEvalSampleResult> updateRetrievalEvalSample(
            @PathVariable UUID libraryId,
            @PathVariable UUID sampleId,
            @Valid @RequestBody UpdateRetrievalEvalSampleCommand command
    ) {
        return ApiResponse.ok(retrievalEvalService.updateSample(libraryId, sampleId, command));
    }

    @Operation(summary = "删除召回评测黄金样本")
    @DeleteMapping("/retrieval-eval-samples/{sampleId}")
    public ApiResponse<Void> deleteRetrievalEvalSample(
            @PathVariable UUID libraryId,
            @PathVariable UUID sampleId
    ) {
        retrievalEvalService.deleteSample(libraryId, sampleId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "批量导入黄金样本 JSON")
    @PostMapping("/retrieval-eval-samples/import")
    public ApiResponse<List<RetrievalEvalSampleResult>> importRetrievalEvalSamples(
            @PathVariable UUID libraryId,
            @Valid @RequestBody ImportRetrievalEvalSamplesCommand command
    ) {
        return ApiResponse.ok(retrievalEvalService.importSamples(libraryId, command));
    }

    @Operation(summary = "从 sample-documents 引导黄金样本")
    @PostMapping("/retrieval-eval-samples/bootstrap-sample-documents")
    public ApiResponse<List<RetrievalEvalSampleResult>> bootstrapSampleDocuments(
            @PathVariable UUID libraryId,
            @RequestParam(defaultValue = "false") boolean replaceExisting
    ) {
        return ApiResponse.ok(retrievalEvalService.bootstrapSampleDocuments(libraryId, replaceExisting));
    }

    @Operation(summary = "根据已入库文档内容生成评测草稿", description = "默认未启用；绑定 expectedDocumentId 与 groundTruth 片段。入库成功时也会自动尝试生成。")
    @PostMapping("/retrieval-eval-samples/generate-drafts")
    public ApiResponse<List<RetrievalEvalSampleResult>> generateRetrievalEvalDrafts(
            @PathVariable UUID libraryId,
            @Valid @RequestBody(required = false) GenerateRetrievalEvalDraftsCommand command
    ) {
        return ApiResponse.ok(retrievalEvalService.generateDrafts(libraryId, command));
    }

    @Operation(summary = "查询 Recall@K 回归基线")
    @GetMapping("/retrieval-eval-baseline")
    public ApiResponse<RetrievalEvalBaselineResult> getRetrievalEvalBaseline(@PathVariable UUID libraryId) {
        return ApiResponse.ok(retrievalEvalService.getBaseline(libraryId));
    }

    @Operation(summary = "将评测运行设为回归基线")
    @PostMapping("/retrieval-evaluations/{evalRunId}/baseline")
    public ApiResponse<RetrievalEvalBaselineResult> pinRetrievalEvalBaseline(
            @PathVariable UUID libraryId,
            @PathVariable UUID evalRunId
    ) {
        return ApiResponse.ok(retrievalEvalService.pinBaseline(libraryId, evalRunId));
    }

    @Operation(summary = "启动批量召回评测", description = "对黄金集执行 Hit@K 评测并计算 Recall@K")
    @PostMapping("/retrieval-evaluations")
    public ApiResponse<RetrievalEvalRunResult> runRetrievalEvaluation(
            @PathVariable UUID libraryId,
            @Valid @RequestBody(required = false) CreateRetrievalEvalRunCommand command
    ) {
        CreateRetrievalEvalRunCommand effective = command == null
                ? new CreateRetrievalEvalRunCommand(null, null, true)
                : command;
        return ApiResponse.ok(retrievalEvalService.runEvaluation(libraryId, effective));
    }

    @Operation(summary = "查询批量召回评测历史")
    @GetMapping("/retrieval-evaluations")
    public ApiResponse<List<RetrievalEvalRunResult>> listRetrievalEvaluations(
            @PathVariable UUID libraryId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(retrievalEvalService.listEvaluations(libraryId, limit));
    }

    @Operation(summary = "查询批量召回评测详情")
    @GetMapping("/retrieval-evaluations/{evalRunId}")
    public ApiResponse<RetrievalEvalRunResult> getRetrievalEvaluation(
            @PathVariable UUID libraryId,
            @PathVariable UUID evalRunId
    ) {
        return ApiResponse.ok(retrievalEvalService.getEvaluation(libraryId, evalRunId));
    }

    private static boolean hasEvalCriteria(CreateLibraryRetrievalTestCommand command) {
        boolean hasDocuments = command.expectedDocumentIds() != null && !command.expectedDocumentIds().isEmpty();
        boolean hasSources = command.expectedSourceUris() != null
                && command.expectedSourceUris().stream().anyMatch(value -> value != null && !value.isBlank());
        boolean hasGroundTruth = command.groundTruthContexts() != null
                && command.groundTruthContexts().stream().anyMatch(value -> value != null && !value.isBlank());
        return hasDocuments || hasSources || hasGroundTruth;
    }

    @Operation(summary = "查询入库任务列表")
    @GetMapping("/ingestion-runs")
    public ApiResponse<List<IngestionRunResult>> listIngestionRuns(
            @PathVariable UUID libraryId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(catalogService.listIngestionRuns(libraryId, limit));
    }

    @Operation(summary = "查询入库任务文档级错误")
    @GetMapping("/ingestion-runs/{runId}/errors")
    public ApiResponse<List<IngestionDocumentErrorResult>> listIngestionRunErrors(
            @PathVariable UUID libraryId,
            @PathVariable UUID runId
    ) {
        return ApiResponse.ok(catalogService.listIngestionErrors(runId));
    }

    @Operation(summary = "查询入库任务文档级索引作业", description = "每个源文件对应一条 DocumentIndexJob，含阶段与状态")
    @GetMapping("/ingestion-runs/{runId}/jobs")
    public ApiResponse<List<DocumentIndexJobResult>> listIngestionRunJobs(
            @PathVariable UUID libraryId,
            @PathVariable UUID runId
    ) {
        return ApiResponse.ok(catalogService.listDocumentIndexJobs(libraryId, runId));
    }

    @Operation(summary = "上传文档并入库", description = "multipart 上传文件到 ObjectStorage 后写入当前 active 索引代次")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentUploadResult> uploadDocuments(
            @PathVariable UUID libraryId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) String documentProfileCode,
            @RequestParam(defaultValue = "true") boolean autoStart
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
        return ApiResponse.ok(documentService.uploadAndIngest(
                libraryId,
                candidates,
                documentProfileCode,
                autoStart
        ));
    }

    @Operation(summary = "查询文档列表", description = "分页返回当前 active 索引代次下的文档；默认 page=1、size=20，单页最多 100 条。可选 indexVersionId 指定代次")
    @GetMapping("/documents")
    public ApiResponse<PageResult<KnowledgeDocumentResult>> listDocuments(
            @PathVariable UUID libraryId,
            @RequestParam(required = false) UUID indexVersionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(documentService.page(libraryId, indexVersionId, page, size));
    }

    @Operation(summary = "查询文档详情")
    @GetMapping("/documents/{documentId}")
    public ApiResponse<KnowledgeDocumentResult> getDocument(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.ok(documentService.get(libraryId, documentId));
    }

    @Operation(summary = "预览原始文档", description = "返回原文二进制流，Content-Disposition 为 inline")
    @GetMapping("/documents/{documentId}/preview")
    public ResponseEntity<byte[]> previewDocument(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        return inlineDocument(documentService.preview(libraryId, documentId));
    }

    @Operation(summary = "下载原始文档", description = "返回原文二进制流，Content-Disposition 为 attachment")
    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        DocumentPreviewContent preview = documentService.preview(libraryId, documentId);
        String encodedFilename = URLEncoder.encode(preview.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(preview.mimeType()))
                .body(preview.content());
    }

    private static ResponseEntity<byte[]> inlineDocument(DocumentPreviewContent preview) {
        String encodedFilename = URLEncoder.encode(preview.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(preview.mimeType()))
                .body(preview.content());
    }

    @Operation(summary = "删除文档", description = "同步删除文档块与向量")
    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        documentService.delete(libraryId, documentId);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "批量删除文档", description = "同步删除所选文档及其块与向量；跳过不属于当前知识库或已不存在的 ID")
    @PostMapping("/documents/batch-delete")
    public ApiResponse<BatchDeleteDocumentsResult> batchDeleteDocuments(
            @PathVariable UUID libraryId,
            @RequestBody DeleteDocumentsCommand command
    ) {
        return ApiResponse.ok(documentService.deleteBatch(libraryId, command.documentIds()));
    }

    @Operation(summary = "重索引文档")
    @PostMapping("/documents/{documentId}/reindex")
    public ApiResponse<IngestionRunResult> reindexDocument(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.ok(documentService.reindex(libraryId, documentId));
    }

    @Operation(summary = "批量重索引失败文档")
    @PostMapping("/documents/reindex-failed")
    public ApiResponse<BatchReindexResult> reindexFailedDocuments(@PathVariable UUID libraryId) {
        return ApiResponse.ok(documentService.reindexFailed(libraryId));
    }

    @Operation(summary = "按 Document Profile 批量重索引")
    @PostMapping("/documents/reindex-by-profile")
    public ApiResponse<BatchReindexResult> reindexByProfile(
            @PathVariable UUID libraryId,
            @RequestParam String documentProfileCode
    ) {
        return ApiResponse.ok(documentService.reindexByDocumentProfile(libraryId, documentProfileCode));
    }

    @Operation(summary = "查询 content_hash 重复文档组")
    @GetMapping("/documents/duplicates")
    public ApiResponse<List<DocumentDuplicateGroupResult>> listDuplicateDocuments(@PathVariable UUID libraryId) {
        return ApiResponse.ok(documentService.listDuplicateGroups(libraryId));
    }

    @Operation(summary = "查询文档块列表", description = "分页返回文档检索分块（不含 document_summary 摘要层）；默认 page=1、size=20，单页最多 100 条")
    @GetMapping("/documents/{documentId}/chunks")
    public ApiResponse<PageResult<DocumentChunkResult>> pageDocumentChunks(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(catalogService.pageDocumentChunks(libraryId, documentId, page, size));
    }

    @Operation(summary = "查询文档入库 Pipeline Trace 定位信息", description = "返回最近一次入库 runId/traceId 与主线阶段 Span 查询入口；chunkCount 为当前检索分块数（不含 document_summary）")
    @GetMapping("/documents/{documentId}/pipeline-trace")
    public ApiResponse<DocumentPipelineTraceResult> getDocumentPipelineTrace(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId
    ) {
        return ApiResponse.ok(catalogService.getDocumentPipelineTrace(libraryId, documentId));
    }

    @Operation(summary = "更新文档块", description = "可编辑块内容（自动重向量化）或切换 retrievalEnabled 参与检索开关")
    @PutMapping("/documents/{documentId}/chunks/{chunkId}")
    public ApiResponse<DocumentChunkResult> updateDocumentChunk(
            @PathVariable UUID libraryId,
            @PathVariable UUID documentId,
            @PathVariable UUID chunkId,
            @Valid @RequestBody UpdateDocumentChunkCommand command
    ) {
        return ApiResponse.ok(documentChunkService.updateChunk(libraryId, documentId, chunkId, command));
    }
}
