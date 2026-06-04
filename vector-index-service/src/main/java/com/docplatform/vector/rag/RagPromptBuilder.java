package com.docplatform.vector.rag;

import com.docplatform.vector.config.RagProperties;
import com.docplatform.vector.dto.SearchHit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RagPromptBuilder {

    private final RagProperties ragProperties;

    public RagPromptBuilder(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public String buildUserMessage(String question, List<SearchHit> hits) {
        StringBuilder context = new StringBuilder();
        int used = 0;
        int index = 1;
        for (SearchHit hit : hits) {
            String block = formatReference(index, hit);
            if (used > 0 && used + block.length() > ragProperties.getMaxContextChars()) {
                break;
            }
            context.append(block);
            used += block.length();
            index++;
        }
        return """
                请仅根据下列「参考资料」回答「用户问题」。参考资料是回答的唯一依据。
                - 不得使用参考资料以外的任何信息。
                - 每个事实性陈述句末须标注引用编号，如 [1]。
                - 若无法从参考资料得出答案，请只回复这一句（不要其它内容）：%s

                【参考资料】
                %s
                【用户问题】
                %s
                """.formatted(RagAnswerTemplates.INSUFFICIENT_IN_PROMPT, context, question.trim());
    }

    public String excerpt(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.strip();
        int max = ragProperties.getExcerptMaxChars();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "…";
    }

    private String formatReference(int index, SearchHit hit) {
        return "[" + index + "] docId=" + hit.docId()
                + " chunk=" + hit.chunkIndex()
                + " score=" + String.format("%.4f", hit.score())
                + "\n" + hit.content().strip() + "\n\n";
    }
}
