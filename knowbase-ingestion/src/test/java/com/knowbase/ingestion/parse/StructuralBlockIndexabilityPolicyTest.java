package com.knowbase.ingestion.parse;

import com.knowbase.ingestion.StructuralBlock;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralBlockIndexabilityPolicyTest {

    @Test
    void headingsAreContextOnlyByDefault() {
        assertFalse(StructuralBlockIndexabilityPolicy.resolveIndexableHint(
                StructuralBlock.heading(1, "Title", 0)
        ));
    }

    @Test
    void respectsRowRoleHints() {
        StructuralBlock block = new StructuralBlock(
                "table_row",
                0,
                "x",
                0,
                Map.of("rowRole", "HEADER")
        );
        assertFalse(StructuralBlockIndexabilityPolicy.resolveIndexableHint(block));
    }

    @Test
    void respectsLayoutRoleHints() {
        StructuralBlock block = new StructuralBlock(
                "paragraph",
                0,
                "Page 1",
                0,
                Map.of("layoutRole", "footer")
        );
        assertFalse(StructuralBlockIndexabilityPolicy.resolveIndexableHint(block));
    }
}
