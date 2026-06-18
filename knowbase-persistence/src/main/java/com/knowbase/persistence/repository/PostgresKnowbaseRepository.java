package com.knowbase.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.IngestionDocumentError;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.IndexVersionStatus;
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
import com.knowbase.persistence.mapper.AgentMapper;
import com.knowbase.persistence.mapper.AgentVersionMapper;
import com.knowbase.persistence.mapper.ChunkMapper;
import com.knowbase.persistence.mapper.DocumentMapper;
import com.knowbase.persistence.mapper.DocumentProfileMapper;
import com.knowbase.persistence.mapper.IndexVersionMapper;
import com.knowbase.persistence.mapper.IngestionRunMapper;
import com.knowbase.persistence.mapper.LibraryMapper;
import com.knowbase.persistence.mapper.LibraryProfileMapper;
import com.knowbase.persistence.mapper.QueryRunMapper;
import com.knowbase.persistence.mapper.TokenizerProfileMapper;
import com.knowbase.persistence.store.EmbeddingStore;
import com.knowbase.persistence.support.DomainMapper;
import com.knowbase.persistence.support.VectorSupport;
import com.pgvector.PGvector;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PostgresKnowbaseRepository implements KnowbaseRepository {

    private final LibraryMapper libraryMapper;
    private final LibraryProfileMapper libraryProfileMapper;
    private final TokenizerProfileMapper tokenizerProfileMapper;
    private final DocumentProfileMapper documentProfileMapper;
    private final IndexVersionMapper indexVersionMapper;
    private final IngestionRunMapper ingestionRunMapper;
    private final DocumentMapper documentMapper;
    private final ChunkMapper chunkMapper;
    private final AgentMapper agentMapper;
    private final AgentVersionMapper agentVersionMapper;
    private final QueryRunMapper queryRunMapper;
    private final EmbeddingStore embeddingStore;
    private final JdbcTemplate jdbcTemplate;

    public PostgresKnowbaseRepository(
            LibraryMapper libraryMapper,
            LibraryProfileMapper libraryProfileMapper,
            TokenizerProfileMapper tokenizerProfileMapper,
            DocumentProfileMapper documentProfileMapper,
            IndexVersionMapper indexVersionMapper,
            IngestionRunMapper ingestionRunMapper,
            DocumentMapper documentMapper,
            ChunkMapper chunkMapper,
            AgentMapper agentMapper,
            AgentVersionMapper agentVersionMapper,
            QueryRunMapper queryRunMapper,
            EmbeddingStore embeddingStore,
            JdbcTemplate jdbcTemplate
    ) {
        this.libraryMapper = libraryMapper;
        this.libraryProfileMapper = libraryProfileMapper;
        this.tokenizerProfileMapper = tokenizerProfileMapper;
        this.documentProfileMapper = documentProfileMapper;
        this.indexVersionMapper = indexVersionMapper;
        this.ingestionRunMapper = ingestionRunMapper;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.agentMapper = agentMapper;
        this.agentVersionMapper = agentVersionMapper;
        this.queryRunMapper = queryRunMapper;
        this.embeddingStore = embeddingStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgeLibrary saveLibrary(KnowledgeLibrary library) {
        LibraryEntity entity = DomainMapper.toLibraryEntity(library);
        if (libraryMapper.selectById(library.libraryId()) == null) {
            libraryMapper.insert(entity);
        } else {
            libraryMapper.updateById(entity);
        }
        return library;
    }

    @Override
    public Optional<KnowledgeLibrary> findLibrary(UUID libraryId) {
        LibraryEntity entity = libraryMapper.selectById(libraryId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toLibrary(entity));
    }

    @Override
    public List<KnowledgeLibrary> listLibraries(String tenantId) {
        LambdaQueryWrapper<LibraryEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(LibraryEntity::getTenantId, tenantId);
        }
        return libraryMapper.selectList(wrapper).stream().map(DomainMapper::toLibrary).toList();
    }

    @Override
    public LibraryProfile saveLibraryProfile(LibraryProfile profile) {
        LibraryProfileEntity entity = DomainMapper.toLibraryProfileEntity(profile);
        LibraryProfileEntity existing = libraryProfileMapper.selectOne(new LambdaQueryWrapper<LibraryProfileEntity>()
                .eq(LibraryProfileEntity::getLibraryId, profile.libraryId())
                .eq(LibraryProfileEntity::getVersion, profile.version()));
        if (existing == null) {
            libraryProfileMapper.insert(entity);
        } else {
            entity.setProfileId(existing.getProfileId());
            libraryProfileMapper.updateById(entity);
        }
        return profile;
    }

    @Override
    public Optional<LibraryProfile> findLatestLibraryProfile(UUID libraryId) {
        LibraryProfileEntity entity = libraryProfileMapper.selectOne(new LambdaQueryWrapper<LibraryProfileEntity>()
                .eq(LibraryProfileEntity::getLibraryId, libraryId)
                .orderByDesc(LibraryProfileEntity::getVersion)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toLibraryProfile(entity));
    }

    @Override
    public TokenizerProfile saveTokenizerProfile(TokenizerProfile profile) {
        var entity = DomainMapper.toTokenizerProfileEntity(profile);
        TokenizerProfileEntity existing = tokenizerProfileMapper.selectOne(new LambdaQueryWrapper<TokenizerProfileEntity>()
                .eq(TokenizerProfileEntity::getProvider, profile.provider())
                .eq(TokenizerProfileEntity::getModelName, profile.modelName()));
        if (existing == null) {
            tokenizerProfileMapper.insert(entity);
        } else {
            entity.setTokenizerProfileId(existing.getTokenizerProfileId());
            tokenizerProfileMapper.updateById(entity);
        }
        return profile;
    }

    @Override
    public Optional<TokenizerProfile> findTokenizerProfile(UUID tokenizerProfileId) {
        TokenizerProfileEntity entity = tokenizerProfileMapper.selectById(tokenizerProfileId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toTokenizerProfile(entity));
    }

    @Override
    public Optional<TokenizerProfile> findTokenizerProfile(String provider, String modelName) {
        TokenizerProfileEntity entity = tokenizerProfileMapper.selectOne(new LambdaQueryWrapper<TokenizerProfileEntity>()
                .eq(TokenizerProfileEntity::getProvider, provider)
                .eq(TokenizerProfileEntity::getModelName, modelName)
                .eq(TokenizerProfileEntity::getEnabled, true));
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toTokenizerProfile(entity));
    }

    @Override
    public List<TokenizerProfile> listTokenizerProfiles(String provider, boolean includeDisabled) {
        LambdaQueryWrapper<TokenizerProfileEntity> wrapper = new LambdaQueryWrapper<>();
        if (provider != null && !provider.isBlank()) {
            wrapper.eq(TokenizerProfileEntity::getProvider, provider);
        }
        if (!includeDisabled) {
            wrapper.eq(TokenizerProfileEntity::getEnabled, true);
        }
        wrapper.orderByAsc(TokenizerProfileEntity::getProvider).orderByAsc(TokenizerProfileEntity::getModelName);
        return tokenizerProfileMapper.selectList(wrapper).stream()
                .map(DomainMapper::toTokenizerProfile)
                .toList();
    }

    @Override
    public DocumentProfile saveDocumentProfile(DocumentProfile profile) {
        DocumentProfileEntity entity = DomainMapper.toDocumentProfileEntity(profile);
        DocumentProfileEntity existing = documentProfileMapper.selectOne(new LambdaQueryWrapper<DocumentProfileEntity>()
                .eq(DocumentProfileEntity::getLibraryId, profile.libraryId())
                .eq(DocumentProfileEntity::getCode, profile.code()));
        if (existing == null) {
            documentProfileMapper.insert(entity);
        } else {
            entity.setDocumentProfileId(existing.getDocumentProfileId());
            documentProfileMapper.updateById(entity);
        }
        return profile;
    }

    @Override
    public List<DocumentProfile> listDocumentProfiles(UUID libraryId) {
        return documentProfileMapper.selectList(new LambdaQueryWrapper<DocumentProfileEntity>()
                        .eq(DocumentProfileEntity::getLibraryId, libraryId))
                .stream()
                .map(DomainMapper::toDocumentProfile)
                .toList();
    }

    @Override
    public Optional<DocumentProfile> findDocumentProfile(UUID libraryId, String code) {
        DocumentProfileEntity entity = documentProfileMapper.selectOne(new LambdaQueryWrapper<DocumentProfileEntity>()
                .eq(DocumentProfileEntity::getLibraryId, libraryId)
                .eq(DocumentProfileEntity::getCode, code));
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toDocumentProfile(entity));
    }

    @Override
    public IngestionRun saveIngestionRun(IngestionRun run) {
        IngestionRunEntity entity = DomainMapper.toIngestionRunEntity(run);
        if (ingestionRunMapper.selectById(run.runId()) == null) {
            ingestionRunMapper.insert(entity);
        } else {
            ingestionRunMapper.updateById(entity);
        }
        return run;
    }

    @Override
    public Optional<IngestionRun> findIngestionRun(UUID runId) {
        IngestionRunEntity entity = ingestionRunMapper.selectById(runId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toIngestionRun(entity));
    }

    @Override
    public IndexVersion saveIndexVersion(IndexVersion indexVersion) {
        IndexVersionEntity entity = DomainMapper.toIndexVersionEntity(indexVersion);
        if (indexVersionMapper.selectById(indexVersion.indexVersionId()) == null) {
            indexVersionMapper.insert(entity);
        } else {
            indexVersionMapper.updateById(entity);
        }
        return indexVersion;
    }

    @Override
    public Optional<IndexVersion> findPublishedIndexVersion(UUID libraryId) {
        IndexVersionEntity entity = indexVersionMapper.selectOne(new LambdaQueryWrapper<IndexVersionEntity>()
                .eq(IndexVersionEntity::getLibraryId, libraryId)
                .eq(IndexVersionEntity::getStatus, IndexVersionStatus.PUBLISHED.name())
                .orderByDesc(IndexVersionEntity::getVersion)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toIndexVersion(entity));
    }

    @Override
    public List<IndexVersion> listIndexVersions(UUID libraryId) {
        return indexVersionMapper.selectList(new LambdaQueryWrapper<IndexVersionEntity>()
                        .eq(IndexVersionEntity::getLibraryId, libraryId)
                        .orderByDesc(IndexVersionEntity::getVersion))
                .stream()
                .map(DomainMapper::toIndexVersion)
                .toList();
    }

    @Override
    public Optional<IndexVersion> findIndexVersion(UUID indexVersionId) {
        IndexVersionEntity entity = indexVersionMapper.selectById(indexVersionId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toIndexVersion(entity));
    }

    @Override
    public List<KnowledgeDocument> listDocuments(UUID libraryId, UUID indexVersionId) {
        LambdaQueryWrapper<DocumentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentEntity::getLibraryId, libraryId);
        if (indexVersionId != null) {
            wrapper.eq(DocumentEntity::getIndexVersionId, indexVersionId);
        }
        wrapper.orderByDesc(DocumentEntity::getCreatedAt);
        return documentMapper.selectList(wrapper).stream().map(DomainMapper::toKnowledgeDocument).toList();
    }

    @Override
    public Optional<KnowledgeDocument> findDocument(UUID documentId) {
        DocumentEntity entity = documentMapper.selectById(documentId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toKnowledgeDocument(entity));
    }

    @Override
    public List<DocumentChunk> listChunksByDocument(UUID documentId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<ChunkEntity>()
                        .eq(ChunkEntity::getDocumentId, documentId)
                        .orderByAsc(ChunkEntity::getChunkId))
                .stream()
                .map(DomainMapper::toDocumentChunk)
                .toList();
    }

    @Override
    public List<IngestionDocumentError> listIngestionDocumentErrors(UUID runId) {
        return jdbcTemplate.query(
                """
                        SELECT error_id, run_id, source_uri, error_code, error_message, created_at
                        FROM kb_ingestion_document_error
                        WHERE run_id = ?
                        ORDER BY created_at ASC
                        """,
                (rs, rowNum) -> new IngestionDocumentError(
                        rs.getObject("error_id", UUID.class),
                        rs.getObject("run_id", UUID.class),
                        rs.getString("source_uri"),
                        rs.getString("error_code"),
                        rs.getString("error_message"),
                        rs.getTimestamp("created_at").toInstant()
                ),
                runId
        );
    }

    @Override
    public IngestionDocumentError saveIngestionDocumentError(IngestionDocumentError error) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_ingestion_document_error (error_id, run_id, source_uri, error_code, error_message, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (error_id) DO NOTHING
                        """,
                error.errorId(),
                error.runId(),
                error.sourceUri(),
                error.errorCode(),
                error.errorMessage(),
                java.sql.Timestamp.from(error.createdAt())
        );
        return error;
    }

    @Override
    public List<IndexedChunk> listChunksByIndexVersion(UUID indexVersionId) {
        return jdbcTemplate.execute((ConnectionCallback<List<IndexedChunk>>) connection -> {
            PGvector.addVectorType(connection);
            try (var statement = connection.prepareStatement(
                    """
                            SELECT c.chunk_id,
                                   c.document_id,
                                   c.library_id,
                                   c.index_version_id,
                                   c.content,
                                   c.token_count,
                                   c.tokenizer_id,
                                   c.tokenizer_version,
                                   c.embedding_model,
                                   c.chunk_boundary_type,
                                   c.parent_chunk_id,
                                   c.metadata_json,
                                   e.embedding
                            FROM kb_chunk c
                            INNER JOIN kb_embedding e ON c.chunk_id = e.chunk_id
                            WHERE c.index_version_id = ?
                            """
            )) {
                statement.setObject(1, indexVersionId);
                try (var resultSet = statement.executeQuery()) {
                    List<IndexedChunk> chunks = new java.util.ArrayList<>();
                    while (resultSet.next()) {
                        ChunkEntity chunkEntity = new ChunkEntity();
                        chunkEntity.setChunkId(resultSet.getObject("chunk_id", UUID.class));
                        chunkEntity.setDocumentId(resultSet.getObject("document_id", UUID.class));
                        chunkEntity.setLibraryId(resultSet.getObject("library_id", UUID.class));
                        chunkEntity.setIndexVersionId(resultSet.getObject("index_version_id", UUID.class));
                        chunkEntity.setContent(resultSet.getString("content"));
                        chunkEntity.setTokenCount(resultSet.getInt("token_count"));
                        chunkEntity.setTokenizerId(resultSet.getString("tokenizer_id"));
                        chunkEntity.setTokenizerVersion(resultSet.getString("tokenizer_version"));
                        chunkEntity.setEmbeddingModel(resultSet.getString("embedding_model"));
                        chunkEntity.setChunkBoundaryType(resultSet.getString("chunk_boundary_type"));
                        chunkEntity.setParentChunkId(resultSet.getObject("parent_chunk_id", UUID.class));
                        chunkEntity.setMetadataJson(resultSet.getString("metadata_json"));
                        chunks.add(DomainMapper.toIndexedChunk(
                                chunkEntity,
                                VectorSupport.fromPgVector(resultSet.getObject("embedding"))
                        ));
                    }
                    return chunks;
                }
            }
        });
    }

    @Override
    public void saveIndexedChunks(List<IndexedChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        Map<UUID, DocumentEntity> documents = new HashMap<>();
        Instant now = Instant.now();
        for (IndexedChunk indexedChunk : chunks) {
            DocumentChunk chunk = indexedChunk.chunk();
            documents.computeIfAbsent(chunk.documentId(), documentId -> {
                DocumentEntity document = new DocumentEntity();
                document.setDocumentId(documentId);
                document.setLibraryId(chunk.libraryId());
                document.setIndexVersionId(chunk.indexVersionId());
                Object sourceUri = chunk.metadata() == null ? null : chunk.metadata().get("sourceUri");
                Object title = chunk.metadata() == null ? null : chunk.metadata().get("title");
                document.setSourceUri(sourceUri == null ? null : String.valueOf(sourceUri));
                document.setTitle(title == null ? null : String.valueOf(title));
                document.setCreatedAt(now);
                return document;
            });
        }
        for (DocumentEntity document : documents.values()) {
            if (documentMapper.selectById(document.getDocumentId()) == null) {
                documentMapper.insert(document);
            }
        }
        for (IndexedChunk indexedChunk : chunks) {
            DocumentChunk chunk = indexedChunk.chunk();
            ChunkEntity chunkEntity = DomainMapper.toChunkEntity(chunk);
            if (chunkMapper.selectById(chunk.chunkId()) == null) {
                chunkMapper.insert(chunkEntity);
            } else {
                chunkMapper.updateById(chunkEntity);
            }
            embeddingStore.insertEmbedding(
                    UUID.randomUUID(),
                    chunk.chunkId(),
                    chunk.embeddingModel(),
                    indexedChunk.embedding().length,
                    indexedChunk.embedding()
            );
        }
    }

    @Override
    public KnowledgeAgent saveAgent(KnowledgeAgent agent) {
        AgentEntity entity = DomainMapper.toAgentEntity(agent);
        if (agentMapper.selectById(agent.agentId()) == null) {
            agentMapper.insert(entity);
        } else {
            agentMapper.updateById(entity);
        }
        return agent;
    }

    @Override
    public Optional<KnowledgeAgent> findAgent(UUID agentId) {
        AgentEntity entity = agentMapper.selectById(agentId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toAgent(entity));
    }

    @Override
    public List<KnowledgeAgent> listAgents(String tenantId) {
        LambdaQueryWrapper<AgentEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(AgentEntity::getTenantId, tenantId);
        }
        return agentMapper.selectList(wrapper).stream().map(DomainMapper::toAgent).toList();
    }

    @Override
    public AgentVersion saveAgentVersion(AgentVersion version) {
        AgentVersionEntity entity = DomainMapper.toAgentVersionEntity(version);
        if (agentVersionMapper.selectById(version.agentVersionId()) == null) {
            agentVersionMapper.insert(entity);
        } else {
            agentVersionMapper.updateById(entity);
        }
        return version;
    }

    @Override
    public Optional<AgentVersion> findAgentVersion(UUID agentVersionId) {
        AgentVersionEntity entity = agentVersionMapper.selectById(agentVersionId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toAgentVersion(entity));
    }

    @Override
    public Optional<AgentVersion> findPublishedAgentVersion(UUID agentId) {
        AgentVersionEntity entity = agentVersionMapper.selectOne(new LambdaQueryWrapper<AgentVersionEntity>()
                .eq(AgentVersionEntity::getAgentId, agentId)
                .eq(AgentVersionEntity::getPublished, true)
                .orderByDesc(AgentVersionEntity::getVersion)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toAgentVersion(entity));
    }

    @Override
    public List<AgentVersion> listAgentVersions(UUID agentId) {
        return agentVersionMapper.selectList(new LambdaQueryWrapper<AgentVersionEntity>()
                        .eq(AgentVersionEntity::getAgentId, agentId)
                        .orderByDesc(AgentVersionEntity::getVersion))
                .stream()
                .map(DomainMapper::toAgentVersion)
                .toList();
    }

    @Override
    public QueryRun saveQueryRun(QueryRun queryRun) {
        QueryRunEntity entity = DomainMapper.toQueryRunEntity(queryRun);
        if (queryRunMapper.selectById(queryRun.queryRunId()) == null) {
            queryRunMapper.insert(entity);
        } else {
            queryRunMapper.updateById(entity);
        }
        return queryRun;
    }

    @Override
    public Optional<QueryRun> findQueryRun(UUID queryRunId) {
        QueryRunEntity entity = queryRunMapper.selectById(queryRunId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toQueryRun(entity));
    }
}
