package com.knowbase.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowbase")
public class KnowbaseProperties {

    private boolean enabled = true;

    private Web web = new Web();

    private Tenant tenant = new Tenant();

    private Persistence persistence = new Persistence();

    private Ollama ollama = new Ollama();

    private Tokenizer tokenizer = new Tokenizer();

    private Ingestion ingestion = new Ingestion();

    private Storage storage = new Storage();

    private Security security = new Security();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Web getWeb() {
        return web;
    }

    public void setWeb(Web web) {
        this.web = web;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Ingestion getIngestion() {
        return ingestion;
    }

    public void setIngestion(Ingestion ingestion) {
        this.ingestion = ingestion;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public Security getSecurity() {
        return security;
    }

    public void setSecurity(Security security) {
        this.security = security;
    }

    public static class Web {
        private boolean exposed = false;

        public boolean isExposed() {
            return exposed;
        }

        public void setExposed(boolean exposed) {
            this.exposed = exposed;
        }
    }

    public static class Tenant {
        private String defaultTenantId = "default";

        public String getDefaultTenantId() {
            return defaultTenantId;
        }

        public void setDefaultTenantId(String defaultTenantId) {
            this.defaultTenantId = defaultTenantId;
        }
    }

    public static class Persistence {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Ollama {
        private boolean enabled = false;
        private String provider = "ollama";
        private String baseUrl = "http://localhost:11434";
        private String embeddingModel = "bge-m3";
        private String chatModel = "llama3.2";
        private int embeddingDimension = 1024;
        private java.time.Duration timeout = java.time.Duration.ofSeconds(60);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public int getEmbeddingDimension() {
            return embeddingDimension;
        }

        public void setEmbeddingDimension(int embeddingDimension) {
            this.embeddingDimension = embeddingDimension;
        }

        public java.time.Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(java.time.Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class Tokenizer {
        private boolean allowApproximateForIndexing = true;

        public boolean isAllowApproximateForIndexing() {
            return allowApproximateForIndexing;
        }

        public void setAllowApproximateForIndexing(boolean allowApproximateForIndexing) {
            this.allowApproximateForIndexing = allowApproximateForIndexing;
        }
    }

    public static class Ingestion {
        private boolean asyncEnabled = false;
        private int asyncPoolSize = 2;

        public boolean isAsyncEnabled() {
            return asyncEnabled;
        }

        public void setAsyncEnabled(boolean asyncEnabled) {
            this.asyncEnabled = asyncEnabled;
        }

        public int getAsyncPoolSize() {
            return asyncPoolSize;
        }

        public void setAsyncPoolSize(int asyncPoolSize) {
            this.asyncPoolSize = asyncPoolSize;
        }
    }

    public static class Storage {
        private String type = "local";
        private String defaultBucket = "knowbase";
        private String localRoot;
        private Minio minio = new Minio();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDefaultBucket() {
            return defaultBucket;
        }

        public void setDefaultBucket(String defaultBucket) {
            this.defaultBucket = defaultBucket;
        }

        public String getLocalRoot() {
            return localRoot;
        }

        public void setLocalRoot(String localRoot) {
            this.localRoot = localRoot;
        }

        public Minio getMinio() {
            return minio;
        }

        public void setMinio(Minio minio) {
            this.minio = minio;
        }
    }

    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private boolean autoCreateBucket = true;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public boolean isAutoCreateBucket() {
            return autoCreateBucket;
        }

        public void setAutoCreateBucket(boolean autoCreateBucket) {
            this.autoCreateBucket = autoCreateBucket;
        }
    }

    public static class Security {
        private boolean aclEnabled = false;

        public boolean isAclEnabled() {
            return aclEnabled;
        }

        public void setAclEnabled(boolean aclEnabled) {
            this.aclEnabled = aclEnabled;
        }
    }
}
