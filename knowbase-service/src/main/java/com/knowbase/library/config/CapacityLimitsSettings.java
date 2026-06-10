package com.knowbase.library.config;

public class CapacityLimitsSettings {

    private int maxDocuments = 10_000;
    private long maxTotalSizeBytes = 10L * 1024 * 1024 * 1024;
    private int maxChunkEntries = 500_000;

    public int getMaxDocuments() {
        return maxDocuments;
    }

    public void setMaxDocuments(int maxDocuments) {
        this.maxDocuments = maxDocuments;
    }

    public long getMaxTotalSizeBytes() {
        return maxTotalSizeBytes;
    }

    public void setMaxTotalSizeBytes(long maxTotalSizeBytes) {
        this.maxTotalSizeBytes = maxTotalSizeBytes;
    }

    public int getMaxChunkEntries() {
        return maxChunkEntries;
    }

    public void setMaxChunkEntries(int maxChunkEntries) {
        this.maxChunkEntries = maxChunkEntries;
    }
}
