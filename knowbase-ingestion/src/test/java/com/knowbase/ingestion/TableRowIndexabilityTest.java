package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TableRowIndexabilityTest {

    @Test
    void narrowSheetsRequireThreePopulatedFields() {
        assertEquals(3, TableRowIndexability.resolveThreshold(4, 3));
        assertTrue(TableRowIndexability.isIndexable("A: Region,B: Q1,C: Q2", 4, 3));
    }

    @Test
    void wideSheetsSkipSparseLayoutRows() {
        assertEquals(4, TableRowIndexability.resolveThreshold(4, 12));
        assertFalse(TableRowIndexability.isIndexable("A: 工作周报标题", 4, 12));
        assertFalse(TableRowIndexability.isIndexable("A: 项目,B: 工作内容,C: 完成情况", 4, 12));
        assertTrue(TableRowIndexability.isIndexable(
                "A: 1,B: FB项目,C: 配合三方测试,F: 杜鹏飞,I: 已完成",
                4,
                12
        ));
    }
}
