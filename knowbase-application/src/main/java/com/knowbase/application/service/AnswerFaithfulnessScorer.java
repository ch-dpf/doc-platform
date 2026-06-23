package com.knowbase.application.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class AnswerFaithfulnessScorer {

    private static final double MIN_SENTENCE_LENGTH = 12;
    private static final double MIN_TOKEN_OVERLAP = 0.35;

    private AnswerFaithfulnessScorer() {
    }

    static double score(String answer, List<String> evidenceContents) {
        if (answer == null || answer.isBlank()) {
            return 0.0;
        }
        List<String> sentences = splitSentences(answer);
        if (sentences.isEmpty()) {
            return 0.0;
        }
        int grounded = 0;
        for (String sentence : sentences) {
            if (isGrounded(sentence, evidenceContents)) {
                grounded++;
            }
        }
        return (double) grounded / sentences.size();
    }

    private static boolean isGrounded(String sentence, List<String> evidenceContents) {
        Set<String> sentenceTokens = tokens(sentence);
        if (sentenceTokens.size() < 3) {
            return true;
        }
        for (String evidence : evidenceContents) {
            if (evidence == null || evidence.isBlank()) {
                continue;
            }
            Set<String> evidenceTokens = tokens(evidence);
            if (evidenceTokens.isEmpty()) {
                continue;
            }
            int overlap = 0;
            for (String token : sentenceTokens) {
                if (evidenceTokens.contains(token)) {
                    overlap++;
                }
            }
            if ((double) overlap / sentenceTokens.size() >= MIN_TOKEN_OVERLAP) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitSentences(String answer) {
        List<String> sentences = new ArrayList<>();
        for (String part : answer.split("[。！？!?\\n]+")) {
            String trimmed = part.trim();
            if (trimmed.length() >= MIN_SENTENCE_LENGTH) {
                sentences.add(trimmed);
            }
        }
        if (sentences.isEmpty() && answer.trim().length() >= MIN_SENTENCE_LENGTH) {
            sentences.add(answer.trim());
        }
        return sentences;
    }

    private static Set<String> tokens(String text) {
        Set<String> tokens = new HashSet<>();
        for (String token : text.toLowerCase(Locale.ROOT).split("[\\s，,；;：:\\[\\]()（）\"'“”]+")) {
            if (token.length() > 1) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
