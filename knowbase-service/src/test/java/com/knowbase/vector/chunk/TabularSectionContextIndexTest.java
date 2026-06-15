package com.knowbase.vector.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabularSectionContextIndexTest {

    private static final String DU_PENGFEI_SAMPLE = """
            周报3月
            \t星图深海软件事业部工作周报
            \t部门\t软件事业部\t\t姓名\t杜鹏飞\t\t\t更新日期\t2025.9.19
            \t2025年9月15日--9月19日
            \t序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明
            \t1\t海图项目\t开发服务订阅的消息触发、新门户的后台接口\t\t45919\t杜鹏飞\t完成开发任务\t\t已完成
            \t2\t海图项目\t接入运维工具服务并进行本地测试环境部署与适配\t\t45919\t杜鹏飞\t完成开发任务\t\t已完成
            \t3\t海图项目\t配合海图客户验收工作\t\t45919\t杜鹏飞\t完成相关任务\t\t已完成
            \t星图深海软件事业部周工作计划
            \t部门\t软件事业部\t\t部门负责人\t杜鹏飞\t\t\t更新日期\t2025.9.19
            \t9.22-9.26
            \t序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明
            \t1\t海图项目\t分析报告自动生成模板定义改造\t\t45926\t杜鹏飞\t完成开发任务\t\t待开展
            \t2\t海图项目\t验收测试问题整改\t\t45926\t杜鹏飞\t完成开发任务\t\t待开展
            \t3\t海图项目\t维护升级部署生产环境服务\t\t45926\t杜鹏飞\t完成相关任务\t\t待开展

            Sheet3""";

    @Test
    void injectsWeeklyReportSectionPrefixForDataChunk() {
        TabularSectionContextIndex index = TabularSectionContextIndex.parse(DU_PENGFEI_SAMPLE);

        String chunk = "1\t海图项目\t开发服务订阅的消息触发、新门户的后台接口\t\t45919\t杜鹏飞\t完成开发任务\t\t已完成";
        String injected = index.injectPrefix(chunk);

        assertTrue(injected.startsWith("【杜鹏飞·工作周报·2025年9月15日--9月19日】"));
        assertTrue(injected.contains("列：序号|类别|工作内容|计划完成时间|责任人|执行要求|执行情况|说明"));
        assertTrue(injected.contains(chunk));
    }

    @Test
    void injectsPlanSectionPrefixForSecondTableRows() {
        TabularSectionContextIndex index = TabularSectionContextIndex.parse(DU_PENGFEI_SAMPLE);

        String chunk = "1\t海图项目\t分析报告自动生成模板定义改造\t\t45926\t杜鹏飞\t完成开发任务\t\t待开展";
        String injected = index.injectPrefix(chunk);

        assertTrue(injected.startsWith("【杜鹏飞·周工作计划·9.22-9.26】"));
        assertTrue(injected.contains(chunk));
    }

    @Test
    void injectsGenericTablePrefixWithFileNameOnly() {
        String generic = """
                产品编号\t产品名称\t单价
                SKU-001\t无线鼠标\t99""";
        TabularSectionContextIndex index = TabularSectionContextIndex.parse(generic, "inventory.xlsx");

        String injected = index.injectPrefix("SKU-001\t无线鼠标\t99");

        assertTrue(injected.startsWith("【表格·inventory.xlsx】"));
        assertTrue(injected.contains("列：产品编号|产品名称|单价"));
        assertFalse(injected.contains("工作周报"));
    }

    @Test
    void skipsDoubleInjection() {
        TabularSectionContextIndex index = TabularSectionContextIndex.parse(DU_PENGFEI_SAMPLE);
        String once = index.injectPrefix("1\t海图项目\t配合海图客户验收工作\t\t45919\t杜鹏飞\t完成相关任务\t\t已完成");
        String twice = index.injectPrefix(once);
        assertTrue(twice.startsWith("【"));
        assertFalse(twice.startsWith("【", twice.indexOf('\n')));
    }
}
