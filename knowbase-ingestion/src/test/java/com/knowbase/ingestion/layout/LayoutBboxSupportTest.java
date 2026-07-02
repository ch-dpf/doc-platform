package com.knowbase.ingestion.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LayoutBboxSupportTest {

    @Test
    void convertsTopLeftCornerBoxToPdfPoints() {
        List<Double> pdf = LayoutBboxSupport.toPdfPoints(List.of(10d, 20d, 110d, 60d), 792d);
        assertNotNull(pdf);
        assertEquals(10d, pdf.get(0), 0.01d);
        assertEquals(732d, pdf.get(1), 0.01d);
        assertEquals(100d, pdf.get(2), 0.01d);
        assertEquals(40d, pdf.get(3), 0.01d);
    }

    @Test
    void convertsWidthHeightBoxToPdfPoints() {
        List<Double> pdf = LayoutBboxSupport.toPdfPoints(List.of(72d, 100d, 50d, 24d), 792d);
        assertNotNull(pdf);
        assertEquals(72d, pdf.get(0), 0.01d);
        assertEquals(668d, pdf.get(1), 0.01d);
        assertEquals(50d, pdf.get(2), 0.01d);
        assertEquals(24d, pdf.get(3), 0.01d);
    }
}
