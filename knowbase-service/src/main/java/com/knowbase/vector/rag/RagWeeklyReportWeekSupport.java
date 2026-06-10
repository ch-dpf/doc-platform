package com.knowbase.vector.rag;

import com.knowbase.ingest.domain.DocMetadata;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.dto.DocumentChunkRow;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.mapper.DocumentChunkMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 按文件名周次 + 分块内容统计「哪周周报含某项目」（规则作答，统一「今年」= 当前日历年）。 */
public final class RagWeeklyReportWeekSupport {

    private static final Pattern WEEK_IN_FILENAME =
            Pattern.compile("周报[（(]([^）)]+)[）)]");
    private static final Pattern YEAR_IN_PATH = Pattern.compile("(?:^|[/\\\\])(20\\d{2})(?:[/\\\\]|$)");
    private static final Pattern WEEK_RANGE = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})-(\\d{1,2})\\.(\\d{1,2})");

    private RagWeeklyReportWeekSupport() {}

    public static Optional<String> tryLibraryWideAnswer(
            String question,
            List<RagChatMessage> history,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore,
            DocumentChunkMapper chunkMapper) {
        if (!RagQuestionAnalyzer.isWeeklyReportWeekQuestion(question)) {
            return Optional.empty();
        }
        String person = resolvePerson(question, history);
        String projectToken = RagQuestionAnalyzer.extractWeekQueryProjectToken(question);
        int calendarYear = RagTemporalSupport.currentCalendarYear();

        Map<Integer, LinkedHashSet<String>> weeksByYear = new LinkedHashMap<>();
        for (DocMetadata doc : docMetadataStore.findActiveByLibrary(libraryId, tenantId)) {
            if (person != null && !fileNameMatchesPerson(doc.getFileName(), person)) {
                continue;
            }
            if (!documentMentionsProject(doc, chunkMapper, projectToken)) {
                continue;
            }
            Integer docYear = extractYearFromFileName(doc.getFileName());
            String weekLabel = formatWeekLabel(doc.getFileName());
            if (weekLabel == null) {
                continue;
            }
            weeksByYear.computeIfAbsent(docYear != null ? docYear : calendarYear, k -> new LinkedHashSet<>())
                    .add(weekLabel);
        }
        if (weeksByYear.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(formatAnswer(question, person, projectToken, calendarYear, weeksByYear));
    }

    static Integer extractYearFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        Matcher matcher = YEAR_IN_PATH.matcher(fileName.replace('\\', '/'));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    static String formatWeekLabel(String fileName) {
        if (fileName == null) {
            return null;
        }
        Matcher matcher = WEEK_IN_FILENAME.matcher(fileName);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1).strip();
        Matcher range = WEEK_RANGE.matcher(raw);
        if (!range.matches()) {
            return raw;
        }
        int startMonth = Integer.parseInt(range.group(1));
        int startDay = Integer.parseInt(range.group(2));
        int endMonth = Integer.parseInt(range.group(3));
        int endDay = Integer.parseInt(range.group(4));
        if (startMonth == endMonth) {
            return startMonth + "月" + startDay + "日至" + endDay + "日";
        }
        return startMonth + "月" + startDay + "日至" + endMonth + "月" + endDay + "日";
    }

    private static boolean documentMentionsProject(
            DocMetadata doc, DocumentChunkMapper chunkMapper, String projectToken) {
        List<DocumentChunkRow> chunks = chunkMapper.listByDocIdAndVersion(doc.getDocId(), doc.getVersion());
        for (DocumentChunkRow chunk : chunks) {
            String content = chunk.content();
            if (content != null && content.contains(projectToken)) {
                return true;
            }
        }
        return false;
    }

    private static String resolvePerson(String question, List<RagChatMessage> history) {
        String fromQuestion = RagQuestionAnalyzer.extractNamedEmployeeFromProjectQuestion(question);
        if (fromQuestion != null) {
            return fromQuestion;
        }
        String fromHistory = RagQuestionAnalyzer.findNamedEmployeeFromHistory(history);
        if (fromHistory != null) {
            return fromHistory;
        }
        Matcher matcher = Pattern.compile("([\\u4e00-\\u9fff]{2,4}).*哪.*周").matcher(question.strip());
        if (matcher.find() && RagEmployeeNameExtractor.looksLikePersonName(matcher.group(1))) {
            return matcher.group(1);
        }
        return null;
    }

    private static boolean fileNameMatchesPerson(String fileName, String person) {
        return fileName != null && person != null && fileName.contains(person);
    }

    private static String formatAnswer(
            String question,
            String person,
            String projectToken,
            int calendarYear,
            Map<Integer, LinkedHashSet<String>> weeksByYear) {
        StringBuilder sb = new StringBuilder();
        sb.append("当前日历年为 ").append(calendarYear).append(" 年，对话中的「今年」指 ")
                .append(calendarYear).append(" 年（非库内文件夹年份）。");

        LinkedHashSet<String> thisYearWeeks = weeksByYear.getOrDefault(calendarYear, new LinkedHashSet<>());
        String subject = person != null ? person + "在" : "库内";
        String projectLabel = projectToken.endsWith("项目") ? projectToken : projectToken + "项目";

        if (!thisYearWeeks.isEmpty()) {
            sb.append("\n\n").append(subject).append(calendarYear).append("年周报中，含「")
                    .append(projectLabel).append("」的周次有：")
                    .append(String.join("、", thisYearWeeks)).append("。[1]");
        } else {
            sb.append("\n\n").append(subject).append(calendarYear)
                    .append("年周报中未检到含「").append(projectLabel).append("」的周次。");
            List<String> priorYearNotes = new ArrayList<>();
            for (Map.Entry<Integer, LinkedHashSet<String>> entry : weeksByYear.entrySet()) {
                if (entry.getKey() == calendarYear || entry.getValue().isEmpty()) {
                    continue;
                }
                priorYearNotes.add(entry.getKey() + "年：" + String.join("、", entry.getValue()));
            }
            if (!priorYearNotes.isEmpty()) {
                sb.append(" 库内历史周报中相关周次为：").append(String.join("；", priorYearNotes)).append("。[1]");
            } else {
                sb.append("[1]");
            }
        }

        if (RagQuestionAnalyzer.containsCalendarYearClause(question)) {
            sb.append("\n\n").append(RagTemporalSupport.calendarYearSentence());
        }
        return sb.toString().strip();
    }
}
