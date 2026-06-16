package com.knowbase.library.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.knowbase.library.config.RetrievalRulesSettings;
import com.knowbase.library.dto.config.LibraryIndexPipelineDto;
import com.knowbase.library.dto.config.LibraryParsingDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateVectorLibraryRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsNameAndDescription() {
        var request = new CreateVectorLibraryRequest(
                "demo", "研发文档库", "存放研发规范与设计文档", List.of(), null, null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsBlankDescription() {
        var request = new CreateVectorLibraryRequest("demo", "研发文档库", "", null, null, null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsNullDescription() {
        var request = new CreateVectorLibraryRequest("demo", "研发文档库", null, null, null, null, null);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsOptionalPipelineSections() {
        var indexPipeline = new LibraryIndexPipelineDto(500, 120, "nomic-embed-text", 768, true, "", "auto");
        var parsing = new LibraryParsingDto(List.of(), "zh-CN", true);
        var retrieval = new RetrievalRulesSettings();
        var request = new CreateVectorLibraryRequest(
                "demo", "研发文档库", null, List.of(), indexPipeline, parsing, retrieval);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsInvalidIndexPipeline() {
        var indexPipeline = new LibraryIndexPipelineDto(50, 120, "nomic-embed-text", 768, true, "", "auto");
        var request = new CreateVectorLibraryRequest(
                "demo", "研发文档库", null, null, indexPipeline, null, null);
        assertFalse(validator.validate(request).isEmpty());
    }
}
