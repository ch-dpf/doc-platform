package com.knowbase.pipeline.content;

/**
 * 解析后文本结构探测点：在 MIME/族群基线之上产出 {@link ContentSignals}。
 */
public interface ContentSignalsDetector {

    /**
     * 对已抽取（尚未分块）的纯文本做轻量启发式探测。
     *
     * @param family   MIME 映射得到的族群
     * @param mimeType 原始 MIME，可为 null
     * @param text     解析后文本，null/blank 时返回 empty signals
     */
    ContentSignals detect(ContentFamily family, String mimeType, String text);
}
