/** Ollama 常用 Embedding 模型（维度用于库配置自动带出） */
export const EMBEDDING_MODEL_OPTIONS = [
  { value: 'nomic-embed-text', label: 'Nomic Embed Text', dimension: 768 },
  { value: 'mxbai-embed-large', label: 'MXBAI Embed Large', dimension: 1024 },
  { value: 'bge-m3', label: 'BGE-M3', dimension: 1024 },
  { value: 'snowflake-arctic-embed', label: 'Snowflake Arctic Embed', dimension: 1024 }
]

export function dimensionForEmbeddingModel(model) {
  const hit = EMBEDDING_MODEL_OPTIONS.find((o) => o.value === model)
  return hit?.dimension ?? 768
}

export function isKnownEmbeddingModel(model) {
  return EMBEDDING_MODEL_OPTIONS.some((o) => o.value === model)
}

/** 列表等紧凑展示：已知模型用短标签，否则回退原始模型名 */
export function labelForEmbeddingModel(model) {
  if (!model) return '—'
  const hit = EMBEDDING_MODEL_OPTIONS.find((o) => o.value === model)
  return hit ? hit.label : model
}

/**
 * Rerank 复用 Ollama Embedding 重算 query–chunk 余弦分；空值跟随库级 Embedding。
 */
export const RERANK_MODEL_OPTIONS = [
  { value: '', label: '使用库 Embedding 模型（默认）' },
  ...EMBEDDING_MODEL_OPTIONS.map((o) => ({
    value: o.value,
    label: `${o.label}（${o.dimension} 维）`
  }))
]
