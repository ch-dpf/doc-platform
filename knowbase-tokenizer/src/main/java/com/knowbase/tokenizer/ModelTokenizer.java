package com.knowbase.tokenizer;

import java.util.List;

public interface ModelTokenizer {

    String tokenizerId();

    String tokenizerVersion();

    boolean approximate();

    TokenCount count(String text);

    List<String> encode(String text);
}
