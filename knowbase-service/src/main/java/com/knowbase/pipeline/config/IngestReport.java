package com.knowbase.pipeline.config;

/**
 * v2 单文档入库质量报告（GATE-02 子集），持久化在 doc_metadata.ingest_report_json。
 */
public class IngestReport {

    private int rawChunkCount;
    private int filteredOutCount;
    private int finalChunkCount;
    private double avgChunkLength;
    private boolean headerOnlyRatioWarning;
    private int pipelineConfigVersion;
    private String contentFamily;
    private String chunkingStrategy;
    private String chunkingAdjustmentReason;
    private boolean multiGranularity;

    public IngestReport() {
    }

    public IngestReport(
            int rawChunkCount,
            int filteredOutCount,
            int finalChunkCount,
            double avgChunkLength,
            boolean headerOnlyRatioWarning,
            int pipelineConfigVersion) {
        this(
                rawChunkCount,
                filteredOutCount,
                finalChunkCount,
                avgChunkLength,
                headerOnlyRatioWarning,
                pipelineConfigVersion,
                null,
                null,
                null,
                false);
    }

    public IngestReport(
            int rawChunkCount,
            int filteredOutCount,
            int finalChunkCount,
            double avgChunkLength,
            boolean headerOnlyRatioWarning,
            int pipelineConfigVersion,
            String contentFamily,
            String chunkingStrategy,
            String chunkingAdjustmentReason) {
        this(
                rawChunkCount,
                filteredOutCount,
                finalChunkCount,
                avgChunkLength,
                headerOnlyRatioWarning,
                pipelineConfigVersion,
                contentFamily,
                chunkingStrategy,
                chunkingAdjustmentReason,
                false);
    }

    public IngestReport(
            int rawChunkCount,
            int filteredOutCount,
            int finalChunkCount,
            double avgChunkLength,
            boolean headerOnlyRatioWarning,
            int pipelineConfigVersion,
            String contentFamily,
            String chunkingStrategy,
            String chunkingAdjustmentReason,
            boolean multiGranularity) {
        this.rawChunkCount = rawChunkCount;
        this.filteredOutCount = filteredOutCount;
        this.finalChunkCount = finalChunkCount;
        this.avgChunkLength = avgChunkLength;
        this.headerOnlyRatioWarning = headerOnlyRatioWarning;
        this.pipelineConfigVersion = pipelineConfigVersion;
        this.contentFamily = contentFamily;
        this.chunkingStrategy = chunkingStrategy;
        this.chunkingAdjustmentReason = chunkingAdjustmentReason;
        this.multiGranularity = multiGranularity;
    }

    public int getRawChunkCount() {
        return rawChunkCount;
    }

    public void setRawChunkCount(int rawChunkCount) {
        this.rawChunkCount = rawChunkCount;
    }

    public int getFilteredOutCount() {
        return filteredOutCount;
    }

    public void setFilteredOutCount(int filteredOutCount) {
        this.filteredOutCount = filteredOutCount;
    }

    public int getFinalChunkCount() {
        return finalChunkCount;
    }

    public void setFinalChunkCount(int finalChunkCount) {
        this.finalChunkCount = finalChunkCount;
    }

    public double getAvgChunkLength() {
        return avgChunkLength;
    }

    public void setAvgChunkLength(double avgChunkLength) {
        this.avgChunkLength = avgChunkLength;
    }

    public boolean isHeaderOnlyRatioWarning() {
        return headerOnlyRatioWarning;
    }

    public void setHeaderOnlyRatioWarning(boolean headerOnlyRatioWarning) {
        this.headerOnlyRatioWarning = headerOnlyRatioWarning;
    }

    public int getPipelineConfigVersion() {
        return pipelineConfigVersion;
    }

    public void setPipelineConfigVersion(int pipelineConfigVersion) {
        this.pipelineConfigVersion = pipelineConfigVersion;
    }

    public String getContentFamily() {
        return contentFamily;
    }

    public void setContentFamily(String contentFamily) {
        this.contentFamily = contentFamily;
    }

    public String getChunkingStrategy() {
        return chunkingStrategy;
    }

    public void setChunkingStrategy(String chunkingStrategy) {
        this.chunkingStrategy = chunkingStrategy;
    }

    public String getChunkingAdjustmentReason() {
        return chunkingAdjustmentReason;
    }

    public void setChunkingAdjustmentReason(String chunkingAdjustmentReason) {
        this.chunkingAdjustmentReason = chunkingAdjustmentReason;
    }

    public boolean isMultiGranularity() {
        return multiGranularity;
    }

    public void setMultiGranularity(boolean multiGranularity) {
        this.multiGranularity = multiGranularity;
    }
}
