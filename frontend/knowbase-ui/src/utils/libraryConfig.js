import { parserLabel } from './parserEngines'

const CHUNKING_LABELS = {
  auto: '自动（按文件类型）',
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
  'chunkingStrategy',
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

const PARSING_FIELD_LABELS = {
  defaultLanguage: '默认语言',
  autoDetectEncoding: '自动检测编码'
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
    key: 'chunkingStrategy',
    label: '分块策略',
    format: (v) => chunkingStrategyLabel(v)
  },
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

function parserRulesSignature(rules) {
  return JSON.stringify(
    (rules || [])
      .map((r) => `${r.fileType}:${r.parserId || 'auto'}`)
      .sort()
  )
}

function formatParserRulesDiff(beforeRules, afterRules) {
  const beforeMap = new Map((beforeRules || []).map((r) => [r.fileType, r.parserId || 'auto']))
  const afterMap = new Map((afterRules || []).map((r) => [r.fileType, r.parserId || 'auto']))
  const types = [...new Set([...beforeMap.keys(), ...afterMap.keys()])].sort()
  const detail = []
  for (const fileType of types) {
    const prev = beforeMap.get(fileType) || 'auto'
    const next = afterMap.get(fileType) || 'auto'
    if (prev !== next) {
      detail.push(`${fileType}: ${parserLabel(prev)} → ${parserLabel(next)}`)
    }
  }
  return detail
}

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

  const beforeParsing = before.parsing || {}
  const afterParsing = after.parsing || {}
  if (parserRulesSignature(beforeParsing.parserRules) !== parserRulesSignature(afterParsing.parserRules)) {
    const detail = formatParserRulesDiff(beforeParsing.parserRules, afterParsing.parserRules)
    pushChange(changes, {
      field: 'parsing.parserRules',
      label: '解析器规则',
      before: '—',
      after: '—',
      needsReindex: true,
      detail
    })
  }
  for (const [key, label] of Object.entries(PARSING_FIELD_LABELS)) {
    const prev = beforeParsing[key]
    const next = afterParsing[key]
    if (typeof prev === 'boolean' || typeof next === 'boolean') {
      if (prev !== next) {
        pushChange(changes, {
          field: `parsing.${key}`,
          label,
          before: formatBool(prev),
          after: formatBool(next),
          needsReindex: true
        })
      }
    } else if (prev !== next) {
      pushChange(changes, {
        field: `parsing.${key}`,
        label,
        before: prev,
        after: next,
        needsReindex: true
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
