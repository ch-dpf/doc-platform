package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public record ChunkPostProcessMetrics(
        boolean applied,
        int beforeCount,
        int afterCount,
        int indexableBeforeCount,
        int indexableAfterCount,
        int summariesAdded,
        int rowsMerged,
        int deduplicated
) {

    private static final Set<String> SUMMARY_ROLES = Set.of("document_summary");

    public static ChunkPostProcessMetrics notApplied(List<DocumentChunk> chunks) {
        int indexable = countIndexable(chunks);
        return new ChunkPostProcessMetrics(false, chunks.size(), chunks.size(), indexable, indexable, 0, 0, 0);
    }

    public static ChunkPostProcessMetrics compute(List<DocumentChunk> before, List<DocumentChunk> after) {
        int summariesAdded = countSummaryChunks(after) - countSummaryChunks(before);
        int rowsMerged = countRole(after, "table_row_group") - countRole(before, "table_row_group");
        int indexableBefore = countIndexable(before);
        int indexableAfter = countIndexable(after);
        int deduplicated = Math.max(0, indexableBefore - indexableAfter - Math.max(0, summariesAdded));
        return new ChunkPostProcessMetrics(
                true,
                before.size(),
                after.size(),
                indexableBefore,
                indexableAfter,
                Math.max(0, summariesAdded),
                Math.max(0, rowsMerged),
                deduplicated
        );
    }

    private static int countIndexable(List<DocumentChunk> chunks) {
        int count = 0;
        for (DocumentChunk chunk : chunks) {
            if (isIndexable(chunk)) {
                count++;
            }
        }
        return count;
    }

    private static int countSummaryChunks(List<DocumentChunk> chunks) {
        int count = 0;
        for (DocumentChunk chunk : chunks) {
            if (isSummaryChunk(chunk)) {
                count++;
            }
        }
        return count;
    }

    private static int countRole(List<DocumentChunk> chunks, String role) {
        int count = 0;
        for (DocumentChunk chunk : chunks) {
            if (role.equals(chunkRole(chunk))) {
                count++;
            }
        }
        return count;
    }

    public static boolean isSummaryChunk(DocumentChunk chunk) {
        if (chunk == null) {
            return false;
        }
        if ("document_summary".equals(chunk.chunkBoundaryType())) {
            return true;
        }
        return SUMMARY_ROLES.contains(chunkRole(chunk));
    }

    private static String chunkRole(DocumentChunk chunk) {
        if (chunk.metadata() == null) {
            return "";
        }
        Object role = chunk.metadata().get("chunkRole");
        return role == null ? "" : String.valueOf(role).trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isIndexable(DocumentChunk chunk) {
        if (chunk.parentChunkId() != null) {
            return true;
        }
        if (chunk.metadata() == null) {
            return true;
        }
        Object indexable = chunk.metadata().get("indexable");
        if (indexable instanceof Boolean value) {
            return value;
        }
        return true;
    }
}
