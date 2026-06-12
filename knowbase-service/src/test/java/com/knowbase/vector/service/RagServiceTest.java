package com.knowbase.vector.service;

import com.knowbase.library.dto.VectorLibraryResponse;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.vector.client.OllamaChatClient;
import com.knowbase.vector.config.OllamaProperties;
import com.knowbase.vector.config.RagProperties;
import com.knowbase.vector.config.RetrievalProperties;
import com.knowbase.vector.retrieval.RagRetrievalCache;
import com.knowbase.library.service.VectorLibraryService;
import com.knowbase.vector.dto.RagChatMessage;
import com.knowbase.vector.dto.RagChatRequest;
import com.knowbase.vector.dto.SearchHit;
import com.knowbase.vector.dto.RagSearchTrace;
import com.knowbase.vector.dto.SearchResponse;
import com.knowbase.vector.rag.RagPromptBuilder;
import com.knowbase.vector.mapper.DocumentChunkMapper;
import com.knowbase.vector.rag.RagQueryRewriteService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorSearchService searchService;
    @Mock
    private OllamaChatClient chatClient;
    @Mock
    private RagPromptBuilder promptBuilder;
    @Mock
    private DocMetadataStore docMetadataStore;
    @Mock
    private VectorLibraryService vectorLibraryService;
    @Mock
    private DocumentChunkMapper documentChunkMapper;

    private static RagSearchTrace traceWithHits(List<SearchHit> hits) {
        return new RagSearchTrace(hits, hits, false, null, false);
    }

    private static RagSearchTrace emptyTrace() {
        return new RagSearchTrace(List.of(), List.of(), false, null, false);
    }

    private void mockSearchHits(List<SearchHit> hits) {
        when(searchService.searchForRagWithTrace(any(), any())).thenReturn(traceWithHits(hits));
        when(searchService.searchForRag(any(), any())).thenReturn(new SearchResponse(hits));
    }

    private RagService newService(RagProperties props, OllamaProperties ollama) {
        RetrievalProperties retrievalProps = new RetrievalProperties();
        retrievalProps.setCacheEnabled(false);
        retrievalProps.setQueryRewriteEnabled(false);
        RagQueryRewriteService queryRewriteService = new RagQueryRewriteService(
                chatClient, ollama, retrievalProps);
        com.knowbase.library.service.LibraryConfigResolver libraryConfigResolver =
                org.mockito.Mockito.mock(com.knowbase.library.service.LibraryConfigResolver.class);
        org.mockito.Mockito.lenient()
                .when(libraryConfigResolver.retrievalFor(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.knowbase.library.config.RetrievalRulesSettings());
        com.knowbase.pipeline.config.ChunkProfileService chunkProfileService =
                org.mockito.Mockito.mock(com.knowbase.pipeline.config.ChunkProfileService.class);
        org.mockito.Mockito.lenient()
                .when(chunkProfileService.isPrimaryProfile(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);
        RagRetrievalService retrievalService = new RagRetrievalService(
                searchService,
                new RagRetrievalCache(retrievalProps),
                props,
                promptBuilder,
                docMetadataStore,
                queryRewriteService,
                libraryConfigResolver,
                chunkProfileService);
        return new RagService(
                props,
                ollama,
                retrievalService,
                chatClient,
                promptBuilder,
                docMetadataStore,
                vectorLibraryService,
                documentChunkMapper,
                libraryConfigResolver,
                chunkProfileService);
    }

    @Test
    void noHitsReturnsNotFoundWithoutLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        when(searchService.searchForRagWithTrace(any(), any())).thenReturn(emptyTrace());

        var response = service.chat(
                new RagChatRequest(
                        VectorLibraryService.DEFAULT_LIBRARY_ID, "demo", "未知问题", 5, null, null, null));

        assertFalse(response.found());
        assertFalse(response.usedLlm());
        assertFalse(response.conversational());
        assertTrue(response.answer().startsWith("未找到"));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void conversationalQuerySkipsSearch() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        ollama.setChatModel("test-chat-model");
        RagService service = newService(props, ollama);
        when(chatClient.chat(anyString(), any(), anyString(), any())).thenReturn("你好，我是知识库助手。");

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "你好，你是什么模型呢？",
                5,
                null,
                null,
                null,
                null));

        assertTrue(response.conversational());
        assertTrue(response.usedLlm());
        assertFalse(response.found());
        assertTrue(response.answer().contains("助手"));
        verify(searchService, never()).searchForRagWithTrace(any(), any());
    }

    @Test
    void chatWithHistoryUsesLlmAndReturnsMetadata() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "向量检索用于问答",
                0.88);
        mockSearchHits(List.of(hit));
        when(docMetadataStore.findFileNamesByDocIds(any())).thenReturn(Map.of());
        when(promptBuilder.buildUserMessage(anyString(), anyList(), anyList(), any())).thenReturn("user prompt");
        when(chatClient.chat(anyString(), anyList(), anyString(), any())).thenReturn("回答 [1]");

        List<RagChatMessage> history = List.of(
                new RagChatMessage("user", "什么是向量检索"),
                new RagChatMessage("assistant", "向量检索是… [1]"));
        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "它有什么用途？",
                5,
                null,
                null,
                null,
                history));

        assertTrue(response.found());
        assertTrue(response.usedLlm());
        assertEquals(2, response.historyUsed());
        assertEquals("什么是向量检索 它有什么用途？", response.searchQuery());
        assertFalse(response.conversational());
        assertNotNull(response.retrievalTrace());
        assertEquals(1, response.retrievalTrace().hitCount());
        verify(chatClient).chat(anyString(), eq(history), eq("user prompt"), any());
    }

    @Test
    void weakRelevantHitsReturnWeakMatchWithoutLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "pgvector 向量数据库配置说明",
                0.75);
        mockSearchHits(List.of(hit));

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "2025年周报截止时间？",
                5,
                null,
                null,
                null,
                null));

        assertFalse(response.found());
        assertFalse(response.usedLlm());
        assertTrue(response.answer().contains("匹配度较低"));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void combinedEmployeeWorkQuestionUsesLlmNotListRule() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID docId = UUID.randomUUID();
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                "姓名\t杜鹏飞\n1\t海图项目\t任务\t\t45896\t杜鹏飞\t完成开发任务",
                0.55);
        mockSearchHits(List.of(hit));
        when(docMetadataStore.findFileNamesByDocIds(any())).thenReturn(
                Map.of(docId, "2025/杜鹏飞-周报（9.8-9.12）.xlsx"));
        when(promptBuilder.buildUserMessage(anyString(), anyList(), anyList(), any())).thenReturn("user prompt");
        when(chatClient.chat(anyString(), anyList(), anyString(), any())).thenReturn(
                "杜鹏飞：完成接口开发 [1]");

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "有哪些员工提交了周报？都汇报了哪些主要的工作内容？",
                5,
                null,
                null,
                null,
                null));

        assertTrue(response.usedLlm());
        assertTrue(response.found());
        assertFalse(response.answer().contains("更新日期"));
        verify(chatClient).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void employeeListQuestionUsesRuleBasedAnswerWithoutLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID docId = UUID.randomUUID();
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                "本周完成接口开发\n姓名\t杜鹏飞\n1\t海图项目\t任务\t\t45896\t杜鹏飞\t完成开发任务",
                0.55);
        mockSearchHits(List.of(hit));
        when(docMetadataStore.findFileNamesByDocIds(any())).thenReturn(
                Map.of(docId, "2025/杜鹏飞-周报（9.8-9.12）.xlsx"));

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "有哪些员工上传了周报材料？",
                5,
                null,
                null,
                null,
                null));

        assertFalse(response.usedLlm());
        assertTrue(response.found());
        assertTrue(response.answer().contains("杜鹏飞"));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void existenceQuestionUsesRuleBasedAnswerWithoutLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID docId = UUID.randomUUID();
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                docId,
                "demo",
                1,
                0,
                "姓名\t杜鹏飞\n1\t海图项目\t任务\t\t45896\t杜鹏飞\t完成相关任务\t\t已完成",
                0.52);
        mockSearchHits(List.of(hit));
        when(docMetadataStore.findFileNamesByDocIds(any())).thenReturn(
                Map.of(docId, "2025/杜鹏飞-周报（8.25-8.29）.xlsx"));

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "是否存在其他员工提交周报？",
                5,
                null,
                null,
                null,
                null));

        assertFalse(response.usedLlm());
        assertTrue(response.found());
        assertTrue(response.answer().contains("不存在其他员工"));
        assertTrue(response.answer().contains("杜鹏飞"));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void employeeCountQuestionUsesRuleBasedAnswerWithoutLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID docId1 = UUID.randomUUID();
        UUID docId2 = UUID.randomUUID();
        SearchHit hit1 = new SearchHit(
                UUID.randomUUID(), docId1, "demo", 1, 0,
                "部门负责人\t杜鹏飞", 0.68);
        SearchHit hit2 = new SearchHit(
                UUID.randomUUID(), docId2, "demo", 1, 0,
                "3\t海图项目\t出差", 0.66);
        when(docMetadataStore.findActiveFileNamesByLibrary(any(), any())).thenReturn(List.of(
                "2025/杜鹏飞-周报（8.25-8.29）.xlsx",
                "2025/杜鹏飞-周报（11.3-11.8）.xlsx"));

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "本库中有多少个人提交了周报材料",
                5,
                null,
                null,
                null,
                null));

        assertFalse(response.usedLlm());
        assertTrue(response.found());
        assertTrue(response.answer().contains("共有 1 人"));
        assertTrue(response.answer().contains("杜鹏飞"));
        assertFalse(response.answer().contains("fileName="));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void weeklySummaryUsesRuleBasedAnswerWithoutLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID docId = UUID.randomUUID();
        String rows = """
                1\t海图项目\t开发优化综合服务模块的后台接口\t\t45877\t杜鹏飞\t已完成\t\t已完成
                2\t海图项目\t配合主中心人员完成运维工具的适配\t\t45878\t杜鹏飞\t已完成\t\t已完成
                """;
        SearchHit hit = new SearchHit(UUID.randomUUID(), docId, "demo", 1, 3, rows, 0.58);
        mockSearchHits(List.of(hit));
        when(docMetadataStore.findFileNamesByDocIds(any())).thenReturn(
                Map.of(docId, "2025/杜鹏飞-周报（8.4-8.8）.xlsx"));

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "周报主要内容汇总？",
                15,
                null,
                null,
                null,
                null));

        assertTrue(response.found());
        assertFalse(response.usedLlm());
        assertTrue(response.answer().contains("开发优化综合服务模块"));
        assertFalse(response.answer().contains("fileName="));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void libraryStatsQuestionUsesMetadataWithoutSearch() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID libraryId = VectorLibraryService.DEFAULT_LIBRARY_ID;
        when(vectorLibraryService.get(libraryId)).thenReturn(new VectorLibraryResponse(
                libraryId,
                "demo",
                "测试库",
                "",
                LibraryStatus.ACTIVE,
                com.knowbase.library.support.LibraryConfigViewMapper.toView(new VectorLibraryConfig()),
                20,
                78,
                Instant.now(),
                Instant.now()));

        var response = service.chat(new RagChatRequest(
                libraryId, "demo", "本知识库有多少文档与切片数据？", 5, null, null, null, null));

        assertTrue(response.found());
        assertFalse(response.usedLlm());
        assertEquals(0, response.retrievedCount());
        assertTrue(response.answer().contains("20 份文档"));
        assertTrue(response.answer().contains("78 个向量切片"));
        verify(searchService, never()).searchForRagWithTrace(any(), any());
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void libraryStatsQuestionWithTrailingPunctuation() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID libraryId = VectorLibraryService.DEFAULT_LIBRARY_ID;
        when(vectorLibraryService.get(libraryId)).thenReturn(new VectorLibraryResponse(
                libraryId,
                "demo",
                "测试库",
                "",
                LibraryStatus.ACTIVE,
                com.knowbase.library.support.LibraryConfigViewMapper.toView(new VectorLibraryConfig()),
                3,
                12,
                Instant.now(),
                Instant.now()));

        var response = service.chat(new RagChatRequest(
                libraryId, "demo", "本知识库有多少个文档？", 5, null, null, null, null));

        assertTrue(response.found());
        assertFalse(response.usedLlm());
        assertEquals(0, response.retrievedCount());
        assertTrue(response.answer().contains("3 份文档"));
        verify(searchService, never()).searchForRagWithTrace(any(), any());
    }

    @Test
    void libraryPurposeQuestionUsesDescriptionWithoutSearch() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        UUID libraryId = VectorLibraryService.DEFAULT_LIBRARY_ID;
        when(vectorLibraryService.get(libraryId)).thenReturn(new VectorLibraryResponse(
                libraryId,
                "demo",
                "周报知识库",
                "存放各部门周报材料。",
                LibraryStatus.ACTIVE,
                com.knowbase.library.support.LibraryConfigViewMapper.toView(new VectorLibraryConfig()),
                5,
                20,
                Instant.now(),
                Instant.now()));

        var response = service.chat(new RagChatRequest(
                libraryId, "demo", "本库主要用于做什么？", 5, null, null, null, null));

        assertTrue(response.found());
        assertFalse(response.usedLlm());
        assertEquals(0, response.retrievedCount());
        assertTrue(response.answer().contains("存放各部门周报材料"));
        verify(searchService, never()).searchForRagWithTrace(any(), any());
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }

    @Test
    void deadlineQuestionWithoutExplicitSourceSkipsLlm() {
        RagProperties props = new RagProperties();
        OllamaProperties ollama = new OllamaProperties();
        RagService service = newService(props, ollama);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "2025年8月25日--8月29日 杜鹏飞 工作周报",
                0.88);
        mockSearchHits(List.of(hit));

        var response = service.chat(new RagChatRequest(
                VectorLibraryService.DEFAULT_LIBRARY_ID,
                "demo",
                "2025年周报截止时间？",
                5,
                null,
                null,
                null,
                null));

        assertFalse(response.found());
        assertFalse(response.usedLlm());
        assertTrue(response.answer().contains("报告周期"));
        verify(chatClient, never()).chat(anyString(), any(), anyString(), any());
    }
}
