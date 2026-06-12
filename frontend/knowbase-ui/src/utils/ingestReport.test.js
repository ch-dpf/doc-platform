import { describe, expect, it } from 'vitest'
import { ingestReportSummary } from './ingestReport'

describe('ingestReportSummary', () => {
  it('includes pipeline trace fields from ingest report', () => {
    const summary = ingestReportSummary({
      rawChunkCount: 8,
      filteredOutCount: 1,
      finalChunkCount: 7,
      avgChunkLength: 420,
      contentFamily: 'document',
      chunkingStrategy: 'heading-level',
      multiGranularity: true,
      pipelineConfigVersion: 3
    })
    expect(summary.finalChunkCount).toBe(7)
    expect(summary.multiGranularity).toBe(true)
    expect(summary.familyLabel).toBe('文档类')
    expect(summary.configVersion).toBe(3)
  })
})
