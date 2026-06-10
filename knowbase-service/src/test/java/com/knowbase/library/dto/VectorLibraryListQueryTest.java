package com.knowbase.library.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VectorLibraryListQueryTest {

    @Test
    void normalizesPageAndSize() {
        VectorLibraryListQuery q = new VectorLibraryListQuery("demo", null, null, 0, 0);
        assertEquals(1, q.page());
        assertEquals(20, q.size());
    }

    @Test
    void capsSizeAt100() {
        VectorLibraryListQuery q = new VectorLibraryListQuery("demo", "kw", "周报", 2, 500);
        assertEquals(2, q.page());
        assertEquals(100, q.size());
    }

    @Test
    void requiresTenantId() {
        assertThrows(IllegalArgumentException.class, () -> new VectorLibraryListQuery("  ", null, null, 1, 20));
    }
}
