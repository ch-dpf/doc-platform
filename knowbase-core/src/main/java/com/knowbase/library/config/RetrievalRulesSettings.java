package com.knowbase.library.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "检索规则（混合检索、重排序、相似度阈值等）")
public class RetrievalRulesSettings {

    @Schema(description = "是否启用混合检索（向量 + 关键字）", example = "false")
    private boolean hybridSearchEnabled = false;
    @Schema(description = "是否启用重排序", example = "false")
    private boolean rerankEnabled = false;
    @Schema(description = "Rerank 模型名称")
    private String rerankModel = "";
    @Schema(description = "元数据过滤字段列表", example = "[\"department\", \"docType\"]")
    private List<String> metadataFilterFields = new ArrayList<>();
    @Schema(description = "相似度阈值（0 表示使用全局默认）", example = "0.0")
    private double similarityThreshold = 0.0;
    @Min(1)
    @Max(30)
    @Schema(description = "默认召回条数 Top K（问答会话可覆盖）", example = "12")
    private int defaultTopK = 12;
    @Schema(description = "是否在检索结果中保留分块元数据", example = "true")
    private boolean retainChunkMetadata = true;

    public boolean isHybridSearchEnabled() {
        return hybridSearchEnabled;
    }

    public void setHybridSearchEnabled(boolean hybridSearchEnabled) {
        this.hybridSearchEnabled = hybridSearchEnabled;
    }

    public boolean isRerankEnabled() {
        return rerankEnabled;
    }

    public void setRerankEnabled(boolean rerankEnabled) {
        this.rerankEnabled = rerankEnabled;
    }

    public String getRerankModel() {
        return rerankModel;
    }

    public void setRerankModel(String rerankModel) {
        this.rerankModel = rerankModel;
    }

    public List<String> getMetadataFilterFields() {
        return metadataFilterFields;
    }

    public void setMetadataFilterFields(List<String> metadataFilterFields) {
        this.metadataFilterFields = metadataFilterFields;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public boolean isRetainChunkMetadata() {
        return retainChunkMetadata;
    }

    public void setRetainChunkMetadata(boolean retainChunkMetadata) {
        this.retainChunkMetadata = retainChunkMetadata;
    }
}
