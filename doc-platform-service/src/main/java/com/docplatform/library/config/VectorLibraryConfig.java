package com.docplatform.library.config;

import com.docplatform.vector.chunk.ChunkingStrategy;

import java.util.List;

/**
 * 向量库级配置（持久化在 vector_library.config_json）。
 */
public class VectorLibraryConfig {

    private String storageType = "minio";
    private String storagePathPrefix = "";
    private String localBasePath = "./data/documents";
    private String metadataDbType = "postgresql";

    private String embeddingProvider = "ollama";
    private String embeddingModel = "nomic-embed-text";
    private int embeddingDimension = 768;

    private ChunkingStrategy chunkingStrategy = ChunkingStrategy.PARAGRAPH_FIRST;
    private int chunkSize = 600;
    private int chunkOverlap = 100;
    private int minChunkSize = 80;
    private int maxChunkSize = 1200;
    private int minParagraphLength = 30;
    private boolean normalizeBeforeChunk = true;

    private boolean textNormalizationEnabled = true;
    private TextNormalizationSettings textNormalization = new TextNormalizationSettings();
    /** 首选数据源：upload=本地文件，crawl=线上采集，both=两者 */
    private String ingestSourceMode = "upload";
    private List<String> allowedMimeTypes;

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getStoragePathPrefix() {
        return storagePathPrefix;
    }

    public void setStoragePathPrefix(String storagePathPrefix) {
        this.storagePathPrefix = storagePathPrefix;
    }

    public String getLocalBasePath() {
        return localBasePath;
    }

    public void setLocalBasePath(String localBasePath) {
        this.localBasePath = localBasePath;
    }

    public String getMetadataDbType() {
        return metadataDbType;
    }

    public void setMetadataDbType(String metadataDbType) {
        this.metadataDbType = metadataDbType;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(int embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public ChunkingStrategy getChunkingStrategy() {
        return chunkingStrategy;
    }

    public void setChunkingStrategy(ChunkingStrategy chunkingStrategy) {
        this.chunkingStrategy = chunkingStrategy;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getMinChunkSize() {
        return minChunkSize;
    }

    public void setMinChunkSize(int minChunkSize) {
        this.minChunkSize = minChunkSize;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public int getMinParagraphLength() {
        return minParagraphLength;
    }

    public void setMinParagraphLength(int minParagraphLength) {
        this.minParagraphLength = minParagraphLength;
    }

    public boolean isNormalizeBeforeChunk() {
        return normalizeBeforeChunk;
    }

    public void setNormalizeBeforeChunk(boolean normalizeBeforeChunk) {
        this.normalizeBeforeChunk = normalizeBeforeChunk;
    }

    public boolean isTextNormalizationEnabled() {
        return textNormalizationEnabled;
    }

    public void setTextNormalizationEnabled(boolean textNormalizationEnabled) {
        this.textNormalizationEnabled = textNormalizationEnabled;
    }

    public TextNormalizationSettings getTextNormalization() {
        return textNormalization;
    }

    public void setTextNormalization(TextNormalizationSettings textNormalization) {
        this.textNormalization = textNormalization;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public String getIngestSourceMode() {
        return ingestSourceMode;
    }

    public void setIngestSourceMode(String ingestSourceMode) {
        this.ingestSourceMode = ingestSourceMode;
    }
}
