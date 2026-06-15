package com.knowbase.vector.rag;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从问句解析绝对/相对时间范围。 */
final class RagTemporalTimeParser {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");

    private static final Pattern YEAR_MONTH_DAY_CN =
            Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月\\s*第?\\s*([一二三四五六七八九十两]|\\d{1,2})\\s*天");
    private static final Pattern YEAR_MONTH_DAY_NUM =
            Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月\\s*(\\d{1,2})\\s*[日号]");
    private static final Pattern YEAR_MONTH_WEEK =
            Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月\\s*第?\\s*([一二三四五1-5])\\s*周");
    private static final Pattern CURRENT_YEAR_MONTH_WEEK =
            Pattern.compile("(?:今年|本年)\\s*(\\d{1,2})\\s*月\\s*第?\\s*([一二三四五1-5])\\s*周");
    private static final Pattern YEAR_ORDINAL_MONTH =
            Pattern.compile("(20\\d{2})\\s*年\\s*第?\\s*([一二三四五六七八九十两]{1,2}|\\d{1,2})\\s*个?\\s*月");
    private static final Pattern CURRENT_YEAR_ORDINAL_MONTH =
            Pattern.compile("(?:今年|本年)\\s*第?\\s*([一二三四五六七八九十两]{1,2}|\\d{1,2})\\s*个?\\s*月");
    private static final Pattern YEAR_CN_MONTH =
            Pattern.compile("(20\\d{2})\\s*年\\s*([一二三四五六七八九十两]{1,2})\\s*月");
    private static final Pattern YEAR_MONTH =
            Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*月");
    private static final Pattern CURRENT_YEAR_MONTH =
            Pattern.compile("(?:今年|本年)\\s*(\\d{1,2})\\s*月");
    private static final Pattern LAST_YEAR_MONTH =
            Pattern.compile("去年\\s*(\\d{1,2})\\s*月");
    private static final Pattern LAST_MONTH = Pattern.compile("上个?月");
    private static final Pattern THIS_MONTH = Pattern.compile("(?:本月|这个月)");
    private static final Pattern MONTH_RANGE =
            Pattern.compile("(20\\d{2})\\s*年\\s*(\\d{1,2})\\s*(?:月)?\\s*[-到至]\\s*(\\d{1,2})\\s*月");
    private static final Pattern MONTH_RANGE_NO_YEAR =
            Pattern.compile("(\\d{1,2})\\s*(?:月)?\\s*[-到至]\\s*(\\d{1,2})\\s*月");
    private static final Pattern QUARTER_CN =
            Pattern.compile("(20\\d{2})\\s*年?\\s*第?\\s*([一二三四1234])\\s*季度");
    private static final Pattern QUARTER_Q =
            Pattern.compile("(20\\d{2})\\s*年?\\s*[Qq]([1-4])");
    private static final Pattern YEAR_ONLY =
            Pattern.compile("(20\\d{2})\\s*年(?![\\s]*(?:\\d{1,2}\\s*月"
                    + "|[一二三四五六七八九十两]{1,2}\\s*月|第\\s*[一二三四五六七八九十两\\d]|周))");
    private static final Pattern CURRENT_YEAR_ONLY =
            Pattern.compile("(?:今年|本年)(?![\\s]*(?:\\d{1,2}\\s*月|第\\s*[一二三四五六七八九十两\\d]))");

    private RagTemporalTimeParser() {}

    static ParsedTime parse(String question) {
        if (question == null || question.isBlank()) {
            return ParsedTime.none();
        }
        String text = question.strip();

        Matcher yearMonthDayCn = YEAR_MONTH_DAY_CN.matcher(text);
        if (yearMonthDayCn.find()) {
            return ParsedTime.day(
                    Integer.parseInt(yearMonthDayCn.group(1)),
                    Integer.parseInt(yearMonthDayCn.group(2)),
                    parseDayToken(yearMonthDayCn.group(3)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher yearMonthDayNum = YEAR_MONTH_DAY_NUM.matcher(text);
        if (yearMonthDayNum.find()) {
            return ParsedTime.day(
                    Integer.parseInt(yearMonthDayNum.group(1)),
                    Integer.parseInt(yearMonthDayNum.group(2)),
                    Integer.parseInt(yearMonthDayNum.group(3)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher yearMonthWeek = YEAR_MONTH_WEEK.matcher(text);
        if (yearMonthWeek.find()) {
            return ParsedTime.week(
                    Integer.parseInt(yearMonthWeek.group(1)),
                    Integer.parseInt(yearMonthWeek.group(2)),
                    parseWeekToken(yearMonthWeek.group(3)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher currentYearMonthWeek = CURRENT_YEAR_MONTH_WEEK.matcher(text);
        if (currentYearMonthWeek.find()) {
            return ParsedTime.week(
                    RagTemporalSupport.currentCalendarYear(),
                    Integer.parseInt(currentYearMonthWeek.group(1)),
                    parseWeekToken(currentYearMonthWeek.group(2)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher range = MONTH_RANGE.matcher(text);
        if (range.find()) {
            return ParsedTime.range(
                    Integer.parseInt(range.group(1)),
                    Integer.parseInt(range.group(2)),
                    Integer.parseInt(range.group(3)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher quarterCn = QUARTER_CN.matcher(text);
        if (quarterCn.find()) {
            int year = Integer.parseInt(quarterCn.group(1));
            int[] bounds = quarterBounds(quarterCn.group(2));
            return ParsedTime.range(year, bounds[0], bounds[1], TemporalParseConfidence.MEDIUM);
        }
        Matcher quarterQ = QUARTER_Q.matcher(text);
        if (quarterQ.find()) {
            int year = Integer.parseInt(quarterQ.group(1));
            int[] bounds = quarterBounds(quarterQ.group(2));
            return ParsedTime.range(year, bounds[0], bounds[1], TemporalParseConfidence.MEDIUM);
        }

        Matcher yearOrdinalMonth = YEAR_ORDINAL_MONTH.matcher(text);
        if (yearOrdinalMonth.find()) {
            return ParsedTime.single(
                    Integer.parseInt(yearOrdinalMonth.group(1)),
                    parseMonthToken(yearOrdinalMonth.group(2)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher currentYearOrdinalMonth = CURRENT_YEAR_ORDINAL_MONTH.matcher(text);
        if (currentYearOrdinalMonth.find()) {
            return ParsedTime.single(
                    RagTemporalSupport.currentCalendarYear(),
                    parseMonthToken(currentYearOrdinalMonth.group(1)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher yearCnMonth = YEAR_CN_MONTH.matcher(text);
        if (yearCnMonth.find()) {
            return ParsedTime.single(
                    Integer.parseInt(yearCnMonth.group(1)),
                    parseMonthToken(yearCnMonth.group(2)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher yearMonth = YEAR_MONTH.matcher(text);
        if (yearMonth.find()) {
            return ParsedTime.single(
                    Integer.parseInt(yearMonth.group(1)),
                    Integer.parseInt(yearMonth.group(2)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher currentYearMonth = CURRENT_YEAR_MONTH.matcher(text);
        if (currentYearMonth.find()) {
            return ParsedTime.single(
                    RagTemporalSupport.currentCalendarYear(),
                    Integer.parseInt(currentYearMonth.group(1)),
                    TemporalParseConfidence.HIGH);
        }

        Matcher lastYearMonth = LAST_YEAR_MONTH.matcher(text);
        if (lastYearMonth.find()) {
            return ParsedTime.single(
                    RagTemporalSupport.currentCalendarYear() - 1,
                    Integer.parseInt(lastYearMonth.group(1)),
                    TemporalParseConfidence.MEDIUM);
        }

        if (LAST_MONTH.matcher(text).find()) {
            YearMonth ym = YearMonth.now(DEFAULT_ZONE).minusMonths(1);
            return ParsedTime.single(ym.getYear(), ym.getMonthValue(), TemporalParseConfidence.MEDIUM);
        }

        if (THIS_MONTH.matcher(text).find()) {
            YearMonth ym = YearMonth.now(DEFAULT_ZONE);
            return ParsedTime.single(ym.getYear(), ym.getMonthValue(), TemporalParseConfidence.MEDIUM);
        }

        Matcher rangeNoYear = MONTH_RANGE_NO_YEAR.matcher(text);
        if (rangeNoYear.find()) {
            int year = RagTemporalSupport.currentCalendarYear();
            return ParsedTime.range(
                    year,
                    Integer.parseInt(rangeNoYear.group(1)),
                    Integer.parseInt(rangeNoYear.group(2)),
                    TemporalParseConfidence.MEDIUM);
        }

        Matcher yearOnly = YEAR_ONLY.matcher(text);
        if (yearOnly.find()) {
            return ParsedTime.yearOnly(
                    Integer.parseInt(yearOnly.group(1)), TemporalParseConfidence.HIGH);
        }

        Matcher currentYearOnly = CURRENT_YEAR_ONLY.matcher(text);
        if (currentYearOnly.find()) {
            return ParsedTime.yearOnly(
                    RagTemporalSupport.currentCalendarYear(), TemporalParseConfidence.HIGH);
        }

        return ParsedTime.none();
    }

    private static int parseMonthToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("empty month token");
        }
        return switch (token) {
            case "一", "1" -> 1;
            case "二", "两", "2" -> 2;
            case "三", "3" -> 3;
            case "四", "4" -> 4;
            case "五", "5" -> 5;
            case "六", "6" -> 6;
            case "七", "7" -> 7;
            case "八", "8" -> 8;
            case "九", "9" -> 9;
            case "十", "10" -> 10;
            case "十一", "11" -> 11;
            case "十二", "12" -> 12;
            default -> Integer.parseInt(token);
        };
    }

    private static int parseDayToken(String token) {
        return switch (token) {
            case "一", "1" -> 1;
            case "二", "2" -> 2;
            case "三", "3" -> 3;
            case "四", "4" -> 4;
            case "五", "5" -> 5;
            case "六", "6" -> 6;
            case "七", "7" -> 7;
            case "八", "8" -> 8;
            case "九", "9" -> 9;
            case "十", "10" -> 10;
            default -> Integer.parseInt(token);
        };
    }

    private static int parseWeekToken(String token) {
        return switch (token) {
            case "一", "1" -> 1;
            case "二", "2" -> 2;
            case "三", "3" -> 3;
            case "四", "4" -> 4;
            case "五", "5" -> 5;
            default -> Integer.parseInt(token);
        };
    }

    private static int[] quarterBounds(String token) {
        return switch (token) {
            case "一", "1" -> new int[] {1, 3};
            case "二", "2" -> new int[] {4, 6};
            case "三", "3" -> new int[] {7, 9};
            case "四", "4" -> new int[] {10, 12};
            default -> new int[] {1, 3};
        };
    }

    record ParsedTime(
            Integer year,
            Integer month,
            Integer monthEnd,
            Integer weekOfMonth,
            Integer dayOfMonth,
            TemporalParseConfidence confidence) {
        static ParsedTime none() {
            return new ParsedTime(null, null, null, null, null, TemporalParseConfidence.NONE);
        }

        static ParsedTime single(int year, int month, TemporalParseConfidence confidence) {
            return new ParsedTime(year, month, null, null, null, confidence);
        }

        static ParsedTime range(int year, int monthStart, int monthEnd, TemporalParseConfidence confidence) {
            return new ParsedTime(year, monthStart, monthEnd, null, null, confidence);
        }

        static ParsedTime week(int year, int month, int weekOfMonth, TemporalParseConfidence confidence) {
            return new ParsedTime(year, month, null, weekOfMonth, null, confidence);
        }

        static ParsedTime day(int year, int month, int dayOfMonth, TemporalParseConfidence confidence) {
            return new ParsedTime(year, month, null, null, dayOfMonth, confidence);
        }

        static ParsedTime yearOnly(int year, TemporalParseConfidence confidence) {
            return new ParsedTime(year, null, null, null, null, confidence);
        }

        boolean scoped() {
            return year != null;
        }
    }
}
