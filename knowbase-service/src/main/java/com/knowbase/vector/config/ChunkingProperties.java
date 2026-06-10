package com.knowbase.vector.config;

import com.knowbase.vector.chunk.ChunkingStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chunking")
public class ChunkingProperties {

    private ChunkingStrategy strategy = ChunkingStrategy.PARAGRAPH_FIRST;
    private int chunkSize = 600;
    private int overlap = 100;
    private int minChunkSize = 80;
    private int maxChunkSize = 1200;
    private int minParagraphLength = 30;
    private boolean normalizeBeforeChunk = true;
    /** 相邻句 embedding 余弦相似度低于该值时切开（semantic 策略） */
    private double semanticSimilarityThreshold = 0.72;

    public ChunkingStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(ChunkingStrategy strategy) {
        this.strategy = strategy;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }

    public void setOverlap(int overlap) {
        this.overlap = overlap;
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
}
