import { describe, expect, it } from 'vitest'
import {
  contentFamilyLabel,
  contentSignalsSummary,
  pipelineTraceSummary,
  resolveContentFamily
} from './contentPipeline'

describe('contentPipeline', () => {
  it('resolves document family from pdf mime', () => {
    expect(resolveContentFamily('application/pdf', null)).toBe('document')
  })

  it('summarizes pipeline trace with multi granularity', () => {
    const trace = pipelineTraceSummary({
      contentFamily: 'document',
      chunkingStrategy: 'heading-level',
      chunkingAdjustmentReason: 'document-heading-density-upgrade',
      multiGranularity: true,
      pipelineConfigVersion: 2
    })
    expect(trace.familyLabel).toBe('文档类')
    expect(trace.multiGranularity).toBe(true)
    expect(trace.adjustmentLabel).toContain('标题')
  })

  it('summarizes content signals hints', () => {
    const summary = contentSignalsSummary({
      contentFamily: 'plain',
      textLength: 3000,
      shortDocument: false,
      markdownHeadings: true,
      headingLineRatio: 0.08
    })
    expect(summary.hints).toContain('含 Markdown 标题')
    expect(contentFamilyLabel('tabular')).toBe('表格类')
  })
})
