package com.knowbase.ingestion.testsupport;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * Programmatic PDF/XLSX fixtures shared by parse regression, chunk snapshots, and eval baselines.
 */
public final class IngestionEvalFixtureFactory {

    public static final String PDF_TABLE = "pdf-table";
    public static final String PDF_MULTI_COLUMN = "pdf-multi-column";
    public static final String PDF_FORMULA = "pdf-formula";
    public static final String XLSX_METRICS = "xlsx-metrics";
    public static final String XLSX_MULTI_HEADER = "xlsx-multi-header";

    private IngestionEvalFixtureFactory() {
    }

    public static List<String> pdfFixtureIds() {
        return List.of(PDF_TABLE, PDF_MULTI_COLUMN, PDF_FORMULA);
    }

    public static List<String> xlsxFixtureIds() {
        return List.of(XLSX_METRICS, XLSX_MULTI_HEADER);
    }

    public static byte[] bytes(String fixtureId) {
        return switch (fixtureId) {
            case PDF_TABLE -> buildTablePdf();
            case PDF_MULTI_COLUMN -> buildMultiColumnPdf();
            case PDF_FORMULA -> buildFormulaPdf();
            case XLSX_METRICS -> buildMetricsWorkbook();
            case XLSX_MULTI_HEADER -> buildMultiHeaderWorkbook();
            default -> throw new IllegalArgumentException("Unknown fixture: " + fixtureId);
        };
    }

    public static String filename(String fixtureId) {
        return switch (fixtureId) {
            case PDF_TABLE -> "metrics.pdf";
            case PDF_MULTI_COLUMN -> "multi-column.pdf";
            case PDF_FORMULA -> "formula.pdf";
            case XLSX_METRICS -> "metrics.xlsx";
            case XLSX_MULTI_HEADER -> "multi-header-metrics.xlsx";
            default -> throw new IllegalArgumentException("Unknown fixture: " + fixtureId);
        };
    }

    public static String mimeType(String fixtureId) {
        return fixtureId.startsWith("pdf")
                ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    public static Map<String, Object> metadata(String fixtureId) {
        if (PDF_MULTI_COLUMN.equals(fixtureId)) {
            return Map.of("readingOrderProvider", "heuristic");
        }
        return Map.of();
    }

    public static byte[] buildTablePdf() {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 720, "Name    Age");
                writeLine(stream, 72, 700, "Alice   30");
                writeLine(stream, 72, 680, "Bob     25");
            }
            return save(document);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build table PDF fixture", exception);
        }
    }

    public static byte[] buildMultiColumnPdf() {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 720, "Left column line one");
                writeLine(stream, 72, 700, "Left column line two");
                writeLine(stream, 320, 680, "Right column line one");
                writeLine(stream, 320, 660, "Right column line two");
            }
            return save(document);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build multi-column PDF fixture", exception);
        }
    }

    public static byte[] buildFormulaPdf() {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                writeLine(stream, 72, 720, "Equation $E=mc^2$ for citation eval.");
            }
            return save(document);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build formula PDF fixture", exception);
        }
    }

    public static byte[] buildMetricsWorkbook() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Metrics");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("姓名");
            header.createCell(1).setCellValue("部门");
            header.createCell(2).setCellValue("得分");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("张三");
            data.createCell(1).setCellValue("研发");
            data.createCell(2).setCellValue(95);
            return save(workbook);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build metrics XLSX fixture", exception);
        }
    }

    public static byte[] buildMultiHeaderWorkbook() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Metrics");
            Row yearHeader = sheet.createRow(0);
            yearHeader.createCell(0).setCellValue("姓名");
            yearHeader.createCell(1).setCellValue("2024");
            yearHeader.createCell(3).setCellValue("2025");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 2));
            Row quarterHeader = sheet.createRow(1);
            quarterHeader.createCell(1).setCellValue("Q1");
            quarterHeader.createCell(2).setCellValue("Q2");
            quarterHeader.createCell(3).setCellValue("Q1");
            Row data = sheet.createRow(2);
            data.createCell(0).setCellValue("张三");
            data.createCell(1).setCellValue(10);
            data.createCell(2).setCellValue(12);
            data.createCell(3).setCellValue(8);
            return save(workbook);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build multi-header XLSX fixture", exception);
        }
    }

    private static void writeLine(PDPageContentStream stream, float x, float y, String text) throws Exception {
        stream.beginText();
        stream.setFont(PDType1Font.HELVETICA, 12);
        stream.newLineAtOffset(x, y);
        stream.showText(text);
        stream.endText();
    }

    private static byte[] save(PDDocument document) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.save(outputStream);
        return outputStream.toByteArray();
    }

    private static byte[] save(Workbook workbook) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        return outputStream.toByteArray();
    }
}
