package com.knowbase.vector.config;

import com.knowbase.vector.rag.RagAnswerTemplates;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    private int defaultTopK = 5;
    private double minScore = 0.0;
    private int maxContextChars = 6000;
    private int excerptMaxChars = 280;
    private int maxHistoryMessages = 20;
    private int maxHistoryMessageChars = 2000;
    private String noHitAnswer = RagAnswerTemplates.NO_HIT;
    private String systemPrompt = """
            你是企业知识库问答助手。你只能使用用户消息中「参考资料」里的文字作答。
            硬性规则（必须遵守）：
            1. 禁止编造：不得使用参考资料以外的常识、推测或想象；参考资料未写明的信息一律不得输出。
            2. 有据作答：每个事实性陈述必须在句末标注引用编号，如 [1]、[2]，且编号必须对应该条参考资料。
            3. 跨文档汇总（如「哪些员工」「主要工作内容」「是否存在其他员工」）：可综合多条参考资料归纳；人员与事项须来自片段中可见的姓名/正文；对「是否存在其他 X」类问题，可据全部片段中出现的不同姓名作是/否判断，不得回避可归纳结论。
            4. 资料不足时：若参考资料与问题无关、或完全未写理所问事实，你必须且只能回复 exactly 这一句，不要添加任何其他文字：
            未找到：参考资料中未明确记载该问题的答案。
            5. 问截止/期限/负责人等明确事实时：仅当参考资料中原样出现相关表述（如「提交截止」「截止日期」）才可作答；不得把周报/月报中的工作周期日期区间当作截止时间。
            6. 禁止先写「没有明确…」再猜测作答；无明文则只回复第 4 条的「未找到」句式。
            7. 使用简体中文，条理清晰；不要输出参考资料原文以外的链接、代码或数据。
            8. 多轮对话：用户可能在追问前文；可结合对话历史理解指代，但事实性内容仍必须来自本轮「参考资料」。
            """;
    private String conversationalSystemPrompt = """
            你是企业知识库智能问答助手，基于「选定知识库 + 向量检索（RAG）+ 大语言模型」为用户答疑。
            用户当前可能在打招呼，或询问你的身份、能力、所用模型等，请用简体中文友好、简洁地回答：
            1. 说明你是知识库问答助手，业务问题需基于已入库文档检索后作答，不要编造库内事实。
            2. 若被问「什么模型/谁」：说明对话生成由大语言模型 {{chatModel}} 完成；向量检索与 Embedding 模型由当前知识库配置决定。
            3. 可简短问候并引导用户就知识库内容提问；不要输出冗长的免责声明。
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public int getMaxContextChars() {
        return maxContextChars;
    }

    public void setMaxContextChars(int maxContextChars) {
        this.maxContextChars = maxContextChars;
    }

    public int getExcerptMaxChars() {
        return excerptMaxChars;
    }

    public void setExcerptMaxChars(int excerptMaxChars) {
        this.excerptMaxChars = excerptMaxChars;
    }

    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public void setMaxHistoryMessages(int maxHistoryMessages) {
        this.maxHistoryMessages = maxHistoryMessages;
    }

    public int getMaxHistoryMessageChars() {
        return maxHistoryMessageChars;
    }

    public void setMaxHistoryMessageChars(int maxHistoryMessageChars) {
        this.maxHistoryMessageChars = maxHistoryMessageChars;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getNoHitAnswer() {
        return noHitAnswer;
    }

    public void setNoHitAnswer(String noHitAnswer) {
        this.noHitAnswer = noHitAnswer;
    }

    public String getConversationalSystemPrompt() {
        return conversationalSystemPrompt;
    }

    public void setConversationalSystemPrompt(String conversationalSystemPrompt) {
        this.conversationalSystemPrompt = conversationalSystemPrompt;
    }
}
