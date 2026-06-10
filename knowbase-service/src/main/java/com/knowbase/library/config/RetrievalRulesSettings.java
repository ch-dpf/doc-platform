package com.knowbase.library.config;

import java.util.ArrayList;
import java.util.List;

public class RetrievalRulesSettings {

    private boolean hybridSearchEnabled = false;
    private boolean rerankEnabled = false;
    private String rerankModel = "";
    private List<String> metadataFilterFields = new ArrayList<>();
    private double similarityThreshold = 0.0;
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

    public boolean isRetainChunkMetadata() {
        return retainChunkMetadata;
    }

    public void setRetainChunkMetadata(boolean retainChunkMetadata) {
        this.retainChunkMetadata = retainChunkMetadata;
    }
}
