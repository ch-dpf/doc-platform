package com.knowbase.vector.chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将文本切分为句子，供语义分块使用。
 */
public final class SentenceSplitter {

    private static final Pattern SENTENCE_END =
            Pattern.compile("(?<=[。！？!?；;])\\s*|(?<=\\.\\s)(?=[A-Z0-9\"'])");

    private SentenceSplitter() {}

    public static List<String> split(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_END.matcher(text.replace('\r', ' ').strip());
        int start = 0;
        while (matcher.find()) {
            appendSentence(sentences, text.substring(start, matcher.start()));
            start = matcher.end();
        }
        appendSentence(sentences, text.substring(start));
        if (sentences.isEmpty()) {
            appendSentence(sentences, text.strip());
        }
        return sentences;
    }

    private static void appendSentence(List<String> sentences, String raw) {
        String sentence = raw == null ? "" : raw.strip();
        if (!sentence.isEmpty()) {
            sentences.add(sentence);
        }
    }
}
