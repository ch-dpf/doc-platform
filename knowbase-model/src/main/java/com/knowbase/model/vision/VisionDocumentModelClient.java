package com.knowbase.model.vision;

import java.util.Map;

/**
 * Vision-language model for document page parsing (e.g. PaddleOCR-VL via Ollama).
 */
public interface VisionDocumentModelClient {

    String modelName();

    /**
     * Recognize text and layout from a single page image.
     *
     * @param imageBytes page raster (typically PNG)
     * @param mimeType   image MIME type
     * @param pageNumber 1-based page index
     * @param options    optional parse hints from document metadata
     * @return markdown-like page content
     */
    String recognizePage(byte[] imageBytes, String mimeType, int pageNumber, Map<String, Object> options);
}
