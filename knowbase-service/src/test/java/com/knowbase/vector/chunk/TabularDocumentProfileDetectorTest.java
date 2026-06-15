package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TabularDocumentProfileDetectorTest {

    @Test
    void detectsWeeklyReportFromFileName() {
        assertEquals(
                TabularDocumentProfile.WEEKLY_REPORT,
                TabularDocumentProfileDetector.detect("任意文本", "杜鹏飞-周报（9.15-9.19）.xlsx"));
    }

    @Test
    void detectsWeeklyReportFromText() {
        String text = "星图深海软件事业部工作周报\n序号\t类别\t工作内容\n1\t海图项目\t开发接口";
        assertEquals(TabularDocumentProfile.WEEKLY_REPORT, TabularDocumentProfileDetector.detect(text, null));
    }

    @Test
    void defaultsToGenericInventoryTable() {
        String text = "产品编号\t产品名称\nSKU-001\t鼠标";
        assertEquals(TabularDocumentProfile.GENERIC, TabularDocumentProfileDetector.detect(text, "inventory.xlsx"));
    }
}
