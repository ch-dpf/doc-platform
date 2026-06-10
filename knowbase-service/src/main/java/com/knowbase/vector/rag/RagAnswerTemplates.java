package com.knowbase.vector.rag;

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
            "未找到：参考资料中未明确记载该问题的答案。请确认相关文档已入库并完成索引，或尝试更换问法。";

    /** 检索命中但与问题关键词匹配度低 */
    public static final String WEAK_MATCH =
            "未找到：已检索到部分文档片段，但与问题关键词匹配度较低，无法可靠作答。"
                    + "请确认文档已 INDEXED，或在知识库配置中开启混合检索 / 调低相似度阈值后重试。";

    /** 问截止/期限，但参考资料仅有报告周期、无提交截止明文 */
    public static final String NO_EXPLICIT_DEADLINE =
            "未找到：参考资料中未记载「提交截止/截止日期」等明确说明。"
                    + "文档中的报告周期（如某日至某日）仅代表周报覆盖的工作时段，不能当作提交截止时间。";

    private RagAnswerTemplates() {
    }
}
