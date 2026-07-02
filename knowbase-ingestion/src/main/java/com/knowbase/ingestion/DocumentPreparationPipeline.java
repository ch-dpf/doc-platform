package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.observability.NoopPipelineObserver;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.ingestion.DocumentMetadataEnricher.MetadataContext;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import com.knowbase.ingestion.parse.IngestionParseOptionsSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DocumentPreparationPipeline {

    private static final Logger log = LoggerFactory.getLogger(DocumentPreparationPipeline.class);

    private final DocumentSourceLoader sourceLoader;
    private final DocumentNormalizer textNormalizer;
    private final DocumentMetadataEnricher metadataEnricher;
    private final TokenBasedDocumentChunker documentChunker;
    private final ChunkPostProcessor chunkPostProcessor;
    private final DocumentLlmSummaryGenerator documentLlmSummaryGenerator;
    private final IngestionStageTracer stageTracer;
    private final Map<String, Object> parseApplicationDefaults;

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentTextNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker
    ) {
        this(
                sourceLoader,
                (DocumentNormalizer) textNormalizer,
                documentChunker,
                new DefaultDocumentMetadataEnricher(),
                CompositeChunkPostProcessor.of(new StructuredTableChunkPostProcessor()),
                null,
                null,
                Map.of()
        );
    }

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker,
            DocumentMetadataEnricher metadataEnricher
    ) {
        this(
                sourceLoader,
                textNormalizer,
                documentChunker,
                metadataEnricher,
                CompositeChunkPostProcessor.of(new StructuredTableChunkPostProcessor()),
                null,
                null,
                Map.of()
        );
    }

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker,
            DocumentMetadataEnricher metadataEnricher,
            ChunkPostProcessor chunkPostProcessor
    ) {
        this(sourceLoader, textNormalizer, documentChunker, metadataEnricher, chunkPostProcessor, null, null, Map.of());
    }

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker,
            DocumentMetadataEnricher metadataEnricher,
            ChunkPostProcessor chunkPostProcessor,
            PipelineObserver pipelineObserver
    ) {
        this(sourceLoader, textNormalizer, documentChunker, metadataEnricher, chunkPostProcessor, null, pipelineObserver, Map.of());
    }

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker,
            DocumentMetadataEnricher metadataEnricher,
            ChunkPostProcessor chunkPostProcessor,
            DocumentLlmSummaryGenerator documentLlmSummaryGenerator,
            PipelineObserver pipelineObserver,
            Map<String, Object> parseApplicationDefaults
    ) {
        this.sourceLoader = Objects.requireNonNull(sourceLoader, "sourceLoader");
        this.textNormalizer = Objects.requireNonNull(textNormalizer, "textNormalizer");
        this.documentChunker = Objects.requireNonNull(documentChunker, "documentChunker");
        this.metadataEnricher = Objects.requireNonNull(metadataEnricher, "metadataEnricher");
        this.chunkPostProcessor = chunkPostProcessor == null
                ? CompositeChunkPostProcessor.noop()
                : chunkPostProcessor;
        this.documentLlmSummaryGenerator = documentLlmSummaryGenerator;
        this.parseApplicationDefaults = parseApplicationDefaults == null ? Map.of() : Map.copyOf(parseApplicationDefaults);
        PipelineObserver observer = pipelineObserver == null ? new NoopPipelineObserver() : pipelineObserver;
        this.stageTracer = new IngestionStageTracer(observer);
    }

    public DocumentPreparationResult prepare(
            String sourceUri,
            Map<String, Object> sourceOptions,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            PreparationStage stage
    ) {
        return prepare(
                sourceUri,
                sourceOptions,
                libraryId,
                documentId,
                indexVersionId,
                libraryProfile,
                documentProfile,
                tokenizer,
                stage,
                null
        );
    }

    public DocumentPreparationResult prepare(
            String sourceUri,
            Map<String, Object> sourceOptions,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            PreparationStage stage,
            IngestionTraceContext traceContext
    ) {
        IngestionTraceContext context = traceContext == null
                ? null
                : traceContext;
        Map<String, Object> effectiveOptions = IngestionParseOptionsSupport.mergeForLoad(
                documentProfile,
                sourceOptions,
                parseApplicationDefaults
        );
        ParsedDocument loaded = stageTracer.trace(
                context,
                "load_source",
                Map.of(),
                () -> ensureExtractedText(sourceLoader.load(sourceUri, effectiveOptions)),
                parsed -> Map.of(
                        "blockCount", parsed.blocks().size(),
                        "contentFamily", parsed.contentFamily().name()
                )
        );
        ParsedDocument parsed = stageTracer.trace(
                context,
                "parse_document",
                Map.of(),
                () -> ParsedDocumentParseEnricher.enrich(
                        ParsedDocumentStructureEnricher.enrich(loaded, sourceUri)
                ),
                enriched -> parseSpanAttributes(enriched)
        );
        return prepareFromParsed(
                parsed,
                sourceUri,
                libraryId,
                documentId,
                indexVersionId,
                libraryProfile,
                documentProfile,
                tokenizer,
                stage,
                sourceOptions,
                context
        );
    }

    public DocumentPreparationResult prepareFromParsed(
            ParsedDocument parsed,
            String sourceUri,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            PreparationStage stage,
            Map<String, Object> sourceOptions
    ) {
        return prepareFromParsed(
                parsed,
                sourceUri,
                libraryId,
                documentId,
                indexVersionId,
                libraryProfile,
                documentProfile,
                tokenizer,
                stage,
                sourceOptions,
                null
        );
    }

    public DocumentPreparationResult prepareFromParsed(
            ParsedDocument parsed,
            String sourceUri,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            PreparationStage stage,
            Map<String, Object> sourceOptions,
            IngestionTraceContext traceContext
    ) {
        PreparationStage executionStage = stage.executionStage();
        if (executionStage == PreparationStage.PARSE) {
            return new DocumentPreparationResult(
                    sourceUri,
                    parsed,
                    null,
                    List.of(),
                    null,
                    documentProfile,
                    sourceOptions,
                    DocumentSummaryStageOutcome.disabled()
            );
        }

        NormalizationResult normalization = stageTracer.trace(
                traceContext,
                "normalize_text",
                Map.of(),
                () -> textNormalizer.normalize(parsed, documentProfile),
                result -> Map.of(
                        "rawCharCount", result.rawCharCount(),
                        "normalizedCharCount", result.normalizedCharCount(),
                        "rawBlockCount", result.rawBlockCount(),
                        "normalizedBlockCount", result.normalizedBlockCount()
                )
        );
        if (executionStage == PreparationStage.NORMALIZE) {
            return new DocumentPreparationResult(
                    sourceUri,
                    normalization.document(),
                    normalization,
                    List.of(),
                    null,
                    documentProfile,
                    sourceOptions,
                    DocumentSummaryStageOutcome.disabled()
            );
        }

        ParsedDocument normalized = stageTracer.trace(
                traceContext,
                "extract_metadata",
                Map.of(),
                () -> metadataEnricher.enrich(
                        normalization.document(),
                        new MetadataContext(
                                sourceUri,
                                libraryId,
                                documentId,
                                indexVersionId,
                                libraryProfile,
                                documentProfile,
                                sourceOptions
                        )
                ),
                enriched -> Map.of(
                        "blockCount", enriched.blocks().size(),
                        "metadataKeys", enriched.metadata() == null ? 0 : enriched.metadata().size()
                )
        );

        List<DocumentChunk> chunks = stageTracer.trace(
                traceContext,
                "chunk_document",
                Map.of(),
                () -> documentChunker.chunk(
                        libraryId,
                        documentId,
                        indexVersionId,
                        normalized,
                        libraryProfile,
                        documentProfile,
                        tokenizer,
                        sourceOptions
                ),
                chunkList -> chunkSpanAttributes(chunkList)
        );
        if (executionStage == PreparationStage.CHUNK) {
            return new DocumentPreparationResult(
                    sourceUri,
                    normalized,
                    normalization,
                    chunks,
                    ChunkPostProcessMetrics.notApplied(chunks),
                    documentProfile,
                    sourceOptions,
                    DocumentSummaryStageOutcome.disabled()
            );
        }

        ChunkPostProcessContext postProcessContext = new ChunkPostProcessContext(
                normalized,
                libraryProfile,
                documentProfile,
                tokenizer,
                sourceOptions
        );
        PostProcessOutcome postProcessOutcome = stageTracer.trace(
                traceContext,
                "post_process_chunks",
                Map.of("beforeCount", chunks.size()),
                () -> applyPostProcess(chunks, postProcessContext),
                outcome -> postProcessSpanAttributes(outcome)
        );

        DocumentSummaryStageOutcome documentSummary = DocumentSummaryStageOutcome.disabled();
        if (executionStage == PreparationStage.DOCUMENT_SUMMARY) {
            documentSummary = runDocumentSummaryFromChunks(
                    normalized,
                    libraryProfile,
                    documentProfile,
                    tokenizer,
                    sourceOptions,
                    postProcessOutcome.chunks(),
                    traceContext
            );
            return new DocumentPreparationResult(
                    sourceUri,
                    normalized,
                    normalization,
                    postProcessOutcome.chunks(),
                    postProcessOutcome.metrics(),
                    documentProfile,
                    sourceOptions,
                    documentSummary
            );
        }

        return new DocumentPreparationResult(
                sourceUri,
                normalized,
                normalization,
                postProcessOutcome.chunks(),
                postProcessOutcome.metrics(),
                documentProfile,
                sourceOptions,
                documentSummary
        );
    }

    private DocumentSummaryStageOutcome runDocumentSummaryFromChunks(
            ParsedDocument document,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            Map<String, Object> sourceOptions,
            List<DocumentChunk> chunks,
            IngestionTraceContext traceContext
    ) {
        if (documentLlmSummaryGenerator == null) {
            return DocumentSummaryStageOutcome.disabled();
        }
        ChunkPostProcessContext context = new ChunkPostProcessContext(
                document,
                libraryProfile,
                documentProfile,
                tokenizer,
                sourceOptions
        );
        return stageTracer.trace(
                traceContext,
                "summarize_document",
                Map.of("chunkCount", chunks.size()),
                () -> documentLlmSummaryGenerator.generateStageOutcome(context, chunks),
                outcome -> Map.of(
                        "enabled", outcome.enabled(),
                        "attempted", outcome.attempted(),
                        "succeeded", outcome.succeeded(),
                        "inputCharCount", outcome.inputCharCount()
                )
        );
    }

    public NormalizationResult normalize(ParsedDocument parsed, DocumentProfile documentProfile) {
        return textNormalizer.normalize(parsed, documentProfile);
    }

    public ParsedDocument parse(String sourceUri, Map<String, Object> sourceOptions) {
        log.info("准备阶段解析开始: sourceUri={}", sourceUri);
        ParsedDocument loaded = ensureExtractedText(sourceLoader.load(sourceUri, sourceOptions));
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(ParsedDocumentStructureEnricher.enrich(loaded, sourceUri));
        log.info(
                "准备阶段解析完成: sourceUri={}, blocks={}, structureAware={}",
                sourceUri,
                parsed.blocks().size(),
                parsed.structureAware()
        );
        return parsed;
    }

    private PostProcessOutcome applyPostProcess(List<DocumentChunk> chunks, ChunkPostProcessContext context) {
        ChunkPostProcessMetrics metrics = ChunkPostProcessMetrics.notApplied(chunks);
        if (!chunkPostProcessor.supports(context)) {
            return new PostProcessOutcome(chunks, metrics);
        }
        List<DocumentChunk> beforePostProcess = chunks;
        List<DocumentChunk> processed = chunkPostProcessor.process(beforePostProcess, context);
        metrics = ChunkPostProcessMetrics.compute(beforePostProcess, processed);
        return new PostProcessOutcome(processed, metrics);
    }

    private static Map<String, Object> parseSpanAttributes(ParsedDocument parsed) {
        String parserCode = parsed.metadata() == null ? null : stringValue(parsed.metadata().get("parserCode"));
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("blockCount", parsed.blocks().size());
        attributes.put("structureAware", parsed.structureAware());
        attributes.put("parserCode", parserCode == null ? "" : parserCode);
        if (parsed.metadata() != null) {
            if (parsed.metadata().get("parseConfidence") != null) {
                attributes.put("parseConfidence", parsed.metadata().get("parseConfidence"));
            }
            if (parsed.metadata().get("indexableBlockCount") != null) {
                attributes.put("indexableBlockCount", parsed.metadata().get("indexableBlockCount"));
            }
            if (parsed.metadata().get("tableRegionCount") != null) {
                attributes.put("tableRegionCount", parsed.metadata().get("tableRegionCount"));
            }
        }
        return Map.copyOf(attributes);
    }

    private static Map<String, Object> chunkSpanAttributes(List<DocumentChunk> chunks) {
        int indexableCount = 0;
        for (DocumentChunk chunk : chunks) {
            if (isIndexableChunk(chunk)) {
                indexableCount++;
            }
        }
        return Map.of(
                "chunkCount", chunks.size(),
                "indexableCount", indexableCount
        );
    }

    private static Map<String, Object> postProcessSpanAttributes(PostProcessOutcome outcome) {
        ChunkPostProcessMetrics metrics = outcome.metrics();
        return Map.of(
                "applied", metrics.applied(),
                "beforeCount", metrics.beforeCount(),
                "afterCount", metrics.afterCount(),
                "summariesAdded", metrics.summariesAdded(),
                "rowsMerged", metrics.rowsMerged(),
                "deduplicated", metrics.deduplicated()
        );
    }

    private static boolean isIndexableChunk(DocumentChunk chunk) {
        if (chunk.parentChunkId() != null) {
            return true;
        }
        if (chunk.metadata() == null) {
            return true;
        }
        Object indexable = chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return true;
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static ParsedDocument ensureExtractedText(ParsedDocument parsed) {
        if ((parsed.text() == null || parsed.text().isBlank()) && !parsed.structureAware()) {
            throw new IllegalStateException("文档未提取到可索引文本: " + parsed.sourceUri());
        }
        if (parsed.text() == null || parsed.text().isBlank()) {
            return new ParsedDocument(
                    parsed.sourceUri(),
                    parsed.title(),
                    rebuildFlatText(parsed.blocks()),
                    parsed.contentFamily(),
                    parsed.metadata(),
                    parsed.blocks()
            );
        }
        return parsed;
    }

    private static String rebuildFlatText(List<StructuralBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (StructuralBlock block : blocks) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            if ("heading".equals(block.blockType())) {
                builder.append("#".repeat(Math.max(1, block.level()))).append(' ').append(block.content());
            } else {
                builder.append(block.content());
            }
        }
        return builder.toString();
    }

    private record PostProcessOutcome(List<DocumentChunk> chunks, ChunkPostProcessMetrics metrics) {
    }
}
