package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeeklyReportChunkHeuristicsTest {

    @Test
    void detectsTsvWorkRow() {
        String chunk = """
                序号\t类别\t工作内容\t责任人

                1\t海图项目\t调整生产环境影像服务\t杜鹏飞""";
        assertFalse(WeeklyReportChunkHeuristics.isHeaderOnlyChunk(chunk));
    }

    @Test
    void detectsKeyValueWorkRow() {
        String chunk = "序号: 1 | 类别: 海图项目 | 工作内容: 调整生产环境影像服务 | 责任人: 杜鹏飞";
        assertFalse(WeeklyReportChunkHeuristics.isHeaderOnlyChunk(chunk));
    }

    @Test
    void filtersHeaderOnlyChunk() {
        String chunk = "序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明";
        assertTrue(WeeklyReportChunkHeuristics.isHeaderOnlyChunk(chunk));
    }
}
