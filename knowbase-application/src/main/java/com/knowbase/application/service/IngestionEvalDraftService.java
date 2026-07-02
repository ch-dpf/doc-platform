package com.knowbase.application.service;

import com.knowbase.api.command.CreateRetrievalEvalSampleCommand;
import com.knowbase.api.command.GenerateRetrievalEvalDraftsCommand;
import com.knowbase.api.result.RetrievalEvalSampleResult;
import com.knowbase.application.mapper.ResultMapper;
import com.knowbase.domain.model.DocumentIndexJob;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeDocument;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.RetrievalEvalSample;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.domain.status.DocumentIndexJobStatus;
import com.knowbase.domain.status.IngestionRunStatus;
import com.knowbase.ingestion.IngestionPipelineOptions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class IngestionEvalDraftService {

    private final KnowbaseRepository repository;
    private final RetrievalEvalDraftGenerator draftGenerator;

    public IngestionEvalDraftService(KnowbaseRepository repository) {
        this(repository, new RetrievalEvalDraftGenerator());
    }

    public IngestionEvalDraftService(KnowbaseRepository repository, RetrievalEvalDraftGenerator draftGenerator) {
        this.repository = repository;
        this.draftGenerator = draftGenerator;
    }

    public int onRunCompleted(IngestionRun run) {
        if (run == null || run.chunkCount() <= 0) {
            return 0;
        }
        if (run.status() != IngestionRunStatus.SUCCEEDED && run.status() != IngestionRunStatus.PARTIAL_FAILED) {
            return 0;
        }
        if (!IngestionPipelineOptions.autoEvalDrafts(run.options())) {
            return 0;
        }
        List<UUID> documentIds = repository.listDocumentIndexJobs(run.runId()).stream()
                .filter(job -> DocumentIndexJobStatus.SUCCEEDED.name().equals(job.status()))
                .map(DocumentIndexJob::documentId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        return generateForDocuments(run.libraryId(), documentIds, false).size();
    }

    public List<RetrievalEvalSampleResult> generateForCommand(UUID libraryId, GenerateRetrievalEvalDraftsCommand command) {
        List<UUID> documentIds = resolveDocumentIds(libraryId, command);
        boolean replace = command.replaceExistingAutoDrafts() != null && command.replaceExistingAutoDrafts();
        return generateForDocuments(libraryId, documentIds, replace).stream()
                .map(ResultMapper::toRetrievalEvalSampleResult)
                .toList();
    }

    private List<UUID> resolveDocumentIds(UUID libraryId, GenerateRetrievalEvalDraftsCommand command) {
        if (command.documentIds() != null && !command.documentIds().isEmpty()) {
            return List.copyOf(command.documentIds());
        }
        if (command.ingestionRunId() != null) {
            return repository.listDocumentIndexJobs(command.ingestionRunId()).stream()
                    .filter(job -> job.libraryId().equals(libraryId))
                    .filter(job -> DocumentIndexJobStatus.SUCCEEDED.name().equals(job.status()))
                    .map(DocumentIndexJob::documentId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
        }
        return repository.listDocuments(libraryId, activeGenerationId(libraryId)).stream()
                .map(KnowledgeDocument::documentId)
                .toList();
    }

    private List<RetrievalEvalSample> generateForDocuments(UUID libraryId, List<UUID> documentIds, boolean replaceAutoDrafts) {
        if (documentIds == null || documentIds.isEmpty()) {
            return List.of();
        }
        if (replaceAutoDrafts) {
            deleteAllAutoDrafts(libraryId);
        }
        int hitRank = resolveHitRank(libraryId);
        Set<String> seenQuestions = replaceAutoDrafts
                ? new LinkedHashSet<>()
                : existingAutoDraftQuestions(libraryId);
        List<CreateRetrievalEvalSampleCommand> pooled = poolDraftCommands(libraryId, documentIds, hitRank, seenQuestions);
        return persistDraftBatch(libraryId, pooled, hitRank);
    }

    private List<CreateRetrievalEvalSampleCommand> poolDraftCommands(
            UUID libraryId,
            List<UUID> documentIds,
            int hitRank,
            Set<String> seenQuestions
    ) {
        List<List<CreateRetrievalEvalSampleCommand>> perDocument = new ArrayList<>();
        for (UUID documentId : documentIds) {
            KnowledgeDocument document = repository.findDocument(documentId)
                    .filter(doc -> doc.libraryId().equals(libraryId))
                    .orElse(null);
            if (document == null) {
                continue;
            }
            List<CreateRetrievalEvalSampleCommand> drafts = draftGenerator.generate(
                    document,
                    repository.listChunksByDocument(documentId),
                    hitRank,
                    RetrievalEvalDraftSupport.MAX_DRAFTS_PER_GENERATION
            );
            if (!drafts.isEmpty()) {
                perDocument.add(drafts);
            }
        }
        List<CreateRetrievalEvalSampleCommand> pooled = new ArrayList<>();
        int round = 0;
        while (pooled.size() < RetrievalEvalDraftSupport.MAX_DRAFTS_PER_GENERATION) {
            boolean addedInRound = false;
            for (List<CreateRetrievalEvalSampleCommand> drafts : perDocument) {
                if (round >= drafts.size()) {
                    continue;
                }
                CreateRetrievalEvalSampleCommand draft = drafts.get(round);
                if (!seenQuestions.add(normalizeQuestion(draft.question()))) {
                    continue;
                }
                pooled.add(draft);
                addedInRound = true;
                if (pooled.size() >= RetrievalEvalDraftSupport.MAX_DRAFTS_PER_GENERATION) {
                    break;
                }
            }
            if (!addedInRound) {
                break;
            }
            round++;
        }
        return List.copyOf(pooled);
    }

    private List<RetrievalEvalSample> persistDraftBatch(
            UUID libraryId,
            List<CreateRetrievalEvalSampleCommand> pooled,
            int hitRank
    ) {
        List<RetrievalEvalSample> saved = new ArrayList<>(pooled.size());
        Instant now = Instant.now();
        for (int index = 0; index < pooled.size(); index++) {
            CreateRetrievalEvalSampleCommand draft = pooled.get(index);
            RetrievalEvalSample sample = new RetrievalEvalSample(
                    UUID.randomUUID(),
                    libraryId,
                    draft.question().trim(),
                    List.copyOf(draft.expectedDocumentIds()),
                    List.copyOf(draft.expectedSourceUris()),
                    List.copyOf(draft.groundTruthContexts()),
                    draft.hitRank() == null ? hitRank : draft.hitRank(),
                    draft.notes(),
                    RetrievalEvalDraftSupport.isEnabledByDefault(index),
                    now,
                    now
            );
            saved.add(repository.saveRetrievalEvalSample(sample));
        }
        return List.copyOf(saved);
    }

    private void deleteAllAutoDrafts(UUID libraryId) {
        for (RetrievalEvalSample sample : repository.listRetrievalEvalSamples(libraryId, false)) {
            if (RetrievalEvalDraftSupport.isAutoDraft(sample)) {
                repository.deleteRetrievalEvalSample(sample.sampleId());
            }
        }
    }

    private Set<String> existingAutoDraftQuestions(UUID libraryId) {
        Set<String> questions = new LinkedHashSet<>();
        for (RetrievalEvalSample sample : repository.listRetrievalEvalSamples(libraryId, false)) {
            if (RetrievalEvalDraftSupport.isAutoDraft(sample)) {
                questions.add(normalizeQuestion(sample.question()));
            }
        }
        return questions;
    }

    private int resolveHitRank(UUID libraryId) {
        return repository.findLatestLibraryProfile(libraryId)
                .map(LibraryProfile::retrievalTopK)
                .filter(topK -> topK > 0)
                .orElse(8);
    }

    private UUID activeGenerationId(UUID libraryId) {
        return repository.findLibrary(libraryId)
                .map(library -> library.activeIndexGenerationId())
                .orElseThrow(() -> new IllegalStateException("知识库不存在: " + libraryId));
    }

    private static String normalizeQuestion(String question) {
        return question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
    }
}
