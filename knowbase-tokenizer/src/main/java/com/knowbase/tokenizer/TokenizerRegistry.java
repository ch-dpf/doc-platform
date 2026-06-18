package com.knowbase.tokenizer;

public interface TokenizerRegistry {

    ModelTokenizer getTokenizer(String provider, String modelName);
}
