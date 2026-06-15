package com.knowbase.ingest.parse;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabularRowLinearizerTest {

    @Test
    void linearizesSimpleTsvTable() {
        String input = """
                序号\t类别\t工作内容\t责任人

                1\t海图项目\t调整生产环境影像服务\t杜鹏飞""";

        String out = TabularRowLinearizer.linearize(input);

        assertEquals(
                "序号: 1 | 类别: 海图项目 | 工作内容: 调整生产环境影像服务 | 责任人: 杜鹏飞",
                out);
    }

    @Test
    void preservesProseAroundTables() {
        String input = """
                周报3月

                星图深海软件事业部工作周报

                部门\t软件事业部\t\t姓名\t杜鹏飞

                序号\t类别\t工作内容\t责任人

                1\t海图项目\t调整生产环境影像服务\t杜鹏飞

                2\t海图项目\t部署 s57\t杜鹏飞""";

        String out = TabularRowLinearizer.linearize(input);

        assertTrue(out.contains("周报3月"));
        assertTrue(out.contains("星图深海软件事业部工作周报"));
        assertTrue(out.contains("部门\t软件事业部"));
        assertTrue(out.contains("序号: 1 | 类别: 海图项目"));
        assertTrue(out.contains("序号: 2 | 类别: 海图项目 | 工作内容: 部署 s57"));
    }

    @Test
    void linearizesMarkdownTable() {
        String input = """
                | 序号 | 类别 | 工作内容 | 责任人 |
                | --- | --- | --- | --- |
                | 1 | 海图项目 | 调整生产环境影像服务 | 杜鹏飞 |""";

        String out = TabularRowLinearizer.linearize(input);

        assertEquals(
                "序号: 1 | 类别: 海图项目 | 工作内容: 调整生产环境影像服务 | 责任人: 杜鹏飞",
                out);
    }

    @Test
    void looksTabularDetectsWeeklyReportSample() {
        assertTrue(TabularRowLinearizer.looksTabular("""
                序号\t类别\t工作内容

                1\t海图项目\t开发接口\t杜鹏飞"""));
        assertFalse(TabularRowLinearizer.looksTabular("纯文本段落，没有表格。"));
    }

    @Test
    void headerOnlyLineWithoutDataRowsIsKeptAsIs() {
        String input = "序号\t类别\t工作内容\t责任人";
        assertEquals(input, TabularRowLinearizer.linearize(input));
    }
}
