package com.knowbase.application.mapper;

import com.knowbase.api.result.CitationResult;
import com.knowbase.api.result.EvidenceResult;
import com.knowbase.api.result.IngestionRunResult;
import com.knowbase.api.result.KnowledgeAgentResult;
import com.knowbase.api.result.LibraryResult;
import com.knowbase.api.result.PresetResult;
import com.knowbase.api.result.QueryRunResult;
import com.knowbase.api.result.TokenUsageResult;
import com.knowbase.api.result.TokenizerProfileResult;
import com.knowbase.domain.model.AgentVersion;
import com.knowbase.domain.model.Citation;
import com.knowbase.domain.model.EvidencePack;
import com.knowbase.domain.model.EvidenceSegment;
import com.knowbase.domain.model.IngestionRun;
import com.knowbase.domain.model.KnowledgeAgent;
import com.knowbase.domain.model.KnowledgeLibrary;
import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.QueryRun;
import com.knowbase.domain.model.SceneRulePreset;
import com.knowbase.domain.model.TokenizerProfile;

public final class ResultMapper {

    private ResultMapper() {
    }

    public static LibraryResult toLibraryResult(KnowledgeLibrary library) {
        return new LibraryResult(
                library.libraryId(),
                library.tenantId(),
                library.name(),
                library.description(),
                library.status().name(),
                library.libraryTypePresetCode(),
                library.tags(),
                library.createdAt(),
                library.updatedAt()
        );
    }

    public static IngestionRunResult toIngestionRunResult(IngestionRun run) {
        return new IngestionRunResult(
                run.runId(),
                run.libraryId(),
                run.status().name(),
                run.inputDocuments(),
                run.succeededDocuments(),
                run.failedDocuments(),
                run.chunkCount(),
                run.indexVersionId(),
                run.message(),
                run.createdAt(),
                run.updatedAt()
        );
    }

    public static KnowledgeAgentResult toKnowledgeAgentResult(KnowledgeAgent agent, AgentVersion version) {
        return new KnowledgeAgentResult(
                agent.agentId(),
                version.agentVersionId(),
                agent.tenantId(),
                agent.name(),
                agent.description(),
                agent.status().name(),
                version.version(),
                version.scenePresetCode(),
                version.libraryIds(),
                version.chatTokenizerProfileId(),
                version.published(),
                agent.createdAt(),
                agent.updatedAt()
        );
    }

    public static QueryRunResult toQueryRunResult(QueryRun queryRun) {
        EvidencePack evidencePack = queryRun.evidencePack();
        return new QueryRunResult(
                queryRun.queryRunId(),
                queryRun.agentId(),
                queryRun.agentVersionId(),
                queryRun.status().name(),
                queryRun.question(),
                queryRun.answer(),
                evidencePack == null ? java.util.List.of() : evidencePack.citations().stream().map(ResultMapper::toCitationResult).toList(),
                evidencePack == null ? java.util.List.of() : evidencePack.segments().stream().map(ResultMapper::toEvidenceResult).toList(),
                new TokenUsageResult(
                        queryRun.promptTokens(),
                        queryRun.completionTokens(),
                        queryRun.promptTokens() + queryRun.completionTokens(),
                        evidencePack == null ? 0 : evidencePack.contextTokens(),
                        evidencePack == null ? null : evidencePack.tokenizerId(),
                        evidencePack == null ? null : evidencePack.tokenizerVersion()
                ),
                queryRun.traceId(),
                queryRun.createdAt(),
                queryRun.completedAt()
        );
    }

    public static PresetResult toPresetResult(LibraryTypePreset preset) {
        return new PresetResult(
                preset.code(),
                preset.name(),
                preset.description(),
                preset.config(),
                preset.builtIn(),
                preset.enabled()
        );
    }

    public static PresetResult toPresetResult(SceneRulePreset preset) {
        return new PresetResult(
                preset.code(),
                preset.name(),
                preset.description(),
                preset.config(),
                preset.builtIn(),
                preset.enabled()
        );
    }

    public static TokenizerProfileResult toTokenizerProfileResult(TokenizerProfile profile) {
        return new TokenizerProfileResult(
                profile.tokenizerProfileId(),
                profile.provider(),
                profile.modelName(),
                profile.tokenizerId(),
                profile.tokenizerVersion(),
                profile.approximate(),
                profile.config(),
                profile.enabled(),
                profile.createdAt(),
                profile.updatedAt()
        );
    }

    public static CitationResult toCitationResult(Citation citation) {
        return new CitationResult(
                citation.citationId(),
                citation.libraryId(),
                citation.documentId(),
                citation.chunkId(),
                citation.indexVersionId(),
                citation.sourceTitle(),
                citation.sourceUri(),
                citation.snippet(),
                citation.score()
        );
    }

    public static EvidenceResult toEvidenceResult(EvidenceSegment segment) {
        return new EvidenceResult(
                segment.evidenceId(),
                segment.libraryId(),
                segment.documentId(),
                segment.chunkId(),
                segment.indexVersionId(),
                segment.content(),
                segment.score(),
                segment.metadata()
        );
    }
}
