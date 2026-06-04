package com.docplatform.vector.chunk;

public enum ChunkingStrategy {
    /** 优先按段落（空行）切分，过长段落再按字符窗口切 */
    PARAGRAPH_FIRST,
    /** 固定字符长度滑动窗口（兼容旧行为） */
    FIXED_CHAR
}
