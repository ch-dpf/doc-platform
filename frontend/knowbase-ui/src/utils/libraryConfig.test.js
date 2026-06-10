import { describe, it, expect } from 'vitest'
import { diffLibraryConfig, diffNeedsReindex } from './libraryConfig'
import { defaultLibraryConfig } from './libraryDefaults'

function baseConfig(overrides = {}) {
  return { ...defaultLibraryConfig('quick'), ...overrides }
}

describe('diffLibraryConfig', () => {
  it('detects tableExtraction change with needsReindex', () => {
    const before = baseConfig()
    const after = baseConfig({
      parsing: { ...before.parsing, tableExtraction: 'structured' }
    })
    const changes = diffLibraryConfig(before, after)
    expect(changes).toHaveLength(1)
    expect(changes[0].field).toBe('parsing.tableExtraction')
    expect(changes[0].needsReindex).toBe(true)
    expect(diffNeedsReindex(changes)).toBe(true)
  })

  it('detects ocrEnabled toggle', () => {
    const before = baseConfig()
    const after = baseConfig({
      parsing: { ...before.parsing, ocrEnabled: true }
    })
    const changes = diffLibraryConfig(before, after)
    expect(changes.some((c) => c.field === 'parsing.ocrEnabled')).toBe(true)
  })

  it('detects chunkingStrategy, chunkSize, minParagraphLength at root', () => {
    const before = baseConfig()
    const after = baseConfig({
      chunkingStrategy: 'semantic',
      chunkSize: 800,
      minParagraphLength: 50
    })
    const changes = diffLibraryConfig(before, after)
    const fields = changes.map((c) => c.field)
    expect(fields).toContain('chunkingStrategy')
    expect(fields).toContain('chunkSize')
    expect(fields).toContain('minParagraphLength')
  })

  it('detects cleaning.removeHeaderFooter toggle', () => {
    const before = baseConfig()
    const after = baseConfig({
      cleaning: { ...before.cleaning, removeHeaderFooter: false }
    })
    const changes = diffLibraryConfig(before, after)
    expect(changes.some((c) => c.field === 'cleaning.removeHeaderFooter')).toBe(true)
  })

  it('detects semanticSimilarityThreshold change', () => {
    const before = baseConfig({ semanticSimilarityThreshold: 0.72 })
    const after = baseConfig({ semanticSimilarityThreshold: 0.85 })
    const changes = diffLibraryConfig(before, after)
    expect(changes).toHaveLength(1)
    expect(changes[0].field).toBe('semanticSimilarityThreshold')
    expect(changes[0].needsReindex).toBe(true)
  })

  it('returns empty array when configs are deeply equal on compared fields', () => {
    const before = baseConfig()
    const after = baseConfig()
    expect(diffLibraryConfig(before, after)).toEqual([])
  })

  it('returns empty when parsing object is replaced but leaf values unchanged', () => {
    const before = baseConfig()
    const after = {
      ...baseConfig(),
      parsing: { ...before.parsing }
    }
    expect(diffLibraryConfig(before, after)).toEqual([])
  })
})
