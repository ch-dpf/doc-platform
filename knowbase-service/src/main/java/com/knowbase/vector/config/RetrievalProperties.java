package com.knowbase.vector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "retrieval")
public class RetrievalProperties {

    /** 混合检索 / Rerank 时相对 topK 的候选倍率。 */
    private int candidateMultiplier = 4;

    /** 单次检索最多拉取的候选片段数（向量与关键词各自上限）。 */
    private int maxCandidates = 50;

    /** RRF 融合常数 k（越大则排名靠后项权重衰减越慢）。 */
    private int rrfK = 60;

    /** Rerank 阶段最多重排的片段数。 */
    private int maxRerankCandidates = 30;

    /** 是否缓存 RAG 检索 Top-K（同库同问在 TTL 内复用）。 */
    private boolean cacheEnabled = true;

    /** 检索缓存 TTL（秒），默认 5 分钟。 */
    private int cacheTtlSeconds = 300;

    /** 是否用 LLM 将用户问题改写为检索友好查询（RAG 向量检索前）。 */
    private boolean queryRewriteEnabled = true;

    /** 改写后查询最大字符数，超出则回退原问句。 */
    private int queryRewriteMaxChars = 96;

    public int getCandidateMultiplier() {
        return candidateMultiplier;
    }

    public void setCandidateMultiplier(int candidateMultiplier) {
        this.candidateMultiplier = candidateMultiplier;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public int getRrfK() {
        return rrfK;
    }

    public void setRrfK(int rrfK) {
        this.rrfK = rrfK;
    }

    public int getMaxRerankCandidates() {
        return maxRerankCandidates;
    }

    public void setMaxRerankCandidates(int maxRerankCandidates) {
        this.maxRerankCandidates = maxRerankCandidates;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public int getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(int cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public boolean isQueryRewriteEnabled() {
        return queryRewriteEnabled;
    }

    public void setQueryRewriteEnabled(boolean queryRewriteEnabled) {
        this.queryRewriteEnabled = queryRewriteEnabled;
    }

    public int getQueryRewriteMaxChars() {
        return queryRewriteMaxChars;
    }

    public void setQueryRewriteMaxChars(int queryRewriteMaxChars) {
        this.queryRewriteMaxChars = queryRewriteMaxChars;
    }
}
