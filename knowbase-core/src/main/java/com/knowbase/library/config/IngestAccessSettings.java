package com.knowbase.library.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据源与接入策略（创建时设定，非立即上传）。
 * 文档采集固定同时支持单文件/多文件上传与文件夹批量接入，{@link #accessMode} 仅作兼容保留，不可配置。
 */
public class IngestAccessSettings {

    /** 固定值：文件上传 + 文件夹批量（不可通过 UI/API 修改） */
    public static final String FIXED_ACCESS_MODE = "upload-and-folder";

    private String accessMode = FIXED_ACCESS_MODE;
    private List<String> supportedFileTypes = defaultFileTypes();
    private CapacityLimitsSettings capacityLimits = new CapacityLimitsSettings();
    private VersionPolicySettings versionPolicy = new VersionPolicySettings();

    public static List<String> defaultFileTypes() {
        return new ArrayList<>(List.of("pdf", "word", "txt", "markdown", "excel"));
    }

    public String getAccessMode() {
        return accessMode;
    }

    public void setAccessMode(String accessMode) {
        this.accessMode = accessMode;
    }

    public List<String> getSupportedFileTypes() {
        return supportedFileTypes;
    }

    public void setSupportedFileTypes(List<String> supportedFileTypes) {
        this.supportedFileTypes = supportedFileTypes;
    }

    public CapacityLimitsSettings getCapacityLimits() {
        return capacityLimits;
    }

    public void setCapacityLimits(CapacityLimitsSettings capacityLimits) {
        this.capacityLimits = capacityLimits;
    }

    public VersionPolicySettings getVersionPolicy() {
        return versionPolicy;
    }

    public void setVersionPolicy(VersionPolicySettings versionPolicy) {
        this.versionPolicy = versionPolicy;
    }
}
