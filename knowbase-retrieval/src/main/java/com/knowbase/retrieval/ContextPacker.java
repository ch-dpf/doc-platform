package com.knowbase.retrieval;

import com.knowbase.domain.model.EvidencePack;
import com.knowbase.tokenizer.ModelTokenizer;

public interface ContextPacker {

    PackedContext pack(EvidencePack evidencePack, ModelTokenizer chatTokenizer, int maxContextTokens);
}
