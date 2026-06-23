package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentIndexJob;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.DocumentStatus;
import com.knowbase.domain.status.IndexVersionStatus;
import com.knowbase.domain.status.IngestionRunStatus;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.model.ollama.OllamaEmbeddingModelClient;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public final class DefaultIngestionPipeline implements IngestionPipeline {

    private final KnowbaseRepository repository;
    private final DocumentPreparationPipeline documentPreparationPipeline;
    private final EmbeddingModelClient embeddingModelClient;
    private final TokenizerRegistry tokenizerRegistry;
    private final PipelineObserver pipelineObserver;
    private final Function<UUID, UUID> activeGenerationResolver;
    private final Consumer<UUID> generationStatsRefresher;
    private final boolean documentUpsertEnabled;
    private final DocumentSourceUriExpander sourceUriExpander = new DocumentSourceUriExpander();
    private final DocumentProfileResolver documentProfileResolver = new DocumentProfileResolver();

    public DefaultIngestionPipeline(
            KnowbaseRepository repository,
            DocumentPreparationPipeline documentPreparationPipeline,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver
    ) {
        this(
                repository,
                documentPreparationPipeline,
                embeddingModelClient,
                tokenizerRegistry,
                pipelineObserver,
                null,
                null
        );
    }

    public DefaultIngestionPipeline(
            KnowbaseRepository repository,
            DocumentPreparationPipeline documentPreparationPipeline,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver,
            Function<UUID, UUID> activeGenerationResolver,
            Consumer<UUID> generationStatsRefresher
    ) {
        this(
                repository,
                documentPreparationPipeline,
                embeddingModelClient,
                tokenizerRegistry,
                pipelineObserver,
                activeGenerationResolver,
                generationStatsRefresher,
                true
        );
    }

    public DefaultIngestionPipeline(
            KnowbaseRepository repository,
            DocumentPreparationPipeline documentPreparationPipeline,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver,
            Function<UUID, UUID> activeGenerationResolver,
            Consumer<UUID> generationStatsRefresher,
            boolean documentUpsertEnabled
    ) {
        this.repository = repository;
        this.documentPreparationPipeline = documentPreparationPipeline;
        this.embeddingModelClient = embeddingModelClient;
        this.tokenizerRegistry = tokenizerRegistry;
        this.pipelineObserver = pipelineObserver == null ? new com.knowbase.domain.observability.NoopPipelineObserver() : pipelineObserver;
        this.activeGenerationResolver = activeGenerationResolver;
        this.generationStatsRefresher = generationStatsRefresher;
        this.documentUpsertEnabled = documentUpsertEnabled;
    }

    @Override
    public IngestionRun run(IngestionRequest request) {
        IngestionRun existing = repository.findIngestionRun(request.runId())
                .orElseThrow(() -> new IllegalStateException("入库运行不存在: " + request.runId()));
        LibraryProfile profile = SegmentationOptionsSupport.applyLibraryProfileOverrides(
                repository.findLatestLibraryProfile(request.libraryId())
                        .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + request.libraryId())),
                request.options() == null ? Map.of() : request.options()
        );
        List<DocumentProfile> documentProfiles = repository.listDocumentProfiles(request.libraryId());
        if (documentProfiles.isEmpty()) {
            throw new IllegalStateException("知识库未配置文档 Profile: " + request.libraryId());
        }
        Map<String, Object> requestOptions = request.options() == null ? Map.of() : request.options();
        String resolvedProfileCode = SegmentationOptionsSupport.resolveDocumentProfileCode(
                request.documentProfileCode(),
                requestOptions
        );
        List<String> sourceUris = sourceUriExpander.expand(request.sourceUris(), requestOptions);
        if (sourceUris.isEmpty()) {
            throw new IllegalStateException("未发现可入库的文档来源: " + request.sourceUris());
        }

        if (!documentUpsertEnabled) {
            return runLegacySnapshotMode(
                    existing,
                    request,
                    profile,
                    documentProfiles,
                    resolvedProfileCode,
                    sourceUris,
                    requestOptions
            );
        }

        IngestionRun running = updateStatus(
                existing,
                IngestionRunStatus.RUNNING,
                sourceUris,
                sourceUris.size(),
                "正在执行入库 Pipeline，已展开 " + sourceUris.size() + " 个文档来源"
        );
        repository.saveIngestionRun(running);
        UUID ingestSpan = pipelineObserver.startSpan("ingestion", request.runId(), "pipeline", Map.of("libraryId", request.libraryId().toString()));

        UUID targetGenerationId = resolveTargetGeneration(request.libraryId(), requestOptions);
        boolean deferDocumentGeneration = IngestionPipelineOptions.deferDocumentGenerationUpdate(requestOptions);

        int succeeded = 0;
        int failed = 0;
        int chunkCount = 0;
        List<String> failureMessages = new ArrayList<>();

        for (String sourceUri : sourceUris) {
            UUID documentSpan = pipelineObserver.startSpan("ingestion", request.runId(), "document", Map.of("sourceUri", sourceUri));
            DocumentIndexJob indexJob = DocumentIndexJobProgress.start(repository, request.runId(), request.libraryId(), sourceUri);
            KnowledgeDocument document = null;
            try {
                DocumentProfile resolvedProfile = documentProfileResolver.resolve(
                        sourceUri,
                        resolvedProfileCode,
                        documentProfiles
                );
                Map<String, Object> sourceOptions = mergeDocumentProfileOptions(
                        requestOptions,
                        resolvedProfile,
                        documentProfileResolver.routingMetadata(sourceUri, resolvedProfile)
                );
                sourceOptions = ParseOptionsSupport.applyParseMode(sourceOptions, sourceUri);
                DocumentProfile documentProfile = SegmentationOptionsSupport.applyDocumentProfileOverrides(
                        resolvedProfile,
                        requestOptions
                );
                TokenizerProfile tokenizerProfile = resolveTokenizerProfile(profile, documentProfile);
                ModelTokenizer tokenizer = resolveTokenizer(profile, tokenizerProfile);
                sourceOptions = withTokenizerMetadata(sourceOptions, tokenizerProfile, tokenizer);

                document = resolveDocument(
                        request.libraryId(),
                        targetGenerationId,
                        sourceUri,
                        resolvedProfile,
                        Instant.now(),
                        deferDocumentGeneration
                );
                indexJob = DocumentIndexJobProgress.advanceJob(
                        repository,
                        indexJob,
                        document.documentId(),
                        DocumentStatus.PARSING,
                        "正在解析文档"
                );
                document = DocumentIndexJobProgress.advanceDocument(repository, document, DocumentStatus.PARSING, "正在解析文档");

                indexJob = DocumentIndexJobProgress.advanceJob(
                        repository,
                        indexJob,
                        document.documentId(),
                        DocumentStatus.NORMALIZING,
                        "正在清洗与规范化"
                );
                document = DocumentIndexJobProgress.advanceDocument(repository, document, DocumentStatus.NORMALIZING, "正在清洗与规范化");

                DocumentPreparationResult prepared = documentPreparationPipeline.prepare(
                        sourceUri,
                        sourceOptions,
                        request.libraryId(),
                        document.documentId(),
                        targetGenerationId,
                        profile,
                        documentProfile,
                        tokenizer,
                        PreparationStage.CHUNK
                );
                List<DocumentChunk> chunks = prepared.chunks();
                List<DocumentChunk> indexableChunks = chunks.stream()
                        .filter(DefaultIngestionPipeline::isIndexableChunk)
                        .toList();

                indexJob = DocumentIndexJobProgress.advanceJob(
                        repository,
                        indexJob,
                        document.documentId(),
                        DocumentStatus.CHUNKING,
                        "已生成 " + chunks.size() + " 个分块"
                );
                document = DocumentIndexJobProgress.advanceDocument(repository, document, DocumentStatus.CHUNKING, "已生成分块");

                indexJob = DocumentIndexJobProgress.advanceJob(
                        repository,
                        indexJob,
                        document.documentId(),
                        DocumentStatus.EMBEDDING,
                        "正在向量化"
                );
                document = DocumentIndexJobProgress.advanceDocument(repository, document, DocumentStatus.EMBEDDING, "正在向量化");

                List<float[]> embeddings = embedChunks(embeddingModelClient, profile, indexableChunks);
                List<IndexedChunk> indexedChunks = new ArrayList<>();
                for (int index = 0; index < indexableChunks.size(); index++) {
                    indexedChunks.add(new IndexedChunk(indexableChunks.get(index), embeddings.get(index)));
                }
                for (DocumentChunk chunk : chunks) {
                    if (!isIndexableChunk(chunk)) {
                        indexedChunks.add(new IndexedChunk(chunk, null));
                    }
                }

                repository.replaceDocumentChunks(document.documentId(), indexedChunks);
                Instant indexedAt = Instant.now();
                String title = prepared.parsed().title();
                UUID documentGenerationId = deferDocumentGeneration
                        ? repository.findDocument(document.documentId())
                        .map(KnowledgeDocument::indexVersionId)
                        .orElse(targetGenerationId)
                        : targetGenerationId;
                document = new KnowledgeDocument(
                        document.documentId(),
                        document.libraryId(),
                        documentGenerationId,
                        sourceUri,
                        title == null || title.isBlank() ? document.title() : title,
                        DocumentStatus.INDEXED,
                        resolvedProfile.documentProfileId(),
                        sourceUri,
                        indexedAt,
                        null,
                        document.createdAt(),
                        indexedAt
                );
                repository.saveDocument(document);
                DocumentIndexJobProgress.succeed(repository, indexJob, document.documentId(), indexableChunks.size());

                chunkCount += indexableChunks.size();
                succeeded++;
                pipelineObserver.finishSpan(documentSpan, "SUCCEEDED", Map.of("chunkCount", chunks.size()));
            } catch (RuntimeException exception) {
                failed++;
                failureMessages.add(shortFailure(sourceUri, exception));
                pipelineObserver.recordIngestionError(request.runId(), sourceUri, "INGEST_DOCUMENT_FAILED", exception.getMessage());
                pipelineObserver.finishSpan(documentSpan, "FAILED", Map.of("error", exception.getMessage()));
                if (document != null) {
                    repository.saveDocument(markStatus(document, DocumentStatus.FAILED, exception.getMessage()));
                    DocumentIndexJobProgress.fail(repository, indexJob, document.documentId(), exception.getMessage());
                } else {
                    DocumentIndexJobProgress.fail(repository, indexJob, null, exception.getMessage());
                    repository.findDocumentBySourceUri(request.libraryId(), sourceUri).ifPresent(failedDocument ->
                            repository.saveDocument(markStatus(failedDocument, DocumentStatus.FAILED, exception.getMessage()))
                    );
                }
            }
        }

        if (chunkCount == 0) {
            IngestionRun failedRun = new IngestionRun(
                    running.runId(),
                    running.libraryId(),
                    IngestionRunStatus.FAILED,
                    running.sourceUris(),
                    running.sourceType(),
                    running.documentProfileCode(),
                    running.publishIndexOnSuccess(),
                    sourceUris.size(),
                    succeeded,
                    failed == 0 ? sourceUris.size() : failed,
                    0,
                    targetGenerationId,
                    "入库失败：没有成功生成任何文本块" + formatFailures(failureMessages),
                    running.options(),
                    running.createdAt(),
                    Instant.now()
            );
            repository.saveIngestionRun(failedRun);
            pipelineObserver.finishSpan(ingestSpan, "FAILED", Map.of("failedDocuments", failed));
            return failedRun;
        }

        if (generationStatsRefresher != null) {
            generationStatsRefresher.accept(targetGenerationId);
        } else {
            repository.refreshIndexVersionStats(targetGenerationId);
        }

        IngestionRunStatus finalStatus = failed > 0 ? IngestionRunStatus.PARTIAL_FAILED : IngestionRunStatus.SUCCEEDED;
        IngestionRun completed = new IngestionRun(
                running.runId(),
                running.libraryId(),
                finalStatus,
                running.sourceUris(),
                running.sourceType(),
                running.documentProfileCode(),
                running.publishIndexOnSuccess(),
                sourceUris.size(),
                succeeded,
                failed,
                chunkCount,
                targetGenerationId,
                finalStatus == IngestionRunStatus.SUCCEEDED
                        ? "入库完成，" + succeeded + " 个文档已写入当前索引代次"
                        : "入库部分成功，请检查失败文档" + formatFailures(failureMessages),
                running.options(),
                running.createdAt(),
                Instant.now()
        );
        pipelineObserver.finishSpan(ingestSpan, finalStatus.name(), Map.of(
                "succeededDocuments", succeeded,
                "failedDocuments", failed,
                "chunkCount", chunkCount
        ));
        return repository.saveIngestionRun(completed);
    }

    private UUID resolveTargetGeneration(UUID libraryId, Map<String, Object> options) {
        UUID override = IngestionPipelineOptions.targetIndexGenerationId(options);
        if (override != null) {
            return override;
        }
        return resolveActiveGeneration(libraryId);
    }

    private UUID resolveActiveGeneration(UUID libraryId) {
        if (activeGenerationResolver != null) {
            return activeGenerationResolver.apply(libraryId);
        }
        return repository.findActiveIndexVersion(libraryId)
                .map(version -> version.indexVersionId())
                .orElseGet(() -> repository.findPublishedIndexVersion(libraryId)
                        .map(version -> version.indexVersionId())
                        .orElseThrow(() -> new IllegalStateException("知识库尚未初始化索引代次: " + libraryId)));
    }

    private KnowledgeDocument resolveDocument(
            UUID libraryId,
            UUID generationId,
            String sourceUri,
            DocumentProfile profile,
            Instant now,
            boolean deferDocumentGeneration
    ) {
        return repository.findDocumentBySourceUri(libraryId, sourceUri)
                .map(existing -> new KnowledgeDocument(
                        existing.documentId(),
                        existing.libraryId(),
                        deferDocumentGeneration ? existing.indexVersionId() : generationId,
                        sourceUri,
                        existing.title(),
                        DocumentStatus.UPLOADED,
                        profile.documentProfileId(),
                        sourceUri,
                        existing.lastIndexedAt(),
                        null,
                        existing.createdAt(),
                        now
                ))
                .orElseGet(() -> new KnowledgeDocument(
                        UUID.randomUUID(),
                        libraryId,
                        generationId,
                        sourceUri,
                        null,
                        DocumentStatus.UPLOADED,
                        profile.documentProfileId(),
                        sourceUri,
                        null,
                        null,
                        now,
                        now
                ));
    }

    private IngestionRun runLegacySnapshotMode(
            IngestionRun existing,
            IngestionRequest request,
            LibraryProfile profile,
            List<DocumentProfile> documentProfiles,
            String resolvedProfileCode,
            List<String> sourceUris,
            Map<String, Object> requestOptions
    ) {
        IngestionRun running = updateStatus(
                existing,
                IngestionRunStatus.RUNNING,
                sourceUris,
                sourceUris.size(),
                "正在执行入库 Pipeline（快照模式），已展开 " + sourceUris.size() + " 个文档来源"
        );
        repository.saveIngestionRun(running);
        UUID ingestSpan = pipelineObserver.startSpan("ingestion", request.runId(), "pipeline", Map.of("libraryId", request.libraryId().toString()));

        UUID draftIndexVersionId = UUID.randomUUID();
        IndexVersion draftIndex = new IndexVersion(
                draftIndexVersionId,
                request.libraryId(),
                profile.profileId(),
                nextIndexVersion(request.libraryId()),
                IndexVersionStatus.BUILDING,
                0,
                0,
                null,
                Instant.now()
        );
        repository.saveIndexVersion(draftIndex);

        int succeeded = 0;
        int failed = 0;
        int chunkCount = 0;
        List<IndexedChunk> indexedChunks = new ArrayList<>();
        List<String> failureMessages = new ArrayList<>();

        for (String sourceUri : sourceUris) {
            UUID documentSpan = pipelineObserver.startSpan("ingestion", request.runId(), "document", Map.of("sourceUri", sourceUri));
            try {
                DocumentProfile resolvedProfile = documentProfileResolver.resolve(
                        sourceUri,
                        resolvedProfileCode,
                        documentProfiles
                );
                Map<String, Object> sourceOptions = mergeDocumentProfileOptions(
                        requestOptions,
                        resolvedProfile,
                        documentProfileResolver.routingMetadata(sourceUri, resolvedProfile)
                );
                sourceOptions = ParseOptionsSupport.applyParseMode(sourceOptions, sourceUri);
                DocumentProfile documentProfile = SegmentationOptionsSupport.applyDocumentProfileOverrides(
                        resolvedProfile,
                        requestOptions
                );
                TokenizerProfile tokenizerProfile = resolveTokenizerProfile(profile, documentProfile);
                ModelTokenizer tokenizer = resolveTokenizer(profile, tokenizerProfile);
                sourceOptions = withTokenizerMetadata(sourceOptions, tokenizerProfile, tokenizer);
                UUID documentId = UUID.randomUUID();
                DocumentPreparationResult prepared = documentPreparationPipeline.prepare(
                        sourceUri,
                        sourceOptions,
                        request.libraryId(),
                        documentId,
                        draftIndexVersionId,
                        profile,
                        documentProfile,
                        tokenizer,
                        PreparationStage.CHUNK
                );
                List<DocumentChunk> chunks = prepared.chunks();
                List<DocumentChunk> indexableChunks = chunks.stream()
                        .filter(DefaultIngestionPipeline::isIndexableChunk)
                        .toList();
                List<float[]> embeddings = embedChunks(embeddingModelClient, profile, indexableChunks);
                for (int index = 0; index < indexableChunks.size(); index++) {
                    indexedChunks.add(new IndexedChunk(indexableChunks.get(index), embeddings.get(index)));
                }
                for (DocumentChunk chunk : chunks) {
                    if (!isIndexableChunk(chunk)) {
                        indexedChunks.add(new IndexedChunk(chunk, null));
                    }
                }
                chunkCount += indexableChunks.size();
                succeeded++;
                pipelineObserver.finishSpan(documentSpan, "SUCCEEDED", Map.of("chunkCount", chunks.size()));
            } catch (RuntimeException exception) {
                failed++;
                failureMessages.add(shortFailure(sourceUri, exception));
                pipelineObserver.recordIngestionError(request.runId(), sourceUri, "INGEST_DOCUMENT_FAILED", exception.getMessage());
                pipelineObserver.finishSpan(documentSpan, "FAILED", Map.of("error", exception.getMessage()));
            }
        }

        if (chunkCount == 0) {
            IngestionRun failedRun = new IngestionRun(
                    running.runId(),
                    running.libraryId(),
                    IngestionRunStatus.FAILED,
                    running.sourceUris(),
                    running.sourceType(),
                    running.documentProfileCode(),
                    running.publishIndexOnSuccess(),
                    sourceUris.size(),
                    succeeded,
                    failed == 0 ? sourceUris.size() : failed,
                    0,
                    null,
                    "入库失败：没有成功生成任何文本块" + formatFailures(failureMessages),
                    running.options(),
                    running.createdAt(),
                    Instant.now()
            );
            repository.saveIngestionRun(failedRun);
            pipelineObserver.finishSpan(ingestSpan, "FAILED", Map.of("failedDocuments", failed));
            repository.saveIndexVersion(new IndexVersion(
                    draftIndexVersionId,
                    request.libraryId(),
                    profile.profileId(),
                    draftIndex.version(),
                    IndexVersionStatus.FAILED,
                    succeeded,
                    0,
                    null,
                    draftIndex.createdAt()
            ));
            return failedRun;
        }

        repository.saveIndexedChunks(indexedChunks);

        UUID publishedIndexVersionId = request.publishIndexOnSuccess() ? draftIndexVersionId : null;
        IndexVersionStatus indexStatus = request.publishIndexOnSuccess()
                ? IndexVersionStatus.PUBLISHED
                : IndexVersionStatus.DRAFT;
        Instant publishedAt = request.publishIndexOnSuccess() ? Instant.now() : null;
        repository.saveIndexVersion(new IndexVersion(
                draftIndexVersionId,
                request.libraryId(),
                profile.profileId(),
                draftIndex.version(),
                indexStatus,
                succeeded,
                chunkCount,
                publishedAt,
                draftIndex.createdAt()
        ));
        if (request.publishIndexOnSuccess()) {
            repository.archivePublishedGenerationsExcept(request.libraryId(), draftIndexVersionId);
            repository.setActiveIndexGeneration(request.libraryId(), draftIndexVersionId);
        }

        IngestionRunStatus finalStatus = failed > 0 ? IngestionRunStatus.PARTIAL_FAILED : IngestionRunStatus.SUCCEEDED;
        IngestionRun completed = new IngestionRun(
                running.runId(),
                running.libraryId(),
                finalStatus,
                running.sourceUris(),
                running.sourceType(),
                running.documentProfileCode(),
                running.publishIndexOnSuccess(),
                sourceUris.size(),
                succeeded,
                failed,
                chunkCount,
                publishedIndexVersionId,
                finalStatus == IngestionRunStatus.SUCCEEDED
                        ? "入库完成，已发布索引快照 v" + draftIndex.version()
                        : "入库部分成功，请检查失败文档" + formatFailures(failureMessages),
                running.options(),
                running.createdAt(),
                Instant.now()
        );
        pipelineObserver.finishSpan(ingestSpan, finalStatus.name(), Map.of(
                "succeededDocuments", succeeded,
                "failedDocuments", failed,
                "chunkCount", chunkCount
        ));
        return repository.saveIngestionRun(completed);
    }

    private int nextIndexVersion(UUID libraryId) {
        return repository.listIndexVersions(libraryId).stream()
                .mapToInt(IndexVersion::version)
                .max()
                .orElse(0) + 1;
    }

    private KnowledgeDocument resolveDocument(
            UUID libraryId,
            UUID generationId,
            String sourceUri,
            DocumentProfile profile,
            Instant now
    ) {
        return resolveDocument(libraryId, generationId, sourceUri, profile, now, false);
    }

    private static KnowledgeDocument markStatus(KnowledgeDocument document, DocumentStatus status, String error) {
        Instant now = Instant.now();
        return new KnowledgeDocument(
                document.documentId(),
                document.libraryId(),
                document.indexVersionId(),
                document.sourceUri(),
                document.title(),
                status,
                document.documentProfileId(),
                document.contentHash(),
                document.lastIndexedAt(),
                error,
                document.createdAt(),
                now
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
        if (indexable instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return chunk.parentChunkId() != null;
    }

    private static List<float[]> embedChunks(
            EmbeddingModelClient embeddingModelClient,
            LibraryProfile profile,
            List<DocumentChunk> chunks
    ) {
        List<String> texts = chunks.stream().map(DocumentChunk::content).toList();
        if (embeddingModelClient instanceof OllamaEmbeddingModelClient ollamaClient) {
            String model = profile.embeddingModel() == null || profile.embeddingModel().isBlank()
                    ? embeddingModelClient.modelName()
                    : profile.embeddingModel();
            return ollamaClient.embed(model, texts);
        }
        return embeddingModelClient.embed(texts);
    }

    private static Map<String, Object> mergeDocumentProfileOptions(
            Map<String, Object> requestOptions,
            DocumentProfile documentProfile,
            Map<String, Object> routingMetadata
    ) {
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>();
        if (requestOptions != null) {
            merged.putAll(requestOptions);
        }
        if (routingMetadata != null) {
            merged.putAll(routingMetadata);
        }
        merged.putIfAbsent("parserCode", documentProfile.parserCode());
        merged.putIfAbsent("documentProfileCode", documentProfile.code());
        merged.putIfAbsent("contentFamily", documentProfile.contentFamily().name());
        merged.putIfAbsent("chunkingStrategy", documentProfile.chunkingStrategy());
        merged.putIfAbsent("metadataSchema", documentProfile.metadataSchema());
        merged.putIfAbsent("documentProfileOptions", documentProfile.options());
        return Map.copyOf(merged);
    }

    private TokenizerProfile resolveTokenizerProfile(LibraryProfile profile, DocumentProfile documentProfile) {
        UUID profileId = documentProfile != null && documentProfile.tokenizerProfileId() != null
                ? documentProfile.tokenizerProfileId()
                : profile.embeddingTokenizerProfileId();
        if (profileId != null) {
            return repository.findTokenizerProfile(profileId)
                    .orElseThrow(() -> new IllegalStateException("Tokenizer Profile 不存在: " + profileId));
        }
        return repository.findTokenizerProfile(profile.embeddingProvider(), profile.embeddingModel()).orElse(null);
    }

    private ModelTokenizer resolveTokenizer(LibraryProfile libraryProfile, TokenizerProfile tokenizerProfile) {
        ModelTokenizer delegate = tokenizerRegistry.getTokenizer(libraryProfile.embeddingProvider(), libraryProfile.embeddingModel());
        if (tokenizerProfile == null) {
            return delegate;
        }
        return new ProfileBackedTokenizer(
                tokenizerProfile.tokenizerId(),
                tokenizerProfile.tokenizerVersion(),
                tokenizerProfile.approximate(),
                delegate
        );
    }

    private static Map<String, Object> withTokenizerMetadata(
            Map<String, Object> options,
            TokenizerProfile tokenizerProfile,
            ModelTokenizer tokenizer
    ) {
        java.util.HashMap<String, Object> enriched = new java.util.HashMap<>();
        if (options != null) {
            enriched.putAll(options);
        }
        enriched.put("tokenizerId", tokenizer.tokenizerId());
        enriched.put("tokenizerVersion", tokenizer.tokenizerVersion());
        enriched.put("tokenizerApproximate", tokenizer.approximate());
        if (tokenizerProfile != null) {
            enriched.put("tokenizerProfileId", tokenizerProfile.tokenizerProfileId().toString());
            enriched.put("tokenizerProvider", tokenizerProfile.provider());
            enriched.put("tokenizerModelName", tokenizerProfile.modelName());
        }
        return Map.copyOf(enriched);
    }

    private static String shortFailure(String sourceUri, RuntimeException exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        return sourceUri + " -> " + message;
    }

    private static String formatFailures(List<String> failureMessages) {
        if (failureMessages.isEmpty()) {
            return "";
        }
        return "；失败样例：" + String.join("；", failureMessages.stream().limit(3).toList());
    }

    private static IngestionRun updateStatus(
            IngestionRun run,
            IngestionRunStatus status,
            List<String> sourceUris,
            int inputDocuments,
            String message
    ) {
        return new IngestionRun(
                run.runId(),
                run.libraryId(),
                status,
                sourceUris,
                run.sourceType(),
                run.documentProfileCode(),
                run.publishIndexOnSuccess(),
                inputDocuments,
                run.succeededDocuments(),
                run.failedDocuments(),
                run.chunkCount(),
                run.indexVersionId(),
                message,
                run.options(),
                run.createdAt(),
                Instant.now()
        );
    }
}
