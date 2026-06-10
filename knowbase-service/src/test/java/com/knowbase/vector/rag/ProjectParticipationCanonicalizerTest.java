package com.knowbase.vector.rag;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectParticipationCanonicalizerTest {

    @Test
    void mergesShanghaiFbVariants() {
        LinkedHashSet<String> raw = new LinkedHashSet<>(List.of(
                "海图项目",
                "上海fb项目",
                "上海浮标项目",
                "FB项目",
                "上海项目"));
        LinkedHashSet<String> deduped = ProjectParticipationCanonicalizer.dedupe(raw, List.of());
        assertEquals(2, deduped.size());
        assertTrue(deduped.contains("海图项目"));
        assertTrue(deduped.contains("上海fb项目") || deduped.contains("上海浮标项目"));
    }

    @Test
    void respectsUserDeclaredAliasGroup() {
        LinkedHashSet<String> raw = new LinkedHashSet<>(List.of(
                "海图项目", "上海fb项目", "上海浮标项目", "FB项目", "上海项目"));
        Set<String> alias = Set.of("上海fb项目", "上海浮标项目", "FB项目", "上海项目");
        LinkedHashSet<String> deduped = ProjectParticipationCanonicalizer.dedupe(raw, List.of(alias));
        assertEquals(2, deduped.size());
        assertTrue(deduped.contains("海图项目"));
        assertTrue(deduped.contains("上海fb项目"));
    }
}
