package com.knowbase.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StructureParsingSupport {

    private static final Pattern MARKDOWN_HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern MARKDOWN_LIST = Pattern.compile("^(\\s*)[-*+]\\s+(.+)$");
    private static final Pattern MARKDOWN_ORDERED_LIST = Pattern.compile("^(\\s*)\\d+\\.\\s+(.+)$");
    private static final Pattern PDF_HEADING = Pattern.compile(
            "^(?:第\\s*[\\d一二三四五六七八九十百]+\\s*[章节篇部].+|Chapter\\s+\\d+.+|\\d+(?:\\.\\d+)*\\s+.+|[一二三四五六七八九十]+、.+)$"
    );
    private static final Pattern PDF_TABLE_ROW = Pattern.compile("\\S+(?:\\s{2,}|\\t+)\\S+");

    private StructureParsingSupport() {
    }

    static List<StructuralBlock> parseMarkdown(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        StringBuilder codeBlock = new StringBuilder();
        boolean inCodeFence = false;
        int ordinal = 0;

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine;
            if (line.trim().startsWith("```")) {
                if (inCodeFence) {
                    blocks.add(StructuralBlock.codeBlock(codeBlock.toString().trim(), ordinal++));
                    codeBlock.setLength(0);
                    inCodeFence = false;
                } else {
                    if (paragraph.length() > 0) {
                        blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                        paragraph.setLength(0);
                    }
                    inCodeFence = true;
                }
                continue;
            }
            if (inCodeFence) {
                if (codeBlock.length() > 0) {
                    codeBlock.append('\n');
                }
                codeBlock.append(line);
                continue;
            }

            Matcher headingMatcher = MARKDOWN_HEADING.matcher(line.trim());
            if (headingMatcher.matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.heading(
                        headingMatcher.group(1).length(),
                        headingMatcher.group(2).trim(),
                        ordinal++
                ));
                continue;
            }

            Matcher listMatcher = MARKDOWN_LIST.matcher(line);
            if (listMatcher.matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                int level = Math.max(1, listMatcher.group(1).length() / 2 + 1);
                blocks.add(StructuralBlock.listItem(listMatcher.group(2).trim(), ordinal++, level));
                continue;
            }

            Matcher orderedMatcher = MARKDOWN_ORDERED_LIST.matcher(line);
            if (orderedMatcher.matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                int level = Math.max(1, orderedMatcher.group(1).length() / 2 + 1);
                blocks.add(StructuralBlock.listItem(orderedMatcher.group(2).trim(), ordinal++, level));
                continue;
            }

            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                continue;
            }

            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(line.trim());
        }

        if (inCodeFence && codeBlock.length() > 0) {
            blocks.add(StructuralBlock.codeBlock(codeBlock.toString().trim(), ordinal++));
        } else if (paragraph.length() > 0) {
            blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
        }
        return enrichHeadingPaths(blocks);
    }

    static List<StructuralBlock> parsePlainText(String text) {
        if (HeadingPatternDetector.hasOutlineStructure(text)) {
            return parseOutlineText(text);
        }
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (String paragraph : text.split("\\R{2,}")) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            blocks.add(StructuralBlock.paragraph(paragraph.trim(), ordinal++));
        }
        if (blocks.isEmpty() && text != null && !text.isBlank()) {
            blocks.add(StructuralBlock.paragraph(text.trim(), 0));
        }
        return blocks;
    }

    static List<StructuralBlock> parseHtml(String html) {
        return parseHtmlWithJsoup(html);
    }

    static List<StructuralBlock> parseHtmlWithJsoup(String html) {
        List<StructuralBlock> blocks = new ArrayList<>();
        org.jsoup.nodes.Document document = org.jsoup.Jsoup.parse(html);
        document.select("script, style, nav, footer, header, noscript").remove();
        org.jsoup.select.Elements elements = document.body() == null
                ? document.select("h1,h2,h3,h4,h5,h6,p,li,tr,table")
                : document.body().select("h1,h2,h3,h4,h5,h6,p,li,tr,table");
        int ordinal = 0;
        for (org.jsoup.nodes.Element element : elements) {
            String tag = element.tagName().toLowerCase();
            String text = element.text().trim();
            if (text.isBlank()) {
                continue;
            }
            switch (tag) {
                case "h1", "h2", "h3", "h4", "h5", "h6" -> blocks.add(StructuralBlock.heading(
                        Integer.parseInt(tag.substring(1)),
                        text,
                        ordinal++
                ));
                case "p" -> blocks.add(StructuralBlock.paragraph(text, ordinal++));
                case "li" -> blocks.add(StructuralBlock.listItem(text, ordinal++, 1));
                case "tr" -> blocks.add(StructuralBlock.tableRow(text, ordinal++, ordinal));
                case "table" -> blocks.add(StructuralBlock.domBlock("table", text, ordinal++));
                default -> blocks.add(StructuralBlock.domBlock(tag, text, ordinal++));
            }
        }
        if (blocks.isEmpty()) {
            String plain = document.text().trim();
            if (!plain.isBlank()) {
                return parsePlainText(plain);
            }
        }
        return enrichHeadingPaths(blocks);
    }

    private static List<StructuralBlock> enrichHeadingPaths(List<StructuralBlock> blocks) {
        List<String> path = new ArrayList<>();
        List<StructuralBlock> enriched = new ArrayList<>();
        for (StructuralBlock block : blocks) {
            if ("heading".equals(block.blockType())) {
                while (path.size() >= Math.max(1, block.level())) {
                    path.remove(path.size() - 1);
                }
                path.add(block.content());
            }
            java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(block.metadata());
            if (!path.isEmpty()) {
                metadata.put("sectionPath", String.join(" > ", path));
            }
            enriched.add(new StructuralBlock(
                    block.blockType(),
                    block.level(),
                    block.content(),
                    block.ordinal(),
                    metadata
            ));
        }
        return enriched;
    }

    static List<StructuralBlock> splitPdfPages(String pageText, int pageNumber, int startOrdinal) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = startOrdinal;
        String[] lines = pageText.split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                continue;
            }
            if (PDF_HEADING.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.heading(2, line, ordinal++));
                continue;
            }
            if (PDF_TABLE_ROW.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.tableRow(line.replaceAll("\\s{2,}|\\t+", " | "), ordinal++, ordinal));
                continue;
            }
            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(line);
        }
        if (paragraph.length() > 0) {
            blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
        }
        if (blocks.isEmpty() && pageText != null && !pageText.isBlank()) {
            blocks.add(StructuralBlock.page(pageText.trim(), ordinal, pageNumber));
        } else {
            for (int index = 0; index < blocks.size(); index++) {
                StructuralBlock block = blocks.get(index);
                java.util.HashMap<String, Object> metadata = new java.util.HashMap<>(block.metadata());
                metadata.put("pageNumber", pageNumber);
                blocks.set(index, new StructuralBlock(
                        block.blockType(),
                        block.level(),
                        block.content(),
                        block.ordinal(),
                        metadata
                ));
            }
        }
        return blocks;
    }

    static List<StructuralBlock> parseOutlineText(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        int ordinal = 0;
        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                continue;
            }
            java.util.OptionalInt headingLevel = HeadingPatternDetector.detectLevel(line);
            if (headingLevel.isPresent()) {
                if (paragraph.length() > 0) {
                    blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
                    paragraph.setLength(0);
                }
                blocks.add(StructuralBlock.heading(headingLevel.getAsInt(), line, ordinal++));
            } else {
                if (paragraph.length() > 0) {
                    paragraph.append('\n');
                }
                paragraph.append(line);
            }
        }
        if (paragraph.length() > 0) {
            blocks.add(StructuralBlock.paragraph(paragraph.toString().trim(), ordinal++));
        }
        if (blocks.isEmpty() && text != null && !text.isBlank()) {
            return parsePlainTextLegacyParagraphs(text);
        }
        return enrichHeadingPaths(blocks);
    }

    private static List<StructuralBlock> parsePlainTextLegacyParagraphs(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        for (String paragraph : text.split("\\R{2,}")) {
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            blocks.add(StructuralBlock.paragraph(paragraph.trim(), ordinal++));
        }
        return blocks;
    }

    static List<StructuralBlock> enrichHeadingPathsPublic(List<StructuralBlock> blocks) {
        return enrichHeadingPaths(blocks);
    }

    static List<StructuralBlock> parseOcrLayout(String text) {
        List<StructuralBlock> blocks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return blocks;
        }
        String[] lines = text.split("\n", -1);
        StringBuilder paragraph = new StringBuilder();
        int ordinal = 0;
        int lineIndex = 0;
        for (String rawLine : lines) {
            lineIndex++;
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                if (paragraph.length() > 0) {
                    blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal++, lineIndex));
                    paragraph.setLength(0);
                }
                continue;
            }
            if (PDF_HEADING.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal++, lineIndex));
                    paragraph.setLength(0);
                }
                blocks.add(layoutHeading(line, ordinal++, lineIndex));
                continue;
            }
            if (PDF_TABLE_ROW.matcher(line).matches()) {
                if (paragraph.length() > 0) {
                    blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal++, lineIndex));
                    paragraph.setLength(0);
                }
                blocks.add(layoutTableRow(line, ordinal++, lineIndex));
                continue;
            }
            if (paragraph.length() > 0) {
                paragraph.append('\n');
            }
            paragraph.append(line);
        }
        if (paragraph.length() > 0) {
            blocks.add(layoutParagraph(paragraph.toString().trim(), ordinal, lineIndex));
        }
        return enrichHeadingPaths(blocks);
    }

    private static StructuralBlock layoutParagraph(String content, int ordinal, int lineIndex) {
        return new StructuralBlock(
                "paragraph",
                0,
                content,
                ordinal,
                Map.of(
                        "boundaryType", "paragraph",
                        "layoutRole", "body",
                        "ocrLineStart", lineIndex,
                        "layoutParsing", true
                )
        );
    }

    private static StructuralBlock layoutHeading(String content, int ordinal, int lineIndex) {
        return new StructuralBlock(
                "heading",
                2,
                content,
                ordinal,
                Map.of(
                        "boundaryType", "section",
                        "layoutRole", "heading",
                        "ocrLineStart", lineIndex,
                        "layoutParsing", true
                )
        );
    }

    private static StructuralBlock layoutTableRow(String content, int ordinal, int lineIndex) {
        return new StructuralBlock(
                "table_row",
                0,
                content.replaceAll("\\s{2,}|\\t+", " | "),
                ordinal,
                Map.of(
                        "boundaryType", "table_row",
                        "layoutRole", "table",
                        "ocrLineStart", lineIndex,
                        "layoutParsing", true
                )
        );
    }

    static String blocksToText(List<StructuralBlock> blocks) {
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

    private static String stripTags(String html) {
        return html.replaceAll("(?is)<script.*?>.*?</script>", " ")
                .replaceAll("(?is)<style.*?>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
