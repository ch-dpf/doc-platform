package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentTextNormalizerTest {

    private final DocumentTextNormalizer normalizer = new DocumentTextNormalizer();

    @Test
    void normalizesControlCharsAndBlankLines() {
        ParsedDocument parsed = new ParsedDocument(
                "memory://doc.txt",
                "Doc",
                "Line1\u0001\u0002\n\n\n\nLine2\u3000Tail   \n",
                ContentFamily.PLAIN_TEXT,
                Map.of(),
                List.of(
                        StructuralBlock.paragraph("Line1\u0001\u0002\n\n\n\nLine2\u3000Tail   \n", 0)
                )
        );
        DocumentProfile profile = new DocumentProfile(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "default_text",
                ContentFamily.PLAIN_TEXT,
                "text",
                "paragraph_token_window",
                null,
                Map.of(),
                Map.of(),
                true
        );

        NormalizationResult result = normalizer.normalize(parsed, profile);

        assertTrue(result.appliedRules().contains("remove_control_chars"));
        assertTrue(result.appliedRules().contains("collapse_excess_blank_lines"));
        assertEquals("Line1\n\nLine2 Tail", result.document().text());
        assertEquals(1, result.normalizedBlockCount());
    }
}
