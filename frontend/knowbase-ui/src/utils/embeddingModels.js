/** Ollama 常用 Embedding 模型（API 不可用时的回退维度表） */
export const EMBEDDING_MODEL_FALLBACKS = [
  { value: 'nomic-embed-text', label: 'nomic-embed-text', dimension: 768 },
  { value: 'mxbai-embed-large', label: 'mxbai-embed-large', dimension: 1024 },
  { value: 'bge-m3', label: 'bge-m3', dimension: 1024 },
  { value: 'snowflake-arctic-embed', label: 'snowflake-arctic-embed', dimension: 1024 }
]

/** @deprecated 使用 API 目录；保留别名供旧引用 */
export const EMBEDDING_MODEL_OPTIONS = EMBEDDING_MODEL_FALLBACKS

export function mapEmbeddingCatalogModels(models = []) {
  return models.map((item) => ({
    value: item.modelId,
    label: item.modelId,
    dimension: item.dimension
  }))
}

export function dimensionForEmbeddingModel(model, options = EMBEDDING_MODEL_FALLBACKS) {
  const hit = options.find((o) => o.value === model)
  return hit?.dimension ?? 768
}

export function isKnownEmbeddingModel(model, options = EMBEDDING_MODEL_FALLBACKS) {
  return options.some((o) => o.value === model)
}

/** 列表等紧凑展示 */
export function labelForEmbeddingModel(model) {
  if (!model) return '—'
  return model
}

export function buildRerankModelOptions(embeddingModels = EMBEDDING_MODEL_FALLBACKS) {
  return [
    { value: '', label: '使用库 Embedding 模型（默认）' },
    ...embeddingModels.map((o) => ({
      value: o.value,
      label: `${o.label}（${o.dimension} 维）`
    }))
  ]
}

/** @deprecated 使用 buildRerankModelOptions(embeddingModels) */
export const RERANK_MODEL_OPTIONS = buildRerankModelOptions()

export const INDEX_PROVIDER_LABEL = 'Ollama'
export const INDEX_VECTOR_STORE_LABEL = 'PostgreSQL (pgvector)'
