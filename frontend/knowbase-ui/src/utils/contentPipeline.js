import { chunkingStrategyLabel } from './libraryConfig'

/** @typedef {import('../api/ingest').DocumentIngestReport} DocumentIngestReport */
/** @typedef {import('../api/ingest').DocumentContentSignals} DocumentContentSignals */

export const CONTENT_FAMILY_LABELS = {
  tabular: '表格类',
  document: '文档类',
  plain: '纯文本类',
  image: '图片类',
  unknown: '未知'
}

export const CHUNKING_ADJUSTMENT_LABELS = {
  'tabular-family-force-paragraph-first': '表格族强制按段落',
  'code-fences-downgrade-semantic': '含代码围栏，降级语义分块',
  'short-document-downgrade-heading': '短文降级为按段落',
  'document-heading-density-upgrade': '标题密度高，升级为按标题',
  'markdown-headings-upgrade': 'Markdown 标题，升级为按标题',
  'plain-heading-density-upgrade': '标题密度高，升级为按标题'
}

/**
 * MIME / 文件名 → 内容族群（与后端 ContentFamilyResolver 对齐）
 * @param {string | null | undefined} mimeType
 * @param {string | null | undefined} fileName
 */
export function resolveContentFamily(mimeType, fileName) {
  if (mimeType) {
    const m = mimeType.toLowerCase()
    if (
      m.includes('spreadsheet') ||
      m.includes('excel') ||
      m.endsWith('.sheet') ||
      m === 'text/csv' ||
      m === 'text/tab-separated-values'
    ) {
      return 'tabular'
    }
    if (m.includes('pdf') || m.includes('word') || m.includes('msword') || m.includes('document')) {
      return 'document'
    }
    if (m.includes('text/plain') || m.includes('markdown') || m === 'text/x-markdown') {
      return 'plain'
    }
    if (m.startsWith('image/')) {
      return 'image'
    }
  }
  if (fileName) {
    const lower = fileName.toLowerCase()
    if (/\.(xls|xlsx|csv|tsv)$/.test(lower)) return 'tabular'
    if (/\.(pdf|doc|docx)$/.test(lower)) return 'document'
    if (/\.(txt|md|markdown)$/.test(lower)) return 'plain'
    if (/\.(png|jpe?g|tif|tiff|bmp|gif)$/.test(lower)) return 'image'
  }
  return 'unknown'
}

export function contentFamilyLabel(wire) {
  if (!wire) return '—'
  return CONTENT_FAMILY_LABELS[wire] || wire
}

export function chunkingAdjustmentLabel(reason) {
  if (!reason) return null
  return CHUNKING_ADJUSTMENT_LABELS[reason] || reason
}

/**
 * @param {Partial<DocumentIngestReport> & { contentFamily?: string, chunkingStrategy?: string, chunkingAdjustmentReason?: string, multiGranularity?: boolean } | null | undefined} trace
 */
export function pipelineTraceSummary(trace) {
  if (!trace) return null
  const family = trace.contentFamily
  const strategy = trace.chunkingStrategy
  const adjustment = chunkingAdjustmentLabel(trace.chunkingAdjustmentReason)
  const multi = trace.multiGranularity === true
  if (!family && !strategy && !adjustment && !multi) return null
  return {
    familyLabel: contentFamilyLabel(family),
    strategyLabel: chunkingStrategyLabel(strategy),
    adjustmentLabel: adjustment,
    multiGranularity: multi,
    configVersion: trace.pipelineConfigVersion ?? null
  }
}

/**
 * @param {DocumentContentSignals | null | undefined} signals
 */
export function contentSignalsSummary(signals) {
  if (!signals || signals.textLength == null) return null
  const hints = []
  if (signals.shortDocument) hints.push('短文')
  if (signals.markdownHeadings) hints.push('含 Markdown 标题')
  if (signals.codeFences) hints.push('含代码围栏')
  if (signals.headingLineRatio != null && signals.headingLineRatio >= 0.05) {
    hints.push(`标题行占比 ${Math.round(signals.headingLineRatio * 100)}%`)
  }
  if (signals.tabularLineRatio != null && signals.tabularLineRatio >= 0.3) {
    hints.push(`表格行占比 ${Math.round(signals.tabularLineRatio * 100)}%`)
  }
  return {
    textLength: signals.textLength,
    hints,
    familyLabel: contentFamilyLabel(signals.contentFamily)
  }
}
