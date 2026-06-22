package com.knowbase.model;

public final class DeterministicChatModelClient implements ChatModelClient {

    private final String provider;
    private final String modelName;

    public DeterministicChatModelClient(String provider, String modelName) {
        this.provider = provider;
        this.modelName = modelName;
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public ChatCompletion complete(ChatRequest request) {
        String context = request.context() == null ? "" : request.context().trim();
        String question = request.userMessage() == null ? "" : request.userMessage().trim();
        if (context.isBlank()) {
            String answer = "未找到足够证据，无法基于知识库回答该问题。";
            return new ChatCompletion(answer, estimateTokens(request.systemPrompt(), question, context), estimateTokens(answer), answer);
        }
        String answer = "基于知识库证据，针对问题「" + question + "」的回答如下：\n\n"
                + summarizeContext(context);
        return new ChatCompletion(
                answer,
                estimateTokens(request.systemPrompt(), question, context),
                estimateTokens(answer),
                answer
        );
    }

    private static String summarizeContext(String context) {
        String[] lines = context.split("\\R");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            builder.append("- ").append(line.trim()).append('\n');
            count++;
            if (count >= 5) {
                break;
            }
        }
        if (builder.isEmpty()) {
            return context;
        }
        return builder.toString().trim();
    }

    private static int estimateTokens(String... parts) {
        int length = 0;
        for (String part : parts) {
            if (part != null) {
                length += part.length();
            }
        }
        return Math.max(1, (int) Math.ceil(length / 4.0));
    }
}
