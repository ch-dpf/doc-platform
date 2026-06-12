/**
 * 将分节 libraryConfig API 视图展平为表单/规则摘要使用的 config 结构。
 */

export function flattenLibraryConfig(lib) {
  const lc = lib?.libraryConfig
  if (!lc) {
    return {}
  }
  const pipe = lc.indexPipeline || {}
  return {
    configVersion: lc.configVersion ?? 1,
    metadataDbType: lc.metadataDbType || 'postgresql',
    tags: lc.tags || [],
    chunkSize: pipe.chunkSize,
    chunkOverlap: pipe.chunkOverlap,
    hierarchicalChunkingEnabled: pipe.hierarchicalChunkingEnabled ?? true,
    chunkDelimiter: pipe.chunkDelimiter ?? '',
    embeddingProvider: 'ollama',
    embeddingModel: pipe.embeddingModel,
    embeddingDimension: pipe.embeddingDimension,
    retrieval: lc.retrieval || {},
    primaryChunkProfileId: lc.primaryChunkProfileId || '',
    allowCustomChunkProfiles: lc.allowCustomChunkProfiles !== false,
    maxActiveChunkProfiles: lc.maxActiveChunkProfiles > 0 ? lc.maxActiveChunkProfiles : 5
  }
}

export function buildIndexPipelinePayload(cfg) {
  return {
    indexPipeline: {
      chunkSize: cfg.chunkSize,
      chunkOverlap: cfg.chunkOverlap,
      embeddingModel: cfg.embeddingModel,
      embeddingDimension: cfg.embeddingDimension,
      hierarchicalChunkingEnabled: cfg.hierarchicalChunkingEnabled !== false,
      chunkDelimiter: cfg.chunkDelimiter || ''
    }
  }
}

export function buildRetrievalPayload(cfg) {
  return { retrieval: cfg.retrieval || {} }
}

const PIPELINE_FIELDS = [
  'chunkSize',
  'chunkOverlap',
  'hierarchicalChunkingEnabled',
  'chunkDelimiter',
  'embeddingProvider',
  'embeddingModel',
  'embeddingDimension'
]

export function libraryWithFlatConfig(lib) {
  if (!lib) return null
  return {
    ...lib,
    config: flattenLibraryConfig(lib)
  }
}

export function pickPipelineSnapshot(cfg) {
  const out = {}
  for (const key of PIPELINE_FIELDS) {
    if (cfg?.[key] !== undefined) {
      out[key] = cfg[key]
    }
  }
  return out
}

const PIPELINE_FIELD_SET = new Set(PIPELINE_FIELDS)

export function hasPipelineChanges(changes = []) {
  return changes.some((c) => PIPELINE_FIELD_SET.has(c.field))
}

export function hasRetrievalChanges(changes = []) {
  return changes.some((c) => String(c.field || '').startsWith('retrieval.'))
}

export function normalizeSubmitConfig(cfg) {
  const copy = JSON.parse(JSON.stringify(cfg))
  copy.embeddingProvider = 'ollama'
  return copy
}
