package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DocumentProfileResolver {

    public DocumentProfile resolve(
            String sourceUri,
            String requestedProfileCode,
            List<DocumentProfile> profiles
    ) {
        if (profiles == null || profiles.isEmpty()) {
            throw new IllegalStateException("知识库未配置文档 Profile");
        }
        if (requestedProfileCode != null && !requestedProfileCode.isBlank()) {
            return profiles.stream()
                    .filter(profile -> profile.code().equals(requestedProfileCode))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("文档 Profile 不存在: " + requestedProfileCode));
        }
        ContentFamily family = detectContentFamily(sourceUri);
        String preferredCode = preferredProfileCode(sourceUri, family);
        Optional<DocumentProfile> codeMatch = profiles.stream()
                .filter(DocumentProfile::enabled)
                .filter(profile -> profile.code().equals(preferredCode))
                .findFirst();
        if (codeMatch.isPresent()) {
            return codeMatch.get();
        }
        Optional<DocumentProfile> exactMatch = profiles.stream()
                .filter(DocumentProfile::enabled)
                .filter(profile -> profile.contentFamily() == family)
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        Optional<DocumentProfile> tikaProfile = profiles.stream()
                .filter(DocumentProfile::enabled)
                .filter(profile -> "tika".equalsIgnoreCase(profile.parserCode()))
                .findFirst();
        if (tikaProfile.isPresent() && isRichBinary(sourceUri)) {
            return tikaProfile.get();
        }
        return profiles.stream()
                .filter(DocumentProfile::enabled)
                .min(Comparator.comparingInt(DocumentProfileResolver::fallbackPriority))
                .orElseGet(() -> profiles.getFirst());
    }

    public Map<String, Object> routingMetadata(String sourceUri, DocumentProfile profile) {
        return Map.of(
                "resolvedDocumentProfileCode", profile.code(),
                "resolvedParserCode", profile.parserCode(),
                "detectedContentFamily", detectContentFamily(sourceUri).name(),
                "detectedFileExtension", extension(sourceUri)
        );
    }

    private static ContentFamily detectContentFamily(String sourceUri) {
        String lower = normalize(sourceUri);
        if (lower.endsWith(".zip")) {
            return ContentFamily.RICH_TEXT;
        }
        if (isQaSource(lower)) {
            return ContentFamily.PLAIN_TEXT;
        }
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv")) {
            return ContentFamily.STRUCTURED_TABLE;
        }
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) {
            return ContentFamily.PRESENTATION;
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return ContentFamily.WEB_PAGE;
        }
        if (lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp")
                || lower.endsWith(".webp")
                || lower.endsWith(".tif")
                || lower.endsWith(".tiff")) {
            return ContentFamily.IMAGE_TEXT;
        }
        if (lower.endsWith(".java")
                || lower.endsWith(".kt")
                || lower.endsWith(".js")
                || lower.endsWith(".ts")
                || lower.endsWith(".vue")
                || lower.endsWith(".py")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".json")
                || lower.endsWith(".xml")
                || lower.endsWith(".properties")) {
            return ContentFamily.CODE_OR_CONFIG;
        }
        if (lower.endsWith(".txt") || lower.startsWith("inline:text:")) {
            return ContentFamily.PLAIN_TEXT;
        }
        return ContentFamily.RICH_TEXT;
    }

    private static String preferredProfileCode(String sourceUri, ContentFamily family) {
        String lower = normalize(sourceUri);
        if (lower.endsWith(".zip")) {
            return "default_zip_bundle";
        }
        if (isQaSource(lower)) {
            return "default_faq";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "default_markdown";
        }
        if (lower.endsWith(".docx")) {
            return "default_docx";
        }
        if (lower.endsWith(".pdf")) {
            return "default_pdf";
        }
        if (lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".bmp")
                || lower.endsWith(".webp")
                || lower.endsWith(".tif")
                || lower.endsWith(".tiff")) {
            return "default_image";
        }
        if (lower.endsWith(".html") || lower.endsWith(".htm")) {
            return "default_web_page";
        }
        return switch (family) {
            case PLAIN_TEXT -> "default_text";
            case STRUCTURED_TABLE -> "default_table";
            case PRESENTATION -> "default_presentation";
            case IMAGE_TEXT, SCANNED_DOCUMENT -> "default_scanned_document";
            case WEB_PAGE -> "default_web_page";
            case CODE_OR_CONFIG -> "default_code_or_config";
            case RICH_TEXT -> "default_rich_text";
        };
    }

    private static int fallbackPriority(DocumentProfile profile) {
        if ("default_markdown".equals(profile.code())) {
            return 10;
        }
        if ("default_docx".equals(profile.code()) || "default_pdf".equals(profile.code())) {
            return 12;
        }
        if ("default_rich_text".equals(profile.code())) {
            return 20;
        }
        if ("tika".equalsIgnoreCase(profile.parserCode())) {
            return 30;
        }
        return 40;
    }

    private static boolean isRichBinary(String sourceUri) {
        ContentFamily family = detectContentFamily(sourceUri);
        return family == ContentFamily.RICH_TEXT
                || family == ContentFamily.STRUCTURED_TABLE
                || family == ContentFamily.PRESENTATION
                || family == ContentFamily.WEB_PAGE;
    }

    private static String extension(String sourceUri) {
        String lower = normalize(sourceUri);
        int dot = lower.lastIndexOf('.');
        if (dot < 0 || dot == lower.length() - 1) {
            return "";
        }
        return lower.substring(dot + 1);
    }

    private static boolean isQaSource(String lower) {
        if (!(lower.endsWith(".csv") || lower.endsWith(".xls") || lower.endsWith(".xlsx"))) {
            return false;
        }
        return lower.contains("faq")
                || lower.contains("qa")
                || lower.contains("问答")
                || lower.contains("question");
    }

    private static String normalize(String sourceUri) {
        return sourceUri == null ? "" : sourceUri.toLowerCase(Locale.ROOT);
    }
}
