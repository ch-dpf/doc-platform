package com.docplatform.vector.rag;

/**
 * RAG 固定话术：无检索命中时不调用 LLM，避免模型编造。
 */
public final class RagAnswerTemplates {

    /** 检索无命中（或全部被 minScore 过滤）时的唯一回复 */
    public static final String NO_HIT =
            "未找到：当前知识库中未检索到与您问题相关的资料，无法作答。"
                    + "请确认文档已入库且索引状态为 INDEXED，或尝试更换问法。";

    /** 要求 LLM 在资料不足时使用的固定句式（须与 system/user Prompt 一致） */
    public static final String INSUFFICIENT_IN_PROMPT =
            "未找到：参考资料不足以回答该问题。";

    private RagAnswerTemplates() {
    }
}
