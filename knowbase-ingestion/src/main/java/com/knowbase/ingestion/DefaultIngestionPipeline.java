package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.observability.PipelineObserver;
import com.knowbase.domain.repository.KnowbaseRepository;
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

public final class DefaultIngestionPipeline implements IngestionPipeline {

    private final KnowbaseRepository repository;
    private final DocumentSourceLoader sourceLoader;
    private final TokenBasedDocumentChunker documentChunker;
    private final EmbeddingModelClient embeddingModelClient;
    private final TokenizerRegistry tokenizerRegistry;
    private final PipelineObserver pipelineObserver;
    private final DocumentSourceUriExpander sourceUriExpander = new DocumentSourceUriExpander();
    private final DocumentProfileResolver documentProfileResolver = new DocumentProfileResolver();

    public DefaultIngestionPipeline(
            KnowbaseRepository repository,
            DocumentSourceLoader sourceLoader,
            TokenBasedDocumentChunker documentChunker,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry,
            PipelineObserver pipelineObserver
    ) {
        this.repository = repository;
        this.sourceLoader = sourceLoader;
        this.documentChunker = documentChunker;
        this.embeddingModelClient = embeddingModelClient;
        this.tokenizerRegistry = tokenizerRegistry;
        this.pipelineObserver = pipelineObserver == null ? new com.knowbase.domain.observability.NoopPipelineObserver() : pipelineObserver;
    }

    @Override
    public IngestionRun run(IngestionRequest request) {
        IngestionRun existing = repository.findIngestionRun(request.runId())
                .orElseThrow(() -> new IllegalStateException("入库运行不存在: " + request.runId()));
        LibraryProfile profile = repository.findLatestLibraryProfile(request.libraryId())
                .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + request.libraryId()));
        List<DocumentProfile> documentProfiles = repository.listDocumentProfiles(request.libraryId());
        if (documentProfiles.isEmpty()) {
            throw new IllegalStateException("知识库未配置文档 Profile: " + request.libraryId());
        }
        List<String> sourceUris = sourceUriExpander.expand(request.sourceUris(), request.options());
        if (sourceUris.isEmpty()) {
            throw new IllegalStateException("未发现可入库的文档来源: " + request.sourceUris());
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
                DocumentProfile documentProfile = documentProfileResolver.resolve(
                        sourceUri,
                        request.documentProfileCode(),
                        documentProfiles
                );
                Map<String, Object> sourceOptions = mergeDocumentProfileOptions(
                        request.options(),
                        documentProfile,
                        documentProfileResolver.routingMetadata(sourceUri, documentProfile)
                );
                TokenizerProfile tokenizerProfile = resolveTokenizerProfile(profile, documentProfile);
                ModelTokenizer tokenizer = resolveTokenizer(profile, tokenizerProfile);
                sourceOptions = withTokenizerMetadata(sourceOptions, tokenizerProfile, tokenizer);
                ParsedDocument parsed = ensureExtractedText(enrichMetadata(sourceLoader.load(sourceUri, sourceOptions), sourceOptions));
                UUID documentId = UUID.randomUUID();
                List<DocumentChunk> chunks = documentChunker.chunk(
                        request.libraryId(),
                        documentId,
                        draftIndexVersionId,
                        parsed,
                        profile,
                        documentProfile,
                        tokenizer
                );
                List<float[]> embeddings = embedChunks(embeddingModelClient, profile, chunks);
                for (int index = 0; index < chunks.size(); index++) {
                    indexedChunks.add(new IndexedChunk(chunks.get(index), embeddings.get(index)));
                }
                chunkCount += chunks.size();
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
                        ? "入库完成，已发布索引版本 " + publishedIndexVersionId
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

    private int nextIndexVersion(UUID libraryId) {
        return repository.findPublishedIndexVersion(libraryId)
                .map(version -> version.version() + 1)
                .orElse(1);
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

    private static ParsedDocument enrichMetadata(ParsedDocument parsed, Map<String, Object> metadata) {
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>();
        if (parsed.metadata() != null) {
            merged.putAll(parsed.metadata());
        }
        if (metadata != null) {
            merged.putAll(metadata);
        }
        return new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                parsed.text(),
                parsed.contentFamily(),
                Map.copyOf(merged)
        );
    }

    private static ParsedDocument ensureExtractedText(ParsedDocument parsed) {
        if (parsed.text() == null || parsed.text().isBlank()) {
            throw new IllegalStateException("文档未提取到可索引文本: " + parsed.sourceUri());
        }
        return parsed;
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
