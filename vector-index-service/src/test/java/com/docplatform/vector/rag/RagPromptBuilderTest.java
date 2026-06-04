package com.docplatform.vector.rag;

import com.docplatform.vector.config.RagProperties;
import com.docplatform.vector.dto.SearchHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPromptBuilderTest {

    @Test
    void buildsUserMessageWithReferences() {
        RagProperties props = new RagProperties();
        props.setMaxContextChars(10_000);
        RagPromptBuilder builder = new RagPromptBuilder(props);
        SearchHit hit = new SearchHit(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "demo",
                1,
                0,
                "pgvector 用于向量检索",
                0.92);
        String message = builder.buildUserMessage("什么是 pgvector？", List.of(hit));
        assertTrue(message.contains("【参考资料】"));
        assertTrue(message.contains("[1]"));
        assertTrue(message.contains("pgvector"));
        assertTrue(message.contains("什么是 pgvector"));
        assertTrue(message.contains("未找到"));
        assertTrue(message.contains("不得使用参考资料以外"));
    }

    @Test
    void truncatesLongExcerpt() {
        RagProperties props = new RagProperties();
        props.setExcerptMaxChars(10);
        RagPromptBuilder builder = new RagPromptBuilder(props);
        String excerpt = builder.excerpt("abcdefghijklmnopqrstuvwxyz");
        assertTrue(excerpt.endsWith("…"));
        assertTrue(excerpt.length() <= 11);
    }
}
