package com.knowbase.ingest.support;

import com.knowbase.library.config.CleaningRulesSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentCleaningServiceTest {

    private final DocumentCleaningService service = new DocumentCleaningService();

    @Test
    void removesHeaderFooterLines() {
        CleaningRulesSettings cleaning = baseCleaning();
        cleaning.setRemoveHeaderFooter(true);
        cleaning.setRemoveDuplicateParagraphs(false);

        String input = "正文第一段。\n第 2 页\nPage 3 of 10\n正文第二段。";
        String result = service.apply(input, cleaning);

        assertTrue(result.contains("正文第一段"));
        assertTrue(result.contains("正文第二段"));
        assertFalse(result.contains("第 2 页"));
        assertFalse(result.contains("Page 3"));
    }

    @Test
    void removesWatermarkLines() {
        CleaningRulesSettings cleaning = baseCleaning();
        cleaning.setRemoveWatermark(true);
        cleaning.setRemoveDuplicateParagraphs(false);

        String input = "有效内容\n样本\nDRAFT\n继续阅读";
        String result = service.apply(input, cleaning);

        assertEquals("有效内容\n继续阅读", result);
    }

    @Test
    void masksPhoneAndIdCard() {
        CleaningRulesSettings cleaning = baseCleaning();
        cleaning.setMaskPhone(true);
        cleaning.setMaskIdCard(true);
        cleaning.setRemoveDuplicateParagraphs(false);

        String input = "联系 13812345678，证件 110101199001011234。";
        String result = service.apply(input, cleaning);

        assertTrue(result.contains("138****5678"));
        assertTrue(result.contains("110101********1234"));
        assertFalse(result.contains("13812345678"));
        assertFalse(result.contains("110101199001011234"));
    }

    @Test
    void filtersEnglishStopwordsOnSpacedLines() {
        CleaningRulesSettings cleaning = baseCleaning();
        cleaning.setStopwordFilter(true);
        cleaning.setRemoveDuplicateParagraphs(false);

        String input = "this is a sample line for testing";
        String result = service.apply(input, cleaning);

        assertEquals("sample line testing", result);
    }

    private static CleaningRulesSettings baseCleaning() {
        CleaningRulesSettings cleaning = new CleaningRulesSettings();
        cleaning.setRemoveHeaderFooter(false);
        cleaning.setRemoveWatermark(false);
        cleaning.setRemoveDuplicateParagraphs(false);
        cleaning.setMaskPhone(false);
        cleaning.setMaskIdCard(false);
        cleaning.setStopwordFilter(false);
        return cleaning;
    }
}
