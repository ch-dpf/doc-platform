package com.knowbase.vector.retrieval;

import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.rag.RagWeeklyReportWeekSupport;
import com.knowbase.vector.rag.TemporalQueryScope;
import com.knowbase.vector.rag.WeeklyReportWorkItemExtractor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 判断检索分块是否落在问句指定的时间范围内（metadata 优先，正文/文件名兜底）。 */
public final class TemporalHitMatcher {

    private static final Pattern PERIOD_CN = Pattern.compile(
            "(\\d{4})年(\\d{1,2})月(\\d{1,2})日\\s*[-—~]+\\s*(?:(\\d{1,2})月)?(\\d{1,2})日");
    private static final Pattern WEEK_RANGE = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})-(\\d{1,2})\\.(\\d{1,2})");

    private TemporalHitMatcher() {}

    public static boolean matches(
            SearchHit hit, TemporalQueryScope scope, String fileName, Map<String, String> chunkMetadata) {
        if (scope == null || !scope.scoped()) {
            return true;
        }
        if (scope.completedWorkOnly() && !passesCompletedWorkFilter(hit, chunkMetadata)) {
            return false;
        }
        if (!scope.persons().isEmpty()) {
            boolean anyPerson = scope.persons().stream()
                    .anyMatch(person -> personMatches(person, hit, fileName, chunkMetadata));
            if (!anyPerson) {
                return false;
            }
        }
        LocalDate queryStart = scope.rangeStart().orElse(null);
        LocalDate queryEnd = scope.rangeEnd().orElse(null);
        if (queryStart == null || queryEnd == null) {
            return true;
        }
        LocalDate chunkStart = null;
        LocalDate chunkEnd = null;
        if (chunkMetadata != null) {
            chunkStart = parseDate(chunkMetadata.get(TemporalMetadataFields.PERIOD_START));
            chunkEnd = parseDate(chunkMetadata.get(TemporalMetadataFields.PERIOD_END));
            if (chunkStart == null
                    && chunkMetadata.get(TemporalMetadataFields.PERIOD_MONTHS) != null
                    && !scope.preciseDateScoped()) {
                return monthListOverlaps(chunkMetadata.get(TemporalMetadataFields.PERIOD_MONTHS), scope);
            }
        }
        if (chunkStart == null || chunkEnd == null) {
            LocalDate[] fallback = extractFromContentOrFile(hit.content(), fileName);
            if (fallback != null) {
                chunkStart = fallback[0];
                chunkEnd = fallback[1];
            }
        }
        if (chunkStart == null || chunkEnd == null) {
            if (scope.preciseDateScoped()) {
                return false;
            }
            return monthFromFileName(fileName, scope);
        }
        if (!dateRangesOverlap(queryStart, queryEnd, chunkStart, chunkEnd)) {
            return false;
        }
        if (scope.dayScoped() && scope.completedWorkOnly()) {
            return WeeklyReportWorkItemExtractor.hasCompletedWorkOnDay(hit.content(), scope);
        }
        return true;
    }

    private static boolean dateRangesOverlap(
            LocalDate queryStart, LocalDate queryEnd, LocalDate chunkStart, LocalDate chunkEnd) {
        return !chunkEnd.isBefore(queryStart) && !chunkStart.isAfter(queryEnd);
    }

    private static boolean passesCompletedWorkFilter(SearchHit hit, Map<String, String> chunkMetadata) {
        if (chunkMetadata != null) {
            String completed = chunkMetadata.get(TemporalMetadataFields.HAS_COMPLETED_WORK);
            if ("false".equals(completed)) {
                return false;
            }
            String section = chunkMetadata.get(TemporalMetadataFields.SECTION_LABEL);
            if ("周工作计划".equals(section)) {
                return false;
            }
        }
        String content = hit != null ? hit.content() : null;
        if (content == null || content.isBlank()) {
            return true;
        }
        if (content.contains("·周工作计划") && !content.contains("\t已完成")) {
            return false;
        }
        return !content.contains("\t待开展") || content.contains("\t已完成");
    }

    private static boolean personMatches(
            String person, SearchHit hit, String fileName, Map<String, String> chunkMetadata) {
        if (fileName != null && fileName.contains(person)) {
            return true;
        }
        if (chunkMetadata != null && person.equals(chunkMetadata.get(TemporalMetadataFields.SUBMITTER))) {
            return true;
        }
        String content = hit.content();
        return content != null && (content.contains("【" + person + "·") || content.contains("\t" + person + "\t"));
    }

    private static boolean monthListOverlaps(String periodMonths, TemporalQueryScope scope) {
        if (periodMonths == null || scope.monthsInRange().isEmpty()) {
            return false;
        }
        for (Integer month : scope.monthsInRange()) {
            if (monthListContains(periodMonths, month)) {
                return true;
            }
        }
        return false;
    }

    private static boolean monthListContains(String periodMonths, Integer month) {
        if (periodMonths == null || month == null) {
            return false;
        }
        String token = String.valueOf(month);
        for (String part : periodMonths.split(",")) {
            if (token.equals(part.strip())) {
                return true;
            }
        }
        return false;
    }

    private static boolean monthFromFileName(String fileName, TemporalQueryScope scope) {
        if (fileName == null || scope.year() == null) {
            return false;
        }
        Integer fileYear = RagWeeklyReportWeekSupport.extractYearFromFileName(fileName);
        if (fileYear != null && !fileYear.equals(scope.year())) {
            return false;
        }
        if (scope.yearScoped()) {
            return fileName.contains(scope.year() + "/") || fileName.contains(scope.year() + "年");
        }
        if (scope.month() == null) {
            return false;
        }
        Matcher matcher = WEEK_RANGE.matcher(fileName);
        if (!matcher.find()) {
            for (Integer month : scope.monthsInRange()) {
                if (fileName.contains(scope.year() + "年") && fileName.contains(month + "月")) {
                    return true;
                }
            }
            return false;
        }
        int startMonth = Integer.parseInt(matcher.group(1));
        int endMonth = Integer.parseInt(matcher.group(3));
        int fileStart = Math.min(startMonth, endMonth);
        int fileEnd = Math.max(startMonth, endMonth);
        for (Integer month : scope.monthsInRange()) {
            if (month >= fileStart && month <= fileEnd) {
                return true;
            }
        }
        return false;
    }

    private static LocalDate[] extractFromContentOrFile(String content, String fileName) {
        if (content != null) {
            Matcher matcher = PERIOD_CN.matcher(content);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(1));
                int startMonth = Integer.parseInt(matcher.group(2));
                int startDay = Integer.parseInt(matcher.group(3));
                String endMonthRaw = matcher.group(4);
                int endMonth = endMonthRaw != null && !endMonthRaw.isBlank()
                        ? Integer.parseInt(endMonthRaw)
                        : startMonth;
                int endDay = Integer.parseInt(matcher.group(5));
                return new LocalDate[] {
                    LocalDate.of(year, startMonth, startDay),
                    LocalDate.of(year, endMonth, endDay)
                };
            }
        }
        if (fileName != null) {
            Matcher week = Pattern.compile("周报[（(]([^）)]+)[）)]").matcher(fileName);
            if (week.find()) {
                Matcher range = WEEK_RANGE.matcher(week.group(1));
                if (range.matches()) {
                    int year = RagWeeklyReportWeekSupport.extractYearFromFileName(fileName) != null
                            ? RagWeeklyReportWeekSupport.extractYearFromFileName(fileName)
                            : LocalDate.now().getYear();
                    return new LocalDate[] {
                        LocalDate.of(year, Integer.parseInt(range.group(1)), Integer.parseInt(range.group(2))),
                        LocalDate.of(year, Integer.parseInt(range.group(3)), Integer.parseInt(range.group(4)))
                    };
                }
            }
        }
        return null;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.strip());
        } catch (Exception ignored) {
            return null;
        }
    }
}
