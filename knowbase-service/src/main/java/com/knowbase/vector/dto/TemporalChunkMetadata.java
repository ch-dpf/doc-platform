package com.knowbase.vector.dto;

import com.knowbase.vector.retrieval.TemporalMetadataFields;

import java.util.LinkedHashMap;
import java.util.Map;

/** 检索命中分块上的时间/人员结构化元数据（来自 chunk.metadata JSONB）。 */
public record TemporalChunkMetadata(
        String periodYear,
        String periodStart,
        String periodEnd,
        String periodMonths,
        String submitter,
        String sectionLabel,
        String hasCompletedWork) {

    public static TemporalChunkMetadata empty() {
        return new TemporalChunkMetadata(null, null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return (periodYear == null || periodYear.isBlank())
                && (periodStart == null || periodStart.isBlank())
                && (periodEnd == null || periodEnd.isBlank())
                && (periodMonths == null || periodMonths.isBlank())
                && (submitter == null || submitter.isBlank())
                && (sectionLabel == null || sectionLabel.isBlank())
                && (hasCompletedWork == null || hasCompletedWork.isBlank());
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>();
        put(map, TemporalMetadataFields.PERIOD_YEAR, periodYear);
        put(map, TemporalMetadataFields.PERIOD_START, periodStart);
        put(map, TemporalMetadataFields.PERIOD_END, periodEnd);
        put(map, TemporalMetadataFields.PERIOD_MONTHS, periodMonths);
        put(map, TemporalMetadataFields.SUBMITTER, submitter);
        put(map, TemporalMetadataFields.SECTION_LABEL, sectionLabel);
        put(map, TemporalMetadataFields.HAS_COMPLETED_WORK, hasCompletedWork);
        return map.isEmpty() ? Map.of() : Map.copyOf(map);
    }

    private static void put(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.strip());
        }
    }
}
