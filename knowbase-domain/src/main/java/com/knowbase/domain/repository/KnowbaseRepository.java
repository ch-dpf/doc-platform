package com.knowbase.domain.repository;

import com.knowbase.domain.model.ChatMessage;
import com.knowbase.domain.model.ChatSession;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowbaseRepository {

    KnowledgeLibrary saveLibrary(KnowledgeLibrary library);

    Optional<KnowledgeLibrary> findLibrary(UUID libraryId);

    List<KnowledgeLibrary> listLibraries(String tenantId);

    LibraryProfile saveLibraryProfile(LibraryProfile profile);

    Optional<LibraryProfile> findLatestLibraryProfile(UUID libraryId);

    TokenizerProfile saveTokenizerProfile(TokenizerProfile profile);

    Optional<TokenizerProfile> findTokenizerProfile(UUID tokenizerProfileId);

    Optional<TokenizerProfile> findTokenizerProfile(String provider, String modelName);

    List<TokenizerProfile> listTokenizerProfiles(String provider, boolean includeDisabled);

    DocumentProfile saveDocumentProfile(DocumentProfile profile);

    List<DocumentProfile> listDocumentProfiles(UUID libraryId);

    Optional<DocumentProfile> findDocumentProfile(UUID libraryId, String code);

    IngestionRun saveIngestionRun(IngestionRun run);

    Optional<IngestionRun> findIngestionRun(UUID runId);

    IndexVersion saveIndexVersion(IndexVersion indexVersion);

    Optional<IndexVersion> findPublishedIndexVersion(UUID libraryId);

    List<IndexVersion> listIndexVersions(UUID libraryId);

    Optional<IndexVersion> findIndexVersion(UUID indexVersionId);

    List<KnowledgeDocument> listDocuments(UUID libraryId, UUID indexVersionId);

    Optional<KnowledgeDocument> findDocument(UUID documentId);

    List<DocumentChunk> listChunksByDocument(UUID documentId);

    List<IngestionDocumentError> listIngestionDocumentErrors(UUID runId);

    IngestionDocumentError saveIngestionDocumentError(IngestionDocumentError error);

    List<IndexedChunk> listChunksByIndexVersion(UUID indexVersionId);

    void saveIndexedChunks(List<IndexedChunk> chunks);

    KnowledgeAgent saveAgent(KnowledgeAgent agent);

    Optional<KnowledgeAgent> findAgent(UUID agentId);

    List<KnowledgeAgent> listAgents(String tenantId);

    AgentVersion saveAgentVersion(AgentVersion version);

    Optional<AgentVersion> findAgentVersion(UUID agentVersionId);

    Optional<AgentVersion> findPublishedAgentVersion(UUID agentId);

    List<AgentVersion> listAgentVersions(UUID agentId);

    QueryRun saveQueryRun(QueryRun queryRun);

    Optional<QueryRun> findQueryRun(UUID queryRunId);

    ChatSession saveChatSession(ChatSession session);

    Optional<ChatSession> findChatSession(UUID sessionId);

    List<ChatSession> listChatSessions(String tenantId, UUID agentId);

    ChatMessage saveChatMessage(ChatMessage message);

    List<ChatMessage> listChatMessages(UUID sessionId);

    Optional<IndexVersion> publishIndexVersion(UUID indexVersionId);
}
