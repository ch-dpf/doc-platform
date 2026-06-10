package com.knowbase.ingest.parse;

/**
 * 对文档内嵌图片执行 OCR，供图片提取规则使用。
 */
@FunctionalInterface
public interface EmbeddedImageOcr {

    String captionFromImageBytes(byte[] imageBytes, String mimeType);
}
