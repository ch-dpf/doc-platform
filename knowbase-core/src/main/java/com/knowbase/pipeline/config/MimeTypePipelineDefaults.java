package com.knowbase.pipeline.config;

import com.knowbase.library.config.CleaningRulesSettings;
import com.knowbase.library.config.ParsingRulesSettings;
import com.knowbase.pipeline.content.ContentFamily;
import com.knowbase.pipeline.content.ContentFamilyPipelineDefaults;
import com.knowbase.pipeline.content.ContentFamilyResolver;
import org.springframework.stereotype.Component;

/**
 * 按 MIME 应用解析/清洗默认（FILE-TYPE-PROCESSING 权威来源）。
 * 分块由库级配置统一；内部委托 {@link ContentFamilyResolver} + {@link ContentFamilyPipelineDefaults}。
 */
@Component
public class MimeTypePipelineDefaults {

    private final ContentFamilyPipelineDefaults familyDefaults;

    public MimeTypePipelineDefaults(ContentFamilyPipelineDefaults familyDefaults) {
        this.familyDefaults = familyDefaults;
    }

    public void apply(String mimeType, ParsingRulesSettings parsing, CleaningRulesSettings cleaning) {
        if (mimeType == null) {
            return;
        }
        ContentFamily family = ContentFamilyResolver.resolve(mimeType);
        familyDefaults.apply(family, mimeType, parsing, cleaning);
    }
}
