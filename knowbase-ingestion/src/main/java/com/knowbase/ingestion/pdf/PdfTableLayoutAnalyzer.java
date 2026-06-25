package com.knowbase.ingestion.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects stream and borderless (aligned-column) PDF table runs from layout lines.
 */
public final class PdfTableLayoutAnalyzer {

    private static final float COLUMN_ANCHOR_TOLERANCE = 18f;

    private PdfTableLayoutAnalyzer() {
    }

    public static boolean isTableRun(List<?> lines) {
        if (lines == null || lines.size() < 2) {
            return false;
        }
        int tableLikeCount = 0;
        Integer previousColumns = null;
        Float previousAnchor = null;
        for (Object rawLine : lines) {
            if (!(rawLine instanceof TableLineCandidate candidate)) {
                continue;
            }
            if (candidate.tableLike()) {
                tableLikeCount++;
                continue;
            }
            if (PdfStreamTableDetector.isStreamTableRow(candidate.text())) {
                tableLikeCount++;
                previousColumns = PdfStreamTableDetector.cells(candidate.text()).size();
                continue;
            }
            int columns = columnCount(candidate);
            Float anchor = columnAnchor(candidate);
            if (columns >= 2 && previousColumns != null && previousColumns == columns
                    && previousAnchor != null && anchor != null
                    && Math.abs(anchor - previousAnchor) <= COLUMN_ANCHOR_TOLERANCE) {
                tableLikeCount++;
            }
            previousColumns = columns;
            previousAnchor = anchor;
        }
        return tableLikeCount >= 2;
    }

    public static String tableDetectionSource(List<?> lines) {
        if (lines == null || lines.isEmpty()) {
            return "unknown";
        }
        boolean aligned = false;
        boolean stream = false;
        boolean ruled = false;
        for (Object rawLine : lines) {
            if (!(rawLine instanceof TableLineCandidate candidate)) {
                continue;
            }
            if (candidate.cellBoundaryX() != null && candidate.cellBoundaryX().size() >= 3) {
                ruled = true;
            }
            if (PdfStreamTableDetector.isStreamTableRow(candidate.text())) {
                stream = true;
            }
            if (candidate.tableLike()) {
                aligned = true;
            }
        }
        if (ruled) {
            return "ruled-column";
        }
        if (stream) {
            return "stream";
        }
        if (aligned) {
            return "aligned-column";
        }
        return "borderless-heuristic";
    }

    private static int columnCount(TableLineCandidate line) {
        List<String> cells = PdfStreamTableDetector.cells(line.text());
        if (cells.size() >= 2) {
            return cells.size();
        }
        if (line.cellBoundaryX() != null && line.cellBoundaryX().size() >= 2) {
            return line.cellBoundaryX().size() - 1;
        }
        return 0;
    }

    private static Float columnAnchor(TableLineCandidate line) {
        if (line.cellBoundaryX() != null && !line.cellBoundaryX().isEmpty()) {
            return line.cellBoundaryX().getFirst();
        }
        return line.minX();
    }

    public interface TableLineCandidate {
        String text();

        float minX();

        boolean tableLike();

        List<Float> cellBoundaryX();
    }

    public static List<TableLineCandidate> adaptLines(List<?> lines) {
        List<TableLineCandidate> adapted = new ArrayList<>();
        for (Object line : lines) {
            if (line instanceof TableLineCandidate candidate) {
                adapted.add(candidate);
            }
        }
        return adapted;
    }
}
