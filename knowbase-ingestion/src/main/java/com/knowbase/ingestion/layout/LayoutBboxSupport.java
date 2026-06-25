package com.knowbase.ingestion.layout;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts layout-detection bbox (top-left image origin) to PDF-style bbox
 * {@code [x, y, width, height]} with bottom-left origin.
 */
public final class LayoutBboxSupport {

    private LayoutBboxSupport() {
    }

    public static List<Double> readBbox(JsonNode bboxNode) {
        if (bboxNode == null || !bboxNode.isArray() || bboxNode.size() < 4) {
            return null;
        }
        List<Double> values = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            values.add(bboxNode.get(index).asDouble());
        }
        return values;
    }

    public static List<Double> toPdfPoints(List<Double> raw, double pageHeight) {
        if (raw == null || raw.size() < 4 || pageHeight <= 0) {
            return null;
        }
        double first = raw.get(0);
        double second = raw.get(1);
        double third = raw.get(2);
        double fourth = raw.get(3);
        double x;
        double yTop;
        double width;
        double height;
        if (third > first && fourth > second) {
            x = first;
            yTop = second;
            width = third - first;
            height = fourth - second;
        } else {
            x = first;
            yTop = second;
            width = third;
            height = fourth;
        }
        width = Math.max(1d, width);
        height = Math.max(1d, height);
        double yBottom = pageHeight - yTop - height;
        return List.of(round(x), round(Math.max(0d, yBottom)), round(width), round(height));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
