package com.knowbase.retrieval;

import com.knowbase.domain.model.Citation;
import com.knowbase.domain.model.EvidencePack;
import com.knowbase.domain.model.EvidenceSegment;
import com.knowbase.tokenizer.ModelTokenizer;

import java.util.ArrayList;
import java.util.List;

public final class DefaultContextPacker implements ContextPacker {

    @Override
    public PackedContext pack(EvidencePack evidencePack, ModelTokenizer chatTokenizer, int maxContextTokens) {
        StringBuilder builder = new StringBuilder();
        List<Citation> citations = new ArrayList<>();
        int usedTokens = 0;
        int index = 1;
        for (EvidenceSegment segment : evidencePack.segments()) {
            String block = "[证据 " + index + "]\n"
                    + "library_id: " + segment.libraryId() + "\n"
                    + "document_id: " + segment.documentId() + "\n"
                    + "chunk_id: " + segment.chunkId() + "\n"
                    + "score: " + segment.score() + "\n"
                    + segment.content();
            int blockTokens = chatTokenizer.count(block).tokens();
            if (usedTokens + blockTokens > maxContextTokens) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append(block);
            usedTokens += blockTokens;
            Citation citation = evidencePack.citations().stream()
                    .filter(item -> item.chunkId().equals(segment.chunkId()))
                    .findFirst()
                    .orElse(null);
            if (citation != null) {
                citations.add(citation);
            }
            index++;
        }
        return new PackedContext(builder.toString(), citations, usedTokens);
    }
}
