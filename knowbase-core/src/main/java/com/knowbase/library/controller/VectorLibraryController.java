package com.knowbase.library.controller;

import com.knowbase.ingest.dto.PageResponse;
import com.knowbase.library.dto.ArchiveCandidatesResponse;
import com.knowbase.library.dto.ArchiveChunkProfileRequest;
import com.knowbase.library.dto.ArchiveChunkProfileResponse;
import com.knowbase.library.dto.ChunkProfileBackfillResponse;
import com.knowbase.library.dto.TemporalMetadataBackfillResponse;
import com.knowbase.library.dto.ChunkProfileSummaryResponse;
import com.knowbase.library.dto.SetPrimaryChunkProfileRequest;
import com.knowbase.library.dto.UpdateChunkGovernanceRequest;
import com.knowbase.library.dto.ChunkStrategySummaryRow;
import com.knowbase.library.dto.CreateVectorLibraryRequest;
import com.knowbase.library.dto.DeleteVectorLibraryResponse;
import com.knowbase.library.dto.UpdateLibraryBasicRequest;
import com.knowbase.library.dto.UpdateLibraryIndexPipelineRequest;
import com.knowbase.library.dto.UpdateLibraryParsingRequest;
import com.knowbase.library.dto.UpdateLibraryRetrievalRequest;
import com.knowbase.library.dto.VectorLibraryListItemResponse;
import com.knowbase.library.dto.VectorLibraryListQuery;
import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.dto.VectorLibraryUpdateResponse;
import com.knowbase.library.dto.CleanupOrphanChunksResponse;
import com.knowbase.library.dto.MigrateToPrimaryRequest;
import com.knowbase.library.dto.MigrateToPrimaryResponse;
import com.knowbase.library.dto.MigrationCandidatesResponse;
import com.knowbase.library.service.ChunkProfileArchiveService;
import com.knowbase.library.service.ChunkProfileCleanupService;
import com.knowbase.library.service.ChunkProfileMigrationService;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.library.support.LibraryChunkStrategySummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "知识库管理", description = "知识库 CRUD 与分节配置（basic / index-pipeline / retrieval）；接入策略见 ingest 系统配置")
@RestController
@RequestMapping("/api/v1/vector-libraries")
public class VectorLibraryController {

    private final VectorLibraryService libraryService;
    private final LibraryChunkStrategySummaryService chunkStrategySummaryService;
    private final ChunkProfileArchiveService chunkProfileArchiveService;
    private final ChunkProfileMigrationService chunkProfileMigrationService;
    private final ChunkProfileCleanupService chunkProfileCleanupService;

    public VectorLibraryController(
            VectorLibraryService libraryService,
            LibraryChunkStrategySummaryService chunkStrategySummaryService,
            ChunkProfileArchiveService chunkProfileArchiveService,
            ChunkProfileMigrationService chunkProfileMigrationService,
            ChunkProfileCleanupService chunkProfileCleanupService) {
        this.libraryService = libraryService;
        this.chunkStrategySummaryService = chunkStrategySummaryService;
        this.chunkProfileArchiveService = chunkProfileArchiveService;
        this.chunkProfileMigrationService = chunkProfileMigrationService;
        this.chunkProfileCleanupService = chunkProfileCleanupService;
    }

    @Operation(
            summary = "知识库列表（分页）",
            description = "按租户查询。支持名称/描述关键字与 `config_json.tags` 标签筛选。列表项为轻量字段，不含完整 `libraryConfig`。")
    @ApiResponse(responseCode = "200", description = "分页列表")
    @GetMapping
    public PageResponse<VectorLibraryListItemResponse> list(
            @Parameter(description = "租户 ID", required = true, example = "demo") @RequestParam String tenantId,
            @Parameter(description = "名称或描述关键字（模糊匹配）") @RequestParam(required = false) String keyword,
            @Parameter(description = "按标签精确筛选") @RequestParam(required = false) String tag,
            @Parameter(description = "页码，从 1 开始", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数", example = "20") @RequestParam(defaultValue = "20") int size) {
        return libraryService.list(new VectorLibraryListQuery(tenantId, keyword, tag, page, size));
    }

    @Operation(summary = "租户下知识库标签索引", description = "汇总当前租户所有知识库 `libraryConfig.tags` 中已使用的标签，供筛选与管理。")
    @ApiResponse(responseCode = "200", description = "去重后的标签列表")
    @GetMapping("/meta/tags")
    public List<String> listTags(
            @Parameter(description = "租户 ID", required = true, example = "demo") @RequestParam String tenantId) {
        return libraryService.listDistinctTags(tenantId);
    }

    @Operation(
            summary = "知识库详情",
            description = """
                    返回知识库元数据与分节 `libraryConfig`。
                    对外视图按库配置 Tab 组织，不包含容量限制等平台内部字段。
                    `documentCount` / `chunkCount` 为实时统计。
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "详情", content = @Content(schema = @Schema(implementation = VectorLibraryResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @GetMapping("/{libraryId}")
    public VectorLibraryResponse get(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId) {
        return libraryService.get(libraryId);
    }

    @Operation(
            summary = "分块策略摘要（只读）",
            description = "按系统支持的文件类型展示 MIME 解析要点与库级统一分块策略，含库级父子块/分隔符影响说明。")
    @ApiResponse(responseCode = "200", description = "策略摘要列表")
    @GetMapping("/{libraryId}/chunk-strategy-summary")
    public List<ChunkStrategySummaryRow> chunkStrategySummary(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId) {
        libraryService.get(libraryId);
        return chunkStrategySummaryService.summarize(libraryId);
    }

    @Operation(
            summary = "新增知识库",
            description = """
                    原子创建：提交基本信息，并可一次附带 indexPipeline / parsing / retrieval 分节。
                    省略分节时由服务端写入产品默认 config_json；编辑已有库仍使用各分节 PUT。
                    初始 `configVersion=1`。
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "创建成功", content = @Content(schema = @Schema(implementation = VectorLibraryResponse.class))),
            @ApiResponse(responseCode = "400", description = "参数校验失败")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VectorLibraryResponse create(@Valid @RequestBody CreateVectorLibraryRequest request) {
        return libraryService.create(request);
    }

    @Operation(
            summary = "更新基本信息",
            description = "对应库配置 Tab「基本信息」。更新名称、描述、标签；`configVersion` 递增。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功", content = @Content(schema = @Schema(implementation = VectorLibraryUpdateResponse.class))),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PutMapping("/{libraryId}/basic")
    public VectorLibraryUpdateResponse updateBasic(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody UpdateLibraryBasicRequest request) {
        return libraryService.updateBasic(libraryId, request);
    }

    @Operation(
            summary = "更新分块向量化配置",
            description = """
                    对应库配置 Tab「分块向量化」。
                    库级含分块策略、分块大小、分块重叠与 Embedding；最小/最大分块等合并规则由系统 chunking.* 决定；MIME 仅影响解析与清洗。
                    **锁定规则**：当 `chunkCount > 0` 时返回 409，拒绝修改（避免与已入库向量不一致）。
                    空库首次入库前可自由调整；变更 Embedding 后 `warnings` 可能提示补偿重索引。
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "409", description = "索引管道已锁定（库内已有向量分块）"),
            @ApiResponse(responseCode = "400", description = "参数校验失败或不支持的 Embedding 提供方"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PutMapping("/{libraryId}/index-pipeline")
    public VectorLibraryUpdateResponse updateIndexPipeline(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody UpdateLibraryIndexPipelineRequest request) {
        return libraryService.updateIndexPipeline(libraryId, request);
    }

    @Operation(
            summary = "更新解析配置",
            description = "对应库配置 Tab「解析配置」。按文件类型选择内置解析器；变更后已有文档需重新解析/重索引。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PutMapping("/{libraryId}/parsing")
    public VectorLibraryUpdateResponse updateParsing(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody UpdateLibraryParsingRequest request) {
        return libraryService.updateParsing(libraryId, request);
    }

    @Operation(
            summary = "更新检索配置",
            description = "对应库配置 Tab「检索」。可独立更新混合检索、重排序、相似度阈值等，不影响索引管道。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PutMapping("/{libraryId}/retrieval")
    public VectorLibraryUpdateResponse updateRetrieval(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody UpdateLibraryRetrievalRequest request) {
        return libraryService.updateRetrieval(libraryId, request);
    }

    @Operation(
            summary = "列出知识库活跃分块档",
            description = "返回各分块档的文档数、分块数及是否为主档（默认问答检索范围）。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @GetMapping("/{libraryId}/chunk-profiles")
    public List<ChunkProfileSummaryResponse> listChunkProfiles(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId) {
        return libraryService.listChunkProfiles(libraryId);
    }

    @Operation(
            summary = "设置库主分块档",
            description = "将已有活跃分块档设为主档；默认问答/检索将仅覆盖此档。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "400", description = "分块档不存在"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PutMapping("/{libraryId}/chunk-profiles/primary")
    public VectorLibraryResponse setPrimaryChunkProfile(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody SetPrimaryChunkProfileRequest request) {
        return libraryService.setPrimaryChunkProfile(libraryId, request);
    }

    @Operation(
            summary = "更新分块档治理策略",
            description = "控制是否允许采集覆盖分块，以及单库最大活跃分块档数量。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PutMapping("/{libraryId}/chunk-governance")
    public VectorLibraryResponse updateChunkGovernance(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody UpdateChunkGovernanceRequest request) {
        return libraryService.updateChunkGovernance(libraryId, request);
    }

    @Operation(
            summary = "回填历史分块档 ID",
            description = "为缺少 chunk_profile_id 的文档与分块 metadata 补写指纹。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "回填完成"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PostMapping("/{libraryId}/chunk-profiles/backfill")
    public ChunkProfileBackfillResponse backfillChunkProfiles(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId) {
        return libraryService.backfillChunkProfiles(libraryId);
    }

    @Operation(
            summary = "回填历史分块时间元数据",
            description = "为缺少 periodYear/submitter 等字段的存量分块补写时间元数据，便于时间感知检索预过滤。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "回填完成"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PostMapping("/{libraryId}/temporal-metadata/backfill")
    public TemporalMetadataBackfillResponse backfillTemporalMetadata(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @RequestParam(required = false) String tenantId) {
        return libraryService.backfillTemporalMetadata(libraryId, tenantId);
    }

    @Operation(
            summary = "迁移到主档候选统计",
            description = "统计不在当前主分块档、但可重索引迁移的已解析文档。")
    @GetMapping("/{libraryId}/chunk-profiles/migration-candidates")
    public MigrationCandidatesResponse migrationCandidates(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @RequestParam String tenantId) {
        return chunkProfileMigrationService.getMigrationCandidates(libraryId, tenantId);
    }

    @Operation(
            summary = "一键迁移到主档",
            description = "对非主档已解析文档批量重索引，使其迁移到当前主分块档指纹。")
    @PostMapping("/{libraryId}/chunk-profiles/migrate-to-primary")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public MigrateToPrimaryResponse migrateToPrimary(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody MigrateToPrimaryRequest request) {
        return chunkProfileMigrationService.scheduleMigrateToPrimary(libraryId, request.tenantId());
    }

    @Operation(
            summary = "清理空档孤儿分块",
            description = "删除无活跃文档引用的非主档向量分块（迁移后自动执行，亦可手动触发）。")
    @PostMapping("/{libraryId}/chunk-profiles/cleanup-orphan-chunks")
    public CleanupOrphanChunksResponse cleanupOrphanChunks(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @RequestParam String tenantId) {
        return chunkProfileCleanupService.cleanupOrphanNonPrimaryChunks(libraryId, tenantId);
    }

    @Operation(
            summary = "归档候选预览",
            description = "列出将被打归档软删除的文档（最多预览 20 条）。")
    @GetMapping("/{libraryId}/chunk-profiles/archive-candidates")
    public ArchiveCandidatesResponse archiveCandidates(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @RequestParam String tenantId,
            @RequestParam String chunkProfileId) {
        return chunkProfileArchiveService.listArchiveCandidates(libraryId, tenantId, chunkProfileId);
    }

    @Operation(
            summary = "归档分块档",
            description = "软删除该档下全部文档并清理向量；禁止归档主档。")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "任务已提交"),
            @ApiResponse(responseCode = "400", description = "主档不可归档或分块档不存在"),
            @ApiResponse(responseCode = "404", description = "知识库不存在")
    })
    @PostMapping("/{libraryId}/chunk-profiles/archive")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ArchiveChunkProfileResponse archiveChunkProfile(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Valid @RequestBody ArchiveChunkProfileRequest request) {
        return chunkProfileArchiveService.scheduleArchive(
                libraryId, request.tenantId(), request.chunkProfileId());
    }

    @Operation(
            summary = "删除知识库",
            description = "永久删除知识库及其文档、向量分块与存储对象。系统默认库不可删除。需传 tenantId 校验归属。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "知识库不存在"),
            @ApiResponse(responseCode = "400", description = "默认库不可删除或租户不匹配")
    })
    @DeleteMapping("/{libraryId}")
    public DeleteVectorLibraryResponse delete(
            @Parameter(description = "知识库 ID", required = true) @PathVariable UUID libraryId,
            @Parameter(description = "租户 ID（校验归属）", required = true, example = "demo") @RequestParam String tenantId) {
        return libraryService.delete(libraryId, tenantId);
    }
}
