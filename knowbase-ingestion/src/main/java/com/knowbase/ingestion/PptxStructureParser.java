package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.office.OfficeTableBlockMapper;
import com.knowbase.ingestion.office.PptxSlideContentExtractor;
import com.knowbase.ingestion.office.PptxTableStructureExtractor;

import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PptxStructureParser implements DocumentParser {

    public static final String PARSER_CODE = "pptx-structure";

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).contains("presentationml.presentation")) {
            return true;
        }
        return sourceUri != null && sourceUri.toLowerCase(Locale.ROOT).endsWith(".pptx");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try (InputStream inputStream = source.inputStream();
             XMLSlideShow slideshow = new XMLSlideShow(inputStream)) {
            List<PptxSlideContentExtractor.SlideContent> slides = PptxSlideContentExtractor.extract(slideshow);
            List<StructuralBlock> blocks = new ArrayList<>();
            int ordinal = 0;
            int globalTableRegionId = 0;
            for (PptxSlideContentExtractor.SlideContent slide : slides) {
                Map<String, Object> slideMetadata = baseSlideMetadata(slide.slideNumber());
                if (!slide.title().isBlank()) {
                    Map<String, Object> headingMetadata = new HashMap<>(slideMetadata);
                    headingMetadata.put("boundaryType", "section");
                    headingMetadata.put("layoutRole", "title");
                    headingMetadata.put("slideTitle", slide.title());
                    blocks.add(new StructuralBlock("heading", 1, slide.title(), ordinal++, Map.copyOf(headingMetadata)));
                }
                for (String paragraph : slide.paragraphs()) {
                    if (paragraph.isBlank() || paragraph.equals(slide.title())) {
                        continue;
                    }
                    Map<String, Object> paragraphMetadata = new HashMap<>(slideMetadata);
                    paragraphMetadata.put("boundaryType", "paragraph");
                    paragraphMetadata.put("layoutRole", "body");
                    blocks.add(new StructuralBlock("paragraph", 0, paragraph, ordinal++, Map.copyOf(paragraphMetadata)));
                }
                for (PptxTableStructureExtractor.PptxTableModel table : slide.tables()) {
                    List<StructuralBlock> tableBlocks = OfficeTableBlockMapper.fromPptxTable(
                            table,
                            globalTableRegionId++,
                            slide.slideNumber(),
                            ordinal
                    );
                    blocks.addAll(tableBlocks);
                    ordinal += tableBlocks.size();
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            if (source.metadata() != null) {
                metadata.putAll(source.metadata());
            }
            metadata.put("parserCode", PARSER_CODE);
            metadata.put("structureAware", !blocks.isEmpty());
            metadata.put("layoutParsing", true);
            metadata.put("blockCount", blocks.size());
            metadata.put("slideCount", slides.size());
            metadata.put("pageCount", slides.size());
            blocks = StructureParsingSupport.enrichHeadingPathsPublic(blocks);
            String flatText = blocks.isEmpty() ? "" : StructureParsingSupport.blocksToTextPublic(blocks);
            return new ParsedDocument(
                    source.sourceUri(),
                    source.filename(),
                    flatText,
                    ContentFamily.PRESENTATION,
                    Map.copyOf(metadata),
                    blocks
            );
        } catch (IOException exception) {
            throw new IllegalStateException("读取 PPTX 文档失败: " + source.sourceUri(), exception);
        }
    }

    private static Map<String, Object> baseSlideMetadata(int slideNumber) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("slideNumber", slideNumber);
        metadata.put("pageNumber", slideNumber);
        metadata.put("layoutParsing", true);
        return metadata;
    }
}
