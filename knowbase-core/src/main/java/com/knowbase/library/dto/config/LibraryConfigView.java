package com.knowbase.library.dto.config;

import com.knowbase.library.config.ParserEngineRule;
import com.knowbase.library.config.RetrievalRulesSettings;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** 库配置对外视图（基本信息、解析、索引管道、检索；接入策略为系统级配置）。 */
@Schema(description = "知识库配置视图（分节结构，对应库配置各 Tab）")
public record LibraryConfigView(
        @Schema(description = "配置版本号（每次分节保存后递增）", example = "1") int configVersion,
        @Schema(description = "元数据存储类型（只读）", example = "postgresql") String metadataDbType,
        @Schema(description = "标签列表", example = "[\"研发\", \"规范\"]") List<String> tags,
        @Schema(description = "解析配置") LibraryParsingDto parsing,
        @Schema(description = "分块向量化") LibraryIndexPipelineDto indexPipeline,
        @Schema(description = "检索规则") RetrievalRulesSettings retrieval,
        @Schema(description = "库默认分块档 ID；默认问答仅检索此档") String primaryChunkProfileId,
        @Schema(description = "是否允许采集侧使用非主档分块覆盖") boolean allowCustomChunkProfiles,
        @Schema(description = "单库最大活跃分块档数量") int maxActiveChunkProfiles) {}
