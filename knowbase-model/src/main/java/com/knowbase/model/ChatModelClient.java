package com.knowbase.model;

public interface ChatModelClient {

    String provider();

    String modelName();

    ChatCompletion complete(ChatRequest request);
}
