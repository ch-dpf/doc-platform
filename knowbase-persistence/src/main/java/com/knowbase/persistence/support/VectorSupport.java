package com.knowbase.persistence.support;

import com.pgvector.PGvector;

public final class VectorSupport {

    private VectorSupport() {
    }

    public static PGvector toPgVector(float[] values) {
        return new PGvector(values);
    }

    public static float[] fromPgVector(Object value) {
        if (value == null) {
            return new float[0];
        }
        if (value instanceof PGvector pgvector) {
            return pgvector.toArray();
        }
        if (value instanceof float[] array) {
            return array;
        }
        throw new IllegalArgumentException("不支持的向量类型: " + value.getClass());
    }
}
