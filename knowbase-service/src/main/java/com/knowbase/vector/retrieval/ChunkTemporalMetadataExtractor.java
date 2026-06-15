package com.knowbase.vector.retrieval;

import com.knowbase.ingest.domain.DocMetadata;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从周报分块正文与文件名提取时间元数据，供入库 metadata 与检索过滤。
 */
public final class ChunkTemporalMetadataExtractor {

    private static final Pattern PREFIX = Pattern.compile("【([^·]+)·([^·]+)·([^】]+)】");
    private static final Pattern PERIOD_CN = Pattern.compile(
            "(\\d{4})年(\\d{1,2})月(\\d{1,2})日\\s*[-—~]+\\s*(?:(\\d{1,2})月)?(\\d{1,2})日");
    private static final Pattern WEEK_IN_FILENAME = Pattern.compile("周报[（(]([^）)]+)[）)]");
    private static final Pattern YEAR_IN_PATH = Pattern.compile("(?:^|[/\\\\])(20\\d{2})(?:[/\\\\]|$)");
    private static final Pattern WEEK_RANGE = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})-(\\d{1,2})\\.(\\d{1,2})");
    private static final Pattern PERSON_IN_FILENAME = Pattern.compile("([\\u4e00-\\u9fff]{2,4})[-_]?周报");

    private ChunkTemporalMetadataExtractor() {}

    public record TemporalMetadata(
            String periodYear,
            String periodStart,
            String periodEnd,
            String periodMonths,
            String submitter,
            String sectionLabel,
            String hasCompletedWork) {}

    public static TemporalMetadata extract(DocMetadata doc, String chunkContent) {
        if (chunkContent == null || chunkContent.isBlank()) {
            return empty();
        }
        String fileName = doc != null ? doc.getFileName() : null;
        String submitter = extractSubmitter(chunkContent, fileName);
        String sectionLabel = extractSectionLabel(chunkContent);
        LocalDate start = null;
        LocalDate end = null;

        Matcher prefixPeriod = PERIOD_CN.matcher(chunkContent);
        if (prefixPeriod.find()) {
            int year = Integer.parseInt(prefixPeriod.group(1));
            int startMonth = Integer.parseInt(prefixPeriod.group(2));
            int startDay = Integer.parseInt(prefixPeriod.group(3));
            String endMonthRaw = prefixPeriod.group(4);
            int endMonth = endMonthRaw != null && !endMonthRaw.isBlank()
                    ? Integer.parseInt(endMonthRaw)
                    : startMonth;
            int endDay = Integer.parseInt(prefixPeriod.group(5));
            start = toDate(year, startMonth, startDay);
            end = toDate(year, endMonth, endDay);
        }
        if (start == null || end == null) {
            LocalDate[] fromFile = extractRangeFromFileName(fileName, doc);
            if (fromFile != null) {
                start = fromFile[0];
                end = fromFile[1];
            }
        }
        if (start == null || end == null) {
            return new TemporalMetadata(
                    yearFromPath(fileName),
                    null,
                    null,
                    null,
                    submitter,
                    sectionLabel,
                    completedFlag(chunkContent, sectionLabel));
        }
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        return new TemporalMetadata(
                String.valueOf(start.getYear()),
                start.toString(),
                end.toString(),
                formatMonths(start, end),
                submitter,
                sectionLabel,
                completedFlag(chunkContent, sectionLabel));
    }

    private static TemporalMetadata empty() {
        return new TemporalMetadata(null, null, null, null, null, null, null);
    }

    private static String extractSubmitter(String content, String fileName) {
        Matcher prefix = PREFIX.matcher(content);
        if (prefix.find()) {
            String person = prefix.group(1).strip();
            if (looksLikePerson(person)) {
                return person;
            }
        }
        if (fileName != null) {
            Matcher matcher = PERSON_IN_FILENAME.matcher(fileName);
            if (matcher.find() && looksLikePerson(matcher.group(1))) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static String extractSectionLabel(String content) {
        Matcher prefix = PREFIX.matcher(content);
        if (prefix.find()) {
            return prefix.group(2).strip();
        }
        if (content.contains("周工作计划")) {
            return "周工作计划";
        }
        if (content.contains("工作周报")) {
            return "工作周报";
        }
        return null;
    }

    private static String completedFlag(String content, String sectionLabel) {
        if ("周工作计划".equals(sectionLabel)) {
            return "false";
        }
        return content.contains("\t已完成") ? "true" : "false";
    }

    private static LocalDate[] extractRangeFromFileName(String fileName, DocMetadata doc) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        Matcher week = WEEK_IN_FILENAME.matcher(fileName);
        if (!week.find()) {
            return null;
        }
        Matcher range = WEEK_RANGE.matcher(week.group(1).strip());
        if (!range.matches()) {
            return null;
        }
        int year = resolveYear(fileName, doc);
        LocalDate start = toDate(year, Integer.parseInt(range.group(1)), Integer.parseInt(range.group(2)));
        LocalDate end = toDate(year, Integer.parseInt(range.group(3)), Integer.parseInt(range.group(4)));
        return new LocalDate[] {start, end};
    }

    private static int resolveYear(String fileName, DocMetadata doc) {
        String fromPath = yearFromPath(fileName);
        if (fromPath != null) {
            return Integer.parseInt(fromPath);
        }
        return LocalDate.now().getYear();
    }

    private static String yearFromPath(String fileName) {
        if (fileName == null) {
            return null;
        }
        Matcher matcher = YEAR_IN_PATH.matcher(fileName.replace('\\', '/'));
        return matcher.find() ? matcher.group(1) : null;
    }

    private static LocalDate toDate(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    private static String formatMonths(LocalDate start, LocalDate end) {
        Set<String> months = new LinkedHashSet<>();
        YearMonth cursor = YearMonth.from(start);
        YearMonth last = YearMonth.from(end);
        while (!cursor.isAfter(last)) {
            months.add(String.valueOf(cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }
        return String.join(",", months);
    }

    private static boolean looksLikePerson(String token) {
        return token != null
                && token.length() >= 2
                && token.length() <= 4
                && token.chars().allMatch(ch -> ch >= 0x4e00 && ch <= 0x9fff);
    }
}
