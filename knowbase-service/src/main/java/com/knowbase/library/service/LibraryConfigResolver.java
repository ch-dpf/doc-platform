package com.knowbase.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowbase.ingest.config.IngestProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.ingest.service.InvalidDocumentException;
import com.knowbase.ingest.parse.DocumentParseOptions;
import com.knowbase.ingest.parse.FormulaExtractionMode;
import com.knowbase.ingest.parse.ImageExtractionMode;
import com.knowbase.ingest.parse.TableExtractionMode;
import com.knowbase.ingest.parse.OcrLanguageMapper;
import com.knowbase.ingest.parse.TikaEncodingMapper;
import com.knowbase.library.config.CapacityLimitsSettings;
import com.knowbase.library.config.EmbeddingSpec;
import com.knowbase.library.config.GovernanceRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.pipeline.config.PlatformPipelineDefaults;
import com.knowbase.library.config.TextNormalizationSettings;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.config.VectorLibraryConfigFactory;
import com.knowbase.library.config.VersionPolicySettings;
import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.vector.config.EmbeddingProperties;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.library.domain.VectorLibrary;
import com.knowbase.library.mapper.VectorLibraryMapper;
import com.knowbase.platform.JsonSupport;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.chunk.ChunkingStrategy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LibraryConfigResolver {

    private final VectorLibraryMapper libraryMapper;
    private final IngestProperties globalIngest;
    private final TextNormalizationProperties globalNormalization;
    private final OllamaProperties ollamaProperties;
    private final EmbeddingProperties embeddingProperties;
    private final OcrProperties ocrProperties;
    private final ChunkingProperties globalChunkingDefaults;

    public LibraryConfigResolver(
            VectorLibraryMapper libraryMapper,
            IngestProperties globalIngest,
            TextNormalizationProperties globalNormalization,
            OllamaProperties ollamaProperties,
            EmbeddingProperties embeddingProperties,
            OcrProperties ocrProperties,
            ChunkingProperties globalChunkingDefaults) {
        this.libraryMapper = libraryMapper;
        this.globalIngest = globalIngest;
        this.globalNormalization = globalNormalization;
        this.ollamaProperties = ollamaProperties;
        this.embeddingProperties = embeddingProperties;
        this.ocrProperties = ocrProperties;
        this.globalChunkingDefaults = globalChunkingDefaults;
    }

    public VectorLibrary requireLibrary(UUID libraryId) {
        VectorLibrary lib = libraryMapper.selectById(libraryId);
        if (lib == null) {
            throw new LibraryNotFoundException(libraryId);
        }
        return lib;
    }

    public VectorLibraryConfig config(UUID libraryId) {
        VectorLibrary lib = requireLibrary(libraryId);
        VectorLibraryConfig cfg = JsonSupport.parseLibraryConfig(lib.getConfigJson());
        if (cfg.getAllowedMimeTypes() == null || cfg.getAllowedMimeTypes().isEmpty()) {
            cfg.setAllowedMimeTypes(globalIngest.getAllowedMimeTypes());
        }
        return cfg;
    }

    public TextNormalizationSettings normalizationFor(UUID libraryId) {
        requireLibrary(libraryId);
        if (!globalNormalization.isEnabled()) {
            TextNormalizationSettings off = new TextNormalizationSettings();
            off.setEnabled(false);
            return off;
        }
        TextNormalizationSettings settings = TextNormalizationSettings.fromGlobal(globalNormalization);
        settings.setEnabled(true);
        return settings;
    }

    public boolean systemTextNormalizationEnabled() {
        return globalNormalization.isEnabled();
    }

    public TextNormalizationSettings systemNormalization() {
        if (!globalNormalization.isEnabled()) {
            TextNormalizationSettings off = new TextNormalizationSettings();
            off.setEnabled(false);
            return off;
        }
        TextNormalizationSettings settings = TextNormalizationSettings.fromGlobal(globalNormalization);
        settings.setEnabled(true);
        return settings;
    }

    public ChunkingProperties chunkingFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        ChunkingProperties p = new ChunkingProperties();
        ChunkingStrategy strategy = globalChunkingDefaults.getStrategy() != null
                ? globalChunkingDefaults.getStrategy()
                : ChunkingStrategy.PARAGRAPH_FIRST;
        p.setStrategy(strategy);
        p.setChunkSize(cfg.getChunkSize() > 0 ? cfg.getChunkSize() : globalChunkingDefaults.getChunkSize());
        p.setOverlap(cfg.getChunkOverlap() >= 0 ? cfg.getChunkOverlap() : globalChunkingDefaults.getOverlap());
        p.setMinChunkSize(globalChunkingDefaults.getMinChunkSize());
        p.setMaxChunkSize(globalChunkingDefaults.getMaxChunkSize());
        p.setMinParagraphLength(globalChunkingDefaults.getMinParagraphLength());
        p.setNormalizeBeforeChunk(globalChunkingDefaults.isNormalizeBeforeChunk());
        p.setSemanticSimilarityThreshold(cfg.getSemanticSimilarityThreshold() > 0
                ? cfg.getSemanticSimilarityThreshold()
                : globalChunkingDefaults.getSemanticSimilarityThreshold());
        p.setHierarchicalChunkingEnabled(cfg.isHierarchicalChunkingEnabled());
        p.setCustomDelimiter(cfg.getChunkDelimiter());
        return p;
    }

    public List<String> allowedMimeTypes(UUID libraryId) {
        return resolveAllowedMimeTypes(config(libraryId));
    }

    public CapacityLimitsSettings capacityLimitsFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        if (cfg.getIngestAccess() != null && cfg.getIngestAccess().getCapacityLimits() != null) {
            return cfg.getIngestAccess().getCapacityLimits();
        }
        return new CapacityLimitsSettings();
    }

    public VersionPolicySettings versionPolicyFor(UUID libraryId) {
        return globalIngest.toVersionPolicySettings();
    }

    private List<String> resolveAllowedMimeTypes(VectorLibraryConfig cfg) {
        return globalIngest.getAllowedMimeTypes();
    }

    /** @deprecated 解析规则由 MIME 代码默认决定；请使用 {@link com.knowbase.pipeline.config.EffectiveConfigResolver} */
    @Deprecated
    public ParsingRulesSettings parsingFor(UUID libraryId) {
        requireLibrary(libraryId);
        return PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
    }

    public RetrievalRulesSettings retrievalFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        return cfg.getRetrieval() != null ? cfg.getRetrieval() : new RetrievalRulesSettings();
    }

    public GovernanceRulesSettings governanceFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        return cfg.getGovernance() != null ? cfg.getGovernance() : new GovernanceRulesSettings();
    }

    public boolean requiresManualReview(UUID libraryId) {
        return globalIngest.requiresManualReview();
    }

    public DocumentParseOptions parseOptionsFor(UUID libraryId) {
        ParsingRulesSettings parsing = parsingFor(libraryId);
        String defaultLanguage = parsing.getDefaultLanguage();
        String language = OcrLanguageMapper.toTesseractLanguage(defaultLanguage, ocrProperties.getLanguage());
        boolean autoDetectEncoding = parsing.isAutoDetectEncoding();
        String contentLanguage = TikaEncodingMapper.normalizeLanguageTag(defaultLanguage);
        String contentEncoding =
                autoDetectEncoding ? null : TikaEncodingMapper.fixedEncodingForLanguage(defaultLanguage);
        return new DocumentParseOptions(
                parsing.isOcrEnabled(),
                language,
                autoDetectEncoding,
                contentLanguage,
                contentEncoding,
                TableExtractionMode.fromConfig(parsing.getTableExtraction()),
                ImageExtractionMode.fromConfig(parsing.getImageExtraction()),
                FormulaExtractionMode.fromConfig(parsing.getFormulaExtraction()));
    }

    public EmbeddingSpec embeddingFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        String provider = cfg.getEmbeddingProvider();
        if (provider == null || provider.isBlank()) {
            provider = "ollama";
        }
        String model = cfg.getEmbeddingModel();
        if (model == null || model.isBlank()) {
            model = ollamaProperties.getEmbeddingModel();
        }
        int dimension = cfg.getEmbeddingDimension() > 0
                ? cfg.getEmbeddingDimension()
                : embeddingProperties.getDimension();
        return new EmbeddingSpec(provider, model, dimension);
    }

    public String ingestSourceMode(UUID libraryId) {
        return "upload";
    }

    public boolean isUploadAllowed(UUID libraryId) {
        return true;
    }

    public boolean isCollectAllowed(UUID libraryId) {
        return false;
    }

    public void requireUploadAllowed(UUID libraryId) {
    }

    public void requireCollectAllowed(UUID libraryId) {
        throw InvalidDocumentException.of(
                InvalidDocumentException.CODE_INGEST_SOURCE_NOT_ALLOWED,
                "URL 采集已下线，请使用文档采集页上传文件或文件夹");
    }

}
