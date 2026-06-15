package com.knowbase.vector.dto;

import java.util.List;

/** 时间范围与 periodMonths 并集预过滤（日期区间重叠 OR 月份列表命中）。 */
public record TemporalOverlapFilter(
        String periodYear,
        String rangeStart,
        String rangeEnd,
        List<Integer> months) {

    public TemporalOverlapFilter {
        months = months == null ? List.of() : List.copyOf(months);
    }
}
