package com.knowbase.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** minio | local-fs */
    private String type = "minio";

    /** 可选：统一加到对象 key 前的路径前缀，如 archive/ */
    private String pathPrefix = "";

    private final Local local = new Local();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
    }

    public Local getLocal() {
        return local;
    }

    public String normalizeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return objectKey;
        }
        String prefix = pathPrefix == null ? "" : pathPrefix.strip();
        if (prefix.isEmpty()) {
            return objectKey;
        }
        if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
        }
        if (objectKey.startsWith(prefix)) {
            return objectKey;
        }
        return prefix + objectKey;
    }

    public static class Local {
        /** 本地文件系统根目录（type=local-fs 时生效） */
        private String basePath = "./data/documents";

        public String getBasePath() {
            return basePath;
        }

        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
    }
}
