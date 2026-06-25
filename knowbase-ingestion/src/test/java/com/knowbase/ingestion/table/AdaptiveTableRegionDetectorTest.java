package com.knowbase.ingestion.table;

import com.knowbase.ingestion.adaptive.TableRowRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdaptiveTableRegionDetectorTest {

    @Test
    void layoutStartsRegion() {
        assertTrue(AdaptiveTableRegionDetector.shouldStartRegion(
                TableRowRole.LAYOUT,
                List.of("下周工作计划"),
                1
        ));
    }

    @Test
    void separatorDoesNotStartRegion() {
        assertFalse(AdaptiveTableRegionDetector.shouldStartRegion(
                TableRowRole.SEPARATOR,
                List.of("2026年5月06日--5月09日"),
                1
        ));
    }
}
