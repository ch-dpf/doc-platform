package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PdfLayoutParser implements DocumentParser {

    public static final String PARSER_CODE = "pdf-layout";

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        return sourceUri != null && sourceUri.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try (InputStream inputStream = source.inputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            List<StructuralBlock> blocks = LayoutPdfTextExtractor.extract(bytes);
            if (blocks.isEmpty()) {
                return fallbackStructureParse(source, bytes);
            }
            Map<String, Object> metadata = new HashMap<>();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata());
            }
            metadata.put("parserCode", PARSER_CODE);
            metadata.put("structureAware", true);
            metadata.put("layoutParsing", true);
            metadata.put("blockCount", blocks.size());
            metadata.put("pageCount", countPages(blocks));
            String flatText = StructureParsingSupport.blocksToText(blocks);
            return new ParsedDocument(
                    source.sourceUri(),
                    source.filename(),
                    flatText,
                    ContentFamily.RICH_TEXT,
                    Map.copyOf(metadata),
                    blocks
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Layout 解析 PDF 失败: " + source.sourceUri(), exception);
        }
    }

    private static ParsedDocument fallbackStructureParse(DocumentSource source, byte[] bytes) {
        PdfStructureParser fallback = new PdfStructureParser();
        return fallback.parse(new DocumentSource(
                source.sourceUri(),
                source.filename(),
                source.mimeType(),
                new java.io.ByteArrayInputStream(bytes),
                source.metadata()
        ));
    }

    private static int countPages(List<StructuralBlock> blocks) {
        return blocks.stream()
                .map(block -> block.metadata().get("pageNumber"))
                .filter(Number.class::isInstance)
                .mapToInt(value -> ((Number) value).intValue())
                .max()
                .orElse(0);
    }
}
