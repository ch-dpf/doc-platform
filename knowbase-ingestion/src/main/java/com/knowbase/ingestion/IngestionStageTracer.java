package com.knowbase.ingestion;

import com.knowbase.domain.observability.PipelineObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

final class IngestionStageTracer {

    private static final Logger log = LoggerFactory.getLogger(IngestionStageTracer.class);

    private final PipelineObserver pipelineObserver;

    IngestionStageTracer(PipelineObserver pipelineObserver) {
        this.pipelineObserver = pipelineObserver;
    }

    <T> T trace(
            IngestionTraceContext context,
            String stage,
            Map<String, Object> startAttributes,
            Supplier<T> action,
            Function<T, Map<String, Object>> finishAttributes
    ) {
        long startedAt = System.currentTimeMillis();
        logStageStart(context, stage, startAttributes);
        if (pipelineObserver == null || context == null) {
            try {
                T result = action.get();
                logStageSuccess(context, stage, startedAt, finishAttributes.apply(result));
                return result;
            } catch (RuntimeException exception) {
                logStageFailure(context, stage, startedAt, exception);
                throw exception;
            }
        }
        UUID spanId = pipelineObserver.startSpan(
                "ingestion",
                context.runId(),
                stage,
                context.attributes(startAttributes)
        );
        try {
            T result = action.get();
            Map<String, Object> attributes = finishAttributes.apply(result);
            pipelineObserver.finishSpan(spanId, "SUCCEEDED", attributes);
            logStageSuccess(context, stage, startedAt, attributes);
            return result;
        } catch (RuntimeException exception) {
            pipelineObserver.finishSpan(spanId, "FAILED", Map.of("error", exception.getMessage()));
            logStageFailure(context, stage, startedAt, exception);
            throw exception;
        }
    }

    void traceVoid(
            IngestionTraceContext context,
            String stage,
            Map<String, Object> startAttributes,
            Runnable action,
            Map<String, Object> finishAttributes
    ) {
        trace(context, stage, startAttributes, () -> {
            action.run();
            return null;
        }, ignored -> finishAttributes);
    }

    private static void logStageStart(
            IngestionTraceContext context,
            String stage,
            Map<String, Object> startAttributes
    ) {
        if (!log.isInfoEnabled()) {
            return;
        }
        if (context != null) {
            log.info(
                    "入库阶段开始: stage={}, runId={}, sourceUri={}, attrs={}",
                    stage,
                    context.runId(),
                    context.sourceUri(),
                    startAttributes
            );
            return;
        }
        log.info("入库阶段开始: stage={}, attrs={}", stage, startAttributes);
    }

    private static void logStageSuccess(
            IngestionTraceContext context,
            String stage,
            long startedAt,
            Map<String, Object> attributes
    ) {
        if (!log.isInfoEnabled()) {
            return;
        }
        long durationMs = Math.max(0, System.currentTimeMillis() - startedAt);
        if (context != null) {
            log.info(
                    "入库阶段成功: stage={}, runId={}, sourceUri={}, durationMs={}, attrs={}",
                    stage,
                    context.runId(),
                    context.sourceUri(),
                    durationMs,
                    attributes
            );
            return;
        }
        log.info(
                "入库阶段成功: stage={}, durationMs={}, attrs={}",
                stage,
                durationMs,
                attributes
        );
    }

    private static void logStageFailure(
            IngestionTraceContext context,
            String stage,
            long startedAt,
            RuntimeException exception
    ) {
        long durationMs = Math.max(0, System.currentTimeMillis() - startedAt);
        if (context != null) {
            log.warn(
                    "入库阶段失败: stage={}, runId={}, sourceUri={}, durationMs={}",
                    stage,
                    context.runId(),
                    context.sourceUri(),
                    durationMs,
                    exception
            );
            return;
        }
        log.warn("入库阶段失败: stage={}, durationMs={}", stage, durationMs, exception);
    }
}
