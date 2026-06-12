import { formatBool } from './libraryConfig'
import { labelForEmbeddingModel } from './embeddingModels'

/** 采集/详情等只读场景：格式化库配置摘要展示值 */
export function formatChunkDelimiter(value) {
  const v = value && String(value).trim()
  return v || '按 MIME 策略'
}

export function formatRerankModelSummary(model, embeddingModel) {
  const v = model && String(model).trim()
  if (!v) return `使用库 Embedding（${labelForEmbeddingModel(embeddingModel)}）`
  return labelForEmbeddingModel(v)
}

export function formatMetadataFilterFields(fields) {
  const list = fields || []
  return list.length ? list.join('、') : '—'
}

export function chunkParamsSummaryLine(summary) {
  if (!summary) return '—'
  const size = summary.chunkSize ?? '—'
  const overlap = summary.chunkOverlap ?? '—'
  const pct =
    summary.chunkSize > 0
      ? Math.round((summary.chunkOverlap / summary.chunkSize) * 100)
      : 0
  const parts = [`${size} 字`, `重叠 ${overlap} 字（≈${pct}%）`]
  parts.push(summary.hierarchicalChunkingEnabled ? '父子块开' : '父子块关')
  if (summary.chunkDelimiter && String(summary.chunkDelimiter).trim()) {
    parts.push('自定义分隔符')
  }
  return parts.join(' · ')
}

export function strategyRowsBrief(rows) {
  if (!rows?.length) return '加载策略摘要…'
  const brief = rows.map((r) => `${r.fileTypeLabel}·${r.chunkingStrategyLabel}`).join(' / ')
  return rows.length > 2 ? `${rows.length} 种类型：${brief}` : brief
}

export function formatRetrievalHybrid(summary) {
  return formatBool(summary?.hybridSearchEnabled !== false)
}

export function formatRetrievalRerank(summary) {
  return formatBool(summary?.rerankEnabled !== false)
}
