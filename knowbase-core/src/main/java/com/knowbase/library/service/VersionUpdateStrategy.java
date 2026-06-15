package com.knowbase.library.service;

import com.knowbase.library.config.VersionPolicySettings;

/**
 * 系统级版本更新策略（ingest.version-policy）。
 */
public enum VersionUpdateStrategy {

    /** 同 checksum 重复上传：覆盖当前版本，不递增 version */
    OVERWRITE,
    /** 同 checksum 重复上传：视为幂等，跳过重新解析 */
    INCREMENTAL,
    /** 同 checksum 重复上传：version+1，保留历史对象 */
    KEEP_HISTORY;

    public static VersionUpdateStrategy from(VersionPolicySettings policy) {
        if (policy == null || !policy.isEnabled()) {
            return OVERWRITE;
        }
        String mode = policy.getUpdateStrategy();
        if (mode == null || mode.isBlank()) {
            return KEEP_HISTORY;
        }
        return switch (mode.trim().toLowerCase()) {
            case "overwrite" -> OVERWRITE;
            case "incremental" -> INCREMENTAL;
            case "keep-history" -> KEEP_HISTORY;
            default -> KEEP_HISTORY;
        };
    }
}
