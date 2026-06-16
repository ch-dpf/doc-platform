package com.knowbase.pipeline.config;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.parse.DocumentParseOptions;
import com.knowbase.ingest.parse.FormulaExtractionMode;
import com.knowbase.ingest.parse.ImageExtractionMode;
import com.knowbase.ingest.parse.OcrLanguageMapper;
import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.ingest.parse.ParserRuleResolver;
import com.knowbase.ingest.parse.TableExtractionMode;
import com.knowbase.ingest.parse.TikaEncodingMapper;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.library.config.TextNormalizationSettings;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.pipeline.content.ContentFamily;
import com.knowbase.pipeline.content.ContentFamilyResolver;
import com.knowbase.pipeline.content.ContentSignals;
import com.knowbase.pipeline.content.ContentSignalsChunkingAdjuster;
import com.knowbase.pipeline.content.ContentSignalsDetector;
import com.knowbase.vector.config.ChunkingProperties;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EffectiveConfigResolver {

    private final LibraryConfigResolver libraryConfigResolver;
    private final DocMetadataStore docMetadataStore;
    private final MimeTypePipelineDefaults mimeDefaults;
    private final OcrProperties ocrProperties;
    private final TextNormalizationProperties globalNormalization;
    private final ContentSignalsDetector contentSignalsDetector;
    private final ContentSignalsChunkingAdjuster contentSignalsChunkingAdjuster;
    private final ParserEngineRegistry parserEngineRegistry;

    public EffectiveConfigResolver(
            LibraryConfigResolver libraryConfigResolver,
            DocMetadataStore docMetadataStore,
            MimeTypePipelineDefaults mimeDefaults,
            OcrProperties ocrProperties,
            TextNormalizationProperties globalNormalization,
            ContentSignalsDetector contentSignalsDetector,
            ContentSignalsChunkingAdjuster contentSignalsChunkingAdjuster,
            ParserEngineRegistry parserEngineRegistry) {
        this.libraryConfigResolver = libraryConfigResolver;
        this.docMetadataStore = docMetadataStore;
        this.mimeDefaults = mimeDefaults;
        this.ocrProperties = ocrProperties;
        this.globalNormalization = globalNormalization;
        this.contentSignalsDetector = contentSignalsDetector;
        this.contentSignalsChunkingAdjuster = contentSignalsChunkingAdjuster;
        this.parserEngineRegistry = parserEngineRegistry;
    }

    /** 解析前：族群/MIME 基线 + ingest profile，不含内容信号（供 extract/OCR 选项）。 */
    public EffectivePipelineConfig forDocument(UUID libraryId, UUID docId) {
        DocMetadata doc = docMetadataStore.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        IngestProfile profile = IngestProfileSupport.parse(doc.getIngestProfileJson());
        return resolve(libraryId, doc.getMimeType(), profile, null);
    }

    /** 分块前：在 forDocument 基线上叠加内容信号二次路由。 */
    public EffectivePipelineConfig forDocumentWithContent(UUID libraryId, UUID docId, String parsedText) {
        DocMetadata doc = docMetadataStore.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        IngestProfile profile = IngestProfileSupport.parse(doc.getIngestProfileJson());
        return resolve(libraryId, doc.getMimeType(), profile, parsedText);
    }

    public EffectivePipelineConfig forIngest(UUID libraryId, String mimeType, IngestProfile profile) {
        return resolve(libraryId, mimeType, profile, null);
    }

    /** 分块预览：库 + MIME + ingest profile + 已解析样本文本的内容信号。 */
    public EffectivePipelineConfig forIngestWithContent(
            UUID libraryId, String mimeType, IngestProfile profile, String parsedText) {
        return resolve(libraryId, mimeType, profile, parsedText);
    }

    /**
     * 批量重索引 / 迁移到主档：库 + MIME 解析，不叠加采集级分块覆盖。
     */
    public EffectivePipelineConfig forDocumentLibraryRebuild(UUID libraryId, UUID docId, String parsedText) {
        DocMetadata doc = docMetadataStore.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));
        return resolve(libraryId, doc.getMimeType(), null, parsedText);
    }

    /** 无库上下文时（如全局解析预览），仅按 MIME 应用平台解析/清洗默认。 */
    public EffectivePipelineConfig forMimeOnly(String mimeType) {
        ParsingRulesSettings parsing =
                PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        CleaningRulesSettings cleaning =
                PlatformPipelineDefaults.copyCleaning(PlatformPipelineDefaults.baselineCleaning());
        ChunkingProperties chunking = new ChunkingProperties();
        mimeDefaults.apply(mimeType, parsing, cleaning);
        boolean textNormEnabled = globalNormalization.isEnabled();
        TextNormalizationSettings normalization = systemNormalization();
        ContentFamily family = ContentFamilyResolver.resolve(mimeType);
        return new EffectivePipelineConfig(
                textNormEnabled, normalization, cleaning, chunking, parsing, 1, family, null);
    }

    public DocumentParseOptions parseOptions(EffectivePipelineConfig effective) {
        ParsingRulesSettings parsing = effective.parsing();
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

    public DocumentParseOptions parseOptionsForDocument(UUID libraryId, UUID docId) {
        return parseOptions(forDocument(libraryId, docId));
    }

    private EffectivePipelineConfig resolve(
            UUID libraryId, String mimeType, IngestProfile profile, String parsedText) {
        VectorLibraryConfig library = libraryConfigResolver.config(libraryId);
        ContentFamily family = ContentFamilyResolver.resolve(mimeType);

        ParsingRulesSettings parsing =
                PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        CleaningRulesSettings cleaning =
                PlatformPipelineDefaults.copyCleaning(PlatformPipelineDefaults.baselineCleaning());
        ChunkingProperties chunking =
                PlatformPipelineDefaults.copyChunking(libraryConfigResolver.chunkingFor(libraryId));
        boolean textNormEnabled = globalNormalization.isEnabled();
        TextNormalizationSettings normalization = systemNormalization();

        mimeDefaults.apply(mimeType, parsing, cleaning);
        applyLibraryParsing(library, mimeType, parsing);

        chunking.setStrategy(ChunkingStrategyResolver.resolve(library.getChunkingStrategy(), mimeType));

        ContentSignals signals = null;
        if (parsedText != null && !parsedText.isBlank()) {
            signals = contentSignalsDetector.detect(family, mimeType, parsedText);
            contentSignalsChunkingAdjuster.apply(family, signals, chunking);
        }

        if (profile != null) {
            overlayProfileChunking(profile, chunking);
        }

        int configVersion = Math.max(1, library.getConfigVersion());
        return new EffectivePipelineConfig(
                textNormEnabled,
                normalization,
                cleaning,
                chunking,
                parsing,
                configVersion,
                family,
                signals);
    }

    private void applyLibraryParsing(VectorLibraryConfig library, String mimeType, ParsingRulesSettings parsing) {
        String fileType = ParserRuleResolver.resolveFileType(mimeType, null);
        String parserId = ParserRuleResolver.resolveParserId(library.getParserRules(), fileType);
        parserEngineRegistry.apply(parserId, parsing);
        overlayLibraryParsingAdvanced(library.getParsing(), parsing);
    }

    /** 库级高级解析项（语言/编码）覆盖引擎与 MIME 默认。 */
    private static void overlayLibraryParsingAdvanced(ParsingRulesSettings libraryParsing, ParsingRulesSettings target) {
        if (libraryParsing == null || target == null) {
            return;
        }
        if (libraryParsing.getDefaultLanguage() != null && !libraryParsing.getDefaultLanguage().isBlank()) {
            target.setDefaultLanguage(libraryParsing.getDefaultLanguage().trim());
        }
        target.setAutoDetectEncoding(libraryParsing.isAutoDetectEncoding());
    }

    private static void overlayProfileChunking(IngestProfile profile, ChunkingProperties chunking) {
        if (profile.getChunkSize() != null && profile.getChunkSize() > 0) {
            chunking.setChunkSize(profile.getChunkSize());
        }
        if (profile.getChunkOverlap() != null && profile.getChunkOverlap() >= 0) {
            chunking.setOverlap(profile.getChunkOverlap());
        }
        if (profile.getMinParagraphLength() != null && profile.getMinParagraphLength() >= 0) {
            chunking.setMinParagraphLength(profile.getMinParagraphLength());
        }
    }

    private TextNormalizationSettings systemNormalization() {
        if (!globalNormalization.isEnabled()) {
            TextNormalizationSettings off = new TextNormalizationSettings();
            off.setEnabled(false);
            return off;
        }
        TextNormalizationSettings settings = TextNormalizationSettings.fromGlobal(globalNormalization);
        settings.setEnabled(true);
        return settings;
    }
}
