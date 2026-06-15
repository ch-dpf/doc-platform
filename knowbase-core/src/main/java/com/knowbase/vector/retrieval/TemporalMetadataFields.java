package com.knowbase.vector.retrieval;

import java.util.List;

/** 分块时间元数据字段名（写入 chunk.metadata JSONB）。 */
public final class TemporalMetadataFields {

    public static final String PERIOD_YEAR = "periodYear";
    public static final String PERIOD_START = "periodStart";
    public static final String PERIOD_END = "periodEnd";
    public static final String PERIOD_MONTHS = "periodMonths";
    public static final String SUBMITTER = "submitter";
    public static final String SECTION_LABEL = "sectionLabel";
    public static final String HAS_COMPLETED_WORK = "hasCompletedWork";

    private TemporalMetadataFields() {}

    public static List<String> defaultFilterWhitelist() {
        return List.of(
                PERIOD_YEAR,
                PERIOD_START,
                PERIOD_END,
                PERIOD_MONTHS,
                SUBMITTER,
                SECTION_LABEL,
                HAS_COMPLETED_WORK,
                "docType",
                "fileName",
                "mimeType");
    }
}
