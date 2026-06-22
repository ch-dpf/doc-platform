package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class DocumentTextNormalizer implements DocumentNormalizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]");
    private static final Pattern ZERO_WIDTH = Pattern.compile("[\\u200B-\\u200D\\uFEFF]");
    private static final Pattern EXCESS_BLANK_LINES = Pattern.compile("\\R{3,}");
    private static final Pattern TRAILING_SPACES = Pattern.compile("[ \\t]+\\R");
    private static final Pattern FULL_WIDTH_SPACE = Pattern.compile("\\u3000");
    private static final Pattern DEHYPHENATE = Pattern.compile("(?<=[\\p{L}])-\\R(?=[\\p{L}])");
    private static final Pattern STANDALONE_PAGE_NUMBER = Pattern.compile("(?m)^\\s*\\d{1,4}\\s*$");
    private static final Pattern PAGE_FOOTER = Pattern.compile("(?im)^\\s*(?:page\\s+\\d+|第\\s*\\d+\\s*页(?:\\s*/\\s*\\d+\\s*页)?)\\s*$");
    private static final Pattern BULLET_VARIANTS = Pattern.compile("(?m)^\\s*[•●◦▪▫]\\s+");
    private static final Pattern HTML_ENTITY = Pattern.compile("&(#\\d+|#x[0-9a-fA-F]+|[a-zA-Z]+);");
    private static final Pattern REPEATED_PUNCT = Pattern.compile("([。！？!?])\\1+");
    private static final Pattern SOFT_HYPHEN = Pattern.compile("\\u00AD");
    private static final Pattern NBSP = Pattern.compile("\\u00A0");
    private static final Pattern CJK_SPACE = Pattern.compile("([\\u4e00-\\u9fff])\\s+([\\u4e00-\\u9fff])");

    @Override
    public NormalizationResult normalize(ParsedDocument parsed, DocumentProfile documentProfile) {
        List<String> appliedRules = new ArrayList<>();
        NormalizationOptions options = NormalizationOptions.from(documentProfile);

        String normalizedText = normalizeText(parsed.text(), appliedRules, options);
        List<StructuralBlock> normalizedBlocks = new ArrayList<>();
        int rawBlockCount = parsed.blocks().size();
        int ordinal = 0;
        for (StructuralBlock block : parsed.blocks()) {
            String normalizedBlockText = normalizeText(block.content(), appliedRules, options);
            if (normalizedBlockText.isBlank()) {
                continue;
            }
            normalizedBlocks.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    normalizedBlockText,
                    ordinal++,
                    block.metadata()
            ));
        }

        if (!normalizedBlocks.isEmpty() && normalizedText.isBlank()) {
            normalizedText = rebuildFlatText(normalizedBlocks);
        }

        ParsedDocument normalizedDocument = new ParsedDocument(
                parsed.sourceUri(),
                parsed.title(),
                normalizedText,
                parsed.contentFamily(),
                mergeMetadata(parsed.metadata(), Map.of(
                        "normalized", true,
                        "normalizationRules", List.copyOf(appliedRules)
                )),
                normalizedBlocks
        );

        return new NormalizationResult(
                normalizedDocument,
                parsed.text() == null ? 0 : parsed.text().length(),
                normalizedText.length(),
                rawBlockCount,
                normalizedBlocks.size(),
                List.copyOf(appliedRules)
        );
    }

    @Override
    public String normalizeText(String text) {
        List<String> appliedRules = new ArrayList<>();
        return normalizeText(text, appliedRules, NormalizationOptions.defaults());
    }

    private static String normalizeText(String text, List<String> appliedRules, NormalizationOptions options) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (!normalized.equals(text)) {
            appliedRules.add("unify_line_endings");
        }

        if (options.unicodeNormalize()) {
            String nfkc = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
            if (!nfkc.equals(normalized)) {
                appliedRules.add("unicode_nfkc");
                normalized = nfkc;
            }
        }

        String decoded = decodeHtmlEntities(normalized, appliedRules);
        normalized = decoded;

        String withoutControl = CONTROL_CHARS.matcher(normalized).replaceAll("");
        if (!withoutControl.equals(normalized)) {
            appliedRules.add("remove_control_chars");
            normalized = withoutControl;
        }

        String withoutZeroWidth = ZERO_WIDTH.matcher(normalized).replaceAll("");
        if (!withoutZeroWidth.equals(normalized)) {
            appliedRules.add("remove_zero_width_chars");
            normalized = withoutZeroWidth;
        }

        if (options.dehyphenateLineBreaks()) {
            String dehyphenated = DEHYPHENATE.matcher(normalized).replaceAll("");
            if (!dehyphenated.equals(normalized)) {
                appliedRules.add("dehyphenate_line_breaks");
                normalized = dehyphenated;
            }
        }

        String withoutSoftHyphen = SOFT_HYPHEN.matcher(normalized).replaceAll("");
        if (!withoutSoftHyphen.equals(normalized)) {
            appliedRules.add("remove_soft_hyphen");
            normalized = withoutSoftHyphen;
        }

        String withoutNbsp = NBSP.matcher(normalized).replaceAll(" ");
        if (!withoutNbsp.equals(normalized)) {
            appliedRules.add("normalize_nbsp");
            normalized = withoutNbsp;
        }

        String cjkFixed = CJK_SPACE.matcher(normalized).replaceAll("$1$2");
        if (!cjkFixed.equals(normalized)) {
            appliedRules.add("collapse_cjk_spaces");
            normalized = cjkFixed;
        }

        if (options.normalizeFullWidthSpace()) {
            String spaced = FULL_WIDTH_SPACE.matcher(normalized).replaceAll(" ");
            if (!spaced.equals(normalized)) {
                appliedRules.add("normalize_full_width_space");
                normalized = spaced;
            }
        }

        if (options.normalizeBullets()) {
            String bullets = BULLET_VARIANTS.matcher(normalized).replaceAll("- ");
            if (!bullets.equals(normalized)) {
                appliedRules.add("normalize_bullets");
                normalized = bullets;
            }
        }

        if (options.removePageFooters()) {
            String withoutFooters = PAGE_FOOTER.matcher(normalized).replaceAll("");
            if (!withoutFooters.equals(normalized)) {
                appliedRules.add("remove_page_footers");
                normalized = withoutFooters;
            }
            String withoutPageNumbers = STANDALONE_PAGE_NUMBER.matcher(normalized).replaceAll("");
            if (!withoutPageNumbers.equals(normalized)) {
                appliedRules.add("remove_standalone_page_numbers");
                normalized = withoutPageNumbers;
            }
        }

        if (options.collapseRepeatedPunctuation()) {
            String punct = REPEATED_PUNCT.matcher(normalized).replaceAll("$1");
            if (!punct.equals(normalized)) {
                appliedRules.add("collapse_repeated_punctuation");
                normalized = punct;
            }
        }

        if (options.trimLineTrailingSpaces()) {
            String trimmedLines = TRAILING_SPACES.matcher(normalized).replaceAll("\n");
            if (!trimmedLines.equals(normalized)) {
                appliedRules.add("trim_line_trailing_spaces");
                normalized = trimmedLines;
            }
        }

        if (options.collapseBlankLines()) {
            String collapsed = EXCESS_BLANK_LINES.matcher(normalized).replaceAll("\n\n");
            if (!collapsed.equals(normalized)) {
                appliedRules.add("collapse_excess_blank_lines");
                normalized = collapsed;
            }
        }

        return normalized.trim();
    }

    private static String decodeHtmlEntities(String text, List<String> appliedRules) {
        if (!text.contains("&")) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        java.util.regex.Matcher matcher = HTML_ENTITY.matcher(text);
        int last = 0;
        boolean changed = false;
        while (matcher.find()) {
            changed = true;
            builder.append(text, last, matcher.start());
            builder.append(decodeEntity(matcher.group(1)));
            last = matcher.end();
        }
        if (!changed) {
            return text;
        }
        appliedRules.add("decode_html_entities");
        builder.append(text.substring(last));
        return builder.toString();
    }

    private static String decodeEntity(String entity) {
        if (entity.startsWith("#x") || entity.startsWith("#X")) {
            int codePoint = Integer.parseInt(entity.substring(2), 16);
            return String.valueOf((char) codePoint);
        }
        if (entity.startsWith("#")) {
            int codePoint = Integer.parseInt(entity.substring(1));
            return String.valueOf((char) codePoint);
        }
        return switch (entity.toLowerCase(Locale.ROOT)) {
            case "nbsp" -> " ";
            case "lt" -> "<";
            case "gt" -> ">";
            case "amp" -> "&";
            case "quot" -> "\"";
            case "apos" -> "'";
            default -> "&" + entity + ";";
        };
    }

    private static String rebuildFlatText(List<StructuralBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (StructuralBlock block : blocks) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            if ("heading".equals(block.blockType())) {
                builder.append("#".repeat(Math.max(1, block.level()))).append(' ').append(block.content());
            } else {
                builder.append(block.content());
            }
        }
        return builder.toString();
    }

    private static Map<String, Object> mergeMetadata(Map<String, Object> base, Map<String, Object> extra) {
        java.util.HashMap<String, Object> merged = new java.util.HashMap<>();
        if (base != null) {
            merged.putAll(base);
        }
        merged.putAll(extra);
        return Map.copyOf(merged);
    }

    private record NormalizationOptions(
            boolean collapseBlankLines,
            boolean trimLineTrailingSpaces,
            boolean normalizeFullWidthSpace,
            boolean unicodeNormalize,
            boolean dehyphenateLineBreaks,
            boolean removePageFooters,
            boolean normalizeBullets,
            boolean collapseRepeatedPunctuation
    ) {

        static NormalizationOptions defaults() {
            return new NormalizationOptions(true, true, true, true, true, true, true, true);
        }

        static NormalizationOptions from(DocumentProfile profile) {
            return new NormalizationOptions(
                    optionBoolean(profile, "collapseBlankLines", true),
                    optionBoolean(profile, "trimLineTrailingSpaces", true),
                    optionBoolean(profile, "normalizeFullWidthSpace", true),
                    optionBoolean(profile, "unicodeNormalize", true),
                    optionBoolean(profile, "dehyphenateLineBreaks", true),
                    optionBoolean(profile, "removePageFooters", true),
                    optionBoolean(profile, "normalizeBullets", true),
                    optionBoolean(profile, "collapseRepeatedPunctuation", true)
            );
        }
    }

    private static boolean optionBoolean(DocumentProfile profile, String key, boolean defaultValue) {
        if (profile == null || profile.options() == null) {
            return defaultValue;
        }
        Object value = profile.options().get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        if (value != null) {
            return Boolean.parseBoolean(String.valueOf(value).trim());
        }
        return defaultValue;
    }
}
