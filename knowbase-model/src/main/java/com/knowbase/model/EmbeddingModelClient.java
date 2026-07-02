package com.knowbase.model;

import java.util.List;

public interface EmbeddingModelClient {

    String provider();

    String modelName();

    int dimension();

    List<float[]> embed(List<String> texts);
}
