package com.knowbase.agent;

import java.util.Map;

public interface QuestionAnalyzer {

    QuestionAnalysis analyze(String question, Map<String, Object> routingPolicy);
}
