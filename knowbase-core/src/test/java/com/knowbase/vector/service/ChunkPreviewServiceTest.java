package com.knowbase.vector.service;

import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.ingest.support.DocumentCleaningService;
import com.knowbase.ingest.support.ParsedTextNormalizer;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.pipeline.chunk.LibraryChunkPipeline;
import com.knowbase.pipeline.config.ChunkProfileService;
import com.knowbase.pipeline.config.EffectiveConfigResolver;
import com.knowbase.pipeline.config.MimeTypePipelineDefaults;
import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.pipeline.content.ContentFamilyPipelineDefaults;
import com.knowbase.pipeline.content.DefaultContentSignalsDetector;
import com.knowbase.vector.config.ChunkingProperties;
import com.knowbase.vector.dto.ChunkPreviewRequest;
import com.knowbase.vector.dto.ChunkPreviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChunkPreviewServiceTest {

    @Mock
    private ParsedTextNormalizer textNormalizer;

    @Mock
    private DocumentCleaningService documentCleaningService;

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    @Mock
    private ChunkProfileService chunkProfileService;

    @Mock
    private DocMetadataStore docMetadataStore;

    private EffectiveConfigResolver effectiveConfigResolver;

    private ChunkPreviewService service;

    @BeforeEach
    void setUp() {
        ChunkingProperties defaults = new ChunkingProperties();
        ChunkingService chunkingService = new ChunkingService(defaults, null);
        OcrProperties ocrProperties = new OcrProperties();
        effectiveConfigResolver = new EffectiveConfigResolver(
                libraryConfigResolver,
                null,
                new MimeTypePipelineDefaults(new ContentFamilyPipelineDefaults(ocrProperties)),
                ocrProperties,
                new TextNormalizationProperties(),
                new DefaultContentSignalsDetector(),
                new ParserEngineRegistry());
        LibraryChunkPipeline pipeline = new LibraryChunkPipeline(
                chunkingService,
                textNormalizer,
                documentCleaningService,
                libraryConfigResolver,
                effectiveConfigResolver,
                docMetadataStore);
        service = new ChunkPreviewService(pipeline, defaults, effectiveConfigResolver, chunkProfileService);
        when(documentCleaningService.apply(any(), any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void previewUsesIndexingChunkFilterAndLibraryChunkParams() {
        String sample = """
                周报3月

                星图深海软件事业部工作周报

                部门\t软件事业部\t\t姓名\t杜鹏飞\t\t\t更新日期\t2025.8.1

                2025年7月28日--8月03日

                序号\t类别\t工作内容\t\t计划完成时间\t责任人\t执行要求\t\t执行情况\t说明

                1\t海图项目\t调整天津海图项目生产环境影像服务遥感影像数据切瓦片功能和气象水文NC服务的异常问题\t\t45866\t杜鹏飞\t功能正常运行\t\t已完成

                2\t海图项目\t开发并优化分析报告自动生成模块后台接口，封装与调整国遥数据接入模块后台接口\t\t45868\t杜鹏飞\t接口满足指标要求\t\t未完成\t第一点，部分功能接口仍在持续优化；

                第二点，国遥接口会进行调整与新开发。

                3\t海图项目\t部署并测试新引入的电子海图工具s57数据切瓦片功能\t\t45868\t杜鹏飞\t工具满足项目指标要求\t\t已完成

                4\t海图项目\t配合第三方测试单位人员，对综合服务板块、信息服务发布板块、自动报告生成板块进行说明与视频录制\t\t45870\t杜鹏飞\t保证客户要求的测试大纲节点\t\t已完成

                5\t海图项目\t多中心土井演示环境部署与联调\t\t45872\t杜鹏飞\t演示环境部署完成\t\t未完成\t周末在北京进行服务部署联调

                星图深海XXX部周工作计划

                部门\t\t\t部门负责人\t\t\t\t更新日期

                2025年 5月6日--5月9日

                Sheet3""";

        ChunkPreviewRequest request = new ChunkPreviewRequest(
                sample,
                500,
                120,
                80,
                1200,
                30,
                null,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                null);

        ChunkPreviewResponse response = service.preview(request);

        assertEquals(4, response.rawTotalChunks());
        assertEquals(1, response.filteredOutCount());
        assertEquals(3, response.totalChunks());
        assertEquals(3, response.chunks().size());
        assertTrue(response.chunks().stream().noneMatch(c -> c.content().contains("序号\t类别")));
        assertTrue(response.chunks().stream().anyMatch(c ->
                c.content().contains("第一点，部分功能接口仍在持续优化")
                        && c.content().contains("第二点，国遥接口会进行调整与新开发")));
        assertTrue(response.chunks().stream().noneMatch(c ->
                c.content().trim().startsWith("第二点，国遥接口")));
    }
}
