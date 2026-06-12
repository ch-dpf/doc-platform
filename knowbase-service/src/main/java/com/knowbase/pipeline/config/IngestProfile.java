package com.knowbase.pipeline.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
/**
 * v2 采集级覆盖：持久化在 doc_metadata.ingest_profile_json，与 semantic documentMetadata 分离。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IngestProfile {

    private ParsingRulesSettings parsing;
    private CleaningRulesSettings cleaning;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private Integer minParagraphLength;

    public ParsingRulesSettings getParsing() {
        return parsing;
    }

    public void setParsing(ParsingRulesSettings parsing) {
        this.parsing = parsing;
    }

    public CleaningRulesSettings getCleaning() {
        return cleaning;
    }

    public void setCleaning(CleaningRulesSettings cleaning) {
        this.cleaning = cleaning;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getMinParagraphLength() {
        return minParagraphLength;
    }

    public void setMinParagraphLength(Integer minParagraphLength) {
        this.minParagraphLength = minParagraphLength;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return parsing == null
                && cleaning == null
                && chunkSize == null
                && chunkOverlap == null
                && minParagraphLength == null;
    }
}
