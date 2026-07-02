package com.knowbase.ingestion.pdf;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTableLayoutAnalyzerTest {

    @Test
    void detectsBorderlessTableRunFromAlignedColumns() {
        List<PdfTableLayoutAnalyzer.TableLineCandidate> lines = List.of(
                line("Name    Age", 72f, List.of()),
                line("Alice   30", 72f, List.of()),
                line("Bob     25", 72f, List.of())
        );
        assertTrue(PdfTableLayoutAnalyzer.isTableRun(lines));
        assertEquals("stream", PdfTableLayoutAnalyzer.tableDetectionSource(lines));
    }

    @Test
    void detectsRuledTableFromCellBoundaries() {
        List<PdfTableLayoutAnalyzer.TableLineCandidate> lines = List.of(
                line("A B C", 72f, List.of(72f, 180f, 280f, 380f)),
                line("1 2 3", 72f, List.of(72f, 180f, 280f, 380f))
        );
        assertTrue(PdfTableLayoutAnalyzer.isTableRun(lines));
        assertEquals("ruled-column", PdfTableLayoutAnalyzer.tableDetectionSource(lines));
    }

    private static PdfTableLayoutAnalyzer.TableLineCandidate line(String text, float minX, List<Float> boundaries) {
        return new PdfTableLayoutAnalyzer.TableLineCandidate() {
            @Override
            public String text() {
                return text;
            }

            @Override
            public float minX() {
                return minX;
            }

            @Override
            public boolean tableLike() {
                return boundaries != null && boundaries.size() >= 3;
            }

            @Override
            public List<Float> cellBoundaryX() {
                return boundaries;
            }
        };
    }
}
