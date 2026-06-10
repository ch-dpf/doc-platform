package com.knowbase.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.util.List;

@ConfigurationProperties(prefix = "ingest")
public class IngestProperties {

    private List<String> allowedMimeTypes = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/markdown",
            "text/x-markdown",
            "text/x-web-markdown");

    private DataSize maxFileSize = DataSize.ofMegabytes(50);

    private int maxBatchFiles = 20;

    /** 超过该大小的文件走异步上传任务 */
    private DataSize asyncUploadThreshold = DataSize.ofMegabytes(5);

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public DataSize getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(DataSize maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSize.toBytes();
    }

    public int getMaxBatchFiles() {
        return maxBatchFiles;
    }

    public void setMaxBatchFiles(int maxBatchFiles) {
        this.maxBatchFiles = maxBatchFiles;
    }

    public DataSize getAsyncUploadThreshold() {
        return asyncUploadThreshold;
    }

    public void setAsyncUploadThreshold(DataSize asyncUploadThreshold) {
        this.asyncUploadThreshold = asyncUploadThreshold;
    }

    public long getAsyncUploadThresholdBytes() {
        return asyncUploadThreshold.toBytes();
    }
}
