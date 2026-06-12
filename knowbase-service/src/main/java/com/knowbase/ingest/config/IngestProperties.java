package com.knowbase.ingest.config;

import com.knowbase.library.config.VersionPolicySettings;
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

    /** 系统级：同名文件重复上传时的版本策略（全库统一） */
    private IngestVersionPolicyProperties versionPolicy = new IngestVersionPolicyProperties();

    /** 系统级：入库审核模式。auto | manual-review */
    private String ingestReviewMode = "auto";

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

    public IngestVersionPolicyProperties getVersionPolicy() {
        return versionPolicy;
    }

    public void setVersionPolicy(IngestVersionPolicyProperties versionPolicy) {
        this.versionPolicy = versionPolicy;
    }

    public String getIngestReviewMode() {
        return ingestReviewMode;
    }

    public void setIngestReviewMode(String ingestReviewMode) {
        this.ingestReviewMode = ingestReviewMode;
    }

    public boolean requiresManualReview() {
        return "manual-review".equalsIgnoreCase(ingestReviewMode);
    }

    public VersionPolicySettings toVersionPolicySettings() {
        VersionPolicySettings settings = new VersionPolicySettings();
        IngestVersionPolicyProperties policy = versionPolicy != null ? versionPolicy : new IngestVersionPolicyProperties();
        settings.setEnabled(policy.isEnabled());
        settings.setUpdateStrategy(policy.getUpdateStrategy());
        return settings;
    }

    public String resolvedVersionUpdateStrategy() {
        IngestVersionPolicyProperties policy = versionPolicy != null ? versionPolicy : new IngestVersionPolicyProperties();
        return policy.isEnabled() ? policy.getUpdateStrategy() : "overwrite";
    }
}
