import { SYSTEM_SUPPORTED_FILE_TYPES } from './libraryDefaults'

const CHUNKING_LABELS = {
  'paragraph-first': '按段落',
  'fixed-char': '固定长度',
  semantic: '语义分块',
  'heading-level': '按标题层级'
}

const INGEST_SOURCE_LABELS = {
  upload: '手动上传',
  crawl: '线上采集（历史）',
  both: '本地 + 线上（历史）',
  'manual-upload': '手动上传',
  'folder-sync': '文件夹批量接入'
}

const ACCESS_MODE_LABELS = {
  'upload-and-folder': '文件上传 + 文件夹批量',
  'manual-upload': '文件上传 + 文件夹批量',
  'folder-sync': '文件上传 + 文件夹批量'
}

export const FIXED_INGEST_ACCESS_LABEL = '文件上传 + 文件夹批量'

/** 影响向量索引结果的配置字段 */
const REINDEX_FIELDS = new Set([
  'chunkSize',
  'chunkOverlap',
  'hierarchicalChunkingEnabled',
  'chunkDelimiter',
  'semanticSimilarityThreshold',
  'embeddingProvider',
  'embeddingModel',
  'embeddingDimension'
])

const RETRIEVAL_FIELD_LABELS = {
  hybridSearchEnabled: '混合检索',
  rerankEnabled: '重排序',
  rerankModel: 'Rerank 模型',
  similarityThreshold: '相似度阈值',
  defaultTopK: '默认 Top K',
  metadataFilterFields: '过滤字段'
}

export function chunkingStrategyLabel(strategy) {
  return CHUNKING_LABELS[strategy] || strategy || '—'
}

export function ingestSourceLabel(mode) {
  return INGEST_SOURCE_LABELS[mode] || mode || '—'
}

export function accessModeLabel(mode) {
  return ACCESS_MODE_LABELS[mode] || FIXED_INGEST_ACCESS_LABEL
}

export function formatBool(value) {
  return value ? '开启' : '关闭'
}

function pushChange(changes, { field, label, before, after, needsReindex = false, detail }) {
  const format = (v) => (v === undefined || v === null ? '—' : String(v))
  if (before === after && !detail?.length) return
  changes.push({
    field,
    label,
    before: format(before),
    after: format(after),
    needsReindex,
    detail
  })
}

function arraysEqual(a, b) {
  return JSON.stringify([...(a || [])].sort()) === JSON.stringify([...(b || [])].sort())
}

const CONFIG_FIELD_SPECS = [
  { key: 'chunkSize', label: '分块大小' },
  { key: 'chunkOverlap', label: '分块重叠' },
  {
    key: 'hierarchicalChunkingEnabled',
    label: '父子块',
    format: (v) => (v ? '开启' : '关闭')
  },
  { key: 'chunkDelimiter', label: '自定义分隔符' },
  { key: 'semanticSimilarityThreshold', label: '语义相似度阈值' },
  { key: 'embeddingProvider', label: 'Embedding 提供方' },
  { key: 'embeddingModel', label: 'Embedding 模型' },
  { key: 'embeddingDimension', label: '向量维度' }
]

/**
 * @returns {{ field: string, label: string, before: string, after: string, needsReindex: boolean }[]}
 */
export function diffLibraryConfig(before = {}, after = {}) {
  const changes = []

  for (const spec of CONFIG_FIELD_SPECS) {
    const prev = before[spec.key]
    const next = after[spec.key]
    if (prev === next) continue
    const format = spec.format || ((v) => String(v ?? '—'))
    changes.push({
      field: spec.key,
      label: spec.label,
      before: format(prev),
      after: format(next),
      needsReindex: REINDEX_FIELDS.has(spec.key)
    })
  }

  const beforeRetrieval = before.retrieval || {}
  const afterRetrieval = after.retrieval || {}
  for (const [key, label] of Object.entries(RETRIEVAL_FIELD_LABELS)) {
    const prev = beforeRetrieval[key]
    const next = afterRetrieval[key]
    if (key === 'metadataFilterFields') {
      if (!arraysEqual(prev, next)) {
        pushChange(changes, {
          field: `retrieval.${key}`,
          label,
          before: (prev || []).join(', ') || '—',
          after: (next || []).join(', ') || '—'
        })
      }
      continue
    }
    if (typeof prev === 'boolean' || typeof next === 'boolean') {
      if (prev !== next) {
        pushChange(changes, {
          field: `retrieval.${key}`,
          label,
          before: formatBool(prev),
          after: formatBool(next)
        })
      }
    } else if (prev !== next) {
      pushChange(changes, {
        field: `retrieval.${key}`,
        label,
        before: prev,
        after: next
      })
    }
  }

  return changes
}

export function diffNeedsReindex(changes) {
  return changes.some((c) => c.needsReindex)
}

/** 库内已有解析/分块/向量数据时为 true，流水线相关配置应只读 */
export function hasIngestedContent(libraryOrCounts) {
  if (!libraryOrCounts) return false
  const doc = libraryOrCounts.documentCount ?? libraryOrCounts.docCount ?? 0
  const chunk = libraryOrCounts.chunkCount ?? 0
  return doc > 0 || chunk > 0
}

export function buildRulesSummary(library) {
  const cfg = library?.config || {}
  const retrieval = cfg.retrieval || {}
  return {
    name: library?.name || '',
    description: library?.description || '',
    tags: cfg.tags || [],
    configVersion: cfg.configVersion ?? 1,
    chunkSize: cfg.chunkSize ?? 500,
    chunkOverlap: cfg.chunkOverlap ?? 120,
    hierarchicalChunkingEnabled: cfg.hierarchicalChunkingEnabled !== false,
    chunkDelimiter: cfg.chunkDelimiter || '',
    embeddingModel: cfg.embeddingModel || 'nomic-embed-text',
    embeddingDimension: cfg.embeddingDimension ?? 768,
    hybridSearchEnabled: retrieval.hybridSearchEnabled !== false,
    rerankEnabled: retrieval.rerankEnabled !== false,
    rerankModel: retrieval.rerankModel || '',
    similarityThreshold: retrieval.similarityThreshold ?? 0.4,
    defaultTopK: retrieval.defaultTopK ?? 12,
    metadataFilterFields: retrieval.metadataFilterFields || [],
    documentCount: library?.documentCount ?? 0,
    chunkCount: library?.chunkCount ?? 0
  }
}
