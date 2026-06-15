package com.knowbase.vector.chunk;

import java.util.ArrayList;
import java.util.List;

/** 表格文档中一个 section 的上下文，用于注入数据块前缀。 */
public record TabularSectionContext(
        TabularDocumentProfile profile,
        String fileName,
        String submitter,
        String sectionLabel,
        String period,
        String columns) {

    public static TabularSectionContext forProfile(
            TabularDocumentProfile profile,
            String fileName,
            String submitter,
            String sectionLabel,
            String period,
            String columns) {
        return new TabularSectionContext(
                profile != null ? profile : TabularDocumentProfile.GENERIC,
                fileName,
                submitter,
                sectionLabel,
                period,
                columns);
    }

    public String formatPrefix() {
        if (profile == TabularDocumentProfile.GENERIC) {
            return formatGenericPrefix();
        }
        return formatWeeklyReportPrefix();
    }

    private String formatGenericPrefix() {
        StringBuilder out = new StringBuilder();
        if (fileName != null && !fileName.isBlank()) {
            out.append("【表格·").append(fileName.strip()).append("】");
        }
        if (columns != null && !columns.isBlank()) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append("列：").append(columns.strip());
        }
        return out.toString().strip();
    }

    private String formatWeeklyReportPrefix() {
        List<String> parts = new ArrayList<>(3);
        if (submitter != null && !submitter.isBlank()) {
            parts.add(submitter.strip());
        }
        if (sectionLabel != null && !sectionLabel.isBlank()) {
            parts.add(sectionLabel.strip());
        }
        if (period != null && !period.isBlank()) {
            parts.add(period.strip());
        }
        if (parts.isEmpty() && (columns == null || columns.isBlank())) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        if (!parts.isEmpty()) {
            out.append("【").append(String.join("·", parts)).append("】");
        }
        if (columns != null && !columns.isBlank()) {
            if (!out.isEmpty()) {
                out.append('\n');
            }
            out.append("列：").append(columns.strip());
        }
        return out.toString().strip();
    }

    public boolean isEmpty() {
        return formatPrefix().isBlank();
    }
}
