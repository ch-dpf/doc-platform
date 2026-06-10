package com.knowbase.ingest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ingest.ocr")
public class OcrProperties {

    /** 全局 OCR 引擎开关（需本机安装 Tesseract 并配置 tessdata） */
    private boolean enabled = false;

    /** tessdata 目录，空则使用 Tesseract 默认路径 */
    private String dataPath = "";

    /** 未指定库语言时的默认 Tesseract 语言包，如 chi_sim+eng */
    private String language = "chi_sim+eng";

    /** Tika 抽取字符数达到该阈值时跳过 OCR（已有文本层的 PDF 等） */
    private int minExtractedCharsToSkip = 32;

    /** 单份 PDF 最多 OCR 页数 */
    private int maxPdfPages = 50;

    /** PDF 渲染 DPI */
    private int pdfRenderDpi = 200;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getMinExtractedCharsToSkip() {
        return minExtractedCharsToSkip;
    }

    public void setMinExtractedCharsToSkip(int minExtractedCharsToSkip) {
        this.minExtractedCharsToSkip = minExtractedCharsToSkip;
    }

    public int getMaxPdfPages() {
        return maxPdfPages;
    }

    public void setMaxPdfPages(int maxPdfPages) {
        this.maxPdfPages = maxPdfPages;
    }

    public int getPdfRenderDpi() {
        return pdfRenderDpi;
    }

    public void setPdfRenderDpi(int pdfRenderDpi) {
        this.pdfRenderDpi = pdfRenderDpi;
    }
}
