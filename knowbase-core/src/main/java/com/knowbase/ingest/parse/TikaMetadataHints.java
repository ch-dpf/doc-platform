package com.knowbase.ingest.parse;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

/**
 * 将库级解析选项写入 Tika Metadata，供正文抽取使用。
 */
public final class TikaMetadataHints {

    private TikaMetadataHints() {}

    public static void apply(Metadata metadata, DocumentParseOptions options) {
        if (options == null) {
            return;
        }
        String language = options.contentLanguage();
        if (language != null && !language.isBlank()) {
            metadata.set(TikaCoreProperties.LANGUAGE, language);
            metadata.set(Metadata.CONTENT_LANGUAGE, language);
        }
        if (!options.autoDetectEncoding()) {
            String encoding = options.contentEncoding();
            if (encoding != null && !encoding.isBlank()) {
                metadata.set(Metadata.CONTENT_ENCODING, encoding);
            }
        }
    }
}
