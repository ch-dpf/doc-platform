package com.knowbase.application.service;

import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.ChatMessage;
import com.knowbase.domain.model.ChatSession;
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
import com.knowbase.domain.status.IndexVersionStatus;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryKnowbaseRepository implements KnowbaseRepository {

    private final ConcurrentMap<UUID, KnowledgeLibrary> libraries = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, LibraryProfile> libraryProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, TokenizerProfile> tokenizerProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<DocumentProfile>> documentProfiles = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, IngestionRun> ingestionRuns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, IndexVersion> indexVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<IndexedChunk>> chunksByIndexVersion = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, KnowledgeDocument> documents = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<IngestionDocumentError>> ingestionErrors = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, KnowledgeAgent> agents = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AgentVersion> agentVersions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, QueryRun> queryRuns = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, ChatSession> chatSessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, List<ChatMessage>> chatMessages = new ConcurrentHashMap<>();

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
                .toList();
    }

    @Override
    public LibraryProfile saveLibraryProfile(LibraryProfile profile) {
        libraryProfiles.put(profile.libraryId(), profile);
        return profile;
    }

    @Override
    public Optional<LibraryProfile> findLatestLibraryProfile(UUID libraryId) {
        return Optional.ofNullable(libraryProfiles.get(libraryId));
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
    public IngestionRun saveIngestionRun(IngestionRun run) {
        ingestionRuns.put(run.runId(), run);
        return run;
    }

    @Override
    public Optional<IngestionRun> findIngestionRun(UUID runId) {
        return Optional.ofNullable(ingestionRuns.get(runId));
    }

    @Override
    public IndexVersion saveIndexVersion(IndexVersion indexVersion) {
        indexVersions.put(indexVersion.indexVersionId(), indexVersion);
        return indexVersion;
    }

    @Override
    public Optional<IndexVersion> findPublishedIndexVersion(UUID libraryId) {
        return indexVersions.values().stream()
                .filter(version -> version.libraryId().equals(libraryId))
                .filter(version -> version.status() == IndexVersionStatus.PUBLISHED)
                .max((left, right) -> Integer.compare(left.version(), right.version()));
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
        return documents.values().stream()
                .filter(document -> document.libraryId().equals(libraryId))
                .filter(document -> indexVersionId == null || indexVersionId.equals(document.indexVersionId()))
                .toList();
    }

    @Override
    public Optional<KnowledgeDocument> findDocument(UUID documentId) {
        return Optional.ofNullable(documents.get(documentId));
    }

    @Override
    public List<DocumentChunk> listChunksByDocument(UUID documentId) {
        return chunksByIndexVersion.values().stream()
                .flatMap(List::stream)
                .map(IndexedChunk::chunk)
                .filter(chunk -> chunk.documentId().equals(documentId))
                .toList();
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
    public List<IndexedChunk> listChunksByIndexVersion(UUID indexVersionId) {
        return List.copyOf(chunksByIndexVersion.getOrDefault(indexVersionId, List.of()));
    }

    @Override
    public void saveIndexedChunks(List<IndexedChunk> chunks) {
        if (chunks.isEmpty()) {
            return;
        }
        UUID indexVersionId = chunks.getFirst().chunk().indexVersionId();
        chunksByIndexVersion.put(indexVersionId, List.copyOf(chunks));
        Instant now = java.time.Instant.now();
        for (IndexedChunk indexedChunk : chunks) {
            DocumentChunk chunk = indexedChunk.chunk();
            documents.putIfAbsent(chunk.documentId(), new KnowledgeDocument(
                    chunk.documentId(),
                    chunk.libraryId(),
                    chunk.indexVersionId(),
                    chunk.metadata() == null ? null : String.valueOf(chunk.metadata().get("sourceUri")),
                    chunk.metadata() == null ? null : String.valueOf(chunk.metadata().get("title")),
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
}
