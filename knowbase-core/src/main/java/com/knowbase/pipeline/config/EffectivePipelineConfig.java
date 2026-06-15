package com.knowbase.pipeline.config;

import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.library.config.TextNormalizationSettings;
import com.knowbase.pipeline.content.ContentFamily;
import com.knowbase.pipeline.content.ContentSignals;
import com.knowbase.vector.config.ChunkingProperties;

/**
 * 合并后的运行时管道配置：系统默认 → 库默认 → 族群/MIME 默认 → 内容信号 → 采集 ingest profile。
 */
public class EffectivePipelineConfig {

    private final boolean textNormalizationEnabled;
    private final TextNormalizationSettings normalization;
    private final CleaningRulesSettings cleaning;
    private final ChunkingProperties chunking;
    private final ParsingRulesSettings parsing;
    private final int pipelineConfigVersion;
    private final ContentFamily contentFamily;
    private final ContentSignals contentSignals;

    public EffectivePipelineConfig(
            boolean textNormalizationEnabled,
            TextNormalizationSettings normalization,
            CleaningRulesSettings cleaning,
            ChunkingProperties chunking,
            ParsingRulesSettings parsing,
            int pipelineConfigVersion) {
        this(
                textNormalizationEnabled,
                normalization,
                cleaning,
                chunking,
                parsing,
                pipelineConfigVersion,
                null,
                null);
    }

    public EffectivePipelineConfig(
            boolean textNormalizationEnabled,
            TextNormalizationSettings normalization,
            CleaningRulesSettings cleaning,
            ChunkingProperties chunking,
            ParsingRulesSettings parsing,
            int pipelineConfigVersion,
            ContentFamily contentFamily,
            ContentSignals contentSignals) {
        this.textNormalizationEnabled = textNormalizationEnabled;
        this.normalization = normalization;
        this.cleaning = cleaning;
        this.chunking = chunking;
        this.parsing = parsing;
        this.pipelineConfigVersion = pipelineConfigVersion;
        this.contentFamily = contentFamily;
        this.contentSignals = contentSignals;
    }

    public boolean isTextNormalizationEnabled() {
        return textNormalizationEnabled;
    }

    public TextNormalizationSettings normalization() {
        return normalization;
    }

    public CleaningRulesSettings cleaning() {
        return cleaning;
    }

    public ChunkingProperties chunking() {
        return chunking;
    }

    public ParsingRulesSettings parsing() {
        return parsing;
    }

    public int pipelineConfigVersion() {
        return pipelineConfigVersion;
    }

    public ContentFamily contentFamily() {
        return contentFamily;
    }

    public ContentSignals contentSignals() {
        return contentSignals;
    }
}
