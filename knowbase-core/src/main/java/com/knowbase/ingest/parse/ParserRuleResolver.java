package com.knowbase.ingest.parse;

import com.knowbase.library.config.ParserEngineRule;
import com.knowbase.library.config.VectorLibraryConfigFactory;

import java.util.List;

/** 按 MIME/文件名解析 fileType，并从库级 parser_rules 解析 parserId。 */
public final class ParserRuleResolver {

    private ParserRuleResolver() {}

    public static String resolveFileType(String mimeType, String fileName) {
        return VectorLibraryConfigFactory.resolveFileType(mimeType, fileName);
    }

    public static String resolveParserId(List<ParserEngineRule> rules, String fileType) {
        if (rules == null || rules.isEmpty() || fileType == null || fileType.isBlank()) {
            return BuiltinParserId.AUTO.wire();
        }
        String normalized = fileType.trim().toLowerCase();
        for (ParserEngineRule rule : rules) {
            if (rule == null || rule.getFileType() == null) {
                continue;
            }
            if (normalized.equals(rule.getFileType().trim().toLowerCase())) {
                String parserId = rule.getParserId();
                return parserId != null && !parserId.isBlank()
                        ? parserId.trim().toLowerCase()
                        : BuiltinParserId.AUTO.wire();
            }
        }
        return BuiltinParserId.AUTO.wire();
    }
}
