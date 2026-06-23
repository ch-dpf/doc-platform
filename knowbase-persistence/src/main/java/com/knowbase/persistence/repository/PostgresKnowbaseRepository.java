package com.knowbase.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowbase.domain.model.ChatMessage;
import com.knowbase.domain.model.ChatSession;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentIndexJob;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.IndexVersion;
import com.knowbase.domain.model.IndexedChunk;
import com.knowbase.domain.model.IngestionDocumentError;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.RetrievalEvalBaseline;
import com.knowbase.domain.model.RetrievalEvalResult;
import com.knowbase.domain.model.RetrievalEvalRun;
import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.IndexVersionStatus;
import com.knowbase.domain.status.RetrievalEvalRunStatus;
import com.knowbase.domain.support.PagedList;
import com.knowbase.persistence.support.JsonSupport;
import com.knowbase.persistence.entity.AgentEntity;
import com.knowbase.persistence.entity.AgentVersionEntity;
import com.knowbase.persistence.entity.ChunkEntity;
import com.knowbase.persistence.entity.DocumentEntity;
import com.knowbase.persistence.entity.DocumentIndexJobEntity;
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
import com.knowbase.persistence.mapper.DocumentIndexJobMapper;
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

import java.sql.Timestamp;
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
    private final DocumentIndexJobMapper documentIndexJobMapper;
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
            DocumentIndexJobMapper documentIndexJobMapper,
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
        this.documentIndexJobMapper = documentIndexJobMapper;
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
        wrapper.orderByDesc(LibraryEntity::getUpdatedAt);
        return libraryMapper.selectList(wrapper).stream().map(DomainMapper::toLibrary).toList();
    }

    @Override
    public PagedList<KnowledgeLibrary> pageLibraries(String tenantId, int page, int size) {
        LambdaQueryWrapper<LibraryEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null && !tenantId.isBlank()) {
            wrapper.eq(LibraryEntity::getTenantId, tenantId);
        }
        wrapper.orderByDesc(LibraryEntity::getUpdatedAt);
        Page<LibraryEntity> result = libraryMapper.selectPage(new Page<>(page, size), wrapper);
        List<KnowledgeLibrary> items = result.getRecords().stream().map(DomainMapper::toLibrary).toList();
        return new PagedList<>(items, result.getTotal(), page, size);
    }

    @Override
    public void deleteLibrary(UUID libraryId) {
        jdbcTemplate.update(
                """
                        DELETE FROM kb_document_index_job
                        WHERE library_id = ?
                        """,
                libraryId
        );
        jdbcTemplate.update(
                """
                        DELETE FROM kb_ingestion_document_error
                        WHERE run_id IN (SELECT run_id FROM kb_ingestion_run WHERE library_id = ?)
                        """,
                libraryId
        );
        ingestionRunMapper.delete(new LambdaQueryWrapper<IngestionRunEntity>()
                .eq(IngestionRunEntity::getLibraryId, libraryId));
        jdbcTemplate.update(
                """
                        DELETE FROM kb_embedding
                        WHERE chunk_id IN (SELECT chunk_id FROM kb_chunk WHERE library_id = ?)
                        """,
                libraryId
        );
        chunkMapper.delete(new LambdaQueryWrapper<ChunkEntity>()
                .eq(ChunkEntity::getLibraryId, libraryId));
        documentMapper.delete(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getLibraryId, libraryId));
        indexVersionMapper.delete(new LambdaQueryWrapper<IndexVersionEntity>()
                .eq(IndexVersionEntity::getLibraryId, libraryId));
        documentProfileMapper.delete(new LambdaQueryWrapper<DocumentProfileEntity>()
                .eq(DocumentProfileEntity::getLibraryId, libraryId));
        libraryProfileMapper.delete(new LambdaQueryWrapper<LibraryProfileEntity>()
                .eq(LibraryProfileEntity::getLibraryId, libraryId));
        jdbcTemplate.update(
                "DELETE FROM kb_acl_entry WHERE resource_type = 'LIBRARY' AND resource_id = ?",
                libraryId
        );
        libraryMapper.deleteById(libraryId);
    }

    @Override
    public boolean isLibraryReferencedByAgent(UUID libraryId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM kb_agent_version WHERE library_ids @> ?::jsonb",
                Integer.class,
                "[\"" + libraryId + "\"]"
        );
        return count != null && count > 0;
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
    public Optional<LibraryProfile> findLibraryProfile(UUID profileId) {
        LibraryProfileEntity entity = libraryProfileMapper.selectById(profileId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toLibraryProfile(entity));
    }

    @Override
    public List<LibraryProfile> listLibraryProfiles(UUID libraryId) {
        return libraryProfileMapper.selectList(new LambdaQueryWrapper<LibraryProfileEntity>()
                        .eq(LibraryProfileEntity::getLibraryId, libraryId)
                        .orderByDesc(LibraryProfileEntity::getVersion))
                .stream()
                .map(DomainMapper::toLibraryProfile)
                .toList();
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
    public void deleteDocumentProfile(UUID libraryId, String code) {
        documentProfileMapper.delete(new LambdaQueryWrapper<DocumentProfileEntity>()
                .eq(DocumentProfileEntity::getLibraryId, libraryId)
                .eq(DocumentProfileEntity::getCode, code));
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
    public List<IngestionRun> listIngestionRuns(UUID libraryId, int limit) {
        int cap = Math.max(1, Math.min(limit, 200));
        return ingestionRunMapper.selectList(new LambdaQueryWrapper<IngestionRunEntity>()
                        .eq(IngestionRunEntity::getLibraryId, libraryId)
                        .orderByDesc(IngestionRunEntity::getCreatedAt)
                        .last("LIMIT " + cap))
                .stream()
                .map(DomainMapper::toIngestionRun)
                .toList();
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
        return findActiveIndexVersion(libraryId).or(() -> {
            IndexVersionEntity entity = indexVersionMapper.selectOne(new LambdaQueryWrapper<IndexVersionEntity>()
                    .eq(IndexVersionEntity::getLibraryId, libraryId)
                    .eq(IndexVersionEntity::getStatus, IndexVersionStatus.PUBLISHED.name())
                    .orderByDesc(IndexVersionEntity::getVersion)
                    .last("LIMIT 1"));
            return entity == null ? Optional.empty() : Optional.of(DomainMapper.toIndexVersion(entity));
        });
    }

    @Override
    public Optional<IndexVersion> findActiveIndexVersion(UUID libraryId) {
        return findLibrary(libraryId)
                .map(KnowledgeLibrary::activeIndexGenerationId)
                .flatMap(id -> id == null ? Optional.empty() : findIndexVersion(id));
    }

    @Override
    public void setActiveIndexGeneration(UUID libraryId, UUID indexVersionId) {
        KnowledgeLibrary library = findLibrary(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("知识库不存在: " + libraryId));
        saveLibrary(new KnowledgeLibrary(
                library.libraryId(),
                library.tenantId(),
                library.name(),
                library.description(),
                library.status(),
                library.libraryTypePresetCode(),
                library.tags(),
                indexVersionId,
                library.createdAt(),
                Instant.now()
        ));
    }

    @Override
    public void archivePublishedGenerationsExcept(UUID libraryId, UUID keepIndexVersionId) {
        for (IndexVersion version : listIndexVersions(libraryId)) {
            if (version.indexVersionId().equals(keepIndexVersionId)) {
                continue;
            }
            if (version.status() != IndexVersionStatus.PUBLISHED) {
                continue;
            }
            saveIndexVersion(new IndexVersion(
                    version.indexVersionId(),
                    version.libraryId(),
                    version.profileId(),
                    version.version(),
                    IndexVersionStatus.ARCHIVED,
                    version.documentCount(),
                    version.chunkCount(),
                    version.publishedAt(),
                    version.createdAt()
            ));
        }
    }

    @Override
    public void refreshIndexVersionStats(UUID indexVersionId) {
        IndexVersion current = findIndexVersion(indexVersionId).orElse(null);
        if (current == null) {
            return;
        }
        int documentCount = Math.toIntExact(documentMapper.selectCount(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getIndexVersionId, indexVersionId)));
        int chunkCount = Math.toIntExact(chunkMapper.selectCount(new LambdaQueryWrapper<ChunkEntity>()
                .eq(ChunkEntity::getIndexVersionId, indexVersionId)));
        saveIndexVersion(new IndexVersion(
                current.indexVersionId(),
                current.libraryId(),
                current.profileId(),
                current.version(),
                current.status(),
                documentCount,
                chunkCount,
                current.publishedAt(),
                current.createdAt()
        ));
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
        UUID generationFilter = indexVersionId;
        if (generationFilter == null) {
            generationFilter = findLibrary(libraryId)
                    .map(KnowledgeLibrary::activeIndexGenerationId)
                    .orElse(null);
        }
        if (generationFilter != null) {
            wrapper.eq(DocumentEntity::getIndexVersionId, generationFilter);
        }
        wrapper.orderByDesc(DocumentEntity::getUpdatedAt);
        return documentMapper.selectList(wrapper).stream().map(DomainMapper::toKnowledgeDocument).toList();
    }

    @Override
    public Optional<KnowledgeDocument> findDocumentBySourceUri(UUID libraryId, String sourceUri) {
        if (sourceUri == null || sourceUri.isBlank()) {
            return Optional.empty();
        }
        DocumentEntity entity = documentMapper.selectOne(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getLibraryId, libraryId)
                .eq(DocumentEntity::getSourceUri, sourceUri)
                .last("LIMIT 1"));
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toKnowledgeDocument(entity));
    }

    @Override
    public KnowledgeDocument saveDocument(KnowledgeDocument document) {
        DocumentEntity entity = DomainMapper.toDocumentEntity(document);
        if (documentMapper.selectById(document.documentId()) == null) {
            documentMapper.insert(entity);
        } else {
            documentMapper.updateById(entity);
        }
        return document;
    }

    @Override
    public void deleteDocumentAndChunks(UUID documentId) {
        jdbcTemplate.update(
                """
                        DELETE FROM kb_embedding
                        WHERE chunk_id IN (SELECT chunk_id FROM kb_chunk WHERE document_id = ?)
                        """,
                documentId
        );
        chunkMapper.delete(new LambdaQueryWrapper<ChunkEntity>().eq(ChunkEntity::getDocumentId, documentId));
        documentMapper.deleteById(documentId);
    }

    @Override
    public void replaceDocumentChunks(UUID documentId, List<IndexedChunk> chunks) {
        jdbcTemplate.update(
                """
                        DELETE FROM kb_embedding
                        WHERE chunk_id IN (SELECT chunk_id FROM kb_chunk WHERE document_id = ?)
                        """,
                documentId
        );
        chunkMapper.delete(new LambdaQueryWrapper<ChunkEntity>().eq(ChunkEntity::getDocumentId, documentId));
        for (IndexedChunk indexedChunk : chunks) {
            DocumentChunk chunk = indexedChunk.chunk();
            ChunkEntity chunkEntity = DomainMapper.toChunkEntity(chunk);
            chunkMapper.insert(chunkEntity);
            if (indexedChunk.embedding() != null) {
                embeddingStore.insertEmbedding(
                        UUID.randomUUID(),
                        chunk.chunkId(),
                        chunk.embeddingModel(),
                        indexedChunk.embedding().length,
                        indexedChunk.embedding()
                );
            }
        }
    }

    @Override
    public Optional<KnowledgeDocument> findDocument(UUID documentId) {
        DocumentEntity entity = documentMapper.selectById(documentId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toKnowledgeDocument(entity));
    }

    @Override
    public List<DocumentChunk> listChunksByDocument(UUID documentId) {
        UUID generationId = findDocument(documentId).map(KnowledgeDocument::indexVersionId).orElse(null);
        LambdaQueryWrapper<ChunkEntity> wrapper = new LambdaQueryWrapper<ChunkEntity>()
                .eq(ChunkEntity::getDocumentId, documentId)
                .orderByAsc(ChunkEntity::getChunkId);
        if (generationId != null) {
            wrapper.eq(ChunkEntity::getIndexVersionId, generationId);
        }
        return chunkMapper.selectList(wrapper).stream()
                .map(DomainMapper::toDocumentChunk)
                .toList();
    }

    @Override
    public PagedList<DocumentChunk> pageChunksByDocument(UUID documentId, int page, int size) {
        UUID generationId = findDocument(documentId).map(KnowledgeDocument::indexVersionId).orElse(null);
        LambdaQueryWrapper<ChunkEntity> wrapper = new LambdaQueryWrapper<ChunkEntity>()
                .eq(ChunkEntity::getDocumentId, documentId)
                .orderByAsc(ChunkEntity::getChunkId);
        if (generationId != null) {
            wrapper.eq(ChunkEntity::getIndexVersionId, generationId);
        }
        Page<ChunkEntity> result = chunkMapper.selectPage(new Page<>(page, size), wrapper);
        List<DocumentChunk> items = result.getRecords().stream()
                .map(DomainMapper::toDocumentChunk)
                .toList();
        return new PagedList<>(items, result.getTotal(), page, size);
    }

    @Override
    public Optional<DocumentChunk> findChunk(UUID chunkId) {
        ChunkEntity entity = chunkMapper.selectById(chunkId);
        return entity == null ? Optional.empty() : Optional.of(DomainMapper.toDocumentChunk(entity));
    }

    @Override
    public Optional<float[]> findChunkEmbedding(UUID chunkId) {
        return jdbcTemplate.execute((ConnectionCallback<Optional<float[]>>) connection -> {
            try (var statement = connection.prepareStatement(
                    """
                            SELECT embedding
                            FROM kb_embedding
                            WHERE chunk_id = ?
                            LIMIT 1
                            """
            )) {
                statement.setObject(1, chunkId);
                try (var resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(VectorSupport.fromPgVector(resultSet.getObject("embedding")));
                }
            }
        });
    }

    @Override
    public void updateIndexedChunk(IndexedChunk indexedChunk) {
        DocumentChunk chunk = indexedChunk.chunk();
        ChunkEntity entity = DomainMapper.toChunkEntity(chunk);
        if (chunkMapper.selectById(chunk.chunkId()) == null) {
            throw new IllegalStateException("文档块不存在: " + chunk.chunkId());
        }
        chunkMapper.updateById(entity);
        if (indexedChunk.embedding() != null) {
            embeddingStore.replaceEmbedding(
                    chunk.chunkId(),
                    chunk.embeddingModel(),
                    indexedChunk.embedding().length,
                    indexedChunk.embedding()
            );
        }
    }

    @Override
    public void reassignDocumentsToGeneration(UUID libraryId, UUID indexGenerationId) {
        jdbcTemplate.update(
                """
                        UPDATE kb_document
                        SET index_version_id = ?, updated_at = NOW()
                        WHERE library_id = ?
                        """,
                indexGenerationId,
                libraryId
        );
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
    public DocumentIndexJob saveDocumentIndexJob(DocumentIndexJob job) {
        DocumentIndexJobEntity entity = DomainMapper.toDocumentIndexJobEntity(job);
        if (documentIndexJobMapper.selectById(job.jobId()) == null) {
            documentIndexJobMapper.insert(entity);
        } else {
            documentIndexJobMapper.updateById(entity);
        }
        return job;
    }

    @Override
    public List<DocumentIndexJob> listDocumentIndexJobs(UUID runId) {
        return documentIndexJobMapper.selectList(new LambdaQueryWrapper<DocumentIndexJobEntity>()
                        .eq(DocumentIndexJobEntity::getRunId, runId)
                        .orderByAsc(DocumentIndexJobEntity::getCreatedAt))
                .stream()
                .map(DomainMapper::toDocumentIndexJob)
                .toList();
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
            } else {
                documentMapper.updateById(document);
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
            if (indexedChunk.embedding() != null) {
                embeddingStore.insertEmbedding(
                        UUID.randomUUID(),
                        chunk.chunkId(),
                        chunk.embeddingModel(),
                        indexedChunk.embedding().length,
                        indexedChunk.embedding()
                );
            }
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

    @Override
    public ChatSession saveChatSession(ChatSession session) {
        jdbcTemplate.update(
                """
                INSERT INTO kb_chat_session (
                    session_id, tenant_id, agent_id, agent_version_id, title, status, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (session_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    status = EXCLUDED.status,
                    agent_version_id = EXCLUDED.agent_version_id,
                    updated_at = EXCLUDED.updated_at
                """,
                session.sessionId(),
                session.tenantId(),
                session.agentId(),
                session.agentVersionId(),
                session.title(),
                session.status(),
                Timestamp.from(session.createdAt()),
                Timestamp.from(session.updatedAt())
        );
        return session;
    }

    @Override
    public Optional<ChatSession> findChatSession(UUID sessionId) {
        List<ChatSession> sessions = jdbcTemplate.query(
                "SELECT session_id, tenant_id, agent_id, agent_version_id, title, status, created_at, updated_at FROM kb_chat_session WHERE session_id = ?",
                (resultSet, rowNum) -> new ChatSession(
                        resultSet.getObject("session_id", UUID.class),
                        resultSet.getString("tenant_id"),
                        resultSet.getObject("agent_id", UUID.class),
                        resultSet.getObject("agent_version_id", UUID.class),
                        resultSet.getString("title"),
                        resultSet.getString("status"),
                        toInstant(resultSet.getTimestamp("created_at")),
                        toInstant(resultSet.getTimestamp("updated_at"))
                ),
                sessionId
        );
        return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.getFirst());
    }

    @Override
    public List<ChatSession> listChatSessions(String tenantId, UUID agentId) {
        String sql = "SELECT session_id, tenant_id, agent_id, agent_version_id, title, status, created_at, updated_at FROM kb_chat_session WHERE tenant_id = ?";
        Object[] args;
        if (agentId != null) {
            sql += " AND agent_id = ?";
            args = new Object[] {tenantId, agentId};
        } else {
            args = new Object[] {tenantId};
        }
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> new ChatSession(
                        resultSet.getObject("session_id", UUID.class),
                        resultSet.getString("tenant_id"),
                        resultSet.getObject("agent_id", UUID.class),
                        resultSet.getObject("agent_version_id", UUID.class),
                        resultSet.getString("title"),
                        resultSet.getString("status"),
                        toInstant(resultSet.getTimestamp("created_at")),
                        toInstant(resultSet.getTimestamp("updated_at"))
                ),
                args
        );
    }

    @Override
    public ChatMessage saveChatMessage(ChatMessage message) {
        jdbcTemplate.update(
                """
                INSERT INTO kb_chat_message (message_id, session_id, role, content, query_run_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                message.messageId(),
                message.sessionId(),
                message.role(),
                message.content(),
                message.queryRunId(),
                Timestamp.from(message.createdAt())
        );
        return message;
    }

    @Override
    public List<ChatMessage> listChatMessages(UUID sessionId) {
        return jdbcTemplate.query(
                "SELECT message_id, session_id, role, content, query_run_id, created_at FROM kb_chat_message WHERE session_id = ? ORDER BY created_at",
                (resultSet, rowNum) -> new ChatMessage(
                        resultSet.getObject("message_id", UUID.class),
                        resultSet.getObject("session_id", UUID.class),
                        resultSet.getString("role"),
                        resultSet.getString("content"),
                        resultSet.getObject("query_run_id", UUID.class),
                        toInstant(resultSet.getTimestamp("created_at"))
                ),
                sessionId
        );
    }

    @Override
    public Optional<IndexVersion> publishIndexVersion(UUID indexVersionId) {
        IndexVersion indexVersion = findIndexVersion(indexVersionId).orElse(null);
        if (indexVersion == null) {
            return Optional.empty();
        }
        Instant publishedAt = Instant.now();
        IndexVersion published = new IndexVersion(
                indexVersion.indexVersionId(),
                indexVersion.libraryId(),
                indexVersion.profileId(),
                indexVersion.version(),
                IndexVersionStatus.PUBLISHED,
                indexVersion.documentCount(),
                indexVersion.chunkCount(),
                publishedAt,
                indexVersion.createdAt()
        );
        saveIndexVersion(published);
        return Optional.of(published);
    }

    @Override
    public RetrievalEvalSample saveRetrievalEvalSample(RetrievalEvalSample sample) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_retrieval_eval_sample (
                            sample_id, library_id, question, expected_document_ids, expected_source_uris,
                            ground_truth_contexts, hit_rank, notes, enabled, created_at, updated_at
                        ) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?, ?, ?, ?, ?)
                        ON CONFLICT (sample_id) DO UPDATE SET
                            question = EXCLUDED.question,
                            expected_document_ids = EXCLUDED.expected_document_ids,
                            expected_source_uris = EXCLUDED.expected_source_uris,
                            ground_truth_contexts = EXCLUDED.ground_truth_contexts,
                            hit_rank = EXCLUDED.hit_rank,
                            notes = EXCLUDED.notes,
                            enabled = EXCLUDED.enabled,
                            updated_at = EXCLUDED.updated_at
                        """,
                sample.sampleId(),
                sample.libraryId(),
                sample.question(),
                JsonSupport.write(sample.expectedDocumentIds()),
                JsonSupport.write(sample.expectedSourceUris()),
                JsonSupport.write(sample.groundTruthContexts()),
                sample.hitRank(),
                sample.notes(),
                sample.enabled(),
                Timestamp.from(sample.createdAt()),
                Timestamp.from(sample.updatedAt())
        );
        return sample;
    }

    @Override
    public Optional<RetrievalEvalSample> findRetrievalEvalSample(UUID sampleId) {
        List<RetrievalEvalSample> samples = jdbcTemplate.query(
                """
                        SELECT sample_id, library_id, question, expected_document_ids, expected_source_uris,
                               ground_truth_contexts, hit_rank, notes, enabled, created_at, updated_at
                        FROM kb_retrieval_eval_sample
                        WHERE sample_id = ?
                        """,
                this::mapRetrievalEvalSample,
                sampleId
        );
        return samples.stream().findFirst();
    }

    @Override
    public List<RetrievalEvalSample> listRetrievalEvalSamples(UUID libraryId, boolean enabledOnly) {
        String sql = """
                SELECT sample_id, library_id, question, expected_document_ids, expected_source_uris,
                       ground_truth_contexts, hit_rank, notes, enabled, created_at, updated_at
                FROM kb_retrieval_eval_sample
                WHERE library_id = ?
                """;
        Object[] args;
        if (enabledOnly) {
            sql += " AND enabled = TRUE";
            args = new Object[] {libraryId};
        } else {
            args = new Object[] {libraryId};
        }
        sql += " ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, this::mapRetrievalEvalSample, args);
    }

    @Override
    public void deleteRetrievalEvalSample(UUID sampleId) {
        jdbcTemplate.update("DELETE FROM kb_retrieval_eval_sample WHERE sample_id = ?", sampleId);
    }

    @Override
    public RetrievalEvalRun saveRetrievalEvalRun(RetrievalEvalRun evalRun) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_retrieval_eval_run (
                            eval_run_id, library_id, status, hit_k, total_samples, passed_samples,
                            recall_at_k, mrr, context_precision_at_k, stratified_recall_json,
                            retrieval_policy_json, message, created_at, completed_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)
                        ON CONFLICT (eval_run_id) DO UPDATE SET
                            status = EXCLUDED.status,
                            hit_k = EXCLUDED.hit_k,
                            total_samples = EXCLUDED.total_samples,
                            passed_samples = EXCLUDED.passed_samples,
                            recall_at_k = EXCLUDED.recall_at_k,
                            mrr = EXCLUDED.mrr,
                            context_precision_at_k = EXCLUDED.context_precision_at_k,
                            stratified_recall_json = EXCLUDED.stratified_recall_json,
                            retrieval_policy_json = EXCLUDED.retrieval_policy_json,
                            message = EXCLUDED.message,
                            completed_at = EXCLUDED.completed_at
                        """,
                evalRun.evalRunId(),
                evalRun.libraryId(),
                evalRun.status().name(),
                evalRun.hitK(),
                evalRun.totalSamples(),
                evalRun.passedSamples(),
                evalRun.recallAtK(),
                evalRun.mrr(),
                evalRun.contextPrecisionAtK(),
                JsonSupport.write(evalRun.stratifiedRecall()),
                JsonSupport.write(evalRun.retrievalPolicy()),
                evalRun.message(),
                Timestamp.from(evalRun.createdAt()),
                evalRun.completedAt() == null ? null : Timestamp.from(evalRun.completedAt())
        );
        return evalRun;
    }

    @Override
    public Optional<RetrievalEvalRun> findRetrievalEvalRun(UUID evalRunId) {
        List<RetrievalEvalRun> runs = jdbcTemplate.query(
                """
                        SELECT eval_run_id, library_id, status, hit_k, total_samples, passed_samples,
                               recall_at_k, mrr, context_precision_at_k, stratified_recall_json,
                               retrieval_policy_json, message, created_at, completed_at
                        FROM kb_retrieval_eval_run
                        WHERE eval_run_id = ?
                        """,
                this::mapRetrievalEvalRun,
                evalRunId
        );
        return runs.stream().findFirst();
    }

    @Override
    public List<RetrievalEvalRun> listRetrievalEvalRuns(UUID libraryId, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT eval_run_id, library_id, status, hit_k, total_samples, passed_samples,
                               recall_at_k, mrr, context_precision_at_k, stratified_recall_json,
                               retrieval_policy_json, message, created_at, completed_at
                        FROM kb_retrieval_eval_run
                        WHERE library_id = ?
                        ORDER BY created_at DESC
                        LIMIT ?
                        """,
                this::mapRetrievalEvalRun,
                libraryId,
                Math.max(1, limit)
        );
    }

    @Override
    public RetrievalEvalResult saveRetrievalEvalResult(RetrievalEvalResult result) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_retrieval_eval_result (
                            result_id, eval_run_id, sample_id, question, hit, hit_rank_used, first_hit_rank,
                            matched_document_id, matched_chunk_id, match_type, retrieved_count, failure_reason,
                            trace_json, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                        ON CONFLICT (result_id) DO NOTHING
                        """,
                result.resultId(),
                result.evalRunId(),
                result.sampleId(),
                result.question(),
                result.hit(),
                result.hitRankUsed(),
                result.firstHitRank(),
                result.matchedDocumentId(),
                result.matchedChunkId(),
                result.matchType(),
                result.retrievedCount(),
                result.failureReason(),
                JsonSupport.write(result.trace()),
                Timestamp.from(result.createdAt())
        );
        return result;
    }

    @Override
    public List<RetrievalEvalResult> listRetrievalEvalResults(UUID evalRunId) {
        return jdbcTemplate.query(
                """
                        SELECT result_id, eval_run_id, sample_id, question, hit, hit_rank_used, first_hit_rank,
                               matched_document_id, matched_chunk_id, match_type, retrieved_count, failure_reason,
                               trace_json, created_at
                        FROM kb_retrieval_eval_result
                        WHERE eval_run_id = ?
                        ORDER BY created_at ASC
                        """,
                this::mapRetrievalEvalResult,
                evalRunId
        );
    }

    @Override
    public RetrievalEvalBaseline saveRetrievalEvalBaseline(RetrievalEvalBaseline baseline) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_retrieval_eval_baseline (
                            library_id, eval_run_id, profile_id, index_generation_id,
                            recall_at_k, hit_k, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (library_id) DO UPDATE SET
                            eval_run_id = EXCLUDED.eval_run_id,
                            profile_id = EXCLUDED.profile_id,
                            index_generation_id = EXCLUDED.index_generation_id,
                            recall_at_k = EXCLUDED.recall_at_k,
                            hit_k = EXCLUDED.hit_k,
                            updated_at = EXCLUDED.updated_at
                        """,
                baseline.libraryId(),
                baseline.evalRunId(),
                baseline.profileId(),
                baseline.indexGenerationId(),
                baseline.recallAtK(),
                baseline.hitK(),
                Timestamp.from(baseline.createdAt()),
                Timestamp.from(baseline.updatedAt())
        );
        return baseline;
    }

    @Override
    public Optional<RetrievalEvalBaseline> findRetrievalEvalBaseline(UUID libraryId) {
        List<RetrievalEvalBaseline> baselines = jdbcTemplate.query(
                """
                        SELECT library_id, eval_run_id, profile_id, index_generation_id,
                               recall_at_k, hit_k, created_at, updated_at
                        FROM kb_retrieval_eval_baseline
                        WHERE library_id = ?
                        """,
                (rs, rowNum) -> new RetrievalEvalBaseline(
                        rs.getObject("library_id", UUID.class),
                        rs.getObject("eval_run_id", UUID.class),
                        rs.getObject("profile_id", UUID.class),
                        rs.getObject("index_generation_id", UUID.class),
                        rs.getDouble("recall_at_k"),
                        rs.getInt("hit_k"),
                        toInstant(rs.getTimestamp("created_at")),
                        toInstant(rs.getTimestamp("updated_at"))
                ),
                libraryId
        );
        return baselines.stream().findFirst();
    }

    @Override
    public void deleteRetrievalEvalSamplesByLibrary(UUID libraryId) {
        jdbcTemplate.update("DELETE FROM kb_retrieval_eval_sample WHERE library_id = ?", libraryId);
    }

    private RetrievalEvalSample mapRetrievalEvalSample(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RetrievalEvalSample(
                rs.getObject("sample_id", UUID.class),
                rs.getObject("library_id", UUID.class),
                rs.getString("question"),
                JsonSupport.readUuidList(rs.getString("expected_document_ids")),
                JsonSupport.readStringList(rs.getString("expected_source_uris")),
                JsonSupport.readStringList(rs.getString("ground_truth_contexts")),
                rs.getInt("hit_rank"),
                rs.getString("notes"),
                rs.getBoolean("enabled"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("updated_at"))
        );
    }

    private RetrievalEvalRun mapRetrievalEvalRun(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RetrievalEvalRun(
                rs.getObject("eval_run_id", UUID.class),
                rs.getObject("library_id", UUID.class),
                RetrievalEvalRunStatus.valueOf(rs.getString("status")),
                rs.getInt("hit_k"),
                rs.getInt("total_samples"),
                rs.getInt("passed_samples"),
                rs.getObject("recall_at_k") == null ? null : rs.getDouble("recall_at_k"),
                rs.getObject("mrr") == null ? null : rs.getDouble("mrr"),
                rs.getObject("context_precision_at_k") == null ? null : rs.getDouble("context_precision_at_k"),
                JsonSupport.readDoubleMap(rs.getString("stratified_recall_json")),
                JsonSupport.readMap(rs.getString("retrieval_policy_json")),
                rs.getString("message"),
                toInstant(rs.getTimestamp("created_at")),
                toInstant(rs.getTimestamp("completed_at"))
        );
    }

    private RetrievalEvalResult mapRetrievalEvalResult(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new RetrievalEvalResult(
                rs.getObject("result_id", UUID.class),
                rs.getObject("eval_run_id", UUID.class),
                rs.getObject("sample_id", UUID.class),
                rs.getString("question"),
                rs.getBoolean("hit"),
                rs.getInt("hit_rank_used"),
                rs.getObject("first_hit_rank") == null ? null : rs.getInt("first_hit_rank"),
                rs.getObject("matched_document_id", UUID.class),
                rs.getObject("matched_chunk_id", UUID.class),
                rs.getString("match_type"),
                rs.getInt("retrieved_count"),
                rs.getString("failure_reason"),
                JsonSupport.readMap(rs.getString("trace_json")),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
