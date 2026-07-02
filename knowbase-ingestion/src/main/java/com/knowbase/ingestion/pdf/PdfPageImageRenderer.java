package com.knowbase.ingestion.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders PDF pages to PNG bytes for vision-language parsing.
 */
public final class PdfPageImageRenderer {

    private static final float DEFAULT_DPI = 200f;

    private PdfPageImageRenderer() {
    }

    public record PageImage(int pageNumber, byte[] pngBytes, double pageWidth, double pageHeight) {
    }

    public static List<PageImage> render(byte[] pdfBytes, int maxPages) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            if (maxPages > 0) {
                pageCount = Math.min(pageCount, maxPages);
            }
            List<PageImage> images = new ArrayList<>(pageCount);
            for (int index = 0; index < pageCount; index++) {
                BufferedImage bufferedImage = renderer.renderImageWithDPI(index, DEFAULT_DPI, ImageType.RGB);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", output);
                PDPage page = document.getPage(index);
                images.add(new PageImage(
                        index + 1,
                        output.toByteArray(),
                        page.getMediaBox().getWidth(),
                        page.getMediaBox().getHeight()
                ));
            }
            return List.copyOf(images);
        } catch (IOException exception) {
            throw new IllegalStateException("PDF 页面渲染失败", exception);
        }
    }
}
