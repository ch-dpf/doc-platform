package com.knowbase.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "knowbase")
public class KnowbaseProperties {

    private boolean enabled = true;

    private Web web = new Web();

    private Tenant tenant = new Tenant();

    private Persistence persistence = new Persistence();

    private Ollama ollama = new Ollama();

    private VisionDocument visionDocument = new VisionDocument();

    private Tokenizer tokenizer = new Tokenizer();

    private Ingestion ingestion = new Ingestion();

    private Storage storage = new Storage();

    private Upload upload = new Upload();

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

    public VisionDocument getVisionDocument() {
        return visionDocument;
    }

    public void setVisionDocument(VisionDocument visionDocument) {
        this.visionDocument = visionDocument == null ? new VisionDocument() : visionDocument;
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

    public Upload getUpload() {
        return upload;
    }

    public void setUpload(Upload upload) {
        this.upload = upload;
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
        /** Vision-language model for document page parsing (e.g. PaddleOCR-VL via Ollama). Empty = disabled. */
        private String visionLanguageModel = "MedAIBase/PaddleOCR-VL:0.9b";
        private int embeddingDimension = 1024;
        private java.time.Duration timeout = java.time.Duration.ofSeconds(60);
        private java.time.Duration visionLanguageTimeout = java.time.Duration.ofSeconds(120);

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

        public String getVisionLanguageModel() {
            return visionLanguageModel;
        }

        public void setVisionLanguageModel(String visionLanguageModel) {
            this.visionLanguageModel = visionLanguageModel;
        }

        public java.time.Duration getVisionLanguageTimeout() {
            return visionLanguageTimeout;
        }

        public void setVisionLanguageTimeout(java.time.Duration visionLanguageTimeout) {
            this.visionLanguageTimeout = visionLanguageTimeout;
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

    /**
     * Official PaddleOCR-VL HTTP pipeline or vLLM OpenAI-compatible VLM for PDF page parsing.
     * When {@code enabled=true}, takes precedence over {@link Ollama#visionLanguageModel}.
     */
    public static class VisionDocument {
        private boolean enabled = false;
        /** paddleocr-vl | vllm | ollama */
        private String provider = "paddleocr-vl";
        private java.time.Duration timeout = java.time.Duration.ofSeconds(600);
        private PaddleOcrVl paddleocrVl = new PaddleOcrVl();
        private Vllm vllm = new Vllm();

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

        public java.time.Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(java.time.Duration timeout) {
            this.timeout = timeout;
        }

        public PaddleOcrVl getPaddleocrVl() {
            return paddleocrVl;
        }

        public void setPaddleocrVl(PaddleOcrVl paddleocrVl) {
            this.paddleocrVl = paddleocrVl == null ? new PaddleOcrVl() : paddleocrVl;
        }

        public Vllm getVllm() {
            return vllm;
        }

        public void setVllm(Vllm vllm) {
            this.vllm = vllm == null ? new Vllm() : vllm;
        }

        public static class PaddleOcrVl {
            private String baseUrl = "http://localhost:8080";
            private String layoutParsingPath = "/layout-parsing";
            private String pipelineName = "PaddleOCR-VL-1.6";
            private boolean prettifyMarkdown = true;
            private boolean returnMarkdownImages = false;
            private Boolean visualize = false;

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getLayoutParsingPath() {
                return layoutParsingPath;
            }

            public void setLayoutParsingPath(String layoutParsingPath) {
                this.layoutParsingPath = layoutParsingPath;
            }

            public String getPipelineName() {
                return pipelineName;
            }

            public void setPipelineName(String pipelineName) {
                this.pipelineName = pipelineName;
            }

            public boolean isPrettifyMarkdown() {
                return prettifyMarkdown;
            }

            public void setPrettifyMarkdown(boolean prettifyMarkdown) {
                this.prettifyMarkdown = prettifyMarkdown;
            }

            public boolean isReturnMarkdownImages() {
                return returnMarkdownImages;
            }

            public void setReturnMarkdownImages(boolean returnMarkdownImages) {
                this.returnMarkdownImages = returnMarkdownImages;
            }

            public Boolean getVisualize() {
                return visualize;
            }

            public void setVisualize(Boolean visualize) {
                this.visualize = visualize;
            }
        }

        public static class Vllm {
            private String baseUrl = "http://localhost:8118";
            private String chatCompletionsPath = "/v1/chat/completions";
            private String model = "PaddleOCR-VL-1.6-0.9B";
            private String apiKey = "";
            private double temperature = 0.1d;

            public String getBaseUrl() {
                return baseUrl;
            }

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public String getChatCompletionsPath() {
                return chatCompletionsPath;
            }

            public void setChatCompletionsPath(String chatCompletionsPath) {
                this.chatCompletionsPath = chatCompletionsPath;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public String getApiKey() {
                return apiKey;
            }

            public void setApiKey(String apiKey) {
                this.apiKey = apiKey;
            }

            public double getTemperature() {
                return temperature;
            }

            public void setTemperature(double temperature) {
                this.temperature = temperature;
            }
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
        private boolean documentUpsertEnabled = true;
        private boolean promoteEvalGateEnabled = true;
        private Summary summary = new Summary();
        private Pdf pdf = new Pdf();
        private Ocr ocr = new Ocr();
        private Layout layout = new Layout();
        private ReadingOrder readingOrder = new ReadingOrder();
        private EvidenceArtifacts evidenceArtifacts = new EvidenceArtifacts();

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

        public boolean isDocumentUpsertEnabled() {
            return documentUpsertEnabled;
        }

        public void setDocumentUpsertEnabled(boolean documentUpsertEnabled) {
            this.documentUpsertEnabled = documentUpsertEnabled;
        }

        public boolean isPromoteEvalGateEnabled() {
            return promoteEvalGateEnabled;
        }

        public void setPromoteEvalGateEnabled(boolean promoteEvalGateEnabled) {
            this.promoteEvalGateEnabled = promoteEvalGateEnabled;
        }

        public Summary getSummary() {
            return summary;
        }

        public void setSummary(Summary summary) {
            this.summary = summary == null ? new Summary() : summary;
        }

        public Pdf getPdf() {
            return pdf;
        }

        public void setPdf(Pdf pdf) {
            this.pdf = pdf == null ? new Pdf() : pdf;
        }

        public Ocr getOcr() {
            return ocr;
        }

        public void setOcr(Ocr ocr) {
            this.ocr = ocr == null ? new Ocr() : ocr;
        }

        public Layout getLayout() {
            return layout;
        }

        public void setLayout(Layout layout) {
            this.layout = layout == null ? new Layout() : layout;
        }

        public ReadingOrder getReadingOrder() {
            return readingOrder;
        }

        public void setReadingOrder(ReadingOrder readingOrder) {
            this.readingOrder = readingOrder == null ? new ReadingOrder() : readingOrder;
        }

        public EvidenceArtifacts getEvidenceArtifacts() {
            return evidenceArtifacts;
        }

        public void setEvidenceArtifacts(EvidenceArtifacts evidenceArtifacts) {
            this.evidenceArtifacts = evidenceArtifacts == null ? new EvidenceArtifacts() : evidenceArtifacts;
        }
    }

    public static class Ocr {
        private String defaultEngine = "tesseract";
        private String language = "auto";
        private double confidenceThreshold = 0.6d;
        private String downweightMode = "downweight";

        public String getDefaultEngine() {
            return defaultEngine;
        }

        public void setDefaultEngine(String defaultEngine) {
            this.defaultEngine = defaultEngine;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public double getConfidenceThreshold() {
            return confidenceThreshold;
        }

        public void setConfidenceThreshold(double confidenceThreshold) {
            this.confidenceThreshold = confidenceThreshold;
        }

        public String getDownweightMode() {
            return downweightMode;
        }

        public void setDownweightMode(String downweightMode) {
            this.downweightMode = downweightMode;
        }
    }

    public static class Layout {
        /** Default layout provider for raster/PDF pages (ML first, heuristic fallback in provider chain). */
        private String defaultProvider = "ollama-layout";
        private Ollama ollama = new Ollama();

        public String getDefaultProvider() {
            return defaultProvider;
        }

        public void setDefaultProvider(String defaultProvider) {
            this.defaultProvider = defaultProvider;
        }

        public Ollama getOllama() {
            return ollama;
        }

        public void setOllama(Ollama ollama) {
            this.ollama = ollama == null ? new Ollama() : ollama;
        }

        public static class Ollama {
            /** Enable Ollama vision layout for ruled/borderless/nested PDF tables. */
            private boolean enabled = true;
            /** Blank = use knowbase.ollama.vision-language-model, else chat-model. */
            private String model = "";
            private boolean fallbackToHeuristic = true;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getModel() {
                return model;
            }

            public void setModel(String model) {
                this.model = model;
            }

            public boolean isFallbackToHeuristic() {
                return fallbackToHeuristic;
            }

            public void setFallbackToHeuristic(boolean fallbackToHeuristic) {
                this.fallbackToHeuristic = fallbackToHeuristic;
            }
        }
    }

    public static class ReadingOrder {
        /** heuristic | http | ollama */
        private String provider = "ollama";
        private String endpoint = "";
        /** Dedicated reading-order model in Ollama ({@code ollama pull knowbase-reading-order}). */
        private String ollamaModel = "knowbase-reading-order";
        private boolean fallbackToHeuristic = true;
        private java.time.Duration timeout = java.time.Duration.ofSeconds(30);

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getOllamaModel() {
            return ollamaModel;
        }

        public void setOllamaModel(String ollamaModel) {
            this.ollamaModel = ollamaModel;
        }

        public boolean isFallbackToHeuristic() {
            return fallbackToHeuristic;
        }

        public void setFallbackToHeuristic(boolean fallbackToHeuristic) {
            this.fallbackToHeuristic = fallbackToHeuristic;
        }

        public java.time.Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(java.time.Duration timeout) {
            this.timeout = timeout == null ? java.time.Duration.ofSeconds(30) : timeout;
        }
    }

    public static class EvidenceArtifacts {
        private boolean enabled = false;
        private String bucket = "knowbase-evidence";
        private int maxPages = 20;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public int getMaxPages() {
            return maxPages;
        }

        public void setMaxPages(int maxPages) {
            this.maxPages = maxPages;
        }
    }

    public static class Pdf {
        /** Route scanned PDFs to vision-language model when configured. */
        private boolean vlOnScanned = true;
        /** Re-parse with VLM when layout confidence is below threshold. */
        private boolean vlOnLowConfidence = true;
        private double vlLowConfidenceThreshold = 0.55d;
        /** Fall back to heuristic layout/OCR when VLM fails. */
        private boolean vlFallbackToHeuristic = true;
        /** Max pages to send to VLM per document; 0 = unlimited. */
        private int vlMaxPages = 0;

        public boolean isVlOnScanned() {
            return vlOnScanned;
        }

        public void setVlOnScanned(boolean vlOnScanned) {
            this.vlOnScanned = vlOnScanned;
        }

        public boolean isVlOnLowConfidence() {
            return vlOnLowConfidence;
        }

        public void setVlOnLowConfidence(boolean vlOnLowConfidence) {
            this.vlOnLowConfidence = vlOnLowConfidence;
        }

        public double getVlLowConfidenceThreshold() {
            return vlLowConfidenceThreshold;
        }

        public void setVlLowConfidenceThreshold(double vlLowConfidenceThreshold) {
            this.vlLowConfidenceThreshold = vlLowConfidenceThreshold;
        }

        public boolean isVlFallbackToHeuristic() {
            return vlFallbackToHeuristic;
        }

        public void setVlFallbackToHeuristic(boolean vlFallbackToHeuristic) {
            this.vlFallbackToHeuristic = vlFallbackToHeuristic;
        }

        public int getVlMaxPages() {
            return vlMaxPages;
        }

        public void setVlMaxPages(int vlMaxPages) {
            this.vlMaxPages = vlMaxPages;
        }
    }

    public static class Summary {
        private String promptId = "default_summary";
        private String language = "the same language as the source content";
        private int maxInputChars = 16_384;
        private int maxOutputChars = 500;
        private int minInputChars = 100;
        private double temperature = 0.3;
        private int maxCompletionTokens = 2048;

        public String getPromptId() {
            return promptId;
        }

        public void setPromptId(String promptId) {
            this.promptId = promptId;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public int getMaxInputChars() {
            return maxInputChars;
        }

        public void setMaxInputChars(int maxInputChars) {
            this.maxInputChars = maxInputChars;
        }

        public int getMaxOutputChars() {
            return maxOutputChars;
        }

        public void setMaxOutputChars(int maxOutputChars) {
            this.maxOutputChars = maxOutputChars;
        }

        public int getMinInputChars() {
            return minInputChars;
        }

        public void setMinInputChars(int minInputChars) {
            this.minInputChars = minInputChars;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxCompletionTokens() {
            return maxCompletionTokens;
        }

        public void setMaxCompletionTokens(int maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
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

    public static class Upload {
        private int maxFilesPerBatch = 50;
        private long maxFileSizeBytes = 100L * 1024 * 1024;

        public int getMaxFilesPerBatch() {
            return maxFilesPerBatch;
        }

        public void setMaxFilesPerBatch(int maxFilesPerBatch) {
            this.maxFilesPerBatch = maxFilesPerBatch;
        }

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
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
