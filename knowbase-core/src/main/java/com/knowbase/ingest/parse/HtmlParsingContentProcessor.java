package com.knowbase.ingest.parse;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基于 Tika HTML 输出的表格、图片与公式处理。
 */
public final class HtmlParsingContentProcessor {

    private static final Pattern TABLE_PATTERN = Pattern.compile("(?is)<table\\b[^>]*>(.*?)</table>");
    private static final Pattern ROW_PATTERN = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>");
    private static final Pattern CELL_PATTERN = Pattern.compile("(?is)<t[dh]\\b[^>]*>(.*?)</t[dh]>");
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("(?is)<img\\b[^>]*>");
    private static final Pattern FIGURE_BLOCK_PATTERN = Pattern.compile("(?is)<figure\\b[^>]*>.*?</figure>");
    private static final Pattern PICTURE_BLOCK_PATTERN = Pattern.compile("(?is)<picture\\b[^>]*>.*?</picture>");
    private static final Pattern SVG_BLOCK_PATTERN = Pattern.compile("(?is)<svg\\b[^>]*>.*?</svg>");
    private static final Pattern MATH_BLOCK_PATTERN = Pattern.compile("(?is)<math\\b[^>]*>.*?</math>");
    private static final Pattern LATEX_ANNOTATION_PATTERN = Pattern.compile(
            "(?is)<annotation\\b[^>]*encoding=[\"'](?:application/x-tex|LaTeX)[\"'][^>]*>(.*?)</annotation>");
    private static final Pattern DATA_URI_PATTERN =
            Pattern.compile("(?is)\\bsrc=[\"']data:image/([^;\"']+);base64,([^\"']+)[\"']");
    private static final Pattern TAG_PATTERN = Pattern.compile("(?is)<[^>]+>");
    private static final Pattern BLOCK_BREAK_PATTERN =
            Pattern.compile("(?i)</(?:p|div|h[1-6]|li|tr|table|section|article|blockquote)>|<br\\s*/?>");
    private static final Pattern ATTR_PATTERN =
            Pattern.compile("(?is)\\b([a-zA-Z_:][-a-zA-Z0-9_:.]*)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");

    private HtmlParsingContentProcessor() {}

    public static String apply(String html, DocumentParseOptions options) {
        return apply(html, options, null);
    }

    public static String apply(String html, DocumentParseOptions options, EmbeddedImageOcr imageOcr) {
        if (html == null || html.isBlank()) {
            return "";
        }
        DocumentParseOptions effective = options != null ? options : DocumentParseOptions.disabled();
        String processed = html;
        processed = applyImageExtraction(processed, effective.imageExtraction(), imageOcr);
        processed = applyFormulaExtraction(processed, effective.formulaExtraction());
        processed = applyTableExtraction(processed, effective.tableExtraction());
        return htmlToPlainText(processed);
    }

    private static String applyImageExtraction(String html, ImageExtractionMode mode, EmbeddedImageOcr imageOcr) {
        if (mode == ImageExtractionMode.SKIP) {
            String withoutBlocks = FIGURE_BLOCK_PATTERN.matcher(html).replaceAll("");
            withoutBlocks = PICTURE_BLOCK_PATTERN.matcher(withoutBlocks).replaceAll("");
            withoutBlocks = SVG_BLOCK_PATTERN.matcher(withoutBlocks).replaceAll("");
            return IMG_TAG_PATTERN.matcher(withoutBlocks).replaceAll("");
        }

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (imgMatcher.find()) {
            String tag = imgMatcher.group();
            String replacement = buildImageCaption(tag, imageOcr);
            imgMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        imgMatcher.appendTail(buffer);
        String withCaptions = buffer.toString();
        withCaptions = FIGURE_BLOCK_PATTERN.matcher(withCaptions).replaceAll(match -> stripToCaption(match.group()));
        withCaptions = PICTURE_BLOCK_PATTERN.matcher(withCaptions).replaceAll(match -> stripToCaption(match.group()));
        withCaptions = SVG_BLOCK_PATTERN.matcher(withCaptions).replaceAll("");
        return withCaptions;
    }

    private static String buildImageCaption(String imgTag, EmbeddedImageOcr imageOcr) {
        String alt = extractAttribute(imgTag, "alt").strip();
        String ocrText = ocrEmbeddedImage(imgTag, imageOcr);
        if (!ocrText.isBlank()) {
            return "\n[图片: " + ocrText + "]\n";
        }
        if (!alt.isBlank()) {
            return "\n[图片: " + alt + "]\n";
        }
        return "\n[图片]\n";
    }

    private static String ocrEmbeddedImage(String imgTag, EmbeddedImageOcr imageOcr) {
        if (imageOcr == null) {
            return "";
        }
        Matcher dataUriMatcher = DATA_URI_PATTERN.matcher(imgTag);
        if (!dataUriMatcher.find()) {
            return "";
        }
        try {
            String subtype = dataUriMatcher.group(1);
            byte[] imageBytes = Base64.getDecoder().decode(dataUriMatcher.group(2));
            String mimeType = "image/" + subtype;
            String caption = imageOcr.captionFromImageBytes(imageBytes, mimeType);
            return caption == null ? "" : caption.strip();
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private static String stripToCaption(String blockHtml) {
        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(blockHtml);
        if (imgMatcher.find()) {
            return buildImageCaption(imgMatcher.group(), null);
        }
        String text = normalizeWhitespace(decodeEntities(stripTags(blockHtml)));
        if (text.isBlank()) {
            return "\n[图片]\n";
        }
        return "\n[图片: " + text + "]\n";
    }

    private static String applyFormulaExtraction(String html, FormulaExtractionMode mode) {
        if (mode == FormulaExtractionMode.SKIP) {
            return MATH_BLOCK_PATTERN.matcher(html).replaceAll("");
        }

        Matcher mathMatcher = MATH_BLOCK_PATTERN.matcher(html);
        StringBuffer buffer = new StringBuffer();
        while (mathMatcher.find()) {
            String mathBlock = mathMatcher.group();
            String replacement = toLatexSnippet(mathBlock);
            mathMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        mathMatcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String toLatexSnippet(String mathBlock) {
        Matcher annotationMatcher = LATEX_ANNOTATION_PATTERN.matcher(mathBlock);
        if (annotationMatcher.find()) {
            String latex = normalizeWhitespace(decodeEntities(stripTags(annotationMatcher.group(1))));
            if (!latex.isBlank()) {
                return "\n$" + latex + "$\n";
            }
        }
        String fallback = normalizeWhitespace(decodeEntities(stripTags(mathBlock)));
        if (fallback.isBlank()) {
            return "";
        }
        return "\n$" + fallback + "$\n";
    }

    private static String applyTableExtraction(String html, TableExtractionMode mode) {
        if (mode == TableExtractionMode.SKIP) {
            return TABLE_PATTERN.matcher(html).replaceAll("");
        }
        if (mode == TableExtractionMode.STRUCTURED) {
            Matcher tableMatcher = TABLE_PATTERN.matcher(html);
            StringBuffer buffer = new StringBuffer();
            while (tableMatcher.find()) {
                String markdown = tableToMarkdown(tableMatcher.group(1));
                String replacement = markdown.isBlank() ? "" : "\n\n" + markdown + "\n\n";
                tableMatcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
            }
            tableMatcher.appendTail(buffer);
            return buffer.toString();
        }
        return html;
    }

    private static String tableToMarkdown(String tableInnerHtml) {
        List<List<String>> rows = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(tableInnerHtml);
        while (rowMatcher.find()) {
            List<String> cells = new ArrayList<>();
            Matcher cellMatcher = CELL_PATTERN.matcher(rowMatcher.group(1));
            while (cellMatcher.find()) {
                cells.add(normalizeCell(stripTags(cellMatcher.group(1))));
            }
            if (!cells.isEmpty()) {
                rows.add(cells);
            }
        }
        if (rows.isEmpty()) {
            return "";
        }

        int columnCount = rows.stream().mapToInt(List::size).max().orElse(0);
        if (columnCount == 0) {
            return "";
        }

        StringBuilder markdown = new StringBuilder();
        List<String> header = padRow(rows.get(0), columnCount);
        appendMarkdownRow(markdown, header);
        appendMarkdownSeparator(markdown, columnCount);

        for (int i = 1; i < rows.size(); i++) {
            appendMarkdownRow(markdown, padRow(rows.get(i), columnCount));
        }
        return markdown.toString().trim();
    }

    private static List<String> padRow(List<String> row, int columnCount) {
        List<String> padded = new ArrayList<>(row);
        while (padded.size() < columnCount) {
            padded.add("");
        }
        if (padded.size() > columnCount) {
            return padded.subList(0, columnCount);
        }
        return padded;
    }

    private static void appendMarkdownRow(StringBuilder markdown, List<String> cells) {
        markdown.append('|');
        for (String cell : cells) {
            markdown.append(' ').append(escapeMarkdownCell(cell)).append(" |");
        }
        markdown.append('\n');
    }

    private static void appendMarkdownSeparator(StringBuilder markdown, int columnCount) {
        markdown.append('|');
        for (int i = 0; i < columnCount; i++) {
            markdown.append(" --- |");
        }
        markdown.append('\n');
    }

    private static String escapeMarkdownCell(String cell) {
        return cell.replace('|', '｜').replace('\n', ' ').replace('\r', ' ').trim();
    }

    private static String extractAttribute(String tag, String name) {
        Matcher matcher = ATTR_PATTERN.matcher(tag);
        while (matcher.find()) {
            if (!name.equalsIgnoreCase(matcher.group(1))) {
                continue;
            }
            if (matcher.group(2) != null) {
                return matcher.group(2);
            }
            if (matcher.group(3) != null) {
                return matcher.group(3);
            }
            return matcher.group(4);
        }
        return "";
    }

    private static String htmlToPlainText(String html) {
        String withBreaks = BLOCK_BREAK_PATTERN.matcher(html).replaceAll("\n");
        String stripped = stripTags(withBreaks);
        return normalizeWhitespace(decodeEntities(stripped));
    }

    private static String stripTags(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return TAG_PATTERN.matcher(value).replaceAll("");
    }

    private static String normalizeCell(String value) {
        return normalizeWhitespace(decodeEntities(value));
    }

    private static String decodeEntities(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private static String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String[] lines = value.replace('\r', '\n').split("\n");
        StringBuilder normalized = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                if (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) != '\n') {
                    normalized.append('\n');
                }
                continue;
            }
            if (!normalized.isEmpty() && normalized.charAt(normalized.length() - 1) != '\n') {
                normalized.append('\n');
            }
            normalized.append(trimmed);
        }
        return normalized.toString().trim();
    }
}
