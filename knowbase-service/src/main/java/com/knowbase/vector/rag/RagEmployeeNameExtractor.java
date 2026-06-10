package com.knowbase.vector.rag;

import com.knowbase.vector.dto.SearchHit;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从周报片段与文件名中抽取提交人姓名（结构化规则，避免把表格行误当人名）。 */
public final class RagEmployeeNameExtractor {

    /** 仅匹配表头字段后的取值（避免「责任人\\t执行要求」等表头行误提取） */
    private static final Pattern FIELD_NAME = Pattern.compile(
            "(?:^|[\\n\\r\\t])(?:姓名|部门负责人|(?<![\\u4e00-\\u9fff])责任人(?![\\u4e00-\\u9fff]))"
                    + "(?:[：:]|\\t)\\s*([\\u4e00-\\u9fff]{2,4})(?=\\t|[\\n\\r]|$)");

    /** Excel 行：… \\t 计划完成时间(5位序列) \\t 责任人 \\t … */
    private static final Pattern RESPONSIBLE_AFTER_EXCEL_DATE = Pattern.compile(
            "\t\\d{5}\t([\\u4e00-\\u9fff]{2,4})\t");

    private static final Pattern FILENAME_SUBMITTER = Pattern.compile(
            "[/\\\\]([\\u4e00-\\u9fff]{2,4})[-_]?周报");

    /** 表头/字段标签，绝非人名 */
    private static final Set<String> HEADER_LABELS = Set.of(
            "更新日期", "执行要求", "执行情况", "计划完成", "工作内容", "完成时间",
            "软件事业", "事业部", "工作周报", "工作计划", "工作周期", "报告周期",
            "序号", "类别", "说明", "部门", "姓名", "责任人", "负责人");

    private static final Set<String> NON_PERSON_TOKENS = Set.of(
            "海图", "项目", "浮标", "服务", "模块", "接口", "系统", "演示", "配合", "优化",
            "调整", "编写", "整理", "协调", "部署", "测试", "研究", "功能", "数据", "报告",
            "周报", "更新", "序号", "类别", "内容", "说明", "执行", "情况", "要求", "计划",
            "部门", "软件", "事业", "开展", "进行", "相关", "开发", "任务", "完成", "待开",
            "星图", "深海", "上海", "武器", "纳管", "态势", "分析", "运维", "资产", "管理",
            "告警", "指令", "消息", "回传", "接入", "联调", "正式", "调试", "调用", "修复",
            "新增", "拆分", "后台", "前端", "同事", "客户", "单位", "人员", "第三", "经理",
            "主任", "工作", "Sheet");

    private RagEmployeeNameExtractor() {}

    public static LinkedHashSet<String> extract(List<SearchHit> hits, Map<UUID, String> fileNames) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (hits != null) {
            for (SearchHit hit : hits) {
                if (hit.content() != null && !hit.content().isBlank()) {
                    collectFromText(hit.content(), names);
                }
            }
        }
        if (fileNames != null) {
            for (String fileName : fileNames.values()) {
                collectFromFileName(fileName, names);
            }
        }
        return names;
    }

    static void collectFromText(String text, Collection<String> names) {
        collectMatches(FIELD_NAME, text, names);
        collectMatches(RESPONSIBLE_AFTER_EXCEL_DATE, text, names);
    }

    static void collectFromFileName(String fileName, Collection<String> names) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Matcher matcher = FILENAME_SUBMITTER.matcher(fileName);
        while (matcher.find()) {
            addIfPersonName(matcher.group(1), names);
        }
    }

    private static void collectMatches(Pattern pattern, String text, Collection<String> names) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            addIfPersonName(matcher.group(1), names);
        }
    }

    public static boolean looksLikePersonName(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        String trimmed = candidate.strip();
        if (trimmed.length() < 2 || trimmed.length() > 4) {
            return false;
        }
        if (NON_PERSON_TOKENS.contains(trimmed) || HEADER_LABELS.contains(trimmed)) {
            return false;
        }
        if (trimmed.endsWith("日期") || trimmed.endsWith("要求") || trimmed.endsWith("情况")
                || trimmed.endsWith("内容") || trimmed.endsWith("时间")) {
            return false;
        }
        return trimmed.chars().allMatch(ch -> ch >= 0x4e00 && ch <= 0x9fff);
    }

    private static void addIfPersonName(String candidate, Collection<String> names) {
        if (!looksLikePersonName(candidate)) {
            return;
        }
        names.add(candidate.strip());
    }
}
