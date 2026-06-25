package com.knowbase.ingestion.office;

import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.XSLFGraphicFrame;
import org.apache.poi.xslf.usermodel.XSLFGroupShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import java.util.ArrayList;
import java.util.List;

public final class PptxSlideContentExtractor {

    private PptxSlideContentExtractor() {
    }

    public record SlideContent(
            int slideNumber,
            String title,
            List<String> paragraphs,
            List<PptxTableStructureExtractor.PptxTableModel> tables
    ) {
    }

    public static List<SlideContent> extract(XMLSlideShow slideshow) {
        List<XSLFSlide> slides = slideshow.getSlides();
        List<SlideContent> contents = new ArrayList<>(slides.size());
        for (int index = 0; index < slides.size(); index++) {
            XSLFSlide slide = slides.get(index);
            int slideNumber = index + 1;
            String title = extractTitle(slide);
            List<String> paragraphs = new ArrayList<>();
            List<PptxTableStructureExtractor.PptxTableModel> tables = new ArrayList<>();
            int[] tableCounter = {0};
            collectShapes(slide.getShapes(), title, paragraphs, tables, tableCounter);
            contents.add(new SlideContent(slideNumber, title, List.copyOf(paragraphs), List.copyOf(tables)));
        }
        return contents;
    }

    private static void collectShapes(
            List<XSLFShape> shapes,
            String slideTitle,
            List<String> paragraphs,
            List<PptxTableStructureExtractor.PptxTableModel> tables,
            int[] tableCounter
    ) {
        for (XSLFShape shape : shapes) {
            if (shape instanceof XSLFGroupShape group) {
                collectShapes(group.getShapes(), slideTitle, paragraphs, tables, tableCounter);
                continue;
            }
            if (shape instanceof XSLFTable table) {
                tables.add(PptxTableStructureExtractor.extract(table, tableCounter[0]++));
                continue;
            }
            if (shape instanceof XSLFTextShape textShape) {
                String text = normalizeText(textShape.getText());
                if (text.isBlank()) {
                    continue;
                }
                if (slideTitle != null && slideTitle.equals(text)) {
                    continue;
                }
                paragraphs.add(text);
                continue;
            }
            if (shape instanceof XSLFGraphicFrame frame && frame.getShapeName() != null) {
                String text = normalizeText(frame.getShapeName());
                if (!text.isBlank()) {
                    paragraphs.add(text);
                }
            }
        }
    }

    private static String extractTitle(XSLFSlide slide) {
        String title = normalizeText(slide.getTitle());
        if (!title.isBlank()) {
            return title;
        }
        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }
            String placeholder = textShape.getPlaceholder() == null ? null : textShape.getPlaceholder().name();
            if (Placeholder.TITLE.name().equals(placeholder) || Placeholder.CENTERED_TITLE.name().equals(placeholder)) {
                String text = normalizeText(textShape.getText());
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.replace('\r', '\n').trim();
    }
}
