package com.knowbase.vector.embedding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbeddingInputFormatterTest {

    @Test
    void usesNomicTaskPrefixes_detectsNomicModels() {
        assertTrue(EmbeddingInputFormatter.usesNomicTaskPrefixes("nomic-embed-text"));
        assertTrue(EmbeddingInputFormatter.usesNomicTaskPrefixes("nomic-embed-text:latest"));
        assertTrue(EmbeddingInputFormatter.usesNomicTaskPrefixes("nomic_embed_text"));
        assertFalse(EmbeddingInputFormatter.usesNomicTaskPrefixes("snowflake-arctic-embed:110m"));
        assertFalse(EmbeddingInputFormatter.usesNomicTaskPrefixes("ibm/granite-embedding:278m"));
    }

    @Test
    void forSearchQuery_addsPrefixForNomicOnly() {
        assertEquals(
                "search_query: 周报主要内容汇总",
                EmbeddingInputFormatter.forSearchQuery("周报主要内容汇总", "nomic-embed-text"));
        assertEquals(
                "周报主要内容汇总",
                EmbeddingInputFormatter.forSearchQuery("周报主要内容汇总", "snowflake-arctic-embed:110m"));
    }

    @Test
    void forSearchDocument_addsPrefixForNomicOnly() {
        assertEquals(
                "search_document: 张三完成了接口开发",
                EmbeddingInputFormatter.forSearchDocument("张三完成了接口开发", "nomic-embed-text"));
        assertEquals(
                "张三完成了接口开发",
                EmbeddingInputFormatter.forSearchDocument("张三完成了接口开发", "mxbai-embed-large"));
    }

    @Test
    void doesNotDoublePrefix() {
        assertEquals(
                "search_query: 已有前缀",
                EmbeddingInputFormatter.forSearchQuery("search_query: 已有前缀", "nomic-embed-text"));
        assertEquals(
                "search_document: 已有前缀",
                EmbeddingInputFormatter.forSearchDocument("search_document: 已有前缀", "nomic-embed-text"));
    }

    @Test
    void forSearchDocuments_appliesToAllItems() {
        List<String> formatted = EmbeddingInputFormatter.forSearchDocuments(
                List.of("片段A", "片段B"), "nomic-embed-text");
        assertEquals(List.of("search_document: 片段A", "search_document: 片段B"), formatted);
    }
}
