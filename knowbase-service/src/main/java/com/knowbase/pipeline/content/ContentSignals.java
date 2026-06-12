package com.knowbase.pipeline.content;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.knowbase.vector.chunk.ChunkingStrategy;

/**
 * 解析后文本的结构探测结果，供分块策略二次路由与入库可观测性。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentSignals {

    public static final int SHORT_DOCUMENT_CHARS = 2000;
    public static final int LONG_DOCUMENT_CHARS = 1500;
    public static final double HEADING_RATIO_THRESHOLD = 0.05;

    private ContentFamily contentFamily;
    private int textLength;
    private int headingLineCount;
    private double headingLineRatio;
    private boolean markdownHeadings;
    private boolean codeFences;
    private double tabularLineRatio;
    private boolean shortDocument;
    /** 内容信号触发的分块策略调整说明（可观测） */
    private String chunkingAdjustmentReason;
    private ChunkingStrategy adjustedChunkingStrategy;

    public ContentSignals() {
    }

    public static ContentSignals empty(ContentFamily family) {
        ContentSignals signals = new ContentSignals();
        signals.contentFamily = family != null ? family : ContentFamily.UNKNOWN;
        return signals;
    }

    public ContentFamily getContentFamily() {
        return contentFamily;
    }

    public void setContentFamily(ContentFamily contentFamily) {
        this.contentFamily = contentFamily;
    }

    public int getTextLength() {
        return textLength;
    }

    public void setTextLength(int textLength) {
        this.textLength = textLength;
    }

    public int getHeadingLineCount() {
        return headingLineCount;
    }

    public void setHeadingLineCount(int headingLineCount) {
        this.headingLineCount = headingLineCount;
    }

    public double getHeadingLineRatio() {
        return headingLineRatio;
    }

    public void setHeadingLineRatio(double headingLineRatio) {
        this.headingLineRatio = headingLineRatio;
    }

    public boolean isMarkdownHeadings() {
        return markdownHeadings;
    }

    public void setMarkdownHeadings(boolean markdownHeadings) {
        this.markdownHeadings = markdownHeadings;
    }

    public boolean isCodeFences() {
        return codeFences;
    }

    public void setCodeFences(boolean codeFences) {
        this.codeFences = codeFences;
    }

    public double getTabularLineRatio() {
        return tabularLineRatio;
    }

    public void setTabularLineRatio(double tabularLineRatio) {
        this.tabularLineRatio = tabularLineRatio;
    }

    public boolean isShortDocument() {
        return shortDocument;
    }

    public void setShortDocument(boolean shortDocument) {
        this.shortDocument = shortDocument;
    }

    public String getChunkingAdjustmentReason() {
        return chunkingAdjustmentReason;
    }

    public void setChunkingAdjustmentReason(String chunkingAdjustmentReason) {
        this.chunkingAdjustmentReason = chunkingAdjustmentReason;
    }

    public ChunkingStrategy getAdjustedChunkingStrategy() {
        return adjustedChunkingStrategy;
    }

    public void setAdjustedChunkingStrategy(ChunkingStrategy adjustedChunkingStrategy) {
        this.adjustedChunkingStrategy = adjustedChunkingStrategy;
    }

    public boolean isEmpty() {
        return textLength == 0
                && headingLineCount == 0
                && !markdownHeadings
                && !codeFences
                && tabularLineRatio == 0;
    }
}
