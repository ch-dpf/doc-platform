package com.knowbase.tokenizer;

import java.util.List;

public interface TokenIdCapable {

    List<Integer> tokenizeToIds(String text);

    String detokenize(List<Integer> tokenIds);
}
