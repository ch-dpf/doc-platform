package com.knowbase.ingest.parse;

import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.library.dto.ParserEngineDescriptor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 内置解析器注册表：声明能力并将引擎预设应用到 {@link ParsingRulesSettings}。 */
@Component
public class ParserEngineRegistry {

    private static final Map<String, Set<String>> RECOMMENDED_FILE_TYPES = Map.of(
            BuiltinParserId.AUTO.wire(), Set.of("pdf", "word", "excel", "txt", "markdown"),
            BuiltinParserId.TIKA_PLAIN.wire(), Set.of("txt", "markdown"),
            BuiltinParserId.TIKA_STRUCTURED.wire(), Set.of("pdf", "word"),
            BuiltinParserId.TIKA_OCR_AUTO.wire(), Set.of("pdf"),
            BuiltinParserId.EXCEL_STRUCTURED.wire(), Set.of("excel"),
            BuiltinParserId.TIKA_TABLE_TEXT.wire(), Set.of("pdf", "word", "excel"));

    public List<ParserEngineDescriptor> listDescriptors() {
        List<ParserEngineDescriptor> list = new ArrayList<>();
        for (BuiltinParserId id : BuiltinParserId.values()) {
            Set<String> types = RECOMMENDED_FILE_TYPES.getOrDefault(id.wire(), Set.of());
            list.add(new ParserEngineDescriptor(
                    id.wire(),
                    id.label(),
                    id.description(),
                    List.copyOf(types)));
        }
        return list;
    }

    /** 将引擎预设覆盖到 parsing（AUTO 不修改 MIME 已应用的值）。 */
    public void apply(String parserId, ParsingRulesSettings parsing) {
        BuiltinParserId id = BuiltinParserId.fromWire(parserId).orElse(BuiltinParserId.AUTO);
        apply(id, parsing);
    }

    public void apply(BuiltinParserId id, ParsingRulesSettings parsing) {
        if (parsing == null || id == null || id == BuiltinParserId.AUTO) {
            return;
        }
        switch (id) {
            case TIKA_PLAIN -> {
                parsing.setOcrEnabled(false);
                parsing.setTableExtraction(TableExtractionMode.TEXT_ONLY.configValue());
                parsing.setImageExtraction(ImageExtractionMode.SKIP.configValue());
                parsing.setFormulaExtraction(FormulaExtractionMode.SKIP.configValue());
            }
            case TIKA_STRUCTURED -> {
                parsing.setOcrEnabled(false);
                parsing.setTableExtraction(TableExtractionMode.STRUCTURED.configValue());
                parsing.setImageExtraction(ImageExtractionMode.SKIP.configValue());
                parsing.setFormulaExtraction(FormulaExtractionMode.SKIP.configValue());
            }
            case TIKA_OCR_AUTO -> {
                parsing.setOcrEnabled(true);
                parsing.setTableExtraction(TableExtractionMode.TEXT_ONLY.configValue());
                parsing.setImageExtraction(ImageExtractionMode.SKIP.configValue());
                parsing.setFormulaExtraction(FormulaExtractionMode.SKIP.configValue());
            }
            case EXCEL_STRUCTURED -> {
                parsing.setOcrEnabled(false);
                parsing.setTableExtraction(TableExtractionMode.STRUCTURED.configValue());
                parsing.setImageExtraction(ImageExtractionMode.SKIP.configValue());
                parsing.setFormulaExtraction(FormulaExtractionMode.SKIP.configValue());
            }
            case TIKA_TABLE_TEXT -> {
                parsing.setOcrEnabled(false);
                parsing.setTableExtraction(TableExtractionMode.TEXT_ONLY.configValue());
                parsing.setImageExtraction(ImageExtractionMode.SKIP.configValue());
                parsing.setFormulaExtraction(FormulaExtractionMode.SKIP.configValue());
            }
            default -> { /* AUTO */ }
        }
    }

    public String labelFor(String parserId) {
        return BuiltinParserId.fromWire(parserId).map(BuiltinParserId::label).orElse(parserId);
    }
}
