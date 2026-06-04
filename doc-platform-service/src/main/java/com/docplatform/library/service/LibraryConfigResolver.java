package com.docplatform.library.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docplatform.ingest.config.IngestProperties;
import com.docplatform.ingest.config.TextNormalizationProperties;
import com.docplatform.ingest.service.InvalidDocumentException;
import com.docplatform.library.config.EmbeddingSpec;
import com.docplatform.library.config.TextNormalizationSettings;
import com.docplatform.library.config.VectorLibraryConfig;
import com.docplatform.vector.config.EmbeddingProperties;
import com.docplatform.vector.config.OllamaProperties;
import com.docplatform.library.domain.VectorLibrary;
import com.docplatform.library.mapper.VectorLibraryMapper;
import com.docplatform.platform.JsonSupport;
import com.docplatform.vector.config.ChunkingProperties;
import com.docplatform.vector.chunk.ChunkingStrategy;
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

    public LibraryConfigResolver(
            VectorLibraryMapper libraryMapper,
            IngestProperties globalIngest,
            TextNormalizationProperties globalNormalization,
            OllamaProperties ollamaProperties,
            EmbeddingProperties embeddingProperties) {
        this.libraryMapper = libraryMapper;
        this.globalIngest = globalIngest;
        this.globalNormalization = globalNormalization;
        this.ollamaProperties = ollamaProperties;
        this.embeddingProperties = embeddingProperties;
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
        if (cfg.getTextNormalization() == null) {
            cfg.setTextNormalization(TextNormalizationSettings.fromGlobal(globalNormalization));
        }
        return cfg;
    }

    public TextNormalizationSettings normalizationFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        if (!cfg.isTextNormalizationEnabled()) {
            TextNormalizationSettings off = new TextNormalizationSettings();
            off.setEnabled(false);
            return off;
        }
        TextNormalizationSettings settings = cfg.getTextNormalization();
        if (settings == null) {
            return TextNormalizationSettings.fromGlobal(globalNormalization);
        }
        settings.setEnabled(true);
        return settings;
    }

    public ChunkingProperties chunkingFor(UUID libraryId) {
        VectorLibraryConfig cfg = config(libraryId);
        ChunkingProperties p = new ChunkingProperties();
        p.setStrategy(cfg.getChunkingStrategy() != null ? cfg.getChunkingStrategy() : ChunkingStrategy.PARAGRAPH_FIRST);
        p.setChunkSize(cfg.getChunkSize());
        p.setOverlap(cfg.getChunkOverlap());
        p.setMinChunkSize(cfg.getMinChunkSize());
        p.setMaxChunkSize(cfg.getMaxChunkSize());
        p.setMinParagraphLength(cfg.getMinParagraphLength());
        p.setNormalizeBeforeChunk(cfg.isNormalizeBeforeChunk());
        return p;
    }

    public List<String> allowedMimeTypes(UUID libraryId) {
        return config(libraryId).getAllowedMimeTypes();
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
        return normalizeIngestSourceMode(config(libraryId).getIngestSourceMode());
    }

    public boolean isUploadAllowed(UUID libraryId) {
        String mode = ingestSourceMode(libraryId);
        return !"crawl".equals(mode);
    }

    public boolean isCollectAllowed(UUID libraryId) {
        String mode = ingestSourceMode(libraryId);
        return !"upload".equals(mode);
    }

    public void requireUploadAllowed(UUID libraryId) {
        if (!isUploadAllowed(libraryId)) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_SOURCE_NOT_ALLOWED,
                    "该向量库数据源为「线上采集」，不支持文件上传，请使用 URL 采集或修改向量库数据源配置");
        }
    }

    public void requireCollectAllowed(UUID libraryId) {
        if (!isCollectAllowed(libraryId)) {
            throw InvalidDocumentException.of(
                    InvalidDocumentException.CODE_INGEST_SOURCE_NOT_ALLOWED,
                    "该向量库数据源为「本地文件」，不支持 URL 采集，请使用文件上传或修改向量库数据源配置");
        }
    }

    private static String normalizeIngestSourceMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "upload";
        }
        return mode.trim().toLowerCase();
    }

}
