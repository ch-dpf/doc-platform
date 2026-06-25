package com.knowbase.application.mapper;

import com.knowbase.api.result.PostProcessStageResult;
import com.knowbase.ingestion.ChunkPostProcessMetrics;

public final class PostProcessStageMapper {

    private PostProcessStageMapper() {
    }

    public static PostProcessStageResult toStageResult(ChunkPostProcessMetrics metrics) {
        if (metrics == null) {
            return null;
        }
        return new PostProcessStageResult(
                metrics.applied(),
                metrics.beforeCount(),
                metrics.afterCount(),
                metrics.indexableBeforeCount(),
                metrics.indexableAfterCount(),
                metrics.summariesAdded(),
                metrics.rowsMerged(),
                metrics.deduplicated()
        );
    }
}
