package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.parse.ParsedDocumentParseEnricher;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxStructureParserTest {

    private final DocxStructureParser parser = new DocxStructureParser();

    @Test
    void headerAndFooterDefaultToNonIndexable() throws Exception {
        byte[] bytes = buildDocxWithHeaderFooter();
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "memory://doc.docx",
                "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new ByteArrayInputStream(bytes),
                Map.of()
        ));

        assertTrue(parsed.blocks().stream().anyMatch(block -> "header".equals(block.metadata().get("layoutRole"))));
        assertTrue(parsed.blocks().stream().anyMatch(block -> "footer".equals(block.metadata().get("layoutRole"))));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "header".equals(block.metadata().get("layoutRole"))
                        || "footer".equals(block.metadata().get("layoutRole")))
                .allMatch(block -> Boolean.FALSE.equals(block.metadata().get("indexableHint"))));
    }

    @Test
    void headerFooterCanBeIndexedWhenProfileOptionEnabled() throws Exception {
        byte[] bytes = buildDocxWithHeaderFooter();
        ParsedDocument parsed = parser.parse(new DocumentSource(
                "memory://doc.docx",
                "doc.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new ByteArrayInputStream(bytes),
                Map.of("indexDocxHeaderFooter", true)
        ));

        assertTrue(parsed.blocks().stream()
                .filter(block -> "header".equals(block.metadata().get("layoutRole")))
                .allMatch(block -> Boolean.TRUE.equals(block.metadata().get("indexableHint"))));
    }

    @Test
    void tableRowsExposeRegionMetadataAfterEnrichment() throws Exception {
        byte[] bytes = buildDocxWithTable();
        ParsedDocument parsed = ParsedDocumentParseEnricher.enrich(parser.parse(new DocumentSource(
                "memory://table.docx",
                "table.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new ByteArrayInputStream(bytes),
                Map.of()
        )));

        assertEquals(ContentFamily.RICH_TEXT, parsed.contentFamily());
        assertTrue(parsed.blocks().stream().anyMatch(block -> "table_row".equals(block.blockType())));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .allMatch(block -> block.metadata().containsKey("tableRegionId")));
        assertTrue(parsed.blocks().stream()
                .filter(block -> "table_row".equals(block.blockType()))
                .anyMatch(block -> block.metadata().containsKey("cellCoordinates")));
    }

    private static byte[] buildDocxWithHeaderFooter() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
            XWPFParagraph headerParagraph = header.createParagraph();
            headerParagraph.createRun().setText("Confidential Header");

            XWPFParagraph body = document.createParagraph();
            body.createRun().setText("Main body paragraph.");

            XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
            XWPFParagraph footerParagraph = footer.createParagraph();
            footerParagraph.createRun().setText("Page Footer");

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private static byte[] buildDocxWithTable() throws Exception {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = document.createTable(2, 2);
            XWPFTableRow headerRow = table.getRow(0);
            headerRow.getCell(0).setText("Name");
            headerRow.getCell(1).setText("Score");
            XWPFTableRow dataRow = table.getRow(1);
            dataRow.getCell(0).setText("Alice");
            dataRow.getCell(1).setText("95");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
