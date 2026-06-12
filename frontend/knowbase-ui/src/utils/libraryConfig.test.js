import { describe, it, expect } from 'vitest'

import { diffLibraryConfig, diffNeedsReindex } from './libraryConfig'

import { defaultLibraryConfig } from './libraryDefaults'



function baseConfig(overrides = {}) {

  return { ...defaultLibraryConfig(), ...overrides }

}



describe('diffLibraryConfig', () => {

  it('detects chunkSize change with needsReindex', () => {

    const before = baseConfig()

    const after = baseConfig({ chunkSize: 800 })

    const changes = diffLibraryConfig(before, after)

    expect(changes).toHaveLength(1)

    expect(changes[0].field).toBe('chunkSize')

    expect(changes[0].needsReindex).toBe(true)

    expect(diffNeedsReindex(changes)).toBe(true)

  })



  it('detects chunkOverlap change with needsReindex', () => {
    const before = baseConfig()
    const after = baseConfig({ chunkOverlap: 200 })
    const changes = diffLibraryConfig(before, after)
    expect(changes).toHaveLength(1)
    expect(changes[0].field).toBe('chunkOverlap')
    expect(changes[0].needsReindex).toBe(true)
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

})

