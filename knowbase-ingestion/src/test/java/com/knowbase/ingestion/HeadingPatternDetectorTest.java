package com.knowbase.ingestion;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadingPatternDetectorTest {

    @Test
    void detectsChineseAndNumberedHeadings() {
        assertEquals(1, HeadingPatternDetector.detectLevel("一、系统概述").orElse(0));
        assertEquals(2, HeadingPatternDetector.detectLevel("1.1 开发目的").orElse(0));
        assertEquals(3, HeadingPatternDetector.detectLevel("2.1.1 客户端（浏览器）").orElse(0));
        assertEquals(1, HeadingPatternDetector.detectLevel("1、浮标运维与资产管理系统").orElse(0));
    }

    @Test
    void ignoresLongBodyAndListItems() {
        assertTrue(HeadingPatternDetector.detectLevel("开发目的: " + "本".repeat(80)).isEmpty());
        assertTrue(HeadingPatternDetector.detectLevel("☑ HTML；☑ Java；").isEmpty());
    }
}
