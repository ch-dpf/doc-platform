package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PptxStructureParserTest {

    private final PptxStructureParser parser = new PptxStructureParser();

    @Test
    void supportsPptxMimeAndExtension() {
        assertTrue(parser.supports("file:///deck.pptx", null));
        assertTrue(parser.supports("memory://deck", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));
    }

    @Test
    void parsesSlideTitleBodyAndTable() throws Exception {
        byte[] bytes = sampleDeckBytes();
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "memory://deck.pptx",
                "deck.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                new ByteArrayInputStream(bytes),
                Map.of()
        ));

        assertEquals(ContentFamily.PRESENTATION, parsed.contentFamily());
        assertEquals(PptxStructureParser.PARSER_CODE, parsed.metadata().get("parserCode"));
        assertTrue(parsed.structureAware());
        assertEquals(1, parsed.metadata().get("slideCount"));
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                "heading".equals(block.blockType()) && block.content().contains("Quarterly Results")));
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                "paragraph".equals(block.blockType()) && block.content().contains("Revenue grew")));
        assertTrue(parsed.blocks().stream().anyMatch(block ->
                "table_row".equals(block.blockType()) && block.content().contains("Users")));
        assertTrue(parsed.blocks().stream()
                .filter(block -> block.metadata().containsKey("slideNumber"))
                .allMatch(block -> Integer.valueOf(1).equals(block.metadata().get("slideNumber"))));
    }

    @Test
    void enricherAddsSlideEvidenceHint() throws Exception {
        byte[] bytes = sampleDeckBytes();
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(parser.parse(new DocumentSource(
                "memory://deck.pptx",
                "deck.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertTrue(parsed.blocks().stream().anyMatch(block -> {
            Object hint = block.metadata().get("evidenceAssetHint");
            return hint instanceof Map<?, ?> hintMap && "slide".equals(hintMap.get("kind"));
        }));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_summary".equals(block.blockType())));
    }

    private static byte[] sampleDeckBytes() throws Exception {
        try (XMLSlideShow slideshow = new XMLSlideShow();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSLFSlideMaster master = slideshow.getSlideMasters().getFirst();
            XSLFSlideLayout layout = master.getLayout(SlideLayout.TITLE_AND_CONTENT);
            XSLFSlide slide = slideshow.createSlide(layout);

            XSLFTextShape title = (XSLFTextShape) slide.getPlaceholder(Placeholder.TITLE);
            if (title == null) {
                title = slide.createTextBox();
                title.setAnchor(new Rectangle(50, 20, 500, 50));
            }
            title.setText("Quarterly Results");

            XSLFTextShape body = (XSLFTextShape) slide.getPlaceholder(Placeholder.CONTENT);
            if (body == null) {
                body = slide.createTextBox();
                body.setAnchor(new Rectangle(50, 100, 500, 80));
            }
            body.setText("Revenue grew 12%.");

            XSLFTable table = slide.createTable();
            table.setAnchor(new Rectangle(50, 200, 400, 200));
            XSLFTableRow headerRow = table.addRow();
            headerRow.addCell().setText("Metric");
            headerRow.addCell().setText("Value");
            XSLFTableRow dataRow = table.addRow();
            dataRow.addCell().setText("Users");
            dataRow.addCell().setText("1000");

            slideshow.write(output);
            return output.toByteArray();
        }
    }
}
