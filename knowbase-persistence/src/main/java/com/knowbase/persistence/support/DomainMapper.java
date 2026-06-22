package com.knowbase.persistence.support;

import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.status.AgentStatus;
import com.knowbase.domain.status.AgentVersionStatus;
import com.knowbase.domain.status.ContentFamily;
import com.knowbase.domain.status.IndexVersionStatus;
import com.knowbase.domain.status.IngestionRunStatus;
import com.knowbase.domain.status.LibraryStatus;
import com.knowbase.domain.status.QueryRunStatus;
import com.knowbase.persistence.entity.AgentEntity;
import com.knowbase.persistence.entity.AgentVersionEntity;
import com.knowbase.persistence.entity.ChunkEntity;
import com.knowbase.persistence.entity.DocumentEntity;
import com.knowbase.persistence.entity.DocumentProfileEntity;
import com.knowbase.persistence.entity.IndexVersionEntity;
import com.knowbase.persistence.entity.IngestionRunEntity;
import com.knowbase.persistence.entity.LibraryEntity;
import com.knowbase.persistence.entity.LibraryProfileEntity;
import com.knowbase.persistence.entity.QueryRunEntity;
import com.knowbase.persistence.entity.TokenizerProfileEntity;

public final class DomainMapper {

    private DomainMapper() {
    }

    public static LibraryEntity toLibraryEntity(KnowledgeLibrary library) {
        LibraryEntity entity = new LibraryEntity();
        entity.setLibraryId(library.libraryId());
        entity.setTenantId(library.tenantId());
        entity.setName(library.name());
        entity.setDescription(library.description());
        entity.setStatus(library.status().name());
        entity.setLibraryTypePresetCode(library.libraryTypePresetCode());
        entity.setTags(JsonSupport.write(library.tags()));
        entity.setCreatedAt(library.createdAt());
        entity.setUpdatedAt(library.updatedAt());
        return entity;
    }

    public static KnowledgeLibrary toLibrary(LibraryEntity entity) {
        return new KnowledgeLibrary(
                entity.getLibraryId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                LibraryStatus.valueOf(entity.getStatus()),
                entity.getLibraryTypePresetCode(),
                JsonSupport.readStringList(entity.getTags()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static LibraryProfileEntity toLibraryProfileEntity(LibraryProfile profile) {
        LibraryProfileEntity entity = new LibraryProfileEntity();
        entity.setProfileId(profile.profileId());
        entity.setLibraryId(profile.libraryId());
        entity.setVersion(profile.version());
        entity.setEmbeddingProvider(profile.embeddingProvider());
        entity.setEmbeddingModel(profile.embeddingModel());
        entity.setEmbeddingDimension(profile.embeddingDimension());
        entity.setEmbeddingTokenizerProfileId(profile.embeddingTokenizerProfileId());
        entity.setChunkMaxTokens(profile.chunkMaxTokens());
        entity.setChunkOverlapTokens(profile.chunkOverlapTokens());
        entity.setRetrievalTopK(profile.retrievalTopK());
        entity.setOptionsJson(JsonSupport.write(profile.options()));
        entity.setCreatedAt(profile.createdAt());
        return entity;
    }

    public static LibraryProfile toLibraryProfile(LibraryProfileEntity entity) {
        return new LibraryProfile(
                entity.getProfileId(),
                entity.getLibraryId(),
                entity.getVersion(),
                entity.getEmbeddingProvider(),
                entity.getEmbeddingModel(),
                entity.getEmbeddingDimension(),
                entity.getEmbeddingTokenizerProfileId(),
                entity.getChunkMaxTokens(),
                entity.getChunkOverlapTokens(),
                entity.getRetrievalTopK(),
                JsonSupport.readMap(entity.getOptionsJson()),
                entity.getCreatedAt()
        );
    }

    public static TokenizerProfileEntity toTokenizerProfileEntity(TokenizerProfile profile) {
        TokenizerProfileEntity entity = new TokenizerProfileEntity();
        entity.setTokenizerProfileId(profile.tokenizerProfileId());
        entity.setProvider(profile.provider());
        entity.setModelName(profile.modelName());
        entity.setTokenizerId(profile.tokenizerId());
        entity.setTokenizerVersion(profile.tokenizerVersion());
        entity.setApproximate(profile.approximate());
        entity.setConfigJson(JsonSupport.write(profile.config()));
        entity.setEnabled(profile.enabled());
        entity.setCreatedAt(profile.createdAt());
        entity.setUpdatedAt(profile.updatedAt());
        return entity;
    }

    public static TokenizerProfile toTokenizerProfile(TokenizerProfileEntity entity) {
        return new TokenizerProfile(
                entity.getTokenizerProfileId(),
                entity.getProvider(),
                entity.getModelName(),
                entity.getTokenizerId(),
                entity.getTokenizerVersion(),
                Boolean.TRUE.equals(entity.getApproximate()),
                JsonSupport.readMap(entity.getConfigJson()),
                Boolean.TRUE.equals(entity.getEnabled()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static DocumentProfileEntity toDocumentProfileEntity(DocumentProfile profile) {
        DocumentProfileEntity entity = new DocumentProfileEntity();
        entity.setDocumentProfileId(profile.documentProfileId());
        entity.setLibraryId(profile.libraryId());
        entity.setCode(profile.code());
        entity.setContentFamily(profile.contentFamily().name());
        entity.setParserCode(profile.parserCode());
        entity.setChunkingStrategy(profile.chunkingStrategy());
        entity.setTokenizerProfileId(profile.tokenizerProfileId());
        entity.setMetadataSchema(JsonSupport.write(profile.metadataSchema()));
        entity.setOptionsJson(JsonSupport.write(profile.options()));
        entity.setEnabled(profile.enabled());
        return entity;
    }

    public static DocumentProfile toDocumentProfile(DocumentProfileEntity entity) {
        return new DocumentProfile(
                entity.getDocumentProfileId(),
                entity.getLibraryId(),
                entity.getCode(),
                ContentFamily.valueOf(entity.getContentFamily()),
                entity.getParserCode(),
                entity.getChunkingStrategy(),
                entity.getTokenizerProfileId(),
                JsonSupport.readMap(entity.getMetadataSchema()),
                JsonSupport.readMap(entity.getOptionsJson()),
                Boolean.TRUE.equals(entity.getEnabled())
        );
    }

    public static IndexVersionEntity toIndexVersionEntity(IndexVersion version) {
        IndexVersionEntity entity = new IndexVersionEntity();
        entity.setIndexVersionId(version.indexVersionId());
        entity.setLibraryId(version.libraryId());
        entity.setProfileId(version.profileId());
        entity.setVersion(version.version());
        entity.setStatus(version.status().name());
        entity.setDocumentCount(version.documentCount());
        entity.setChunkCount(version.chunkCount());
        entity.setPublishedAt(version.publishedAt());
        entity.setCreatedAt(version.createdAt());
        return entity;
    }

    public static IndexVersion toIndexVersion(IndexVersionEntity entity) {
        return new IndexVersion(
                entity.getIndexVersionId(),
                entity.getLibraryId(),
                entity.getProfileId(),
                entity.getVersion(),
                IndexVersionStatus.valueOf(entity.getStatus()),
                entity.getDocumentCount(),
                entity.getChunkCount(),
                entity.getPublishedAt(),
                entity.getCreatedAt()
        );
    }

    public static IngestionRunEntity toIngestionRunEntity(IngestionRun run) {
        IngestionRunEntity entity = new IngestionRunEntity();
        entity.setRunId(run.runId());
        entity.setLibraryId(run.libraryId());
        entity.setStatus(run.status().name());
        entity.setSourceUris(JsonSupport.write(run.sourceUris()));
        entity.setSourceType(run.sourceType());
        entity.setDocumentProfileCode(run.documentProfileCode());
        entity.setPublishIndexOnSuccess(run.publishIndexOnSuccess());
        entity.setInputDocuments(run.inputDocuments());
        entity.setSucceededDocuments(run.succeededDocuments());
        entity.setFailedDocuments(run.failedDocuments());
        entity.setChunkCount(run.chunkCount());
        entity.setIndexVersionId(run.indexVersionId());
        entity.setMessage(run.message());
        entity.setOptionsJson(JsonSupport.write(run.options()));
        entity.setCreatedAt(run.createdAt());
        entity.setUpdatedAt(run.updatedAt());
        return entity;
    }

    public static IngestionRun toIngestionRun(IngestionRunEntity entity) {
        return new IngestionRun(
                entity.getRunId(),
                entity.getLibraryId(),
                IngestionRunStatus.valueOf(entity.getStatus()),
                JsonSupport.readStringList(entity.getSourceUris()),
                entity.getSourceType(),
                entity.getDocumentProfileCode(),
                Boolean.TRUE.equals(entity.getPublishIndexOnSuccess()),
                entity.getInputDocuments(),
                entity.getSucceededDocuments(),
                entity.getFailedDocuments(),
                entity.getChunkCount(),
                entity.getIndexVersionId(),
                entity.getMessage(),
                JsonSupport.readMap(entity.getOptionsJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static ChunkEntity toChunkEntity(DocumentChunk chunk) {
        ChunkEntity entity = new ChunkEntity();
        entity.setChunkId(chunk.chunkId());
        entity.setDocumentId(chunk.documentId());
        entity.setLibraryId(chunk.libraryId());
        entity.setIndexVersionId(chunk.indexVersionId());
        entity.setContent(chunk.content());
        entity.setTokenCount(chunk.tokenCount());
        entity.setTokenizerId(chunk.tokenizerId());
        entity.setTokenizerVersion(chunk.tokenizerVersion());
        entity.setEmbeddingModel(chunk.embeddingModel());
        entity.setChunkBoundaryType(chunk.chunkBoundaryType());
        entity.setParentChunkId(chunk.parentChunkId());
        entity.setMetadataJson(JsonSupport.write(chunk.metadata()));
        return entity;
    }

    public static DocumentChunk toDocumentChunk(ChunkEntity entity) {
        return new DocumentChunk(
                entity.getChunkId(),
                entity.getDocumentId(),
                entity.getLibraryId(),
                entity.getIndexVersionId(),
                entity.getContent(),
                entity.getTokenCount(),
                entity.getTokenizerId(),
                entity.getTokenizerVersion(),
                entity.getEmbeddingModel(),
                entity.getChunkBoundaryType(),
                entity.getParentChunkId(),
                JsonSupport.readMap(entity.getMetadataJson())
        );
    }

    public static IndexedChunk toIndexedChunk(ChunkEntity chunkEntity, float[] embedding) {
        return new IndexedChunk(toDocumentChunk(chunkEntity), embedding);
    }

    public static AgentEntity toAgentEntity(KnowledgeAgent agent) {
        AgentEntity entity = new AgentEntity();
        entity.setAgentId(agent.agentId());
        entity.setTenantId(agent.tenantId());
        entity.setName(agent.name());
        entity.setDescription(agent.description());
        entity.setStatus(agent.status().name());
        entity.setCreatedAt(agent.createdAt());
        entity.setUpdatedAt(agent.updatedAt());
        return entity;
    }

    public static KnowledgeAgent toAgent(AgentEntity entity) {
        return new KnowledgeAgent(
                entity.getAgentId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                AgentStatus.valueOf(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static AgentVersionEntity toAgentVersionEntity(AgentVersion version) {
        AgentVersionEntity entity = new AgentVersionEntity();
        entity.setAgentVersionId(version.agentVersionId());
        entity.setAgentId(version.agentId());
        entity.setVersion(version.version());
        entity.setStatus(version.status().name());
        entity.setScenePresetCode(version.scenePresetCode());
        entity.setLibraryIds(JsonSupport.write(version.libraryIds()));
        entity.setRoutingPolicyJson(JsonSupport.write(version.routingPolicy()));
        entity.setRetrievalPolicyJson(JsonSupport.write(version.retrievalPolicy()));
        entity.setAnswerPolicyJson(JsonSupport.write(version.answerPolicy()));
        entity.setSystemPrompt(version.systemPrompt());
        entity.setChatTokenizerProfileId(version.chatTokenizerProfileId());
        entity.setPublished(version.published());
        entity.setCreatedAt(version.createdAt());
        return entity;
    }

    public static AgentVersion toAgentVersion(AgentVersionEntity entity) {
        return new AgentVersion(
                entity.getAgentVersionId(),
                entity.getAgentId(),
                entity.getVersion(),
                AgentVersionStatus.valueOf(entity.getStatus()),
                entity.getScenePresetCode(),
                JsonSupport.readUuidList(entity.getLibraryIds()),
                JsonSupport.readMap(entity.getRoutingPolicyJson()),
                JsonSupport.readMap(entity.getRetrievalPolicyJson()),
                JsonSupport.readMap(entity.getAnswerPolicyJson()),
                entity.getSystemPrompt(),
                entity.getChatTokenizerProfileId(),
                Boolean.TRUE.equals(entity.getPublished()),
                entity.getCreatedAt()
        );
    }

    public static KnowledgeDocument toKnowledgeDocument(DocumentEntity entity) {
        return new KnowledgeDocument(
                entity.getDocumentId(),
                entity.getLibraryId(),
                entity.getIndexVersionId(),
                entity.getSourceUri(),
                entity.getTitle(),
                entity.getCreatedAt()
        );
    }

    public static QueryRunEntity toQueryRunEntity(QueryRun queryRun) {
        QueryRunEntity entity = new QueryRunEntity();
        entity.setQueryRunId(queryRun.queryRunId());
        entity.setAgentId(queryRun.agentId());
        entity.setAgentVersionId(queryRun.agentVersionId());
        entity.setStatus(queryRun.status().name());
        entity.setQuestion(queryRun.question());
        entity.setAnswer(queryRun.answer());
        entity.setEvidencePackJson(JsonSupport.write(queryRun.evidencePack()));
        entity.setTraceId(queryRun.traceId());
        entity.setPromptTokens(queryRun.promptTokens());
        entity.setCompletionTokens(queryRun.completionTokens());
        entity.setCreatedAt(queryRun.createdAt());
        entity.setCompletedAt(queryRun.completedAt());
        return entity;
    }

    public static QueryRun toQueryRun(QueryRunEntity entity) {
        return new QueryRun(
                entity.getQueryRunId(),
                entity.getAgentId(),
                entity.getAgentVersionId(),
                QueryRunStatus.valueOf(entity.getStatus()),
                entity.getQuestion(),
                entity.getAnswer(),
                JsonSupport.readEvidencePack(entity.getEvidencePackJson()),
                entity.getTraceId(),
                entity.getPromptTokens() == null ? 0 : entity.getPromptTokens(),
                entity.getCompletionTokens() == null ? 0 : entity.getCompletionTokens(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }
}
