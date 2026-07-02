package com.knowbase.ingestion;

import com.knowbase.domain.status.ContentFamily;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class QaDocumentParser implements DocumentParser {

    private static final Set<String> QUESTION_HEADERS = Set.of(
            "question", "questions", "q", "prompt", "query", "问题", "问", "提问", "标题"
    );
    private static final Set<String> ANSWER_HEADERS = Set.of(
            "answer", "answers", "a", "response", "reply", "content", "答案", "答", "回答", "内容"
    );

    private final DataFormatter formatter = new DataFormatter();

    @Override
    public boolean supports(String sourceUri, String mimeType) {
        return isTabularSource(sourceUri, mimeType);
    }

    public boolean supportsExplicit(String sourceUri, String mimeType, String preferredParser) {
        if (preferredParser != null && "qa".equalsIgnoreCase(preferredParser.trim())) {
            return isTabularSource(sourceUri, mimeType);
        }
        if (!isTabularSource(sourceUri, mimeType)) {
            return false;
        }
        String lower = normalize(sourceUri);
        return lower.contains("faq")
                || lower.contains("qa")
                || lower.contains("问答")
                || lower.contains("question");
    }

    @Override
    public ParsedDocument parse(DocumentSource source) {
        try {
            byte[] content = source.inputStream().readAllBytes();
            QaSheet sheet = parseContent(source.sourceUri(), source.filename(), content);
            if (sheet.pairs().isEmpty()) {
                throw new IllegalStateException("未识别到问答对列，请使用 question/answer 或 问/答 列头");
            }
            String text = formatPairs(sheet.pairs());
            Map<String, Object> metadata = new HashMap<>(source.metadata());
            metadata.put("parser", "qa");
            metadata.put("qaPairCount", sheet.pairs().size());
            metadata.put("questionColumn", sheet.questionColumn());
            metadata.put("answerColumn", sheet.answerColumn());
            return new ParsedDocument(
                    source.sourceUri(),
                    firstNonBlank(source.filename(), source.sourceUri()),
                    text,
                    ContentFamily.PLAIN_TEXT,
                    Map.copyOf(metadata)
            );
        } catch (IOException exception) {
            throw new IllegalStateException("问答对解析失败: " + source.sourceUri(), exception);
        }
    }

    private QaSheet parseContent(String sourceUri, String filename, byte[] content) throws IOException {
        String lower = normalize(firstNonBlank(filename, sourceUri));
        if (lower.endsWith(".csv")) {
            return parseCsv(content);
        }
        return parseSpreadsheet(new ByteArrayInputStream(content));
    }

    private QaSheet parseCsv(byte[] content) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add(splitCsvLine(line));
                }
            }
        }
        return toQaSheet(rows);
    }

    private QaSheet parseSpreadsheet(ByteArrayInputStream inputStream) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return new QaSheet(List.of(), "", "");
            }
            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                List<String> values = new ArrayList<>();
                for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
                    Cell cell = row.getCell(cellIndex);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
                }
                rows.add(values.toArray(String[]::new));
            }
        }
        return toQaSheet(rows);
    }

    private static QaSheet toQaSheet(List<String[]> rows) {
        if (rows.isEmpty()) {
            return new QaSheet(List.of(), "", "");
        }
        String[] headers = rows.getFirst();
        int questionIndex = findColumn(headers, QUESTION_HEADERS);
        int answerIndex = findColumn(headers, ANSWER_HEADERS);
        if (questionIndex < 0 || answerIndex < 0) {
            return new QaSheet(List.of(), "", "");
        }
        List<QaPair> pairs = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            String[] row = rows.get(index);
            String question = valueAt(row, questionIndex);
            String answer = valueAt(row, answerIndex);
            if (!question.isBlank() && !answer.isBlank()) {
                pairs.add(new QaPair(question, answer));
            }
        }
        return new QaSheet(
                pairs,
                headers[questionIndex],
                headers[answerIndex]
        );
    }

    private static int findColumn(String[] headers, Set<String> candidates) {
        for (int index = 0; index < headers.length; index++) {
            String normalized = normalizeHeader(headers[index]);
            if (candidates.contains(normalized)) {
                return index;
            }
        }
        return -1;
    }

    private static String valueAt(String[] row, int index) {
        return index >= 0 && index < row.length && row[index] != null ? row[index].trim() : "";
    }

    private static String formatPairs(List<QaPair> pairs) {
        StringBuilder builder = new StringBuilder();
        for (QaPair pair : pairs) {
            builder.append("问：").append(pair.question()).append('\n');
            builder.append("答：").append(pair.answer()).append("\n\n");
        }
        return builder.toString().trim();
    }

    private static String[] splitCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (ch == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString().trim());
        return values.toArray(String[]::new);
    }

    private static boolean isTabularSource(String sourceUri, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (lowerMime.contains("csv") || lowerMime.contains("excel") || lowerMime.contains("spreadsheet")) {
            return true;
        }
        String lowerUri = normalize(sourceUri);
        return lowerUri.endsWith(".csv") || lowerUri.endsWith(".xls") || lowerUri.endsWith(".xlsx");
    }

    private static String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "untitled";
    }

    private record QaPair(String question, String answer) {
    }

    private record QaSheet(List<QaPair> pairs, String questionColumn, String answerColumn) {
    }
}
