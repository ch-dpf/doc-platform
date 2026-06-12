package com.knowbase.library.dto.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class LibraryIndexPipelineDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsChunkBoundsInRange() {
        var dto = new LibraryIndexPipelineDto(500, 120, "nomic-embed-text", 768, true, "");
        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsChunkSizeBelowMinimum() {
        var dto = new LibraryIndexPipelineDto(99, 120, "nomic-embed-text", 768, true, "");
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsChunkSizeAboveMaximum() {
        var dto = new LibraryIndexPipelineDto(8001, 120, "nomic-embed-text", 768, true, "");
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void rejectsChunkOverlapAboveMaximum() {
        var dto = new LibraryIndexPipelineDto(500, 2001, "nomic-embed-text", 768, true, "");
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void acceptsZeroChunkOverlap() {
        var dto = new LibraryIndexPipelineDto(500, 0, "nomic-embed-text", 768, true, "");
        assertTrue(validator.validate(dto).isEmpty());
    }
}
