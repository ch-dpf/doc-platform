package com.knowbase.vector.rag;

import com.knowbase.vector.chunk.WeeklyReportChunkHeuristics;
import com.knowbase.vector.dto.SearchHit;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从周报 Excel 分块中抽取「工作内容」列，跳过表头块。 */
public final class WeeklyReportWorkItemExtractor {

    /** 序号 \\t 类别 \\t 工作内容 … */
    private static final Pattern WORK_ROW = Pattern.compile(
            "(?m)^\\d+\\t[^\\t\\n]+\\t([^\\t\\n]+)");

    private static final Set<String> SKIP_CONTENT = Set.of(
            "工作内容", "执行情况", "执行要求", "计划完成", "说明", "类别", "序号", "责任人", "部门");

    private WeeklyReportWorkItemExtractor() {}

    public record WorkItem(String project, String content, int refIndex, UUID docId) {}

    public static List<WorkItem> extract(List<SearchHit> hits, Map<UUID, String> fileNames) {
        List<WorkItem> items = new ArrayList<>();
        if (hits == null) {
            return items;
        }
        for (int i = 0; i < hits.size(); i++) {
            SearchHit hit = hits.get(i);
            if (hit.content() == null || hit.content().isBlank() || isHeaderOnlyChunk(hit.content())) {
                continue;
            }
            int refIndex = i + 1;
            collectFromChunk(hit.content(), hit.docId(), refIndex, items);
        }
        return dedupe(items);
    }

    public static boolean isHeaderOnlyChunk(String content) {
        return WeeklyReportChunkHeuristics.isHeaderOnlyChunk(content);
    }

    private static void collectFromChunk(String content, UUID docId, int refIndex, List<WorkItem> items) {
        Matcher matcher = WORK_ROW.matcher(content);
        while (matcher.find()) {
            String work = matcher.group(1).strip();
            if (!isValidWorkContent(work)) {
                continue;
            }
            String project = extractProject(content, matcher.start());
            items.add(new WorkItem(project, work, refIndex, docId));
        }
    }

    private static String extractProject(String content, int workMatchStart) {
        int lineStart = content.lastIndexOf('\n', workMatchStart) + 1;
        String line = content.substring(lineStart, Math.min(content.length(), workMatchStart + 200));
        int firstTab = line.indexOf('\t');
        if (firstTab < 0) {
            return "";
        }
        int secondTab = line.indexOf('\t', firstTab + 1);
        if (secondTab < 0) {
            return "";
        }
        int thirdTab = line.indexOf('\t', secondTab + 1);
        if (thirdTab < 0) {
            return "";
        }
        return line.substring(firstTab + 1, secondTab).strip();
    }

    private static boolean isValidWorkContent(String work) {
        if (work.length() < 4 || work.length() > 200) {
            return false;
        }
        if (SKIP_CONTENT.contains(work)) {
            return false;
        }
        if (work.matches("(?m)^\\d+$") || work.matches(".*工作周报.*")) {
            return false;
        }
        if (work.contains("软件事业部") && work.length() < 12) {
            return false;
        }
        return work.chars().anyMatch(ch -> ch >= 0x4e00 && ch <= 0x9fff);
    }

    private static List<WorkItem> dedupe(List<WorkItem> items) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<WorkItem> result = new ArrayList<>();
        for (WorkItem item : items) {
            String key = normalize(item.content());
            if (seen.add(key)) {
                result.add(item);
            }
        }
        return result;
    }

    static String normalize(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", "");
    }

    public static LinkedHashSet<String> extractSubmitterNames(List<SearchHit> hits, Map<UUID, String> fileNames) {
        return RagEmployeeNameExtractor.extract(hits, fileNames);
    }
}
