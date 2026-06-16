package com.knowbase.ingest.parse;

import com.knowbase.library.config.ParserEngineRule;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.pipeline.config.PlatformPipelineDefaults;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParserEngineRegistryTest {

    private final ParserEngineRegistry registry = new ParserEngineRegistry();

    @Test
    void ocrAutoParserEnablesOcr() {
        ParsingRulesSettings parsing = PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        registry.apply(BuiltinParserId.TIKA_OCR_AUTO.wire(), parsing);
        assertTrue(parsing.isOcrEnabled());
    }

    @Test
    void excelStructuredUsesStructuredTableMode() {
        ParsingRulesSettings parsing = PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        registry.apply(BuiltinParserId.EXCEL_STRUCTURED.wire(), parsing);
        assertEquals("structured", parsing.getTableExtraction());
    }

    @Test
    void autoParserDoesNotChangeParsing() {
        ParsingRulesSettings parsing = PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        parsing.setOcrEnabled(true);
        registry.apply(BuiltinParserId.AUTO.wire(), parsing);
        assertTrue(parsing.isOcrEnabled());
    }

    @Test
    void resolveParserIdFromLibraryRules() {
        ParserEngineRule pdf = new ParserEngineRule();
        pdf.setFileType("pdf");
        pdf.setParserId("tika-ocr-auto");
        assertEquals("tika-ocr-auto", ParserRuleResolver.resolveParserId(List.of(pdf), "pdf"));
        assertEquals("auto", ParserRuleResolver.resolveParserId(List.of(pdf), "excel"));
    }

    @Test
    void structuredParserDisablesOcr() {
        ParsingRulesSettings parsing = PlatformPipelineDefaults.copyParsing(PlatformPipelineDefaults.baselineParsing());
        registry.apply(BuiltinParserId.TIKA_STRUCTURED.wire(), parsing);
        assertFalse(parsing.isOcrEnabled());
        assertEquals("structured", parsing.getTableExtraction());
    }
}
