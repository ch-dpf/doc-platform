import { describe, it, expect } from 'vitest'

import {
  buildIngestProfileJson,
  canUseIngestProfileOverride,
  emptyIngestProfileForm,
  formatIngestProfileSummary,
  parseIngestProfileJson,
  validateIngestProfileForm
} from './ingestProfile'

const LIB_DEFAULTS = { chunkSize: 500, chunkOverlap: 120 }

describe('emptyIngestProfileForm', () => {
  it('returns disabled form with null overrides', () => {
    const form = emptyIngestProfileForm()
    expect(form.enabled).toBe(false)
    expect(form.chunkSize).toBeNull()
    expect(form.chunkOverlap).toBeNull()
  })
})

describe('canUseIngestProfileOverride', () => {
  it('allows override when library permits custom chunk profiles', () => {
    expect(canUseIngestProfileOverride({ chunkOverrideAllowed: true })).toBe(true)
    expect(canUseIngestProfileOverride({ chunkOverrideAllowed: false })).toBe(false)
    expect(canUseIngestProfileOverride(null)).toBe(true)
  })
})

describe('buildIngestProfileJson', () => {
  it('returns null when profile override is disabled', () => {
    expect(buildIngestProfileJson(emptyIngestProfileForm(), LIB_DEFAULTS)).toBeNull()
    expect(buildIngestProfileJson({ enabled: false, chunkSize: 600 }, LIB_DEFAULTS)).toBeNull()
  })

  it('returns null when enabled but values match library defaults', () => {
    expect(
      buildIngestProfileJson(
        { enabled: true, chunkSize: 500, chunkOverlap: 120 },
        LIB_DEFAULTS
      )
    ).toBeNull()
  })

  it('serializes only fields that differ from library defaults', () => {
    const json = buildIngestProfileJson(
      { enabled: true, chunkSize: 600, chunkOverlap: 120 },
      LIB_DEFAULTS
    )
    expect(json).not.toBeNull()
    const profile = JSON.parse(json)
    expect(profile.chunkSize).toBe(600)
    expect(profile.chunkOverlap).toBeUndefined()
  })

  it('omits invalid numeric values', () => {
    expect(
      buildIngestProfileJson({ enabled: true, chunkSize: 0, chunkOverlap: -1 }, LIB_DEFAULTS)
    ).toBeNull()
  })
})

describe('parseIngestProfileJson', () => {
  it('parses chunk overrides from JSON string', () => {
    expect(parseIngestProfileJson('{"chunkSize":400,"chunkOverlap":80}')).toEqual({
      chunkSize: 400,
      chunkOverlap: 80
    })
  })
})

describe('formatIngestProfileSummary', () => {
  it('shows delta against library defaults', () => {
    expect(formatIngestProfileSummary({ chunkSize: 600, chunkOverlap: 80 }, LIB_DEFAULTS)).toBe(
      '分块大小 500 → 600 · 分块重叠 120 → 80'
    )
  })

  it('returns null when profile has no chunk overrides', () => {
    expect(formatIngestProfileSummary({ chunkSize: null, chunkOverlap: null })).toBeNull()
    expect(formatIngestProfileSummary(null)).toBeNull()
  })
})

describe('validateIngestProfileForm', () => {
  it('requires at least one field when enabled', () => {
    expect(validateIngestProfileForm({ enabled: true, chunkSize: null, chunkOverlap: null })).toBe(
      '请至少填写一项与库默认不同的分块数值'
    )
  })

  it('validates ranges', () => {
    expect(validateIngestProfileForm({ enabled: true, chunkSize: 50, chunkOverlap: null })).toMatch(
      /分块大小/
    )
  })
})
