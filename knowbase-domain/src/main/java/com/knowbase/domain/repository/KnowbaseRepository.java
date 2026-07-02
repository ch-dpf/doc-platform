package com.knowbase.domain.repository;

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
import com.knowbase.domain.support.PagedList;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KnowbaseRepository {

    KnowledgeLibrary saveLibrary(KnowledgeLibrary library);

    Optional<KnowledgeLibrary> findLibrary(UUID libraryId);

    List<KnowledgeLibrary> listLibraries(String tenantId);

    PagedList<KnowledgeLibrary> pageLibraries(String tenantId, int page, int size);

    void deleteLibrary(UUID libraryId);

    boolean isLibraryReferencedByAgent(UUID libraryId);

    LibraryProfile saveLibraryProfile(LibraryProfile profile);

    Optional<LibraryProfile> findLatestLibraryProfile(UUID libraryId);

    Optional<LibraryProfile> findLibraryProfile(UUID profileId);

    List<LibraryProfile> listLibraryProfiles(UUID libraryId);

    TokenizerProfile saveTokenizerProfile(TokenizerProfile profile);

    Optional<TokenizerProfile> findTokenizerProfile(UUID tokenizerProfileId);

    Optional<TokenizerProfile> findTokenizerProfile(String provider, String modelName);

    List<TokenizerProfile> listTokenizerProfiles(String provider, boolean includeDisabled);

    DocumentProfile saveDocumentProfile(DocumentProfile profile);

    List<DocumentProfile> listDocumentProfiles(UUID libraryId);

    Optional<DocumentProfile> findDocumentProfile(UUID libraryId, String code);

    void deleteDocumentProfile(UUID libraryId, String code);

    IngestionRun saveIngestionRun(IngestionRun run);

    Optional<IngestionRun> findIngestionRun(UUID runId);

    List<IngestionRun> listIngestionRuns(UUID libraryId, int limit);

    IndexVersion saveIndexVersion(IndexVersion indexVersion);

    Optional<IndexVersion> findPublishedIndexVersion(UUID libraryId);

    Optional<IndexVersion> findActiveIndexVersion(UUID libraryId);

    List<IndexVersion> listIndexVersions(UUID libraryId);

    Optional<IndexVersion> findIndexVersion(UUID indexVersionId);

    List<KnowledgeDocument> listDocuments(UUID libraryId, UUID indexVersionId);

    PagedList<KnowledgeDocument> pageDocuments(UUID libraryId, UUID indexVersionId, int page, int size);

    Optional<KnowledgeDocument> findDocument(UUID documentId);

    Optional<KnowledgeDocument> findDocumentBySourceUri(UUID libraryId, String sourceUri);

    KnowledgeDocument saveDocument(KnowledgeDocument document);

    void deleteDocumentAndChunks(UUID documentId);

    void replaceDocumentChunks(UUID documentId, List<IndexedChunk> chunks);

    void refreshIndexVersionStats(UUID indexVersionId);

    void setActiveIndexGeneration(UUID libraryId, UUID indexVersionId);

    void archivePublishedGenerationsExcept(UUID libraryId, UUID keepIndexVersionId);

    void reassignDocumentsToGeneration(UUID libraryId, UUID indexGenerationId);

    List<DocumentChunk> listChunksByDocument(UUID documentId);

    PagedList<DocumentChunk> pageChunksByDocument(UUID documentId, int page, int size);

    Optional<DocumentChunk> findChunk(UUID chunkId);

    Optional<float[]> findChunkEmbedding(UUID chunkId);

    void updateIndexedChunk(IndexedChunk indexedChunk);

    List<IngestionDocumentError> listIngestionDocumentErrors(UUID runId);

    IngestionDocumentError saveIngestionDocumentError(IngestionDocumentError error);

    DocumentIndexJob saveDocumentIndexJob(DocumentIndexJob job);

    List<DocumentIndexJob> listDocumentIndexJobs(UUID runId);

    java.util.Optional<DocumentIndexJob> findLatestDocumentIndexJob(UUID documentId);

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

    RetrievalEvalSample saveRetrievalEvalSample(RetrievalEvalSample sample);

    Optional<RetrievalEvalSample> findRetrievalEvalSample(UUID sampleId);

    List<RetrievalEvalSample> listRetrievalEvalSamples(UUID libraryId, boolean enabledOnly);

    void deleteRetrievalEvalSample(UUID sampleId);

    RetrievalEvalRun saveRetrievalEvalRun(RetrievalEvalRun evalRun);

    Optional<RetrievalEvalRun> findRetrievalEvalRun(UUID evalRunId);

    List<RetrievalEvalRun> listRetrievalEvalRuns(UUID libraryId, int limit);

    RetrievalEvalResult saveRetrievalEvalResult(RetrievalEvalResult result);

    List<RetrievalEvalResult> listRetrievalEvalResults(UUID evalRunId);

    RetrievalEvalBaseline saveRetrievalEvalBaseline(RetrievalEvalBaseline baseline);

    Optional<RetrievalEvalBaseline> findRetrievalEvalBaseline(UUID libraryId);

    void deleteRetrievalEvalSamplesByLibrary(UUID libraryId);
}
