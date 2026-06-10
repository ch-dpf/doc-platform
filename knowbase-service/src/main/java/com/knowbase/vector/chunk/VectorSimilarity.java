package com.knowbase.vector.chunk;

/**
 * 向量余弦相似度。
 */
public final class VectorSimilarity {

    private VectorSimilarity() {}

    public static double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 0.0d;
        }
        double dot = 0.0d;
        double normLeft = 0.0d;
        double normRight = 0.0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            normLeft += left[i] * left[i];
            normRight += right[i] * right[i];
        }
        if (normLeft == 0.0d || normRight == 0.0d) {
            return 0.0d;
        }
        return dot / (Math.sqrt(normLeft) * Math.sqrt(normRight));
    }
}
