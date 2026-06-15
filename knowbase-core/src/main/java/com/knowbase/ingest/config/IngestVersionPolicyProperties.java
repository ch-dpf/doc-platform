package com.knowbase.ingest.config;

/**
 * 系统级版本与重复文档策略（application.yml → ingest.version-policy）。
 * 全库统一：同名文件重复上传时的处理方式。
 */
public class IngestVersionPolicyProperties {

    /** 是否启用版本策略；false 时等同 overwrite */
    private boolean enabled = true;
    /** overwrite | incremental | keep-history */
    private String updateStrategy = "keep-history";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUpdateStrategy() {
        return updateStrategy;
    }

    public void setUpdateStrategy(String updateStrategy) {
        this.updateStrategy = updateStrategy;
    }
}
