package com.docplatform.vector.config;

import com.docplatform.vector.rag.RagAnswerTemplates;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = true;
    private int defaultTopK = 5;
    private double minScore = 0.0;
    private int maxContextChars = 6000;
    private int excerptMaxChars = 280;
    private String noHitAnswer = RagAnswerTemplates.NO_HIT;
    private String systemPrompt = """
            你是企业知识库问答助手。你只能使用用户消息中「参考资料」里的文字作答。
            硬性规则（必须遵守）：
            1. 禁止编造：不得使用参考资料以外的常识、推测或想象；参考资料未写明的信息一律不得输出。
            2. 有据作答：每个事实性陈述必须在句末标注引用编号，如 [1]、[2]，且编号必须对应该条参考资料。
            3. 资料不足时：若参考资料与问题无关、内容为空、或无法支撑完整回答，你必须且只能回复 exactly 这一句，不要添加任何其他文字：
            未找到：参考资料不足以回答该问题。
            4. 使用简体中文，条理清晰；不要输出参考资料原文以外的链接、代码或数据。
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
}
