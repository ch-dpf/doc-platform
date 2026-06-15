package com.knowbase.library.dto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateVectorLibraryRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsNameAndDescription() {
        var request = new CreateVectorLibraryRequest("demo", "研发文档库", "存放研发规范与设计文档", List.of());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsBlankDescription() {
        var request = new CreateVectorLibraryRequest("demo", "研发文档库", "", null);
        assertFalse(validator.validate(request).isEmpty());
    }
}
