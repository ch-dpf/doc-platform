package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DocxStructureParser implements DocumentParser {

    public static final String PARSER_CODE = "docx-structure";

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase().contains("wordprocessingml.document")) {
            return true;
        }
        return sourceUri != null && sourceUri.toLowerCase(Locale.ROOT).endsWith(".docx");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try (InputStream inputStream = source.inputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {
            List<StructuralBlock> blocks = new ArrayList<>();
            int ordinal = 0;
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText().trim();
                    if (text.isBlank()) {
                        continue;
                    }
                    if (paragraph.getNumID() != null && paragraph.getNumID().intValue() > 0) {
                        int listLevel = paragraph.getNumIlvl() == null ? 1 : paragraph.getNumIlvl().intValue() + 1;
                        blocks.add(StructuralBlock.listItem(text, ordinal++, Math.max(1, listLevel)));
                        continue;
                    }
                    if (HeadingPatternDetector.isListParagraphStyle(paragraph.getStyle())) {
                        blocks.add(StructuralBlock.listItem(text, ordinal++, 1));
                        continue;
                    }
                    int headingLevel = headingLevel(paragraph.getStyle());
                    if (headingLevel <= 0) {
                        headingLevel = HeadingPatternDetector.detectLevel(text).orElse(0);
                    }
                    if (headingLevel > 0) {
                        blocks.add(StructuralBlock.heading(headingLevel, text, ordinal++));
                    } else {
                        blocks.add(StructuralBlock.paragraph(text, ordinal++));
                    }
                } else if (element instanceof XWPFTable table) {
                    var models = com.knowbase.ingestion.office.DocxTableStructureExtractor.extractTables(List.of(table));
                    for (var model : models) {
                        List<StructuralBlock> tableBlocks = com.knowbase.ingestion.office.OfficeTableBlockMapper.fromDocxTable(model, ordinal);
                        blocks.addAll(tableBlocks);
                        ordinal += tableBlocks.size();
                    }
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata());
            }
            metadata.put("parserCode", PARSER_CODE);
            metadata.put("structureAware", true);
            metadata.put("blockCount", blocks.size());
            blocks = StructureParsingSupport.enrichHeadingPathsPublic(blocks);
            String flatText = blocks.isEmpty() ? "" : StructureParsingSupport.blocksToText(blocks);
            return new ParsedDocument(
                    source.sourceUri(),
                    source.filename(),
                    flatText,
                    ContentFamily.RICH_TEXT,
                    Map.copyOf(metadata),
                    blocks
            );
        } catch (IOException exception) {
            throw new IllegalStateException("读取 DOCX 文档失败: " + source.sourceUri(), exception);
        }
    }

    private static int headingLevel(String style) {
        if (style == null || style.isBlank()) {
            return 0;
        }
        String normalized = style.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("heading")) {
            try {
                return Integer.parseInt(normalized.replace("heading", "").trim());
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }
        if (normalized.contains("标题")) {
            for (int level = 1; level <= 6; level++) {
                if (normalized.contains(String.valueOf(level))) {
                    return level;
                }
            }
            return 1;
        }
        return 0;
    }
}
