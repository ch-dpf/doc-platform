package com.knowbase.vector.rag;

import com.knowbase.library.dto.VectorLibraryResponse;

import java.util.regex.Pattern;

/** 基于知识库元数据（非向量片段）回答文档数、切片数等统计问题。 */
public final class RagLibraryStatsSupport {

    private static final Pattern ASK_DOCUMENTS = Pattern.compile(
            "文档|文件|份数|入库文档", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASK_CHUNKS = Pattern.compile(
            "切片|分块|chunk|片段|向量块|向量片段", Pattern.CASE_INSENSITIVE);

    private RagLibraryStatsSupport() {}

    public static String formatAnswer(VectorLibraryResponse library, String question) {
        String name = library.name() != null && !library.name().isBlank()
                ? library.name()
                : "当前知识库";
        boolean askDocs = ASK_DOCUMENTS.matcher(question).find();
        boolean askChunks = ASK_CHUNKS.matcher(question).find();
        if (!askDocs && !askChunks) {
            askDocs = true;
            askChunks = true;
        }
        if (askDocs && askChunks) {
            return "知识库「" + name + "」当前共有 "
                    + library.documentCount() + " 份文档、"
                    + library.chunkCount() + " 个向量切片（分块）。"
                    + "（统计来自知识库元数据）";
        }
        if (askDocs) {
            return "知识库「" + name + "」当前共有 "
                    + library.documentCount() + " 份文档。（统计来自知识库元数据）";
        }
        return "知识库「" + name + "」当前共有 "
                + library.chunkCount() + " 个向量切片（分块）。（统计来自知识库元数据）";
    }

    /** 基于知识库名称与简介回答用途类问题。 */
    public static String formatPurposeAnswer(VectorLibraryResponse library) {
        String name = library.name() != null && !library.name().isBlank()
                ? library.name()
                : "当前知识库";
        String description = library.description() != null ? library.description().strip() : "";
        if (!description.isBlank()) {
            return "知识库「" + name + "」：" + description + "（来自知识库简介）";
        }
        return "知识库「" + name + "」暂未填写简介说明。"
                + "当前已入库 " + library.documentCount() + " 份文档，"
                + "请针对文档具体内容提问。（来自知识库元数据）";
    }
}
