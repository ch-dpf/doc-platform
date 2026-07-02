package com.knowbase.ingestion.summary;

/**
 * WeKnora-aligned long content sampling: head 60%, middle 20%, tail 20%.
 */
public final class LongContentSampler {

    public static final String OMIT_MARKER = "[...content omitted...]";

    private LongContentSampler() {
    }

    public static String sample(String content, int maxChars) {
        if (content == null || content.isBlank()) {
            return "";
        }
        if (content.length() <= maxChars) {
            return content;
        }
        int omitLen = OMIT_MARKER.length();
        int usable = maxChars - 2 * omitLen;
        if (usable < 100) {
            return truncate(content, maxChars);
        }
        int headLen = usable * 60 / 100;
        int tailLen = usable * 20 / 100;
        int midLen = usable - headLen - tailLen;

        String head = content.substring(0, headLen);
        String tail = content.substring(content.length() - tailLen);

        int midStart = content.length() / 2 - midLen / 2;
        if (midStart < headLen) {
            midStart = headLen;
        }
        int midEnd = midStart + midLen;
        if (midEnd > content.length() - tailLen) {
            midEnd = content.length() - tailLen;
            midStart = Math.max(headLen, midEnd - midLen);
        }
        String middle = content.substring(midStart, midEnd);
        return head + OMIT_MARKER + middle + OMIT_MARKER + tail;
    }

    private static String truncate(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)).trim() + "...";
    }
}
