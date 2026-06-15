package com.knowbase.vector.rag;

import com.knowbase.vector.dto.RagChatMessage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 识别需要参考资料中「明文写出」才能作答的问题类型。 */
public final class RagQuestionAnalyzer {

    private static final Pattern DEADLINE_QUESTION = Pattern.compile(
            ".*(截止时间|截止日期|截止日|提交截止|截止).*",
            Pattern.CASE_INSENSITIVE);

    /** 跨多篇文档归纳、名单、主要工作、存在性判断等汇总类问题 */
    private static final Pattern SYNTHESIS_QUESTION = Pattern.compile(
            ".*(哪些|有哪些|列出|汇总|归纳|统计|分别|都有|哪些人|哪些员工|谁上传|上传了|提交了|提交"
                    + "|主要工作|工作内容|做了什么|做了哪些|材料"
                    + "|是否存在|有没有|是否还有|有无|其它员工|其他员工|别的员工|除.*外).*",
            Pattern.CASE_INSENSITIVE);

    /** 带时间范围且询问已完成工作的汇总类问题（宜库级扫描 + 规则抽取，不宜 Top-K + LLM）。 */
    private static final Pattern TEMPORAL_COMPLETED_WORK = Pattern.compile(
            ".*(完成|已完成|做了|做过).*(工作|内容|任务|事项).*|.*(工作|内容|任务|事项).*(完成|已完成|做了|做过).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern TEMPORAL_HINT = Pattern.compile(
            "(20\\d{2})\\s*年"
                    + "|(?:今年|本年|去年|上月|本月|这个月)"
                    + "|第[一二三四五六七八九十两\\d]{1,2}个?月"
                    + "|第[一二三四五1-5]周"
                    + "|第[一二三四五六七八九十两\\d]{1,2}天"
                    + "|第[一二三四1234]季度"
                    + "|[Qq][1-4]",
            Pattern.CASE_INSENSITIVE);

    private RagQuestionAnalyzer() {}

    public static boolean isDeadlineQuestion(String question) {
        return question != null && DEADLINE_QUESTION.matcher(question.strip()).matches();
    }

    public static boolean isSynthesisQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        if (isDeadlineQuestion(question)) {
            return false;
        }
        return SYNTHESIS_QUESTION.matcher(question.strip()).matches();
    }

    /**
     * 识别「某人在某时段完成了哪些工作」类汇总问句（与 {@link RagTemporalQueryParser} 配合：
     * 本方法看意图，解析器负责结构化时间范围）。
     */
    public static boolean isTemporalCompletedWorkQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = normalizeQuestion(question);
        if (!TEMPORAL_COMPLETED_WORK.matcher(normalized).matches()) {
            return false;
        }
        if (!TEMPORAL_HINT.matcher(normalized).find()) {
            return false;
        }
        return normalized.contains("工作") || normalized.contains("内容") || normalized.contains("任务");
    }

    private static final Pattern EMPLOYEE_EXISTENCE = Pattern.compile(
            ".*((是否存在|有没有|是否还有|有无).*(其他|别的|其它).*(员工|人员|同事)"
                    + "|(其他|别的|其它).*(员工|人员|同事).*(提交|上传)).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EMPLOYEE_LIST = Pattern.compile(
            ".*((哪些|有哪些|谁|哪些人).*(员工|人员|同事).*(上传|提交|周报|材料)"
                    + "|(上传|提交).*(周报|材料).*(哪些|有哪些|谁|哪些人)).*",
            Pattern.CASE_INSENSITIVE);

    /** 统计提交周报的人数（非文档份数） */
    private static final Pattern EMPLOYEE_COUNT = Pattern.compile(
            ".*((大概|约|大约)?(多少|有几|几个|几人|几位).*(人|员工|人员).*(提交|上传).*(周报|材料)"
                    + "|(提交|上传).*(周报|材料).*(多少|有几|几个|几人|几位).*(人|员工|人员)"
                    + "|(多少|有几|几个|几人).*(人|员工).*(提交|上传)"
                    + "|(大概|约|大约)?(多少|有几|几个|几人|几位).*(员工|人).*(周报|周报材料)"
                    + "|(周报|周报材料).*(多少|有几|几个|几人|几位).*(员工|人)).*",
            Pattern.CASE_INSENSITIVE);

    public static boolean isEmployeeExistenceQuestion(String question) {
        return question != null && EMPLOYEE_EXISTENCE.matcher(normalizeQuestion(question)).matches();
    }

    public static boolean isEmployeeListQuestion(String question) {
        return question != null && EMPLOYEE_LIST.matcher(normalizeQuestion(question)).matches();
    }

    public static boolean isEmployeeCountQuestion(String question) {
        return question != null && EMPLOYEE_COUNT.matcher(normalizeQuestion(question)).matches();
    }

    private static final Set<String> GENERIC_PERSON_TERMS = Set.of(
            "员工", "人员", "同事", "本库", "本库中", "知识库", "当前库", "这个库", "该库");

    private static final Pattern NAMED_EMPLOYEE_PROJECT_CLAUSE = Pattern.compile(
            "^(?:今年|本年)?([\\u4e00-\\u9fff]{2,4})\\s*参与(?:了)?.*(多少|有几|几个|哪些|有哪些).*(项目)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern EMPLOYEE_PROJECT_COUNT = Pattern.compile(
            ".*(多少|有几|几个).*(项目).*",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PROJECT_RECOUNT = Pattern.compile(
            ".*(重新统计|重新计算|重新数|去重|合并统计|同一个项目|同一项目).*",
            Pattern.CASE_INSENSITIVE);

    /** 询问当前日历年，不应拼接上轮话题或走向量检索。 */
    private static final Pattern CALENDAR_YEAR = Pattern.compile(
            "^(今年|本年|现在|当前)(是|为)?(哪一|什么|几)年[?？!！。,，;；\\s]*$"
                    + "|^(哪一|什么)年(是|为)?今年[?？!！。,，;；\\s]*$"
                    + "|^现在是几几年[?？!！。,，;；\\s]*$"
                    + "|^今年(?:是|为)?(?:哪一|什么|几)年[?？!！。,，;；\\s]*$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PROJECT_ALIAS_GROUP = Pattern.compile(
            "(?:已知)?(.+?)(?:为|是)(?:同一个|同一)项目");

    public static String extractNamedEmployeeFromProjectQuestion(String question) {
        if (question == null || question.isBlank()) {
            return null;
        }
        String normalized = normalizeQuestion(question);
        String[] clauses = normalized.split("[?？!！。,，;；]");
        for (int i = clauses.length - 1; i >= 0; i--) {
            String clause = clauses[i].strip();
            if (clause.isEmpty()) {
                continue;
            }
            Matcher matcher = NAMED_EMPLOYEE_PROJECT_CLAUSE.matcher(clause);
            if (!matcher.matches()) {
                continue;
            }
            String person = matcher.group(1).strip();
            if (GENERIC_PERSON_TERMS.contains(person)
                    || person.contains("库")
                    || person.contains("主要")
                    || person.contains("哪些")
                    || person.contains("哪一")
                    || person.contains("今年")
                    || person.contains("本年")
                    || person.endsWith("年")) {
                continue;
            }
            if (RagEmployeeNameExtractor.looksLikePersonName(person)) {
                return person;
            }
        }
        return null;
    }

    public static boolean isEmployeeProjectQuestion(String question) {
        return extractNamedEmployeeFromProjectQuestion(question) != null;
    }

    public static boolean isEmployeeProjectCountQuestion(String question) {
        if (extractNamedEmployeeFromProjectQuestion(question) == null) {
            return false;
        }
        return EMPLOYEE_PROJECT_COUNT.matcher(normalizeQuestion(question)).matches();
    }

    public static boolean isProjectRecountQuestion(String question) {
        return question != null && PROJECT_RECOUNT.matcher(normalizeQuestion(question)).matches();
    }

    public static boolean isCalendarYearQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = normalizeQuestion(question);
        return CALENDAR_YEAR.matcher(normalized).matches();
    }

    private static final Pattern WEEKLY_REPORT_WEEK = Pattern.compile(
            ".*(今年|本年).*(哪一|哪|哪个|哪些).*(周|星期).*(周报|工作周报).*"
                    + "|.*(哪一|哪|哪个|哪些).*(周|星期).*(周报|工作周报).*(今年|本年).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern CALENDAR_YEAR_CLAUSE = Pattern.compile(
            "(今年|本年)(是|为)?(哪一|什么|几)年[?？!！。,，;；\\s]*");

    public static boolean isWeeklyReportWeekQuestion(String question) {
        return question != null && WEEKLY_REPORT_WEEK.matcher(normalizeQuestion(question)).matches();
    }

    public static boolean containsCalendarYearClause(String question) {
        return question != null && CALENDAR_YEAR_CLAUSE.matcher(question.strip()).find();
    }

    /** 问句含「今年/本年」且未指明其它历史年份时，项目类统计应限定在当前日历年。 */
    public static boolean scopesToCurrentCalendarYear(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = normalizeQuestion(question);
        if (!normalized.contains("今年") && !normalized.contains("本年")) {
            return false;
        }
        return !Pattern.compile("(20\\d{2})\\s*年").matcher(normalized).find();
    }

    public static String extractWeekQueryProjectToken(String question) {
        if (question == null || question.isBlank()) {
            return "海图";
        }
        String q = question.strip();
        if (q.contains("海图项目") || q.contains("海图")) {
            return "海图";
        }
        Matcher matcher = Pattern.compile("([\\u4e00-\\u9fffA-Za-z]{2,12}项目)").matcher(q);
        if (matcher.find()) {
            return matcher.group(1).replace("项目", "");
        }
        return "海图";
    }

    /** 从追问中解析用户声明的「同一项目」别名组，如「A、B、C为同一个项目」。 */
    public static List<Set<String>> extractProjectAliasGroups(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        Matcher matcher = PROJECT_ALIAS_GROUP.matcher(question.strip());
        if (!matcher.find()) {
            return List.of();
        }
        String names = matcher.group(1).strip();
        LinkedHashSet<String> group = new LinkedHashSet<>();
        for (String part : names.split("[、,，/]")) {
            String name = part.strip();
            if (!name.isBlank()) {
                group.add(name);
            }
        }
        return group.size() >= 2 ? List.of(group) : List.of();
    }

    /** 从多轮历史中提取最近一次「X参与…项目」问句里的员工姓名。 */
    public static String findNamedEmployeeFromHistory(List<RagChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            RagChatMessage msg = history.get(i);
            if (!"user".equals(msg.role()) || msg.content() == null) {
                continue;
            }
            String person = extractNamedEmployeeFromProjectQuestion(msg.content());
            if (person != null) {
                return person;
            }
        }
        return null;
    }

    /** 同时问提交人名单与主要工作内容，需 LLM 综合归纳，不能只用名单规则短路 */
    private static final Pattern EMPLOYEE_WORK_COMBINED = Pattern.compile(
            ".*(哪些|有哪些|谁|哪些人).*(员工|人员|同事).*(提交|上传).*(周报|材料).*"
                    + ".*(主要工作|工作内容|汇报了|做了哪些|哪些工作).*"
            + "|.*(主要工作|工作内容|汇报了|做了哪些|哪些工作).*"
                    + ".*(哪些|有哪些|谁|哪些人).*(员工|人员|同事).*(提交|上传).*(周报|材料).*",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static boolean isCombinedEmployeeWorkQuestion(String question) {
        return question != null && EMPLOYEE_WORK_COMBINED.matcher(question.strip()).matches();
    }

    /** 询问当前知识库文档数、切片数等元数据统计，应查库表而非向量片段。 */
    private static final Pattern LIBRARY_STATS = Pattern.compile(
            ".*(本知识库|当前知识库|这个知识库|所选知识库|该知识库|知识库|库内|入库)"
                    + ".*(多少|有几|几个|数量|统计|总数|共计|一共).*(文档|文件|切片|分块|chunk|片段|向量).*"
            + "|.*(多少|有几|几个|数量|统计|总数|共计|一共).*(文档|文件).*(与|和|、).*(切片|分块|chunk|片段|向量).*"
            + "|.*(文档|文件).*(与|和|、).*(切片|分块|chunk|片段|向量).*(多少|数量|数据|统计).*"
            + "|.*(多少|有几|几个|数量|统计|总数|共计|一共).*(文档|文件|切片|分块|chunk|片段|向量).*",
            Pattern.CASE_INSENSITIVE);

    /** 询问知识库用途、简介等元数据，应读 vector_library.description 而非向量片段。 */
    private static final Pattern LIBRARY_PURPOSE = Pattern.compile(
            ".*(本库|本知识库|当前知识库|这个知识库|所选知识库|该知识库|知识库)"
                    + ".*(主要|用途|做什么|干什么|做什么用|用来|用于|介绍|简介|概述|说明|是干什么的).*"
            + "|.*(介绍一下|简要介绍|介绍下).*(本库|本知识库|知识库|当前库).*"
            + "|.*(本库|本知识库|知识库).*(是做什么|做什么的|什么用途).*",
            Pattern.CASE_INSENSITIVE);

    public static boolean isLibraryStatsQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        return LIBRARY_STATS.matcher(normalizeQuestion(question)).matches();
    }

    /** 周报/工作内容汇总归纳（应规则抽取，不宜由 LLM 自由复述参考资料块）。 */
    private static final Pattern WEEKLY_REPORT_SUMMARY = Pattern.compile(
            ".*(周报|工作周报).*(主要)?(内容|工作).*(汇总|归纳|总结|梳理)"
                    + "|.*(主要)?(内容|工作).*(汇总|归纳|总结).*(周报|工作周报)"
                    + "|.*周报.*(讲了什么|说了什么|写了什么).*",
            Pattern.CASE_INSENSITIVE);

    public static boolean isWeeklyReportSummaryQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        if (isCombinedEmployeeWorkQuestion(question)) {
            return false;
        }
        return WEEKLY_REPORT_SUMMARY.matcher(normalizeQuestion(question)).matches();
    }

    public static boolean isLibraryPurposeQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        if (isLibraryStatsQuestion(question) || isSynthesisQuestion(question)) {
            return false;
        }
        return LIBRARY_PURPOSE.matcher(normalizeQuestion(question)).matches();
    }

    private static String normalizeQuestion(String question) {
        return question.strip().replaceAll("[?？!！。,，;；\\s]+$", "");
    }
}
