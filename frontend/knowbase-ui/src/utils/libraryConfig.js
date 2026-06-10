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

const NORM_FIELD_LABELS = {
  trimLines: '行首尾去空白',
  removeControlChars: '移除控制字符',
  normalizeUnicodeSpaces: '统一空格',
  collapseBlankLines: '合并空行',
  dropNoiseLines: '丢弃噪声行',
  minLineLength: '最短行',
  linePatternsToDrop: '行级清洗'
}

/** 影响向量索引结果的配置字段 */
const REINDEX_FIELDS = new Set([
  'textNormalizationEnabled',
  'chunkingStrategy',
  'chunkSize',
  'chunkOverlap',
  'minChunkSize',
  'maxChunkSize',
  'minParagraphLength',
  'normalizeBeforeChunk',
  'semanticSimilarityThreshold',
  'embeddingProvider',
  'embeddingModel',
  'embeddingDimension',
  'textNormalization',
  'parsing.ocrEnabled',
  'parsing.tableExtraction',
  'parsing.imageExtraction',
  'parsing.formulaExtraction',
  'cleaning.removeDuplicateParagraphs'
])

const TABLE_EXTRACTION_LABELS = {
  'text-only': '纯文本',
  structured: '结构化',
  skip: '跳过'
}

const IMAGE_EXTRACTION_LABELS = {
  'ocr-caption': 'OCR 描述',
  skip: '跳过'
}

const FORMULA_EXTRACTION_LABELS = {
  latex: 'LaTeX',
  skip: '跳过'
}

const RETRIEVAL_FIELD_LABELS = {
  hybridSearchEnabled: '混合检索',
  rerankEnabled: '重排序',
  rerankModel: 'Rerank 模型',
  similarityThreshold: '相似度阈值',
  metadataFilterFields: '过滤字段'
}

const GOVERNANCE_FIELD_LABELS = {
  ingestReviewMode: '入库审核',
  inheritLibraryPermissions: '权限继承',
  retentionDays: '保留天数',
  archivePolicy: '归档规则',
  auditLogEnabled: '审计日志'
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

function formatNormValue(key, value) {
  if (key === 'linePatternsToDrop') {
    const lines = Array.isArray(value) ? value : []
    return lines.length ? `${lines.length} 条` : '无'
  }
  if (typeof value === 'boolean') return formatBool(value)
  return value ?? '—'
}

function normSnapshot(norm = {}) {
  return {
    trimLines: norm.trimLines !== false,
    removeControlChars: norm.removeControlChars !== false,
    normalizeUnicodeSpaces: norm.normalizeUnicodeSpaces !== false,
    collapseBlankLines: norm.collapseBlankLines !== false,
    dropNoiseLines: norm.dropNoiseLines !== false,
    minLineLength: norm.minLineLength ?? 2,
    linePatternsToDrop: [...(norm.linePatternsToDrop || [])].sort()
  }
}

function patternsEqual(a, b) {
  const left = [...(a || [])].sort().join('\n')
  const right = [...(b || [])].sort().join('\n')
  return left === right
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

function normEqual(a, b) {
  const left = normSnapshot(a)
  const right = normSnapshot(b)
  return (
    left.trimLines === right.trimLines &&
    left.removeControlChars === right.removeControlChars &&
    left.normalizeUnicodeSpaces === right.normalizeUnicodeSpaces &&
    left.collapseBlankLines === right.collapseBlankLines &&
    left.dropNoiseLines === right.dropNoiseLines &&
    left.minLineLength === right.minLineLength &&
    patternsEqual(left.linePatternsToDrop, right.linePatternsToDrop)
  )
}

const CONFIG_FIELD_SPECS = [
  { key: 'textNormalizationEnabled', label: '文本清洗', format: (v) => formatBool(v) },
  { key: 'chunkingStrategy', label: '分块策略', format: (v) => chunkingStrategyLabel(v) },
  { key: 'chunkSize', label: '块大小' },
  { key: 'chunkOverlap', label: '块重叠' },
  { key: 'minChunkSize', label: '最小块' },
  { key: 'maxChunkSize', label: '最大块' },
  { key: 'minParagraphLength', label: '最短段落' },
  { key: 'semanticSimilarityThreshold', label: '语义相似度阈值' },
  { key: 'normalizeBeforeChunk', label: '分块前规范化', format: (v) => formatBool(v) },
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

  if (!normEqual(before.textNormalization, after.textNormalization)) {
    const prev = normSnapshot(before.textNormalization)
    const next = normSnapshot(after.textNormalization)
    const parts = []
    for (const [key, label] of Object.entries(NORM_FIELD_LABELS)) {
      if (JSON.stringify(prev[key]) !== JSON.stringify(next[key])) {
        parts.push(`${label}: ${formatNormValue(key, prev[key])} → ${formatNormValue(key, next[key])}`)
      }
    }
    changes.push({
      field: 'textNormalization',
      label: '清洗子规则',
      before: parts.length ? '见详情' : '—',
      after: parts.join('；') || '—',
      needsReindex: true,
      detail: parts
    })
  }

  const beforeParsing = before.parsing || {}
  const afterParsing = after.parsing || {}
  if (beforeParsing.ocrEnabled !== afterParsing.ocrEnabled) {
    pushChange(changes, {
      field: 'parsing.ocrEnabled',
      label: 'OCR',
      before: formatBool(beforeParsing.ocrEnabled),
      after: formatBool(afterParsing.ocrEnabled),
      needsReindex: true
    })
  }
  if (beforeParsing.defaultLanguage !== afterParsing.defaultLanguage) {
    pushChange(changes, {
      field: 'parsing.defaultLanguage',
      label: '默认语言',
      before: beforeParsing.defaultLanguage,
      after: afterParsing.defaultLanguage,
      needsReindex: true
    })
  }
  if (beforeParsing.autoDetectEncoding !== afterParsing.autoDetectEncoding) {
    pushChange(changes, {
      field: 'parsing.autoDetectEncoding',
      label: '自动识别编码',
      before: formatBool(beforeParsing.autoDetectEncoding),
      after: formatBool(afterParsing.autoDetectEncoding),
      needsReindex: true
    })
  }
  if (beforeParsing.tableExtraction !== afterParsing.tableExtraction) {
    pushChange(changes, {
      field: 'parsing.tableExtraction',
      label: '表格提取',
      before: TABLE_EXTRACTION_LABELS[beforeParsing.tableExtraction] || beforeParsing.tableExtraction,
      after: TABLE_EXTRACTION_LABELS[afterParsing.tableExtraction] || afterParsing.tableExtraction,
      needsReindex: true
    })
  }
  if (beforeParsing.imageExtraction !== afterParsing.imageExtraction) {
    pushChange(changes, {
      field: 'parsing.imageExtraction',
      label: '图片提取',
      before: IMAGE_EXTRACTION_LABELS[beforeParsing.imageExtraction] || beforeParsing.imageExtraction,
      after: IMAGE_EXTRACTION_LABELS[afterParsing.imageExtraction] || afterParsing.imageExtraction,
      needsReindex: true
    })
  }
  if (beforeParsing.formulaExtraction !== afterParsing.formulaExtraction) {
    pushChange(changes, {
      field: 'parsing.formulaExtraction',
      label: '公式提取',
      before: FORMULA_EXTRACTION_LABELS[beforeParsing.formulaExtraction] || beforeParsing.formulaExtraction,
      after: FORMULA_EXTRACTION_LABELS[afterParsing.formulaExtraction] || afterParsing.formulaExtraction,
      needsReindex: true
    })
  }

  const beforeCleaning = before.cleaning || {}
  const afterCleaning = after.cleaning || {}
  for (const [key, label] of [
    ['removeHeaderFooter', '去页眉页脚'],
    ['removeWatermark', '去水印'],
    ['removeDuplicateParagraphs', '去重复段落'],
    ['maskPhone', '手机号脱敏'],
    ['maskIdCard', '身份证脱敏'],
    ['stopwordFilter', '停用词过滤']
  ]) {
    if (beforeCleaning[key] !== afterCleaning[key]) {
      pushChange(changes, {
        field: `cleaning.${key}`,
        label,
        before: formatBool(beforeCleaning[key]),
        after: formatBool(afterCleaning[key]),
        needsReindex: REINDEX_FIELDS.has(`cleaning.${key}`)
      })
    }
  }

  const beforeIngest = before.ingestAccess || {}
  const afterIngest = after.ingestAccess || {}
  if (!arraysEqual(beforeIngest.supportedFileTypes, afterIngest.supportedFileTypes)) {
    pushChange(changes, {
      field: 'ingestAccess.supportedFileTypes',
      label: '数据类型',
      before: (beforeIngest.supportedFileTypes || []).join(', ') || '—',
      after: (afterIngest.supportedFileTypes || []).join(', ') || '—'
    })
  }
  const beforeCap = beforeIngest.capacityLimits || {}
  const afterCap = afterIngest.capacityLimits || {}
  for (const [key, label] of [
    ['maxDocuments', '容量-文档数'],
    ['maxTotalSizeBytes', '容量-总大小'],
    ['maxChunkEntries', '容量-向量条目']
  ]) {
    if (beforeCap[key] !== afterCap[key]) {
      const fmt = key === 'maxTotalSizeBytes'
        ? (v) => `${Math.round((v || 0) / (1024 ** 3))} GB`
        : (v) => v
      pushChange(changes, {
        field: `ingestAccess.capacityLimits.${key}`,
        label,
        before: fmt(beforeCap[key]),
        after: fmt(afterCap[key])
      })
    }
  }
  const beforeVer = beforeIngest.versionPolicy || {}
  const afterVer = afterIngest.versionPolicy || {}
  if (beforeVer.enabled !== afterVer.enabled) {
    pushChange(changes, {
      field: 'ingestAccess.versionPolicy.enabled',
      label: '版本管理',
      before: formatBool(beforeVer.enabled),
      after: formatBool(afterVer.enabled)
    })
  }
  if (beforeVer.updateStrategy !== afterVer.updateStrategy) {
    pushChange(changes, {
      field: 'ingestAccess.versionPolicy.updateStrategy',
      label: '版本更新策略',
      before: beforeVer.updateStrategy,
      after: afterVer.updateStrategy
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

  const beforeGov = before.governance || {}
  const afterGov = after.governance || {}
  for (const [key, label] of Object.entries(GOVERNANCE_FIELD_LABELS)) {
    const prev = beforeGov[key]
    const next = afterGov[key]
    if (typeof prev === 'boolean' || typeof next === 'boolean') {
      if (prev !== next) {
        pushChange(changes, {
          field: `governance.${key}`,
          label,
          before: formatBool(prev),
          after: formatBool(next)
        })
      }
    } else if (prev !== next) {
      pushChange(changes, {
        field: `governance.${key}`,
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
  const norm = cfg.textNormalization || {}
  const access = cfg.ingestAccess || {}
  return {
    configVersion: cfg.configVersion ?? 1,
    chunkingStrategy: chunkingStrategyLabel(cfg.chunkingStrategy),
    chunkSize: cfg.chunkSize ?? 600,
    chunkOverlap: cfg.chunkOverlap ?? 100,
    maxChunkSize: cfg.maxChunkSize ?? 1200,
    textNormalizationEnabled: cfg.textNormalizationEnabled !== false,
    normalizeBeforeChunk: cfg.normalizeBeforeChunk !== false,
    embeddingModel: cfg.embeddingModel || 'nomic-embed-text',
    embeddingDimension: cfg.embeddingDimension ?? 768,
    dropPatternCount: (norm.linePatternsToDrop || []).length,
    supportedFileTypes: access.supportedFileTypes || [],
    ingestAccessLabel: FIXED_INGEST_ACCESS_LABEL,
    documentCount: library?.documentCount ?? 0,
    chunkCount: library?.chunkCount ?? 0
  }
}
