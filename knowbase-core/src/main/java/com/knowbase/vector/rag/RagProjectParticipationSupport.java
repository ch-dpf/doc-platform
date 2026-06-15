package com.knowbase.vector.rag;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.dto.DocumentChunkRow;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.mapper.DocumentChunkMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 从周报分块统计指定员工参与的项目数量/名单（规则作答，避免 LLM 漏数）。 */
public final class RagProjectParticipationSupport {

    /** 序号 \\t 类别(项目名) \\t 工作内容 */
    private static final Pattern PROJECT_ROW = Pattern.compile("(?m)^\\d+\\t([^\\t\\n]+)\\t");

    private RagProjectParticipationSupport() {}

    public static Optional<String> tryLibraryWideAnswer(
            String question,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        if (!RagQuestionAnalyzer.isEmployeeProjectQuestion(question)) {
            return Optional.empty();
        }
        String person = extractTargetPerson(question);
        if (person == null || person.isBlank()) {
            return Optional.empty();
        }
        int calendarYear = RagTemporalSupport.currentCalendarYear();
        if (RagQuestionAnalyzer.scopesToCurrentCalendarYear(question)) {
            Map<Integer, LinkedHashSet<String>> byYear = collectProjectsByYearForPerson(
                    person, libraryId, tenantId, docMetadataStore, chunkMapper);
            if (byYear.isEmpty()) {
                return Optional.empty();
            }
            LinkedHashSet<String> thisYear = byYear.getOrDefault(calendarYear, new LinkedHashSet<>());
            return Optional.of(formatYearScopedAnswer(person, calendarYear, thisYear, byYear, question, List.of()));
        }
        LinkedHashSet<String> projects = collectProjectsForPerson(
                person, libraryId, tenantId, docMetadataStore, chunkMapper, null);
        if (projects.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(formatAnswer(person, projects, question, List.of()));
    }

    public static Optional<String> tryRecountFollowUp(
            String question,
            List<RagChatMessage> history,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        if (!RagQuestionAnalyzer.isProjectRecountQuestion(question)) {
            return Optional.empty();
        }
        String person = RagQuestionAnalyzer.findNamedEmployeeFromHistory(history);
        if (person == null || person.isBlank()) {
            return Optional.empty();
        }
        List<Set<String>> aliasGroups = RagQuestionAnalyzer.extractProjectAliasGroups(question);
        LinkedHashSet<String> projects = collectProjectsForPerson(
                person, libraryId, tenantId, docMetadataStore, chunkMapper, null);
        if (projects.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(formatAnswer(person, projects, question, aliasGroups));
    }

    public static Optional<String> tryRuleBasedAnswer(
            String question,
            List<SearchHit> hits,
            Map<UUID, String> fileNames) {
        if (!RagQuestionAnalyzer.isEmployeeProjectQuestion(question) || hits == null || hits.isEmpty()) {
            return Optional.empty();
        }
        String person = extractTargetPerson(question);
        if (person == null || person.isBlank()) {
            return Optional.empty();
        }
        LinkedHashSet<String> projects = new LinkedHashSet<>();
        for (SearchHit hit : hits) {
            if (!matchesPerson(hit, person, fileNames)) {
                continue;
            }
            projects.addAll(extractDistinctProjects(hit.content()));
        }
        if (projects.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(formatAnswer(person, projects, question, List.of()));
    }

    static String extractTargetPerson(String question) {
        return RagQuestionAnalyzer.extractNamedEmployeeFromProjectQuestion(question);
    }

    static LinkedHashSet<String> extractDistinctProjects(String content) {
        LinkedHashSet<String> projects = new LinkedHashSet<>();
        if (content == null || content.isBlank() || WeeklyReportWorkItemExtractor.isHeaderOnlyChunk(content)) {
            return projects;
        }
        Matcher matcher = PROJECT_ROW.matcher(content);
        while (matcher.find()) {
            String project = matcher.group(1).strip();
            if (isValidProjectName(project)) {
                projects.add(project);
            }
        }
        return projects;
    }

    private static boolean isValidProjectName(String project) {
        if (project.length() < 2 || project.length() > 40) {
            return false;
        }
        if (!project.contains("项目") && !project.matches("(?i)fb项目|q4指标")) {
            return false;
        }
        return project.chars().anyMatch(ch -> ch >= 0x4e00 && ch <= 0x9fff)
                || project.toLowerCase().contains("fb");
    }

    private static boolean fileNameMatchesPerson(String fileName, String person) {
        return fileName != null && person != null && fileName.contains(person);
    }

    private static boolean matchesPerson(SearchHit hit, String person, Map<UUID, String> fileNames) {
        if (hit.content() != null && hit.content().contains(person)) {
            return true;
        }
        if (fileNames != null && hit.docId() != null) {
            String fileName = fileNames.get(hit.docId());
            return fileNameMatchesPerson(fileName, person);
        }
        return false;
    }

    private static LinkedHashSet<String> collectProjectsForPerson(
            String person,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper,
            Integer yearFilter) {
        LinkedHashSet<String> projects = new LinkedHashSet<>();
        for (DocMetadata doc : docMetadataStore.findActiveByLibrary(libraryId, tenantId)) {
            if (!fileNameMatchesPerson(doc.getFileName(), person)) {
                continue;
            }
            if (yearFilter != null && !yearFilter.equals(resolveDocumentYear(doc.getFileName()))) {
                continue;
            }
            List<DocumentChunkRow> chunks = chunkMapper.listByDocIdAndVersion(doc.getDocId(), doc.getVersion());
            for (DocumentChunkRow chunk : chunks) {
                projects.addAll(extractDistinctProjects(chunk.content()));
            }
        }
        return projects;
    }

    private static Map<Integer, LinkedHashSet<String>> collectProjectsByYearForPerson(
            String person,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        Map<Integer, LinkedHashSet<String>> byYear = new LinkedHashMap<>();
        for (DocMetadata doc : docMetadataStore.findActiveByLibrary(libraryId, tenantId)) {
            if (!fileNameMatchesPerson(doc.getFileName(), person)) {
                continue;
            }
            int docYear = resolveDocumentYear(doc.getFileName());
            List<DocumentChunkRow> chunks = chunkMapper.listByDocIdAndVersion(doc.getDocId(), doc.getVersion());
            LinkedHashSet<String> projects = byYear.computeIfAbsent(docYear, k -> new LinkedHashSet<>());
            for (DocumentChunkRow chunk : chunks) {
                projects.addAll(extractDistinctProjects(chunk.content()));
            }
        }
        return byYear;
    }

    private static int resolveDocumentYear(String fileName) {
        Integer year = RagWeeklyReportWeekSupport.extractYearFromFileName(fileName);
        return year != null ? year : RagTemporalSupport.currentCalendarYear();
    }

    private static String formatYearScopedAnswer(
            String person,
            int calendarYear,
            LinkedHashSet<String> rawThisYear,
            Map<Integer, LinkedHashSet<String>> byYear,
            String question,
            List<Set<String>> aliasGroups) {
        LinkedHashSet<String> thisYear = ProjectParticipationCanonicalizer.dedupe(rawThisYear, aliasGroups);
        StringBuilder sb = new StringBuilder();
        sb.append("当前日历年为 ").append(calendarYear).append(" 年，对话中的「今年」指 ")
                .append(calendarYear).append(" 年（非库内文件夹年份）。");

        boolean countStyle = RagQuestionAnalyzer.isEmployeeProjectCountQuestion(question)
                || RagQuestionAnalyzer.isProjectRecountQuestion(question);
        if (!thisYear.isEmpty()) {
            String joined = thisYear.stream().collect(Collectors.joining("、"));
            String note = aliasGroups != null && !aliasGroups.isEmpty()
                    ? "（已按您说明合并同一项目）"
                    : "（已合并同义项目名）";
            if (countStyle) {
                sb.append("\n\n根据参考资料（").append(calendarYear).append(" 年周报「类别/项目」列），")
                        .append(person).append("共参与 ").append(thisYear.size()).append(" 个项目")
                        .append(note).append("：").append(joined).append("。[1]");
            } else {
                sb.append("\n\n根据参考资料（").append(calendarYear).append(" 年周报「类别/项目」列），")
                        .append(person).append("参与的项目包括：").append(joined).append("。[1]");
            }
        } else {
            sb.append("\n\n").append(person).append("在 ").append(calendarYear)
                    .append(" 年周报中未检到参与项目记录。");
            List<String> priorYearNotes = new ArrayList<>();
            for (Map.Entry<Integer, LinkedHashSet<String>> entry : byYear.entrySet()) {
                if (entry.getKey() == calendarYear || entry.getValue().isEmpty()) {
                    continue;
                }
                LinkedHashSet<String> deduped = ProjectParticipationCanonicalizer.dedupe(entry.getValue(), aliasGroups);
                if (!deduped.isEmpty()) {
                    priorYearNotes.add(entry.getKey() + " 年：" + deduped.stream().collect(Collectors.joining("、")));
                }
            }
            if (!priorYearNotes.isEmpty()) {
                sb.append(" 库内历史周报中参与的项目包括：").append(String.join("；", priorYearNotes)).append("。[1]");
            } else {
                sb.append("[1]");
            }
        }
        if (RagQuestionAnalyzer.containsCalendarYearClause(question)) {
            sb.append("\n\n").append(RagTemporalSupport.calendarYearSentence());
        }
        return sb.toString().strip();
    }

    private static String formatAnswer(
            String person,
            LinkedHashSet<String> rawProjects,
            String question,
            List<Set<String>> aliasGroups) {
        LinkedHashSet<String> projects = ProjectParticipationCanonicalizer.dedupe(rawProjects, aliasGroups);
        String joined = projects.stream().collect(Collectors.joining("、"));
        boolean countStyle = RagQuestionAnalyzer.isEmployeeProjectCountQuestion(question)
                || RagQuestionAnalyzer.isProjectRecountQuestion(question);
        if (countStyle) {
            String note = aliasGroups != null && !aliasGroups.isEmpty()
                    ? "（已按您说明合并同一项目）"
                    : "（已合并同义项目名）";
            return "根据参考资料（周报「类别/项目」列），" + person + "共参与 "
                    + projects.size() + " 个项目" + note + "：" + joined + "。[1]";
        }
        return "根据参考资料（周报「类别/项目」列），" + person + "参与的项目包括：" + joined + "。[1]";
    }
}
