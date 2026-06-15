package com.knowbase.vector.chunk;

import com.knowbase.pipeline.chunk.PipelineChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabularSectionContextInjectorTest {

    private static final String SAMPLE = """
            星图深海软件事业部工作周报
            部门\t软件事业部\t\t姓名\t杜鹏飞
            2025年9月15日--9月19日
            序号\t类别\t工作内容\t责任人
            1\t海图项目\t开发服务订阅的消息触发\t杜鹏飞
            2\t海图项目\t接入运维工具服务\t杜鹏飞""";

    @Test
    void injectsPrefixIntoPipelineChunks() {
        List<PipelineChunk> chunks = List.of(
                PipelineChunk.leaf("1\t海图项目\t开发服务订阅的消息触发\t杜鹏飞"),
                PipelineChunk.leaf("2\t海图项目\t接入运维工具服务\t杜鹏飞"));

        List<PipelineChunk> injected = TabularSectionContextInjector.inject(chunks, SAMPLE);

        assertEquals(2, injected.size());
        assertTrue(injected.get(0).content().startsWith("【杜鹏飞·工作周报·2025年9月15日--9月19日】"));
        assertTrue(injected.get(1).content().contains("2\t海图项目\t接入运维工具服务"));
    }

    @Test
    void injectsGenericPrefixWhenFileNameProvided() {
        String generic = """
                产品编号\t产品名称
                SKU-001\t鼠标""";
        List<PipelineChunk> chunks = List.of(PipelineChunk.leaf("SKU-001\t鼠标"));
        List<PipelineChunk> injected =
                TabularSectionContextInjector.inject(chunks, generic, "parts.xlsx");
        assertTrue(injected.get(0).content().startsWith("【表格·parts.xlsx】"));
    }

    @Test
    void headerOnlyChunkRemainsUnchangedUntilFiltered() {
        List<PipelineChunk> chunks = List.of(
                PipelineChunk.leaf("序号\t类别\t工作内容\t责任人"));

        List<PipelineChunk> injected = TabularSectionContextInjector.inject(chunks, SAMPLE);

        assertEquals("序号\t类别\t工作内容\t责任人", injected.get(0).content());
    }
}
