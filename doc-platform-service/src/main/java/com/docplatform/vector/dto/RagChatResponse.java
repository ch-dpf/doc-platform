package com.docplatform.vector.dto;

import java.util.List;

public record RagChatResponse(
        String answer,
        List<RagCitation> citations,
        int retrievedCount,
        boolean usedLlm,
        /** 是否在知识库中检索到可用片段（false 时不应出现编造内容） */
        boolean found
) {
}
