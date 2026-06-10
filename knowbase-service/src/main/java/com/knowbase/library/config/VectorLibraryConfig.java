package com.knowbase.library.config;

import com.knowbase.vector.chunk.ChunkingStrategy;

import java.util.List;

/**
 * 向量库级配置（持久化在 vector_library.config_json）。
 */
public class VectorLibraryConfig {

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
    private double semanticSimilarityThreshold = 0.0;

    private boolean textNormalizationEnabled = true;
    private TextNormalizationSettings textNormalization = new TextNormalizationSettings();
    /** 首选数据源：upload=本地文件（二期默认）；crawl/both 仅兼容历史库 */
    private String ingestSourceMode = "upload";
    private List<String> allowedMimeTypes;

    /** 二期：配置快照版本，每次规则变更递增 */
    private int configVersion = 1;
    private List<String> tags = new java.util.ArrayList<>();
    /** quick | advanced */
    private String wizardMode = "quick";
    /** Phase 3 建库预设 id；custom 或空表示手调 */
    private String libraryPresetId;
    private IngestAccessSettings ingestAccess = new IngestAccessSettings();
    private ParsingRulesSettings parsing = new ParsingRulesSettings();
    private CleaningRulesSettings cleaning = new CleaningRulesSettings();
    private RetrievalRulesSettings retrieval = new RetrievalRulesSettings();
    private GovernanceRulesSettings governance = new GovernanceRulesSettings();

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

    public double getSemanticSimilarityThreshold() {
        return semanticSimilarityThreshold;
    }

    public void setSemanticSimilarityThreshold(double semanticSimilarityThreshold) {
        this.semanticSimilarityThreshold = semanticSimilarityThreshold;
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

    public int getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getWizardMode() {
        return wizardMode;
    }

    public void setWizardMode(String wizardMode) {
        this.wizardMode = wizardMode;
    }

    public String getLibraryPresetId() {
        return libraryPresetId;
    }

    public void setLibraryPresetId(String libraryPresetId) {
        this.libraryPresetId = libraryPresetId;
    }

    public IngestAccessSettings getIngestAccess() {
        return ingestAccess;
    }

    public void setIngestAccess(IngestAccessSettings ingestAccess) {
        this.ingestAccess = ingestAccess;
    }

    public ParsingRulesSettings getParsing() {
        return parsing;
    }

    public void setParsing(ParsingRulesSettings parsing) {
        this.parsing = parsing;
    }

    public CleaningRulesSettings getCleaning() {
        return cleaning;
    }

    public void setCleaning(CleaningRulesSettings cleaning) {
        this.cleaning = cleaning;
    }

    public RetrievalRulesSettings getRetrieval() {
        return retrieval;
    }

    public void setRetrieval(RetrievalRulesSettings retrieval) {
        this.retrieval = retrieval;
    }

    public GovernanceRulesSettings getGovernance() {
        return governance;
    }

    public void setGovernance(GovernanceRulesSettings governance) {
        this.governance = governance;
    }
}
