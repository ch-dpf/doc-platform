package com.knowbase.ingest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "上传约束（系统级接入策略 + 库级解析 OCR 等运行时信息）")
public record UploadConstraintsResponse(
        @Schema(
                description = "系统级支持的文件类型标识（来自 ingest.allowed-mime-types）",
                example = "[\"pdf\", \"word\", \"txt\", \"markdown\", \"excel\"]")
        List<String> supportedFileTypes,
        @Schema(description = "系统级允许的 MIME 类型") List<String> allowedMimeTypes,
        long maxFileSizeBytes,
        String maxFileSizeDisplay,
        int maxBatchFiles,
        String storageType,
        @Schema(description = "固定为 upload") String ingestSourceMode,
        boolean uploadAllowed,
        boolean collectAllowed,
        boolean ocrEnabled,
        boolean ocrEngineAvailable,
        @Schema(description = "系统级 ingest.ingest-review-mode 是否为 manual-review") boolean manualReviewRequired,
        @Schema(description = "系统级 ingest.ingest-review-mode", allowableValues = {"auto", "manual-review"})
        String ingestReviewMode,
        @Schema(description = "系统级 ingest.version-policy.enabled") boolean versionPolicyEnabled,
        @Schema(
                description = "系统级版本更新策略（enabled=false 时为 overwrite）",
                allowableValues = {"overwrite", "incremental", "keep-history"})
        String versionUpdateStrategy,
        @Schema(description = "库默认分块档 ID，默认问答仅检索此档") String primaryChunkProfileId,
        @Schema(description = "是否允许采集侧使用非主档分块覆盖") boolean chunkOverrideAllowed,
        @Schema(description = "当前库活跃分块档数量") int activeProfileCount,
        @Schema(description = "单库最大活跃分块档数量") int maxActiveChunkProfiles) {}
