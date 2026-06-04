package com.docplatform.library.config;

/**
 * 向量库解析后的向量化配置（用于入库与检索）。
 */
public record EmbeddingSpec(String provider, String model, int dimension) {}
