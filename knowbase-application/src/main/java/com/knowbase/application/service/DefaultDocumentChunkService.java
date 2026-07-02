package com.knowbase.application.service;

import com.knowbase.api.command.UpdateDocumentChunkCommand;
import com.knowbase.api.result.DocumentChunkResult;
import com.knowbase.application.security.AccessControlService;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.security.AclPermission;
import com.knowbase.model.EmbeddingModelClient;
import com.knowbase.model.ollama.OllamaEmbeddingModelClient;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultDocumentChunkService {

    private final KnowbaseRepository repository;
    private final AccessControlService accessControlService;
    private final EmbeddingModelClient embeddingModelClient;
    private final TokenizerRegistry tokenizerRegistry;

    public DefaultDocumentChunkService(
            KnowbaseRepository repository,
            AccessControlService accessControlService,
            EmbeddingModelClient embeddingModelClient,
            TokenizerRegistry tokenizerRegistry
    ) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.embeddingModelClient = embeddingModelClient;
        this.tokenizerRegistry = tokenizerRegistry;
    }

    public DocumentChunkResult updateChunk(
            UUID libraryId,
            UUID documentId,
            UUID chunkId,
            UpdateDocumentChunkCommand command
    ) {
        accessControlService.requireLibraryAccess(libraryId, AclPermission.WRITE);
        KnowledgeDocument document = repository.findDocument(documentId)
                .filter(item -> item.libraryId().equals(libraryId))
                .orElseThrow(() -> new ResourceNotFoundException("文档不存在: " + documentId));
        DocumentChunk existing = repository.findChunk(chunkId)
                .filter(chunk -> chunk.documentId().equals(documentId))
                .orElseThrow(() -> new ResourceNotFoundException("文档块不存在: " + chunkId));

        boolean contentChanged = command.content() != null && !command.content().isBlank()
                && !command.content().equals(existing.content());
        boolean retrievalChanged = command.retrievalEnabled() != null;
        if (!contentChanged && !retrievalChanged) {
            throw new IllegalArgumentException("请提供 content 或 retrievalEnabled");
        }

        String nextContent = contentChanged ? command.content().trim() : existing.content();
        Map<String, Object> metadata = new HashMap<>(existing.metadata() == null ? Map.of() : existing.metadata());
        if (retrievalChanged) {
            metadata.put("retrievalEnabled", command.retrievalEnabled());
        }

        int tokenCount = existing.tokenCount();
        float[] embedding = repository.findChunkEmbedding(chunkId).orElse(null);
        if (contentChanged) {
            LibraryProfile profile = repository.findLatestLibraryProfile(libraryId)
                    .orElseThrow(() -> new IllegalStateException("知识库缺少 Library Profile: " + libraryId));
            ModelTokenizer tokenizer = resolveTokenizer(profile);
            tokenCount = tokenizer.count(nextContent).tokens();
            embedding = embedSingle(profile, nextContent);
        }

        DocumentChunk updated = new DocumentChunk(
                existing.chunkId(),
                existing.documentId(),
                existing.libraryId(),
                existing.indexVersionId(),
                nextContent,
                tokenCount,
                existing.tokenizerId(),
                existing.tokenizerVersion(),
                existing.embeddingModel(),
                existing.chunkBoundaryType(),
                existing.parentChunkId(),
                Map.copyOf(metadata)
        );
        repository.updateIndexedChunk(new IndexedChunk(updated, embedding));
        if (contentChanged) {
            repository.refreshIndexVersionStats(document.indexVersionId());
        }
        return toResult(updated);
    }

    private ModelTokenizer resolveTokenizer(LibraryProfile libraryProfile) {
        TokenizerProfile tokenizerProfile = null;
        if (libraryProfile.embeddingTokenizerProfileId() != null) {
            tokenizerProfile = repository.findTokenizerProfile(libraryProfile.embeddingTokenizerProfileId()).orElse(null);
        }
        if (tokenizerProfile == null) {
            tokenizerProfile = repository.findTokenizerProfile(
                    libraryProfile.embeddingProvider(),
                    libraryProfile.embeddingModel()
            ).orElse(null);
        }
        ModelTokenizer delegate = tokenizerRegistry.getTokenizer(
                libraryProfile.embeddingProvider(),
                libraryProfile.embeddingModel()
        );
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

    private float[] embedSingle(LibraryProfile profile, String content) {
        if (embeddingModelClient instanceof OllamaEmbeddingModelClient ollamaClient) {
            String model = profile.embeddingModel() == null || profile.embeddingModel().isBlank()
                    ? embeddingModelClient.modelName()
                    : profile.embeddingModel();
            return ollamaClient.embed(model, List.of(content)).getFirst();
        }
        return embeddingModelClient.embed(List.of(content)).getFirst();
    }

    private static DocumentChunkResult toResult(DocumentChunk chunk) {
        return new DocumentChunkResult(
                chunk.chunkId(),
                chunk.documentId(),
                chunk.libraryId(),
                chunk.indexVersionId(),
                chunk.content(),
                chunk.tokenCount(),
                chunk.tokenizerId(),
                chunk.tokenizerVersion(),
                chunk.embeddingModel(),
                chunk.chunkBoundaryType(),
                chunk.parentChunkId(),
                chunk.metadata()
        );
    }
}
