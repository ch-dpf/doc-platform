package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import com.knowbase.ingestion.adaptive.AdaptiveTableSheetProcessor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结构化表格文档解析器
 * <p>支持 CSV、Excel (.xls/.xlsx)、ODS 等表格格式的解析。
 * 采用自适应表格处理策略，能够识别表头、合并单元格、公式等复杂结构，
 * 并生成结构化的文本块用于后续的 RAG 检索。</p>
 */
public final class StructuredTableDocumentParser implements DocumentParser {

    /** Excel 日期格式化模板 */
    private static final DateTimeFormatter EXCEL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** Apache Tika 实例，用于降级解析和 MIME 类型检测 */
    private final Tika tika = new Tika();
    /** POI 数据格式化器，负责将单元格值转换为字符串表示 */
    private final DataFormatter formatter = new DataFormatter();

    /**
     * 判断是否支持指定类型的文档
     *
     * @param sourceUri 文档 URI（通常包含文件扩展名）
     * @param mimeType  MIME 类型标识
     * @return 如果是 CSV 或电子表格格式返回 true
     */
    @Override
    public boolean supports(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase();
        if (lowerMime.contains("csv")
                || lowerMime.contains("spreadsheet")
                || lowerMime.contains("excel")
                || lowerMime.contains("sheet")) {
            return true;
        }
        String lowerUri = sourceUri == null ? "" : sourceUri.toLowerCase();
        return lowerUri.endsWith(".csv")
                || lowerUri.endsWith(".xls")
                || lowerUri.endsWith(".xlsx")
                || lowerUri.endsWith(".ods");
    }

    /**
     * 解析文档源为结构化文档对象
     * <p>根据文件扩展名选择 CSV 或电子表格解析策略，若解析结果为空则降级使用 Tika 解析。</p>
     *
     * @param source 文档源，包含输入流、URI、元数据等信息
     * @return 解析后的文档对象，包含文本内容、结构化块和元数据
     * @throws IllegalStateException 当解析过程中发生 IO 异常时抛出
     */
    @Override
    public ParsedDocument parse(DocumentSource source) {
        String lowerUri = source.sourceUri() == null ? "" : source.sourceUri().toLowerCase();
        try {
            TableParseResult table;
            Map<String, Object> parsedMetadata = new HashMap<>(source.metadata());
            if (lowerUri.endsWith(".csv")) {
                table = parseCsv(new String(source.inputStream().readAllBytes(), StandardCharsets.UTF_8));
                parsedMetadata.put("tableFormat", "csv");
            } else {
                table = parseSpreadsheet(source);
                parsedMetadata.put("tableFormat", "spreadsheet");
            }
            String text = table.text();
            if (text == null || text.isBlank()) {
                Metadata metadata = new Metadata();
                text = tika.parseToString(source.inputStream(), metadata);
                parsedMetadata.put("fallbackParser", "tika");
                table = new TableParseResult(text, List.of());
            }
            parsedMetadata.put("parser", "table-deep");
            parsedMetadata.put("parserEngine", "table-adaptive");
            parsedMetadata.put("rowSerialization", "adaptive");
            parsedMetadata.put("columnKeyStrategy", "header_then_letter");
            parsedMetadata.put("rowGroupCount", table.rowGroupCount());
            parsedMetadata.put("structureAware", !table.blocks().isEmpty());
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    text,
                    ContentFamily.STRUCTURED_TABLE,
                    parsedMetadata,
                    table.blocks()
            );
        } catch (IOException | TikaException exception) {
            throw new IllegalStateException("表格深度解析失败: " + source.sourceUri(), exception);
        }
    }

    private TableParseResult parseSpreadsheet(DocumentSource source) throws IOException {
        StringBuilder builder = new StringBuilder();
        List<StructuralBlock> blocks = new ArrayList<>();
        int ordinal = 0;
        try (Workbook workbook = WorkbookFactory.create(source.inputStream())) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
                int columnCount = maxColumnCount(sheet);
                AdaptiveTableSheetProcessor.SheetParseResult sheetResult = AdaptiveTableSheetProcessor.process(
                        sheet,
                        sheetIndex,
                        columnCount,
                        evaluator,
                        mergedRegions,
                        this::readRowValues,
                        ordinal
                );
                builder.append(sheetResult.sheetText());
                blocks.addAll(sheetResult.blocks());
                ordinal += sheetResult.blocks().size();
            }
        }
        return new TableParseResult(builder.toString(), blocks);
    }

    private TableParseResult parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return new TableParseResult("", List.of());
        }
        String[] lines = csv.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        char delimiter = detectDelimiter(lines);
        List<List<String>> rowValues = new ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                rowValues.add(List.of());
                continue;
            }
            rowValues.add(parseDelimitedLine(line, delimiter));
        }
        AdaptiveTableSheetProcessor.SheetParseResult sheetResult =
                AdaptiveTableSheetProcessor.processCsvRows("CSV", rowValues, 0);
        if (sheetResult.blocks().isEmpty()) {
            return new TableParseResult(csv, List.of(StructuralBlock.tableRow(csv.trim(), 0, 0)));
        }
        return new TableParseResult(sheetResult.sheetText(), sheetResult.blocks());
    }

    private List<String> readRowValues(
            Sheet sheet,
            int rowIndex,
            int columnCount,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions
    ) {
        List<String> values = new ArrayList<>(columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            values.add(cellValue(sheet, rowIndex, columnIndex, evaluator, mergedRegions));
        }
        return values;
    }

    private String cellValue(
            Sheet sheet,
            int rowIndex,
            int columnIndex,
            FormulaEvaluator evaluator,
            List<CellRangeAddress> mergedRegions
    ) {
        Row row = sheet.getRow(rowIndex);
        Cell cell = row == null ? null : row.getCell(columnIndex);
        String value = formatCell(cell, evaluator);
        if (!value.isBlank() || mergedRegions == null || mergedRegions.isEmpty()) {
            return value;
        }
        for (CellRangeAddress range : mergedRegions) {
            if (rowIndex < range.getFirstRow() || rowIndex > range.getLastRow()
                    || columnIndex < range.getFirstColumn() || columnIndex > range.getLastColumn()) {
                continue;
            }
            Row masterRow = sheet.getRow(range.getFirstRow());
            if (masterRow == null) {
                return "";
            }
            return formatCell(masterRow.getCell(range.getFirstColumn()), evaluator);
        }
        return value;
    }

    private static int maxColumnCount(Sheet sheet) {
        int max = 0;
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row != null) {
                max = Math.max(max, row.getLastCellNum());
            }
        }
        return max;
    }

    private String formatCell(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            try {
                return EXCEL_DATE_FORMAT.format(cell.getLocalDateTimeCellValue());
            } catch (RuntimeException ignored) {
                // fall through to DataFormatter
            }
        }
        if (evaluator == null) {
            return formatter.formatCellValue(cell).trim();
        }
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private static char detectDelimiter(String[] lines) {
        String sample = "";
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                sample = line;
                break;
            }
        }
        int comma = count(sample, ',');
        int tab = count(sample, '\t');
        int semicolon = count(sample, ';');
        if (tab >= comma && tab >= semicolon) {
            return '\t';
        }
        if (semicolon > comma) {
            return ';';
        }
        return ',';
    }

    private static int count(String value, char delimiter) {
        int count = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) == delimiter) {
                count++;
            }
        }
        return count;
    }

    private static List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
                continue;
            }
            if (ch == delimiter && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString().trim());
        return values;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }

    private record TableParseResult(String text, List<StructuralBlock> blocks) {
        private TableParseResult {
            text = text == null ? "" : text;
            blocks = blocks == null ? List.of() : List.copyOf(blocks);
        }

        private int rowGroupCount() {
            return blocks.isEmpty()
                    ? (int) text.lines().filter(line -> line.contains(": ")).count()
                    : blocks.size();
        }
    }
}
