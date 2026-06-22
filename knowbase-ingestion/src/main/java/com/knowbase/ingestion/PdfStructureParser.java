package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PdfStructureParser implements DocumentParser {

    public static final String PARSER_CODE = "pdf-structure";

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("pdf")) {
            return true;
        }
        return sourceUri != null && sourceUri.toLowerCase(Locale.ROOT).endsWith(".pdf");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try (InputStream inputStream = source.inputStream();
             PDDocument document = PDDocument.load(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setAddMoreFormatting(true);
            stripper.setLineSeparator("\n");
            stripper.setWordSeparator(" ");
            stripper.setPageStart("");
            stripper.setPageEnd("");
            stripper.setParagraphEnd("\n\n");
            List<StructuralBlock> blocks = new ArrayList<>();
            int ordinal = 0;
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document).trim();
                if (pageText.isBlank()) {
                    continue;
                }
                List<StructuralBlock> pageBlocks = StructureParsingSupport.splitPdfPages(pageText, page, ordinal);
                blocks.addAll(pageBlocks);
                ordinal += pageBlocks.size();
            }

            Map<String, Object> metadata = new HashMap<>();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata());
            }
            metadata.put("parserCode", PARSER_CODE);
            metadata.put("structureAware", true);
            metadata.put("blockCount", blocks.size());
            metadata.put("pageCount", pageCount);
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
            throw new IllegalStateException("读取 PDF 文档失败: " + source.sourceUri(), exception);
        }
    }
}
