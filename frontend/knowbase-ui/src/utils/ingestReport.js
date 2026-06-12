/**
 * v2 GATE-01/02 入库质量报告展示辅助。
 */

import { contentFamilyLabel, contentSignalsSummary, pipelineTraceSummary } from './contentPipeline'
import { chunkingStrategyLabel } from './libraryConfig'

export const GATE01_WARNING_TITLE = 'GATE-01：表头块占比过高'
export const GATE01_WARNING_DESC =
  '超过半数分块被识别为表头/低价值片段并已过滤，检索质量可能受影响，建议检查源文档或调整分块策略。'

/**
 * @param {import('../api/ingest').DocumentIngestReport | null | undefined} report
 */
export function hasHeaderOnlyWarning(report) {
  return report?.headerOnlyRatioWarning === true
}

/**
 * @param {number | null | undefined} value
 */
export function formatAvgChunkLength(value) {
  if (value == null || Number.isNaN(value)) return '—'
  return Math.round(value).toLocaleString()
}

/**
 * @param {import('../api/ingest').DocumentIngestReport | null | undefined} report
 */
export function ingestReportSummary(report) {
  if (!report) return null
  const trace = pipelineTraceSummary(report)
  return {
    rawChunkCount: report.rawChunkCount ?? 0,
    filteredOutCount: report.filteredOutCount ?? 0,
    finalChunkCount: report.finalChunkCount ?? 0,
    avgChunkLength: formatAvgChunkLength(report.avgChunkLength),
    headerOnlyRatioWarning: hasHeaderOnlyWarning(report),
    pipelineTrace: trace,
    familyLabel: trace?.familyLabel ?? contentFamilyLabelFromReport(report),
    strategyLabel: trace?.strategyLabel ?? chunkingStrategyLabel(report.chunkingStrategy),
    adjustmentLabel: trace?.adjustmentLabel ?? null,
    multiGranularity: trace?.multiGranularity ?? report.multiGranularity === true,
    configVersion: report.pipelineConfigVersion ?? null
  }
}

function contentFamilyLabelFromReport(report) {
  return contentFamilyLabel(report.contentFamily)
}

/**
 * 文档级入库诊断：报告 + 内容信号
 * @param {{ ingestReport?: import('../api/ingest').DocumentIngestReport, contentSignals?: import('../api/ingest').DocumentContentSignals } | null | undefined} doc
 */
export function documentIngestDiagnostics(doc) {
  if (!doc) return null
  return {
    report: ingestReportSummary(doc.ingestReport),
    signals: contentSignalsSummary(doc.contentSignals)
  }
}

/**
 * Poll document detail until ingest report or terminal index status.
 * @param {(docId: string) => Promise<{ data?: object }>} fetchDocument
 */
export async function pollDocumentIngestReport(docId, fetchDocument, options = {}) {
  const maxAttempts = options.maxAttempts ?? 15
  const intervalMs = options.intervalMs ?? 2000
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    const { data } = await fetchDocument(docId)
    if (data?.ingestReport) return data
    if (data?.indexStatus === 'INDEXED' || data?.indexStatus === 'FAILED') {
      return data
    }
    if (attempt < maxAttempts - 1) {
      await new Promise((resolve) => setTimeout(resolve, intervalMs))
    }
  }
  return null
}
