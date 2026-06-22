package com.knowbase.persistence.repository;

import com.knowbase.domain.model.EvalRun;
import com.knowbase.domain.model.EvalSample;
import com.knowbase.domain.model.PipelineSpan;
import com.knowbase.domain.repository.ObservabilityRepository;
import com.knowbase.persistence.support.JsonSupport;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PostgresObservabilityRepository implements ObservabilityRepository {

    private static final RowMapper<PipelineSpan> SPAN_MAPPER = (rs, rowNum) -> new PipelineSpan(
            rs.getObject("span_id", UUID.class),
            rs.getObject("trace_id", UUID.class),
            rs.getString("pipeline"),
            rs.getObject("run_id", UUID.class),
            rs.getString("stage"),
            rs.getString("status"),
            rs.getObject("duration_ms") == null ? null : rs.getLong("duration_ms"),
            JsonSupport.readMap(rs.getString("attributes_json")),
            rs.getTimestamp("started_at").toInstant(),
            rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant()
    );

    private static final RowMapper<EvalRun> EVAL_RUN_MAPPER = (rs, rowNum) -> new EvalRun(
            rs.getObject("eval_run_id", UUID.class),
            rs.getString("tenant_id"),
            rs.getObject("agent_id", UUID.class),
            rs.getString("eval_type"),
            rs.getString("status"),
            JsonSupport.readMap(rs.getString("metrics_json")),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toInstant()
    );

    private static final RowMapper<EvalSample> EVAL_SAMPLE_MAPPER = (rs, rowNum) -> new EvalSample(
            rs.getObject("sample_id", UUID.class),
            rs.getObject("eval_run_id", UUID.class),
            rs.getString("question"),
            rs.getString("expected_answer"),
            rs.getString("actual_answer"),
            rs.getObject("score") == null ? null : rs.getDouble("score"),
            JsonSupport.readMap(rs.getString("metrics_json")),
            rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public PostgresObservabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PipelineSpan savePipelineSpan(PipelineSpan span) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_pipeline_span (span_id, trace_id, pipeline, run_id, stage, status, duration_ms, attributes_json, started_at, finished_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                        ON CONFLICT (span_id) DO UPDATE SET
                            status = EXCLUDED.status,
                            duration_ms = EXCLUDED.duration_ms,
                            attributes_json = EXCLUDED.attributes_json,
                            finished_at = EXCLUDED.finished_at
                        """,
                span.spanId(),
                span.traceId(),
                span.pipeline(),
                span.runId(),
                span.stage(),
                span.status(),
                span.durationMs(),
                JsonSupport.write(span.attributes()),
                Timestamp.from(span.startedAt()),
                span.finishedAt() == null ? null : Timestamp.from(span.finishedAt())
        );
        return span;
    }

    @Override
    public List<PipelineSpan> listPipelineSpans(UUID traceId) {
        return jdbcTemplate.query(
                "SELECT * FROM kb_pipeline_span WHERE trace_id = ? ORDER BY started_at ASC",
                SPAN_MAPPER,
                traceId
        );
    }

    @Override
    public List<PipelineSpan> listPipelineSpansByRun(String pipeline, UUID runId) {
        return jdbcTemplate.query(
                "SELECT * FROM kb_pipeline_span WHERE pipeline = ? AND run_id = ? ORDER BY started_at ASC",
                SPAN_MAPPER,
                pipeline,
                runId
        );
    }

    @Override
    public EvalRun saveEvalRun(EvalRun evalRun) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_eval_run (eval_run_id, tenant_id, agent_id, eval_type, status, metrics_json, created_at, finished_at)
                        VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, ?)
                        ON CONFLICT (eval_run_id) DO UPDATE SET
                            status = EXCLUDED.status,
                            metrics_json = EXCLUDED.metrics_json,
                            finished_at = EXCLUDED.finished_at
                        """,
                evalRun.evalRunId(),
                evalRun.tenantId(),
                evalRun.agentId(),
                evalRun.evalType(),
                evalRun.status(),
                JsonSupport.write(evalRun.metrics()),
                Timestamp.from(evalRun.createdAt()),
                evalRun.finishedAt() == null ? null : Timestamp.from(evalRun.finishedAt())
        );
        return evalRun;
    }

    @Override
    public Optional<EvalRun> findEvalRun(UUID evalRunId) {
        List<EvalRun> rows = jdbcTemplate.query(
                "SELECT * FROM kb_eval_run WHERE eval_run_id = ?",
                EVAL_RUN_MAPPER,
                evalRunId
        );
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    @Override
    public List<EvalRun> listEvalRuns(String tenantId, UUID agentId) {
        if (agentId != null) {
            return jdbcTemplate.query(
                    "SELECT * FROM kb_eval_run WHERE tenant_id = ? AND agent_id = ? ORDER BY created_at DESC",
                    EVAL_RUN_MAPPER,
                    tenantId,
                    agentId
            );
        }
        return jdbcTemplate.query(
                "SELECT * FROM kb_eval_run WHERE tenant_id = ? ORDER BY created_at DESC",
                EVAL_RUN_MAPPER,
                tenantId
        );
    }

    @Override
    public EvalSample saveEvalSample(EvalSample sample) {
        jdbcTemplate.update(
                """
                        INSERT INTO kb_eval_sample (sample_id, eval_run_id, question, expected_answer, actual_answer, score, metrics_json, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
                        ON CONFLICT (sample_id) DO UPDATE SET
                            actual_answer = EXCLUDED.actual_answer,
                            score = EXCLUDED.score,
                            metrics_json = EXCLUDED.metrics_json
                        """,
                sample.sampleId(),
                sample.evalRunId(),
                sample.question(),
                sample.expectedAnswer(),
                sample.actualAnswer(),
                sample.score(),
                JsonSupport.write(sample.metrics()),
                Timestamp.from(sample.createdAt())
        );
        return sample;
    }

    @Override
    public List<EvalSample> listEvalSamples(UUID evalRunId) {
        return jdbcTemplate.query(
                "SELECT * FROM kb_eval_sample WHERE eval_run_id = ? ORDER BY created_at ASC",
                EVAL_SAMPLE_MAPPER,
                evalRunId
        );
    }
}
