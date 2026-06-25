package com.knowbase.ingestion.table;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiLevelHeaderStackTest {

    @Test
    void buildsFlatAndPathHeaders() {
        MultiLevelHeaderStack stack = new MultiLevelHeaderStack();
        stack.pushHeaderRow(List.of("Quarter", "Quarter", "Quarter"));
        stack.pushHeaderRow(List.of("Region", "Q1", "Q2"));

        assertEquals("Region", stack.activeFlatHeaders(3)[0]);
        assertEquals(List.of("Quarter", "Region"), stack.headerPathForColumn(0, 3));
        assertEquals(List.of("Quarter", "Q1"), stack.headerPathForColumn(1, 3));
    }

    @Test
    void resetClearsHeaderContext() {
        MultiLevelHeaderStack stack = new MultiLevelHeaderStack();
        stack.pushHeaderRow(List.of("A", "B"));
        stack.reset();
        assertEquals(0, stack.headerRowCount());
        assertEquals("A", stack.columnKeys(2).get(0));
    }
}
