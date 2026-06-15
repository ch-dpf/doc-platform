package com.knowbase.vector.rag;

import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.dto.SearchHit;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** 员工名单 / 是否存在其他员工类问题的规则化作答（避免 LLM 误读 Excel 表格行）。 */
public final class RagEmployeeRosterSupport {

    private RagEmployeeRosterSupport() {}

    /** 基于库内全部文档文件名统计提交周报的员工人数（不依赖向量 Top-K）。 */
    public static Optional<String> tryLibraryWideCountAnswer(
            String question,
            UUID libraryId,
            String tenantId,
            DocMetadataStore docMetadataStore) {
        if (!RagQuestionAnalyzer.isEmployeeCountQuestion(question)) {
            return Optional.empty();
        }
        List<String> fileNames = docMetadataStore.findActiveFileNamesByLibrary(libraryId, tenantId);
        LinkedHashSet<String> names = extractSubmittersFromFileNames(fileNames);
        if (names.isEmpty()) {
            return Optional.of("根据知识库内 " + fileNames.size()
                    + " 份文档的文件名，未能识别提交周报的员工姓名。（统计来自知识库元数据）");
        }
        return Optional.of(formatCountAnswer(names)
                + " 知识库内共有 " + fileNames.size() + " 份文档。（员工姓名来自文档文件名）");
    }

    public static Optional<String> tryRuleBasedAnswer(String question, List<SearchHit> hits, Map<UUID, String> fileNames) {
        if (hits == null || hits.isEmpty()) {
            return Optional.empty();
        }
        if (RagQuestionAnalyzer.isCombinedEmployeeWorkQuestion(question)) {
            return Optional.empty();
        }
        LinkedHashSet<String> names = RagEmployeeNameExtractor.extract(hits, fileNames);
        if (names.isEmpty()) {
            return Optional.empty();
        }
        if (RagQuestionAnalyzer.isEmployeeExistenceQuestion(question)) {
            return Optional.of(formatExistenceAnswer(names));
        }
        if (RagQuestionAnalyzer.isEmployeeCountQuestion(question)) {
            return Optional.of(formatCountAnswer(names));
        }
        if (RagQuestionAnalyzer.isEmployeeListQuestion(question)) {
            return Optional.of(formatListAnswer(names));
        }
        return Optional.empty();
    }

    static LinkedHashSet<String> extractSubmittersFromFileNames(List<String> fileNames) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (fileNames != null) {
            for (String fileName : fileNames) {
                RagEmployeeNameExtractor.collectFromFileName(fileName, names);
            }
        }
        return names;
    }

    static String formatExistenceAnswer(LinkedHashSet<String> names) {
        if (names.size() == 1) {
            String only = names.iterator().next();
            return "根据参考资料中的文件名与「姓名/责任人」字段，仅见" + only
                    + "一人提交周报，不存在其他员工。[1]";
        }
        String joined = String.join("、", names);
        return "根据参考资料中的文件名与「姓名/责任人」字段，可见多名员工提交周报，包括："
                + joined + "。[1]";
    }

    static String formatListAnswer(LinkedHashSet<String> names) {
        String joined = names.stream().collect(Collectors.joining("、"));
        return "根据参考资料（文件名及正文中的姓名/责任人字段），上传周报材料的员工有：" + joined + "。[1]";
    }

    static String formatCountAnswer(LinkedHashSet<String> names) {
        int count = names.size();
        String joined = names.stream().collect(Collectors.joining("、"));
        if (count == 1) {
            return "根据参考资料（文件名及正文中的姓名/责任人字段），共有 1 人提交了周报材料：" + joined + "。[1]";
        }
        return "根据参考资料（文件名及正文中的姓名/责任人字段），共有 " + count
                + " 人提交了周报材料，包括：" + joined + "。[1]";
    }
}
