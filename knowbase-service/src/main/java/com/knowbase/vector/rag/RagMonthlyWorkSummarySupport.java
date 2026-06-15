package com.knowbase.vector.rag;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.dto.DocumentChunkRow;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import com.knowbase.vector.retrieval.TemporalHitMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 按月汇总某人已完成工作（库级规则扫描 + 检索结果规则归纳）。 */
public final class RagMonthlyWorkSummarySupport {

    private static final int MAX_ITEMS = 30;
    private static final int MAX_ITEMS_YEAR = 100;
    private static final int LIST_ALL_THRESHOLD = 100;
    private static final String OTHER_PROJECT = "其他";

    private RagMonthlyWorkSummarySupport() {}

    public static boolean isMonthlyCompletedWorkQuestion(String question) {
        if (!RagQuestionAnalyzer.isTemporalCompletedWorkQuestion(question)) {
            return false;
        }
        TemporalQueryScope scope = RagTemporalQueryParser.parse(question, List.of());
        return scope.scoped() && scope.completedWorkOnly();
    }

    public static Optional<String> tryLibraryWideAnswer(
            String question,
            List<RagChatMessage> history,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        if (!isMonthlyCompletedWorkQuestion(question)) {
            return Optional.empty();
        }
        TemporalQueryScope scope = RagTemporalQueryParser.parse(question, history);
        if (!scope.scoped()) {
            return Optional.empty();
        }
        List<WeeklyReportWorkItemExtractor.WorkItem> items = new ArrayList<>();
        int ref = 1;
        for (DocMetadata doc : docMetadataStore.findActiveByLibrary(libraryId, tenantId)) {
            if (!scope.persons().isEmpty()) {
                boolean any = scope.persons().stream()
                        .anyMatch(p -> doc.getFileName() != null && doc.getFileName().contains(p));
                if (!any) {
                    continue;
                }
            }
            List<DocumentChunkRow> chunks = chunkMapper.listByDocIdAndVersion(doc.getDocId(), doc.getVersion());
            for (DocumentChunkRow chunk : chunks) {
                SearchHit pseudo = new SearchHit(
                        UUID.randomUUID(),
                        doc.getDocId(),
                        tenantId,
                        doc.getVersion(),
                        chunk.chunkIndex(),
                        chunk.content(),
                        1.0);
                if (!TemporalHitMatcher.matches(pseudo, scope, doc.getFileName(), null)) {
                    continue;
                }
                List<WeeklyReportWorkItemExtractor.WorkItem> extracted =
                        WeeklyReportWorkItemExtractor.extractCompleted(
                                List.of(pseudo), Map.of(doc.getDocId(), doc.getFileName()), scope);
                for (WeeklyReportWorkItemExtractor.WorkItem item : extracted) {
                    items.add(new WeeklyReportWorkItemExtractor.WorkItem(
                            item.project(), item.content(), ref++, item.docId()));
                }
            }
        }
        if (items.isEmpty()) {
            return Optional.empty();
        }
        LinkedHashSet<String> submitters = new LinkedHashSet<>();
        if (scope.person() != null) {
            submitters.add(scope.person());
        }
        return Optional.of(formatMonthlyAnswer(scope, items, submitters));
    }

    public static Optional<String> tryRuleBasedAnswer(
            String question,
            List<SearchHit> hits,
            Map<UUID, String> fileNames,
            List<RagChatMessage> history) {
        if (!isMonthlyCompletedWorkQuestion(question) || hits == null || hits.isEmpty()) {
            return Optional.empty();
        }
        TemporalQueryScope scope = RagTemporalQueryParser.parse(question, history);
        List<SearchHit> scopedHits = hits.stream()
                .filter(hit -> TemporalHitMatcher.matches(
                        hit, scope, fileNames.get(hit.docId()), null))
                .toList();
        if (scopedHits.isEmpty()) {
            return Optional.empty();
        }
        List<WeeklyReportWorkItemExtractor.WorkItem> items =
                WeeklyReportWorkItemExtractor.extractCompleted(scopedHits, fileNames, scope);
        if (items.isEmpty()) {
            return Optional.empty();
        }
        LinkedHashSet<String> submitters = WeeklyReportWorkItemExtractor.extractSubmitterNames(scopedHits, fileNames);
        if (scope.person() != null) {
            submitters.add(scope.person());
        }
        return Optional.of(formatMonthlyAnswer(scope, items, submitters));
    }

    private static String formatMonthlyAnswer(
            TemporalQueryScope scope,
            List<WeeklyReportWorkItemExtractor.WorkItem> items,
            LinkedHashSet<String> submitters) {
        String subject = scope.person() != null
                ? scope.person()
                : (submitters.isEmpty() ? "相关人员" : String.join("、", submitters));
        StringBuilder sb = new StringBuilder();
        sb.append("根据参考资料，")
                .append(subject)
                .append("在")
                .append(formatPeriodLabel(scope))
                .append("已完成的主要工作如下（按项目分组）：\n\n");

        int maxItems = resolveMaxItems(scope, items.size());
        List<ProjectGroup> groups = groupByProject(items);
        int listed = 0;
        for (ProjectGroup group : groups) {
            if (listed >= maxItems) {
                break;
            }
            sb.append('【').append(group.project()).append('】')
                    .append('（').append(group.items().size()).append(" 项）\n");
            int itemNum = 1;
            for (WeeklyReportWorkItemExtractor.WorkItem item : group.items()) {
                if (listed >= maxItems) {
                    break;
                }
                sb.append("  ").append(itemNum++).append(". ")
                        .append(item.content().strip())
                        .append(" [").append(item.refIndex()).append("]\n");
                listed++;
            }
            sb.append('\n');
        }
        if (items.size() > maxItems) {
            sb.append("（另有 ").append(items.size() - maxItems)
                    .append(" 条未列出，共 ").append(items.size()).append(" 项）");
        } else {
            sb.append("共计 ").append(items.size()).append(" 项工作，涉及 ")
                    .append(groups.size()).append(" 个项目。");
        }
        return sb.toString().strip();
    }

    private static int resolveMaxItems(TemporalQueryScope scope, int totalItems) {
        if (totalItems <= LIST_ALL_THRESHOLD) {
            return totalItems;
        }
        return scope.yearScoped() ? MAX_ITEMS_YEAR : MAX_ITEMS;
    }

    private static List<ProjectGroup> groupByProject(List<WeeklyReportWorkItemExtractor.WorkItem> items) {
        Map<String, List<WeeklyReportWorkItemExtractor.WorkItem>> grouped = new LinkedHashMap<>();
        Map<String, String> canonicalToDisplay = new LinkedHashMap<>();
        for (WeeklyReportWorkItemExtractor.WorkItem item : items) {
            String groupKey = resolveProjectGroupKey(item.project());
            canonicalToDisplay.putIfAbsent(groupKey, resolveProjectLabel(item.project()));
            grouped.computeIfAbsent(groupKey, key -> new ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream()
                .sorted((left, right) -> {
                    int byCount = Integer.compare(right.getValue().size(), left.getValue().size());
                    if (byCount != 0) {
                        return byCount;
                    }
                    String leftLabel = canonicalToDisplay.get(left.getKey());
                    String rightLabel = canonicalToDisplay.get(right.getKey());
                    return leftLabel.compareTo(rightLabel);
                })
                .map(entry -> new ProjectGroup(
                        canonicalToDisplay.get(entry.getKey()),
                        List.copyOf(entry.getValue())))
                .toList();
    }

    private static String resolveProjectGroupKey(String project) {
        if (project == null || project.isBlank()) {
            return OTHER_PROJECT;
        }
        String key = ProjectParticipationCanonicalizer.groupingKey(project);
        return key.isBlank() ? OTHER_PROJECT : key;
    }

    private static String resolveProjectLabel(String project) {
        if (project == null || project.isBlank()) {
            return OTHER_PROJECT;
        }
        String label = ProjectParticipationCanonicalizer.groupedDisplayLabel(project);
        return label.isBlank() ? OTHER_PROJECT : label;
    }

    private record ProjectGroup(String project, List<WeeklyReportWorkItemExtractor.WorkItem> items) {}

    private static String formatPeriodLabel(TemporalQueryScope scope) {
        if (scope.yearScoped()) {
            return scope.year() + "年";
        }
        StringBuilder label = new StringBuilder();
        label.append(scope.year()).append('年').append(scope.month()).append('月');
        if (scope.dayScoped()) {
            label.append(scope.dayOfMonth()).append('日');
        } else if (scope.weekScoped()) {
            label.append("第").append(scope.weekOfMonth()).append("周");
        }
        return label.toString();
    }
}
