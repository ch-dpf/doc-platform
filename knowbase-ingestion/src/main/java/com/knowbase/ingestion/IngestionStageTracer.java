package com.knowbase.ingestion;

import com.knowbase.domain.observability.PipelineObserver;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

final class IngestionStageTracer {

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
        if (pipelineObserver == null || context == null) {
            return action.get();
        }
        UUID spanId = pipelineObserver.startSpan(
                "ingestion",
                context.runId(),
                stage,
                context.attributes(startAttributes)
        );
        try {
            T result = action.get();
            pipelineObserver.finishSpan(spanId, "SUCCEEDED", finishAttributes.apply(result));
            return result;
        } catch (RuntimeException exception) {
            pipelineObserver.finishSpan(spanId, "FAILED", Map.of("error", exception.getMessage()));
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
}
