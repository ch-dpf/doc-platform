package com.knowbase.application.service;

import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.ChatMessage;
import com.knowbase.domain.model.ChatSession;
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
import com.knowbase.domain.status.DocumentStatus;
import com.knowbase.domain.status.IndexVersionStatus;
import com.knowbase.domain.model.RetrievalEvalBaseline;
import com.knowbase.domain.model.RetrievalEvalResult;
import com.knowbase.domain.model.RetrievalEvalRun;
import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.support.PagedList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryKnowbaseRepository implements KnowbaseRepository {

    private final ConcurrentMap<UUID, KnowledgeLibrary> libraries = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, LibraryProfile> libraryProfilesById = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TokenizerProfile> tokenizerProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<DocumentProfile>> documentProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, IngestionRun> ingestionRuns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, IndexVersion> indexVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<IndexedChunk>> chunksByIndexVersion = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, KnowledgeDocument> documents = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<IngestionDocumentError>> ingestionErrors = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, DocumentIndexJob> documentIndexJobs = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, KnowledgeAgent> agents = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AgentVersion> agentVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, QueryRun> queryRuns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ChatSession> chatSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<ChatMessage>> chatMessages = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RetrievalEvalSample> retrievalEvalSamples = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RetrievalEvalRun> retrievalEvalRuns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<RetrievalEvalResult>> retrievalEvalResults = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, RetrievalEvalBaseline> retrievalEvalBaselines = new ConcurrentHashMap<>();

    @Override
    public KnowledgeLibrary saveLibrary(KnowledgeLibrary library) {
        libraries.put(library.libraryId(), library);
        return library;
    }

    @Override
    public Optional<KnowledgeLibrary> findLibrary(UUID libraryId) {
        return Optional.ofNullable(libraries.get(libraryId));
    }

    @Override
    public List<KnowledgeLibrary> listLibraries(String tenantId) {
        return libraries.values().stream()
                .filter(item -> tenantId == null || tenantId.equals(item.tenantId()))
                .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                .toList();
    }

    @Override
    public PagedList<KnowledgeLibrary> pageLibraries(String tenantId, int page, int size) {
        List<KnowledgeLibrary> all = listLibraries(tenantId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, all.size());
        int toIndex = Math.min(fromIndex + safeSize, all.size());
        return new PagedList<>(all.subList(fromIndex, toIndex), all.size(), safePage, safeSize);
    }

    @Override
    public void deleteLibrary(UUID libraryId) {
        documentIndexJobs.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        libraries.remove(libraryId);
        libraryProfilesById.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        documentProfiles.remove(libraryId);
        ingestionRuns.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        indexVersions.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        documents.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        chunksByIndexVersion.entrySet().removeIf(entry ->
                indexVersions.containsKey(entry.getKey())
                        && indexVersions.get(entry.getKey()).libraryId().equals(libraryId));
        ingestionErrors.entrySet().removeIf(entry ->
                ingestionRuns.containsKey(entry.getKey())
                        && ingestionRuns.get(entry.getKey()).libraryId().equals(libraryId));
        retrievalEvalResults.entrySet().removeIf(entry ->
                retrievalEvalRuns.containsKey(entry.getKey())
                        && retrievalEvalRuns.get(entry.getKey()).libraryId().equals(libraryId));
        retrievalEvalRuns.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        retrievalEvalSamples.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
        retrievalEvalBaselines.remove(libraryId);
    }

    @Override
    public boolean isLibraryReferencedByAgent(UUID libraryId) {
        return agentVersions.values().stream()
                .anyMatch(version -> version.libraryIds().contains(libraryId));
    }

    @Override
    public LibraryProfile saveLibraryProfile(LibraryProfile profile) {
        libraryProfilesById.put(profile.profileId(), profile);
        return profile;
    }

    @Override
    public Optional<LibraryProfile> findLatestLibraryProfile(UUID libraryId) {
        return libraryProfilesById.values().stream()
                .filter(profile -> profile.libraryId().equals(libraryId))
                .max((left, right) -> Integer.compare(left.version(), right.version()));
    }

    @Override
    public Optional<LibraryProfile> findLibraryProfile(UUID profileId) {
        return Optional.ofNullable(libraryProfilesById.get(profileId));
    }

    @Override
    public List<LibraryProfile> listLibraryProfiles(UUID libraryId) {
        return libraryProfilesById.values().stream()
                .filter(profile -> profile.libraryId().equals(libraryId))
                .sorted((left, right) -> Integer.compare(right.version(), left.version()))
                .toList();
    }

    @Override
    public TokenizerProfile saveTokenizerProfile(TokenizerProfile profile) {
        tokenizerProfiles.values().removeIf(existing -> existing.provider().equals(profile.provider())
                && existing.modelName().equals(profile.modelName()));
        tokenizerProfiles.put(profile.tokenizerProfileId(), profile);
        return profile;
    }

    @Override
    public Optional<TokenizerProfile> findTokenizerProfile(UUID tokenizerProfileId) {
        return Optional.ofNullable(tokenizerProfiles.get(tokenizerProfileId));
    }

    @Override
    public Optional<TokenizerProfile> findTokenizerProfile(String provider, String modelName) {
        return tokenizerProfiles.values().stream()
                .filter(TokenizerProfile::enabled)
                .filter(profile -> profile.provider().equals(provider))
                .filter(profile -> profile.modelName().equals(modelName))
                .findFirst();
    }

    @Override
    public List<TokenizerProfile> listTokenizerProfiles(String provider, boolean includeDisabled) {
        return tokenizerProfiles.values().stream()
                .filter(profile -> provider == null || provider.isBlank() || provider.equals(profile.provider()))
                .filter(profile -> includeDisabled || profile.enabled())
                .toList();
    }

    @Override
    public DocumentProfile saveDocumentProfile(DocumentProfile profile) {
        documentProfiles.compute(profile.libraryId(), (libraryId, existing) -> {
            List<DocumentProfile> profiles = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            profiles.removeIf(item -> item.code().equals(profile.code()));
            profiles.add(profile);
            return profiles;
        });
        return profile;
    }

    @Override
    public List<DocumentProfile> listDocumentProfiles(UUID libraryId) {
        return List.copyOf(documentProfiles.getOrDefault(libraryId, List.of()));
    }

    @Override
    public Optional<DocumentProfile> findDocumentProfile(UUID libraryId, String code) {
        return listDocumentProfiles(libraryId).stream()
                .filter(profile -> profile.code().equals(code))
                .findFirst();
    }

    @Override
    public void deleteDocumentProfile(UUID libraryId, String code) {
        documentProfiles.computeIfPresent(libraryId, (id, profiles) -> {
            List<DocumentProfile> remaining = profiles.stream()
                    .filter(profile -> !profile.code().equals(code))
                    .toList();
            return remaining.isEmpty() ? null : new ArrayList<>(remaining);
        });
    }

    @Override
    public IngestionRun saveIngestionRun(IngestionRun run) {
        ingestionRuns.put(run.runId(), run);
        return run;
    }

    @Override
    public Optional<IngestionRun> findIngestionRun(UUID runId) {
        return Optional.ofNullable(ingestionRuns.get(runId));
    }

    @Override
    public List<IngestionRun> listIngestionRuns(UUID libraryId, int limit) {
        int cap = Math.max(1, Math.min(limit, 200));
        return ingestionRuns.values().stream()
                .filter(run -> run.libraryId().equals(libraryId))
                .sorted((a, b) -> b.createdAt().compareTo(a.createdAt()))
                .limit(cap)
                .toList();
    }

    @Override
    public IndexVersion saveIndexVersion(IndexVersion indexVersion) {
        indexVersions.put(indexVersion.indexVersionId(), indexVersion);
        return indexVersion;
    }

    @Override
    public Optional<IndexVersion> findPublishedIndexVersion(UUID libraryId) {
        return findActiveIndexVersion(libraryId).or(() -> indexVersions.values().stream()
                .filter(version -> version.libraryId().equals(libraryId))
                .filter(version -> version.status() == IndexVersionStatus.PUBLISHED)
                .max((left, right) -> Integer.compare(left.version(), right.version())));
    }

    @Override
    public Optional<IndexVersion> findActiveIndexVersion(UUID libraryId) {
        return findLibrary(libraryId)
                .map(KnowledgeLibrary::activeIndexGenerationId)
                .flatMap(id -> id == null ? Optional.empty() : findIndexVersion(id));
    }

    @Override
    public void setActiveIndexGeneration(UUID libraryId, UUID indexVersionId) {
        KnowledgeLibrary library = libraries.get(libraryId);
        if (library == null) {
            throw new IllegalArgumentException("知识库不存在: " + libraryId);
        }
        libraries.put(libraryId, new KnowledgeLibrary(
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
        int documentCount = (int) documents.values().stream()
                .filter(document -> indexVersionId.equals(document.indexVersionId()))
                .count();
        int chunkCount = listChunksByIndexVersion(indexVersionId).size();
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
        return indexVersions.values().stream()
                .filter(version -> version.libraryId().equals(libraryId))
                .sorted((left, right) -> Integer.compare(right.version(), left.version()))
                .toList();
    }

    @Override
    public Optional<IndexVersion> findIndexVersion(UUID indexVersionId) {
        return Optional.ofNullable(indexVersions.get(indexVersionId));
    }

    @Override
    public List<KnowledgeDocument> listDocuments(UUID libraryId, UUID indexVersionId) {
        return filterDocuments(libraryId, indexVersionId);
    }

    @Override
    public PagedList<KnowledgeDocument> pageDocuments(UUID libraryId, UUID indexVersionId, int page, int size) {
        List<KnowledgeDocument> all = filterDocuments(libraryId, indexVersionId);
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min((safePage - 1) * safeSize, all.size());
        int toIndex = Math.min(fromIndex + safeSize, all.size());
        return new PagedList<>(all.subList(fromIndex, toIndex), all.size(), safePage, safeSize);
    }

    private List<KnowledgeDocument> filterDocuments(UUID libraryId, UUID indexVersionId) {
        UUID generationFilter = indexVersionId;
        if (generationFilter == null) {
            generationFilter = findLibrary(libraryId)
                    .map(KnowledgeLibrary::activeIndexGenerationId)
                    .orElse(null);
        }
        UUID filter = generationFilter;
        return documents.values().stream()
                .filter(document -> document.libraryId().equals(libraryId))
                .filter(document -> filter == null || filter.equals(document.indexVersionId()))
                .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                .toList();
    }

    @Override
    public Optional<KnowledgeDocument> findDocumentBySourceUri(UUID libraryId, String sourceUri) {
        if (sourceUri == null || sourceUri.isBlank()) {
            return Optional.empty();
        }
        return documents.values().stream()
                .filter(document -> document.libraryId().equals(libraryId))
                .filter(document -> sourceUri.equals(document.sourceUri()))
                .findFirst();
    }

    @Override
    public KnowledgeDocument saveDocument(KnowledgeDocument document) {
        documents.put(document.documentId(), document);
        return document;
    }

    @Override
    public void deleteDocumentAndChunks(UUID documentId) {
        documents.remove(documentId);
        for (Map.Entry<UUID, List<IndexedChunk>> entry : chunksByIndexVersion.entrySet()) {
            List<IndexedChunk> remaining = entry.getValue().stream()
                    .filter(chunk -> !chunk.chunk().documentId().equals(documentId))
                    .toList();
            chunksByIndexVersion.put(entry.getKey(), new ArrayList<>(remaining));
        }
    }

    @Override
    public void replaceDocumentChunks(UUID documentId, List<IndexedChunk> chunks) {
        for (Map.Entry<UUID, List<IndexedChunk>> entry : chunksByIndexVersion.entrySet()) {
            List<IndexedChunk> remaining = entry.getValue().stream()
                    .filter(chunk -> !chunk.chunk().documentId().equals(documentId))
                    .toList();
            chunksByIndexVersion.put(entry.getKey(), new ArrayList<>(remaining));
        }
        if (chunks.isEmpty()) {
            return;
        }
        UUID indexVersionId = chunks.getFirst().chunk().indexVersionId();
        List<IndexedChunk> merged = new ArrayList<>(chunksByIndexVersion.getOrDefault(indexVersionId, List.of()));
        merged.addAll(chunks);
        chunksByIndexVersion.put(indexVersionId, merged);
    }

    @Override
    public void reassignDocumentsToGeneration(UUID libraryId, UUID indexGenerationId) {
        for (KnowledgeDocument document : documents.values()) {
            if (!document.libraryId().equals(libraryId)) {
                continue;
            }
            Instant now = Instant.now();
            documents.put(document.documentId(), new KnowledgeDocument(
                    document.documentId(),
                    document.libraryId(),
                    indexGenerationId,
                    document.sourceUri(),
                    document.title(),
                    document.status(),
                    document.documentProfileId(),
                    document.contentHash(),
                    document.lastIndexedAt(),
                    document.lastError(),
                    document.createdAt(),
                    now
            ));
        }
    }

    @Override
    public List<DocumentChunk> listChunksByDocument(UUID documentId) {
        UUID generationId = findDocument(documentId).map(KnowledgeDocument::indexVersionId).orElse(null);
        return chunksByIndexVersion.values().stream()
                .flatMap(List::stream)
                .map(IndexedChunk::chunk)
                .filter(chunk -> chunk.documentId().equals(documentId))
                .filter(chunk -> generationId == null || generationId.equals(chunk.indexVersionId()))
                .sorted(java.util.Comparator.comparing(DocumentChunk::chunkId))
                .toList();
    }

    @Override
    public PagedList<DocumentChunk> pageChunksByDocument(UUID documentId, int page, int size) {
        List<DocumentChunk> all = listChunksByDocument(documentId);
        int from = Math.max(0, (page - 1) * size);
        if (from >= all.size()) {
            return new PagedList<>(List.of(), all.size(), page, size);
        }
        int to = Math.min(from + size, all.size());
        return new PagedList<>(all.subList(from, to), all.size(), page, size);
    }

    @Override
    public Optional<DocumentChunk> findChunk(UUID chunkId) {
        return chunksByIndexVersion.values().stream()
                .flatMap(List::stream)
                .map(IndexedChunk::chunk)
                .filter(chunk -> chunk.chunkId().equals(chunkId))
                .findFirst();
    }

    @Override
    public Optional<float[]> findChunkEmbedding(UUID chunkId) {
        return chunksByIndexVersion.values().stream()
                .flatMap(List::stream)
                .filter(indexed -> indexed.chunk().chunkId().equals(chunkId))
                .map(IndexedChunk::embedding)
                .findFirst();
    }

    @Override
    public void updateIndexedChunk(IndexedChunk indexedChunk) {
        UUID indexVersionId = indexedChunk.chunk().indexVersionId();
        UUID chunkId = indexedChunk.chunk().chunkId();
        List<IndexedChunk> list = new ArrayList<>(chunksByIndexVersion.getOrDefault(indexVersionId, List.of()));
        boolean replaced = false;
        for (int index = 0; index < list.size(); index++) {
            if (list.get(index).chunk().chunkId().equals(chunkId)) {
                list.set(index, indexedChunk);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            throw new ResourceNotFoundException("文档块不存在: " + chunkId);
        }
        chunksByIndexVersion.put(indexVersionId, list);
    }

    @Override
    public Optional<KnowledgeDocument> findDocument(UUID documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    @Override
    public List<IngestionDocumentError> listIngestionDocumentErrors(UUID runId) {
        return ingestionErrors.getOrDefault(runId, List.of());
    }

    @Override
    public IngestionDocumentError saveIngestionDocumentError(IngestionDocumentError error) {
        ingestionErrors.compute(error.runId(), (key, existing) -> {
            List<IngestionDocumentError> updated = new ArrayList<>(existing == null ? List.of() : existing);
            updated.add(error);
            return List.copyOf(updated);
        });
        return error;
    }

    @Override
    public DocumentIndexJob saveDocumentIndexJob(DocumentIndexJob job) {
        documentIndexJobs.put(job.jobId(), job);
        return job;
    }

    @Override
    public List<DocumentIndexJob> listDocumentIndexJobs(UUID runId) {
        return documentIndexJobs.values().stream()
                .filter(job -> job.runId().equals(runId))
                .sorted(java.util.Comparator.comparing(DocumentIndexJob::createdAt))
                .toList();
    }

    @Override
    public java.util.Optional<DocumentIndexJob> findLatestDocumentIndexJob(UUID documentId) {
        return documentIndexJobs.values().stream()
                .filter(job -> documentId.equals(job.documentId()))
                .max(java.util.Comparator.comparing(DocumentIndexJob::updatedAt));
    }

    @Override
    public List<IndexedChunk> listChunksByIndexVersion(UUID indexVersionId) {
        return List.copyOf(chunksByIndexVersion.getOrDefault(indexVersionId, List.of()));
    }

    @Override
    public void saveIndexedChunks(List<IndexedChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        UUID indexVersionId = chunks.getFirst().chunk().indexVersionId();
        List<IndexedChunk> merged = new ArrayList<>(chunksByIndexVersion.getOrDefault(indexVersionId, List.of()));
        merged.addAll(chunks);
        chunksByIndexVersion.put(indexVersionId, merged);
        Instant now = Instant.now();
        for (IndexedChunk indexedChunk : chunks) {
            DocumentChunk chunk = indexedChunk.chunk();
            documents.putIfAbsent(chunk.documentId(), new KnowledgeDocument(
                    chunk.documentId(),
                    chunk.libraryId(),
                    chunk.indexVersionId(),
                    chunk.metadata() == null ? null : String.valueOf(chunk.metadata().get("sourceUri")),
                    chunk.metadata() == null ? null : String.valueOf(chunk.metadata().get("title")),
                    DocumentStatus.INDEXED,
                    null,
                    null,
                    now,
                    null,
                    now,
                    now
            ));
        }
    }

    @Override
    public KnowledgeAgent saveAgent(KnowledgeAgent agent) {
        agents.put(agent.agentId(), agent);
        return agent;
    }

    @Override
    public Optional<KnowledgeAgent> findAgent(UUID agentId) {
        return Optional.ofNullable(agents.get(agentId));
    }

    @Override
    public List<KnowledgeAgent> listAgents(String tenantId) {
        return agents.values().stream()
                .filter(item -> tenantId == null || tenantId.equals(item.tenantId()))
                .toList();
    }

    @Override
    public AgentVersion saveAgentVersion(AgentVersion version) {
        agentVersions.put(version.agentVersionId(), version);
        return version;
    }

    @Override
    public Optional<AgentVersion> findAgentVersion(UUID agentVersionId) {
        return Optional.ofNullable(agentVersions.get(agentVersionId));
    }

    @Override
    public Optional<AgentVersion> findPublishedAgentVersion(UUID agentId) {
        return agentVersions.values().stream()
                .filter(version -> version.agentId().equals(agentId))
                .filter(AgentVersion::published)
                .max((left, right) -> Integer.compare(left.version(), right.version()));
    }

    @Override
    public List<AgentVersion> listAgentVersions(UUID agentId) {
        return agentVersions.values().stream()
                .filter(version -> version.agentId().equals(agentId))
                .sorted((left, right) -> Integer.compare(right.version(), left.version()))
                .toList();
    }

    @Override
    public QueryRun saveQueryRun(QueryRun queryRun) {
        queryRuns.put(queryRun.queryRunId(), queryRun);
        return queryRun;
    }

    @Override
    public Optional<QueryRun> findQueryRun(UUID queryRunId) {
        return Optional.ofNullable(queryRuns.get(queryRunId));
    }

    @Override
    public ChatSession saveChatSession(ChatSession session) {
        chatSessions.put(session.sessionId(), session);
        return session;
    }

    @Override
    public Optional<ChatSession> findChatSession(UUID sessionId) {
        return Optional.ofNullable(chatSessions.get(sessionId));
    }

    @Override
    public List<ChatSession> listChatSessions(String tenantId, UUID agentId) {
        return chatSessions.values().stream()
                .filter(session -> tenantId == null || tenantId.equals(session.tenantId()))
                .filter(session -> agentId == null || agentId.equals(session.agentId()))
                .toList();
    }

    @Override
    public ChatMessage saveChatMessage(ChatMessage message) {
        chatMessages.computeIfAbsent(message.sessionId(), ignored -> new ArrayList<>()).add(message);
        return message;
    }

    @Override
    public List<ChatMessage> listChatMessages(UUID sessionId) {
        return List.copyOf(chatMessages.getOrDefault(sessionId, List.of()));
    }

    @Override
    public Optional<IndexVersion> publishIndexVersion(UUID indexVersionId) {
        IndexVersion indexVersion = indexVersions.get(indexVersionId);
        if (indexVersion == null) {
            return Optional.empty();
        }
        IndexVersion published = new IndexVersion(
                indexVersion.indexVersionId(),
                indexVersion.libraryId(),
                indexVersion.profileId(),
                indexVersion.version(),
                com.knowbase.domain.status.IndexVersionStatus.PUBLISHED,
                indexVersion.documentCount(),
                indexVersion.chunkCount(),
                java.time.Instant.now(),
                indexVersion.createdAt()
        );
        indexVersions.put(indexVersionId, published);
        return Optional.of(published);
    }

    @Override
    public RetrievalEvalSample saveRetrievalEvalSample(RetrievalEvalSample sample) {
        retrievalEvalSamples.put(sample.sampleId(), sample);
        return sample;
    }

    @Override
    public Optional<RetrievalEvalSample> findRetrievalEvalSample(UUID sampleId) {
        return Optional.ofNullable(retrievalEvalSamples.get(sampleId));
    }

    @Override
    public List<RetrievalEvalSample> listRetrievalEvalSamples(UUID libraryId, boolean enabledOnly) {
        return retrievalEvalSamples.values().stream()
                .filter(sample -> sample.libraryId().equals(libraryId))
                .filter(sample -> !enabledOnly || sample.enabled())
                .sorted((left, right) -> right.updatedAt().compareTo(left.updatedAt()))
                .toList();
    }

    @Override
    public void deleteRetrievalEvalSample(UUID sampleId) {
        retrievalEvalSamples.remove(sampleId);
    }

    @Override
    public RetrievalEvalRun saveRetrievalEvalRun(RetrievalEvalRun evalRun) {
        retrievalEvalRuns.put(evalRun.evalRunId(), evalRun);
        return evalRun;
    }

    @Override
    public Optional<RetrievalEvalRun> findRetrievalEvalRun(UUID evalRunId) {
        return Optional.ofNullable(retrievalEvalRuns.get(evalRunId));
    }

    @Override
    public List<RetrievalEvalRun> listRetrievalEvalRuns(UUID libraryId, int limit) {
        return retrievalEvalRuns.values().stream()
                .filter(run -> run.libraryId().equals(libraryId))
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public RetrievalEvalResult saveRetrievalEvalResult(RetrievalEvalResult result) {
        retrievalEvalResults.computeIfAbsent(result.evalRunId(), ignored -> new ArrayList<>()).add(result);
        return result;
    }

    @Override
    public List<RetrievalEvalResult> listRetrievalEvalResults(UUID evalRunId) {
        return List.copyOf(retrievalEvalResults.getOrDefault(evalRunId, List.of()));
    }

    @Override
    public RetrievalEvalBaseline saveRetrievalEvalBaseline(RetrievalEvalBaseline baseline) {
        retrievalEvalBaselines.put(baseline.libraryId(), baseline);
        return baseline;
    }

    @Override
    public Optional<RetrievalEvalBaseline> findRetrievalEvalBaseline(UUID libraryId) {
        return Optional.ofNullable(retrievalEvalBaselines.get(libraryId));
    }

    @Override
    public void deleteRetrievalEvalSamplesByLibrary(UUID libraryId) {
        retrievalEvalSamples.entrySet().removeIf(entry -> entry.getValue().libraryId().equals(libraryId));
    }
}
