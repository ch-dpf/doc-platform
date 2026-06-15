package com.knowbase.vector.retrieval;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.dto.TemporalOverlapFilter;
import com.knowbase.vector.rag.TemporalParseConfidence;
import com.knowbase.vector.rag.TemporalQueryScope;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** 将问句时间范围转为 metadata 预过滤与检索后过滤。 */
public final class TemporalRetrievalSupport {

    private TemporalRetrievalSupport() {}

    public record PreFilterPlan(
            List<MetadataFilterClause> equalityFilters,
            TemporalOverlapFilter overlapFilter,
            String routingNote) {

        static PreFilterPlan empty() {
            return new PreFilterPlan(List.of(), null, null);
        }
    }

    public static PreFilterPlan buildPreFilterPlan(
            TemporalQueryScope scope, RetrievalRulesSettings retrieval) {
        if (scope == null || !scope.scoped()) {
            return PreFilterPlan.empty();
        }
        TemporalParseConfidence confidence =
                scope.confidence() != null ? scope.confidence() : TemporalParseConfidence.MEDIUM;
        TemporalOverlapFilter overlap = buildOverlapFilter(scope);
        return switch (confidence) {
            case NONE, LOW -> new PreFilterPlan(List.of(), null, "低置信度：仅检索后过滤");
            case MEDIUM -> new PreFilterPlan(
                    resolveAllowed(buildTimeEqualityFilters(scope), retrieval),
                    overlap,
                    "中置信度：时间元数据预过滤");
            case HIGH -> new PreFilterPlan(
                    resolveAllowed(buildFullEqualityFilters(scope), retrieval),
                    overlap,
                    "高置信度：时间+人员元数据预过滤");
        };
    }

    public static List<MetadataFilterClause> buildPreFilters(
            TemporalQueryScope scope, RetrievalRulesSettings retrieval) {
        return buildPreFilterPlan(scope, retrieval).equalityFilters();
    }

    public static TemporalOverlapFilter buildOverlapFilter(TemporalQueryScope scope) {
        if (scope == null || !scope.scoped() || scope.year() == null) {
            return null;
        }
        List<Integer> months = scope.preciseDateScoped() ? List.of() : scope.monthsInRange();
        return new TemporalOverlapFilter(
                String.valueOf(scope.year()),
                scope.rangeStart().map(Object::toString).orElse(null),
                scope.rangeEnd().map(Object::toString).orElse(null),
                months);
    }

    public static List<SearchHit> applyPostFilter(
            List<SearchHit> hits, TemporalQueryScope scope, Map<UUID, String> fileNames) {
        if (scope == null || !scope.scoped() || hits == null || hits.isEmpty()) {
            return hits == null ? List.of() : hits;
        }
        List<SearchHit> kept = new ArrayList<>();
        for (SearchHit hit : hits) {
            String fileName = fileNames != null ? fileNames.get(hit.docId()) : null;
            if (TemporalHitMatcher.matches(hit, scope, fileName, hit.temporalMetadataMap())) {
                kept.add(hit);
            }
        }
        return kept;
    }

    private static List<MetadataFilterClause> buildTimeEqualityFilters(TemporalQueryScope scope) {
        List<MetadataFilterClause> clauses = new ArrayList<>();
        if (scope.year() != null) {
            clauses.add(new MetadataFilterClause(TemporalMetadataFields.PERIOD_YEAR, String.valueOf(scope.year())));
        }
        if (scope.completedWorkOnly()) {
            clauses.add(new MetadataFilterClause(TemporalMetadataFields.HAS_COMPLETED_WORK, "true"));
            clauses.add(new MetadataFilterClause(TemporalMetadataFields.SECTION_LABEL, "工作周报"));
        }
        return clauses;
    }

    private static List<MetadataFilterClause> buildFullEqualityFilters(TemporalQueryScope scope) {
        List<MetadataFilterClause> clauses = new ArrayList<>(buildTimeEqualityFilters(scope));
        if (!scope.persons().isEmpty()) {
            clauses.add(new MetadataFilterClause(TemporalMetadataFields.SUBMITTER, scope.person()));
        }
        return clauses;
    }

    public static List<MetadataFilterClause> mergeFilters(
            List<MetadataFilterClause> userFilters, List<MetadataFilterClause> temporalFilters) {
        List<MetadataFilterClause> merged = new ArrayList<>();
        if (temporalFilters != null) {
            merged.addAll(temporalFilters);
        }
        if (userFilters != null) {
            merged.addAll(userFilters);
        }
        return List.copyOf(merged);
    }

    private static List<MetadataFilterClause> resolveAllowed(
            List<MetadataFilterClause> clauses, RetrievalRulesSettings retrieval) {
        if (clauses.isEmpty()) {
            return List.of();
        }
        Set<String> allowed = allowedFields(retrieval);
        if (allowed.isEmpty()) {
            return List.copyOf(clauses);
        }
        return clauses.stream().filter(clause -> allowed.contains(clause.field())).collect(Collectors.toList());
    }

    private static Set<String> allowedFields(RetrievalRulesSettings retrieval) {
        if (retrieval == null || retrieval.getMetadataFilterFields() == null) {
            return Set.of();
        }
        return retrieval.getMetadataFilterFields().stream()
                .filter(field -> field != null && !field.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public static Map<String, String> toEqualityMetadataMap(List<MetadataFilterClause> clauses) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (clauses == null) {
            return metadata;
        }
        for (MetadataFilterClause clause : clauses) {
            if (clause.operator() == MetadataFilterClause.FilterOperator.EQ) {
                metadata.put(clause.field(), clause.value());
            }
        }
        return metadata;
    }
}
