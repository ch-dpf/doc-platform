package com.knowbase.vector.chunk;

import com.knowbase.ingest.parse.TabularRowLinearizer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 单次扫描表格文本：识别表头与数据行，并绑定 section 上下文（P1/P2）。
 */
public final class TabularTableScanner {

    private static final Pattern NUMBERED_DATA_ROW = Pattern.compile("^\\d+\\t");

    private TabularTableScanner() {}

    public record RowBinding(String line, TabularSectionContext context) {}

    public static List<RowBinding> collectDataRows(String text, String fileName) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        TabularDocumentProfile profile = TabularDocumentProfileDetector.detect(text, fileName);
        String normalized = TabularContinuationNormalizer.joinContinuations(
                text.replace("\r\n", "\n").replace('\r', '\n').strip());
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<RowBinding> bindings = new ArrayList<>();
        String headerLine = null;
        String columns = null;
        String lastWeeklyReportColumns = null;
        String submitter = null;
        String sectionLabel = null;
        String period = null;

        for (String raw : normalized.split("\n")) {
            String line = TabularSectionContextIndex.normalizeLine(raw);
            if (line.isEmpty()) {
                continue;
            }
            if (profile == TabularDocumentProfile.WEEKLY_REPORT) {
                String detectedSection = TabularSectionContextIndex.detectSectionLabel(line);
                if (detectedSection != null) {
                    sectionLabel = detectedSection;
                }
                String person = TabularSectionContextIndex.extractSubmitter(line);
                if (person != null) {
                    submitter = person;
                }
                String detectedPeriod = TabularSectionContextIndex.extractPeriod(line);
                if (detectedPeriod != null) {
                    period = detectedPeriod;
                }
            }

            if (isTableHeaderLine(line, profile)) {
                headerLine = line;
                columns = TabularSectionContextIndex.compactColumns(line);
                if (profile == TabularDocumentProfile.WEEKLY_REPORT && isWeeklyReportSchemaHeader(line)) {
                    lastWeeklyReportColumns = columns;
                }
                continue;
            }

            if (isDataRow(line, headerLine, profile)) {
                String effectiveColumns = resolveColumns(profile, columns, lastWeeklyReportColumns);
                TabularSectionContext context = TabularSectionContext.forProfile(
                        profile, fileName, submitter, sectionLabel, period, effectiveColumns);
                bindings.add(new RowBinding(line, context));
            } else if (!line.contains("\t")) {
                headerLine = null;
                columns = null;
            }
        }
        return bindings;
    }

    static boolean isDataRow(String line, String headerLine, TabularDocumentProfile profile) {
        if (line == null || line.isBlank()) {
            return false;
        }
        if (NUMBERED_DATA_ROW.matcher(line).find()) {
            return true;
        }
        if (profile == TabularDocumentProfile.GENERIC && looksLikeGenericDataRow(line)) {
            return true;
        }
        if (headerLine == null || !line.contains("\t") || isTableHeaderLine(line, profile)) {
            return false;
        }
        return TabularRowLinearizer.isTableDataLine(line);
    }

    static boolean isTableHeaderLine(String line, TabularDocumentProfile profile) {
        if (line == null || !line.contains("\t")) {
            return false;
        }
        if (profile == TabularDocumentProfile.WEEKLY_REPORT) {
            if (isWeeklyMetadataLine(line)) {
                return false;
            }
            return isWeeklyReportSchemaHeader(line)
                    || TabularRowLinearizer.isTableHeaderLine(line);
        }
        if (!TabularRowLinearizer.isTableHeaderLine(line) || isWeeklyMetadataLine(line)) {
            return false;
        }
        return !looksLikeGenericDataRow(line);
    }

    static boolean looksLikeGenericDataRow(String line) {
        if (line == null || !line.contains("\t")) {
            return false;
        }
        String firstCell = line.split("\t", -1)[0].strip();
        if (firstCell.isEmpty()) {
            return false;
        }
        if (NUMBERED_DATA_ROW.matcher(line).find()) {
            return true;
        }
        if (firstCell.matches("^\\d+$")) {
            return true;
        }
        return firstCell.matches("^[A-Za-z]{1,}[-_]\\d+.*")
                || firstCell.matches("^[A-Z]{2,}\\d+$")
                || firstCell.matches("^[A-Za-z0-9]{6,}$");
    }

    private static boolean isWeeklyMetadataLine(String line) {
        if (line == null) {
            return false;
        }
        return line.contains("部门\t") && (line.contains("姓名\t") || line.contains("部门负责人\t"));
    }

    private static boolean isWeeklyReportSchemaHeader(String line) {
        return line != null
                && line.contains("序号")
                && line.contains("工作内容")
                && !NUMBERED_DATA_ROW.matcher(line).find();
    }

    private static String resolveColumns(
            TabularDocumentProfile profile, String columns, String lastWeeklyReportColumns) {
        if (columns != null && !columns.isBlank()) {
            return columns;
        }
        if (profile == TabularDocumentProfile.WEEKLY_REPORT
                && lastWeeklyReportColumns != null
                && !lastWeeklyReportColumns.isBlank()) {
            return lastWeeklyReportColumns;
        }
        return columns;
    }
}
