package com.knowbase.ingestion.layout;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Downscales / compresses raster pages before VLM HTTP calls to stay within vLLM body limits.
 */
public final class VisionPageImageSupport {

    private static final int MAX_EDGE_PX = 1280;
    private static final float JPEG_QUALITY = 0.82f;
    private static final int TARGET_MAX_BYTES = 400_000;

    public record PreparedImage(byte[] bytes, String mimeType) {
    }

    private VisionPageImageSupport() {
    }

    public static PreparedImage prepareForVlm(byte[] imageBytes, String mimeType) {
        if (imageBytes == null || imageBytes.length == 0) {
            return new PreparedImage(imageBytes, mimeType == null ? "image/png" : mimeType);
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                return new PreparedImage(imageBytes, normalizeMime(mimeType));
            }
            BufferedImage scaled = scaleToMaxEdge(image, MAX_EDGE_PX);
            byte[] jpegBytes = writeJpeg(scaled);
            if (jpegBytes.length > TARGET_MAX_BYTES) {
                scaled = scaleToMaxEdge(image, 960);
                jpegBytes = writeJpeg(scaled);
            }
            return new PreparedImage(jpegBytes, "image/jpeg");
        } catch (IOException exception) {
            return new PreparedImage(imageBytes, normalizeMime(mimeType));
        }
    }

    private static String normalizeMime(String mimeType) {
        return mimeType == null || mimeType.isBlank() ? "image/png" : mimeType;
    }

    private static BufferedImage scaleToMaxEdge(BufferedImage image, int maxEdgePx) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longest = Math.max(width, height);
        if (longest <= maxEdgePx) {
            return image;
        }
        double scale = maxEdgePx / (double) longest;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        Image scaled = image.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage output = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();
        return output;
    }

    private static byte[] writeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            ByteArrayOutputStream fallback = new ByteArrayOutputStream();
            ImageIO.write(image, "png", fallback);
            return fallback.toByteArray();
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);
            }
            writer.setOutput(ImageIO.createImageOutputStream(output));
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return output.toByteArray();
    }
}
