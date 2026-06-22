package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.status.ContentFamily;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class StructureSegmenter {

    public List<StructuralSegment> segment(ParsedDocument document, DocumentProfile documentProfile) {
        if (document.structureAware()) {
            return segmentFromBlocks(document, documentProfile);
        }
        return segmentFromFlatText(document, documentProfile);
    }

    private List<StructuralSegment> segmentFromBlocks(ParsedDocument document, DocumentProfile documentProfile) {
        String strategy = resolveStrategy(document, documentProfile);
        List<StructuralBlock> blocks = document.blocks();
        if (blocks.isEmpty()) {
            return segmentFromFlatText(document, documentProfile);
        }

        if (strategy.contains("table_row")) {
            return blocksToSegments(blocks, strategy, block -> "table_row".equals(block.blockType()), true);
        }
        if (strategy.contains("page")) {
            return blocksToSegments(blocks, strategy, block -> "page".equals(block.blockType()), false);
        }
        if (strategy.contains("paragraph")) {
            return blocksToSegments(blocks, strategy, block -> !"heading".equals(block.blockType()), false);
        }
        if (strategy.contains("dom")) {
            return blocksToSegments(blocks, strategy, block -> true, false);
        }
        if (strategy.contains("qa")) {
            return segmentFromFlatText(document, documentProfile);
        }

        return segmentByHeadingSections(blocks, strategy);
    }

    private static List<StructuralSegment> segmentByHeadingSections(List<StructuralBlock> blocks, String strategy) {
        List<StructuralSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        Map<String, Object> currentMetadata = new HashMap<>();
        String boundaryType = "section";
        int sectionOrdinal = 0;

        for (StructuralBlock block : blocks) {
            if ("heading".equals(block.blockType()) && current.length() > 0) {
                segments.add(buildSegment(current.toString(), boundaryType, strategy, sectionOrdinal++, currentMetadata));
                current.setLength(0);
                currentMetadata = new HashMap<>();
            }
            if ("heading".equals(block.blockType())) {
                boundaryType = "section";
                currentMetadata.put("sectionTitle", block.content());
                if (block.metadata().get("sectionPath") != null) {
                    currentMetadata.put("sectionPath", block.metadata().get("sectionPath"));
                }
            } else if ("code_block".equals(block.blockType())) {
                boundaryType = "code_block";
            } else if ("table_row".equals(block.blockType())) {
                boundaryType = "table_row";
            } else if ("page".equals(block.blockType())) {
                boundaryType = "page";
            } else if ("list_item".equals(block.blockType())) {
                boundaryType = "list_item";
            } else if ("dom_block".equals(block.blockType())) {
                boundaryType = "dom_block";
            }

            if (block.metadata().get("pageNumber") != null) {
                currentMetadata.putIfAbsent("pageNumber", block.metadata().get("pageNumber"));
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            if ("heading".equals(block.blockType())) {
                current.append("#".repeat(Math.max(1, block.level()))).append(' ').append(block.content());
            } else {
                current.append(block.content());
            }
        }

        if (current.length() > 0) {
            segments.add(buildSegment(current.toString(), boundaryType, strategy, sectionOrdinal, currentMetadata));
        }
        if (segments.isEmpty()) {
            segments.add(buildSegment(StructureParsingSupport.blocksToText(blocks), "document", strategy, 0, Map.of()));
        }
        return segments;
    }

    private static List<StructuralSegment> blocksToSegments(
            List<StructuralBlock> blocks,
            String strategy,
            java.util.function.Predicate<StructuralBlock> includeBlock,
            boolean oneBlockPerSegment
    ) {
        List<StructuralSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int ordinal = 0;
        for (StructuralBlock block : blocks) {
            if (!includeBlock.test(block)) {
                continue;
            }
            if (oneBlockPerSegment) {
                segments.add(buildSegment(block.content(), blockTypeToBoundary(block.blockType()), strategy, ordinal++, block.metadata()));
                continue;
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(block.content());
        }
        if (!oneBlockPerSegment && current.length() > 0) {
            segments.add(buildSegment(current.toString(), "paragraph", strategy, ordinal, Map.of()));
        }
        return segments;
    }

    private List<StructuralSegment> segmentFromFlatText(ParsedDocument document, DocumentProfile documentProfile) {
        String text = document.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String strategy = resolveStrategy(document, documentProfile);
        String pattern = patternForStrategy(strategy, document, documentProfile);
        String[] parts = Pattern.compile(pattern).split(text);
        List<StructuralSegment> segments = new ArrayList<>();
        int ordinal = 0;
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            segments.add(buildSegment(part.trim(), boundaryTypeForStrategy(strategy), strategy, ordinal++, Map.of()));
        }
        if (segments.isEmpty()) {
            segments.add(buildSegment(text.trim(), "document", strategy, 0, Map.of()));
        }
        return segments;
    }

    private static StructuralSegment buildSegment(
            String content,
            String boundaryType,
            String strategy,
            int ordinal,
            Map<String, Object> blockMetadata
    ) {
        Map<String, Object> metadata = new HashMap<>(blockMetadata);
        metadata.put("sourceStructure", strategy);
        metadata.put("boundaryType", boundaryType);
        metadata.put("structureAware", true);
        return new StructuralSegment(content, boundaryType, ordinal, Map.copyOf(metadata));
    }

    private static String blockTypeToBoundary(String blockType) {
        return switch (blockType) {
            case "table_row" -> "table_row";
            case "code_block" -> "code_block";
            case "page" -> "page";
            case "list_item" -> "list_item";
            case "dom_block" -> "dom_block";
            case "heading" -> "section";
            default -> "paragraph";
        };
    }

    private static String resolveStrategy(ParsedDocument document, DocumentProfile documentProfile) {
        if (documentProfile != null && documentProfile.chunkingStrategy() != null && !documentProfile.chunkingStrategy().isBlank()) {
            return documentProfile.chunkingStrategy().trim().toLowerCase(Locale.ROOT);
        }
        ContentFamily family = documentProfile == null ? document.contentFamily() : documentProfile.contentFamily();
        return switch (family) {
            case STRUCTURED_TABLE -> "table_row_token_window";
            case CODE_OR_CONFIG -> "code_token_window";
            case PRESENTATION -> "slide_token_window";
            case SCANNED_DOCUMENT, IMAGE_TEXT -> "page_token_window";
            case WEB_PAGE -> "dom_token_window";
            case RICH_TEXT -> "structure_token_window";
            case PLAIN_TEXT -> "paragraph_token_window";
        };
    }

    private static String patternForStrategy(String strategy, ParsedDocument document, DocumentProfile documentProfile) {
        if (strategy.contains("qa")) {
            return "(?m)^(?:Q[:：]|问[:：]|A[:：]|答[:：]).*$|\\R{2,}";
        }
        if (strategy.contains("table_row")) {
            return "\\R(?=\\S)";
        }
        if (strategy.contains("code") || strategy.contains("heading_code")) {
            return "(?m)^\\s*(?:class|interface|record|enum|def|function|export|public|private|protected)\\b|\\R{2,}";
        }
        if (strategy.contains("slide")) {
            return "\\R{2,}|(?m)^\\s*(?:Slide|Page|幻灯片|第\\s*\\d+\\s*页)\\b.*$";
        }
        if (strategy.contains("page")) {
            return "\\R{2,}|(?m)^\\s*(?:Page|第\\s*\\d+\\s*页)\\b.*$";
        }
        if (strategy.contains("dom")) {
            return "(?m)^\\s*<[^>]+>.*$|\\R{2,}";
        }
        if (strategy.contains("paragraph")) {
            return "\\R{2,}";
        }
        ContentFamily family = documentProfile == null ? document.contentFamily() : documentProfile.contentFamily();
        if (family == ContentFamily.STRUCTURED_TABLE) {
            return "\\R(?=\\S)";
        }
        return "(?m)^#{1,6}\\s+.+$|\\R{2,}";
    }

    private static String boundaryTypeForStrategy(String strategy) {
        if (strategy.contains("table_row")) {
            return "table_row";
        }
        if (strategy.contains("code") || strategy.contains("heading_code")) {
            return "code_block";
        }
        if (strategy.contains("slide")) {
            return "slide";
        }
        if (strategy.contains("page")) {
            return "page";
        }
        if (strategy.contains("qa")) {
            return "qa_pair";
        }
        if (strategy.contains("paragraph")) {
            return "paragraph";
        }
        if (strategy.contains("dom")) {
            return "dom_block";
        }
        return "section";
    }
}
