package com.knowbase.vector.rag;

import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.domain.LibraryStatus;
import com.knowbase.library.dto.VectorLibraryResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RagLibraryStatsSupportTest {

    @Test
    void formatsDocumentAndChunkCounts() {
        VectorLibraryResponse lib = new VectorLibraryResponse(
                UUID.randomUUID(),
                "demo",
                "周报知识库",
                "",
                LibraryStatus.ACTIVE,
                new VectorLibraryConfig(),
                20,
                78,
                Instant.now(),
                Instant.now());

        String answer = RagLibraryStatsSupport.formatAnswer(lib, "本知识库有多少文档与切片数据？");
        assertTrue(answer.contains("20 份文档"));
        assertTrue(answer.contains("78 个向量切片"));
        assertTrue(answer.contains("周报知识库"));
    }

    @Test
    void formatsPurposeFromDescription() {
        VectorLibraryResponse lib = new VectorLibraryResponse(
                UUID.randomUUID(),
                "demo",
                "周报知识库",
                "存放各部门周报与月报，供检索与问答。",
                LibraryStatus.ACTIVE,
                new VectorLibraryConfig(),
                5,
                20,
                Instant.now(),
                Instant.now());

        String answer = RagLibraryStatsSupport.formatPurposeAnswer(lib);
        assertTrue(answer.contains("周报知识库"));
        assertTrue(answer.contains("存放各部门周报与月报"));
        assertTrue(answer.contains("知识库简介"));
    }

    @Test
    void formatsPurposeWhenDescriptionEmpty() {
        VectorLibraryResponse lib = new VectorLibraryResponse(
                UUID.randomUUID(),
                "demo",
                "测试库",
                "",
                LibraryStatus.ACTIVE,
                new VectorLibraryConfig(),
                3,
                0,
                Instant.now(),
                Instant.now());

        String answer = RagLibraryStatsSupport.formatPurposeAnswer(lib);
        assertTrue(answer.contains("暂未填写简介"));
        assertTrue(answer.contains("3 份文档"));
    }
}
