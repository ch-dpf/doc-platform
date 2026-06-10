package com.knowbase.library.dto;

import com.knowbase.library.config.VectorLibraryConfig;
import jakarta.validation.constraints.NotBlank;

/**
 * 低风险更新：名称、描述、分块/清洗/向量化配置。
 * 不包含存储类型、元数据库、数据源模式等高风险字段。建仓流水线步骤固定全开。
 */
public record UpdateVectorLibrarySettingsRequest(
        @NotBlank String name, String description, VectorLibraryConfig config) {}
