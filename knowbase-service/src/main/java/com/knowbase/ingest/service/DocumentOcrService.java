package com.knowbase.ingest.service;

import com.knowbase.ingest.config.OcrProperties;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentOcrService {

    private static final Logger log = LoggerFactory.getLogger(DocumentOcrService.class);

    private final OcrProperties properties;
    private final Object initLock = new Object();
    private volatile boolean initAttempted;
    private volatile boolean initSuccess;
    private volatile Tesseract tesseract;

    public DocumentOcrService(OcrProperties properties) {
        this.properties = properties;
    }

    public boolean isAvailable() {
        if (!properties.isEnabled()) {
            return false;
        }
        ensureInitialized();
        return initSuccess;
    }

    public String extract(byte[] bytes, String mimeType, String fileName, String language) {
        ensureInitialized();
        if (!initSuccess || tesseract == null) {
            throw new ParseException(
                    "OCR 已开启但引擎不可用：请在 application.yml 配置 ingest.ocr 并安装 Tesseract（含语言包）",
                    null);
        }
        String lang = language == null || language.isBlank() ? properties.getLanguage() : language;
        try {
            tesseract.setLanguage(lang);
            if (isPdf(mimeType, fileName)) {
                return ocrPdf(bytes);
            }
            if (isImage(mimeType, fileName)) {
                return ocrImage(bytes);
            }
            throw new ParseException("OCR 不支持该文件类型: " + fileName, null);
        } catch (TesseractException e) {
            throw new ParseException("OCR 识别失败: " + fileName, e);
        } catch (IOException e) {
            throw new ParseException("OCR 读取文件失败: " + fileName, e);
        }
    }

    /**
     * 对内嵌图片做 OCR；引擎不可用或识别失败时返回空字符串，不中断主解析流程。
     */
    public String tryOcrImage(byte[] bytes, String mimeType, String language) {
        if (!isAvailable() || bytes == null || bytes.length == 0) {
            return "";
        }
        if (!isImage(mimeType, "embedded." + mimeTypeSuffix(mimeType))) {
            return "";
        }
        try {
            String lang = language == null || language.isBlank() ? properties.getLanguage() : language;
            tesseract.setLanguage(lang);
            return ocrImage(bytes);
        } catch (Exception e) {
            log.debug("Embedded image OCR skipped: {}", e.getMessage());
            return "";
        }
    }

    private static String mimeTypeSuffix(String mimeType) {
        if (mimeType == null || !mimeType.contains("/")) {
            return "png";
        }
        return mimeType.substring(mimeType.indexOf('/') + 1);
    }

    private String resolveDataPath() {
        String configured = properties.getDataPath();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Path path = Paths.get(configured.trim()).toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("OCR tessdata directory not found: " + path);
        }
        Path chiSim = path.resolve("chi_sim.traineddata");
        if (!Files.isRegularFile(chiSim)) {
            throw new IllegalStateException(
                    "Missing chi_sim.traineddata under " + path + " — run scripts/setup-tesseract.ps1");
        }
        return path.toString();
    }

    private void ensureInitialized() {
        if (!properties.isEnabled()) {
            return;
        }
        if (initAttempted) {
            return;
        }
        synchronized (initLock) {
            if (initAttempted) {
                return;
            }
            initAttempted = true;
            try {
                Tesseract instance = new Tesseract();
                String dataPath = resolveDataPath();
                if (dataPath != null) {
                    instance.setDatapath(dataPath);
                }
                instance.setLanguage(properties.getLanguage());
                instance.setPageSegMode(1);
                tesseract = instance;
                initSuccess = true;
                log.info(
                        "Tesseract OCR engine initialized (language={}, datapath={})",
                        properties.getLanguage(),
                        dataPath != null ? dataPath : "tess4j-default");
            } catch (Exception e) {
                initSuccess = false;
                log.warn("Tesseract OCR unavailable: {}", e.getMessage());
            }
        }
    }

    private String ocrPdf(byte[] bytes) throws IOException, TesseractException {
        try (PDDocument document = PDDocument.load(bytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();
            int pages = Math.min(totalPages, Math.max(1, properties.getMaxPdfPages()));
            if (totalPages > pages) {
                log.warn("PDF has {} pages, OCR limited to first {}", totalPages, pages);
            }
            StringBuilder sb = new StringBuilder();
            int dpi = Math.max(72, properties.getPdfRenderDpi());
            for (int i = 0; i < pages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                String pageText = tesseract.doOCR(image);
                if (pageText != null && !pageText.isBlank()) {
                    if (!sb.isEmpty()) {
                        sb.append("\n\n");
                    }
                    sb.append(pageText.trim());
                }
            }
            return sb.toString().trim();
        }
    }

    private String ocrImage(byte[] bytes) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new ParseException("无法读取图片用于 OCR", null);
        }
        String text = tesseract.doOCR(image);
        return text == null ? "" : text.trim();
    }

    private static boolean isPdf(String mimeType, String fileName) {
        if (mimeType != null && "application/pdf".equalsIgnoreCase(mimeType)) {
            return true;
        }
        return fileName != null && fileName.toLowerCase().endsWith(".pdf");
    }

    private static boolean isImage(String mimeType, String fileName) {
        if (mimeType != null && mimeType.toLowerCase().startsWith("image/")) {
            return true;
        }
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".tif")
                || lower.endsWith(".tiff")
                || lower.endsWith(".bmp")
                || lower.endsWith(".gif");
    }
}
