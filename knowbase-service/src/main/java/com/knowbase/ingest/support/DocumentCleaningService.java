package com.knowbase.ingest.support;

import com.knowbase.library.config.CleaningRulesSettings;
import org.springframework.stereotype.Component;

@Component
public class DocumentCleaningService {

    public String apply(String text, CleaningRulesSettings cleaning) {
        if (text == null || cleaning == null) {
            return text;
        }
        String result = text;
        if (cleaning.isRemoveHeaderFooter()) {
            result = ContentCleaningEngine.removeHeaderFooterLines(result);
        }
        if (cleaning.isRemoveWatermark()) {
            result = ContentCleaningEngine.removeWatermarkLines(result);
        }
        if (cleaning.isMaskPhone()) {
            result = ContentCleaningEngine.maskPhoneNumbers(result);
        }
        if (cleaning.isMaskIdCard()) {
            result = ContentCleaningEngine.maskIdCardNumbers(result);
        }
        if (cleaning.isStopwordFilter()) {
            result = ContentCleaningEngine.filterStopwords(result);
        }
        if (cleaning.isRemoveDuplicateParagraphs()) {
            result = DuplicateParagraphCleaner.removeDuplicates(result);
        }
        return result;
    }
}
