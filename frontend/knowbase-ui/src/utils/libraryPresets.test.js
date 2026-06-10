import { describe, it, expect } from 'vitest'
import {
  LIBRARY_PRESETS,
  LIBRARY_PRESET_CUSTOM,
  applyLibraryPreset,
  resolveLibraryPresetLabel,
  getLibraryPresetById
} from './libraryPresets'
import { defaultLibraryConfig } from './libraryDefaults'
import { APPENDIX_A_EXPECTATIONS, ROADMAP_ANCHOR_EXPECTATIONS } from './appendixAPresetAudit'

const PRESET_IDS = [
  'weekly-report-excel',
  'policy-longform',
  'scan-reimbursement',
  'general-mixed'
]

describe('LIBRARY_PRESETS', () => {
  it('has exactly 4 presets with expected ids', () => {
    expect(LIBRARY_PRESETS).toHaveLength(4)
    const ids = LIBRARY_PRESETS.map((p) => p.id)
    for (const id of PRESET_IDS) {
      expect(ids).toContain(id)
    }
  })

  it('each preset has non-empty summary (PRESET-02)', () => {
    for (const preset of LIBRARY_PRESETS) {
      expect(typeof preset.summary).toBe('string')
      expect(preset.summary.trim().length).toBeGreaterThan(0)
    }
  })
})

describe('applyLibraryPreset', () => {
  const base = defaultLibraryConfig('quick')

  it('fills parsing/cleaning/chunking subtree fields', () => {
    for (const id of PRESET_IDS) {
      const config = applyLibraryPreset(base, id)
      expect(config.parsing).toBeDefined()
      expect(config.cleaning).toBeDefined()
      expect(config.chunkingStrategy).toBeDefined()
      expect(config.chunkSize).toBeDefined()
      expect(config.chunkOverlap).toBeDefined()
    }
  })

  it('sets libraryPresetId to preset id', () => {
    for (const id of PRESET_IDS) {
      const config = applyLibraryPreset(base, id)
      expect(config.libraryPresetId).toBe(id)
    }
  })

  it('returns custom for unknown preset id', () => {
    const config = applyLibraryPreset(base, 'nonexistent-preset')
    expect(config.libraryPresetId).toBe(LIBRARY_PRESET_CUSTOM)
  })

  it('weekly-report-excel matches ROADMAP anchor (TYPE-03 / 周报 xlsx)', () => {
    const config = applyLibraryPreset(base, 'weekly-report-excel')
    const exp = ROADMAP_ANCHOR_EXPECTATIONS['weekly-report-excel']
    expect(config.chunkingStrategy).toBe(exp.chunkingStrategy)
    expect(config.parsing.tableExtraction).toBe(exp.parsing.tableExtraction)
    expect(config.parsing.ocrEnabled).toBe(exp.parsing.ocrEnabled)
    expect(config.ingestAccess.supportedFileTypes).toEqual(exp.supportedFileTypes)
    expect(config.ingestAccess.supportedFileTypes).toEqual(['excel'])
  })

  it('scan-reimbursement matches ROADMAP anchor (TYPE-01 / 扫描 pdf)', () => {
    const config = applyLibraryPreset(base, 'scan-reimbursement')
    const exp = ROADMAP_ANCHOR_EXPECTATIONS['scan-reimbursement']
    expect(config.parsing.ocrEnabled).toBe(true)
    expect(config.ingestAccess.supportedFileTypes).toContain('pdf')
    expect(config.chunkingStrategy).toBe(exp.chunkingStrategy)
  })

  it('policy-longform matches ROADMAP anchor (TYPE-02 / 制度 docx)', () => {
    const config = applyLibraryPreset(base, 'policy-longform')
    const exp = ROADMAP_ANCHOR_EXPECTATIONS['policy-longform']
    expect(config.chunkingStrategy).toBe('heading-level')
    expect(config.parsing.tableExtraction).toBe('structured')
    expect(config.ingestAccess.supportedFileTypes).toContain('word')
    expect(config.ingestAccess.supportedFileTypes).toEqual(exp.supportedFileTypes)
  })

  it('general-mixed has all five supported file types', () => {
    const config = applyLibraryPreset(base, 'general-mixed')
    expect(config.ingestAccess.supportedFileTypes.sort()).toEqual(
      ['pdf', 'word', 'txt', 'markdown', 'excel'].sort()
    )
    expect(config.chunkingStrategy).toBe('paragraph-first')
    expect(config.parsing.ocrEnabled).toBe(false)
  })
})

describe('appendix A alignment', () => {
  const base = defaultLibraryConfig('quick')

  it('weekly-report-excel aligns with appendix A excel expectations', () => {
    const config = applyLibraryPreset(base, 'weekly-report-excel')
    const exp = APPENDIX_A_EXPECTATIONS.excel
    expect(config.chunkingStrategy).toBe(exp.chunkingStrategy)
    expect(config.parsing.tableExtraction).toBe(exp.parsing.tableExtraction)
  })

  it('scan-reimbursement pdf override: ocrEnabled true (D-07 layer 2)', () => {
    const config = applyLibraryPreset(base, 'scan-reimbursement')
    expect(config.parsing.ocrEnabled).toBe(true)
    expect(config.chunkingStrategy).toBe(APPENDIX_A_EXPECTATIONS.pdf.chunkingStrategy)
  })

  it('policy-longform aligns with appendix A word expectations', () => {
    const config = applyLibraryPreset(base, 'policy-longform')
    const exp = APPENDIX_A_EXPECTATIONS.word
    expect(config.chunkingStrategy).toBe(exp.chunkingStrategy)
    expect(config.parsing.tableExtraction).toBe(exp.parsing.tableExtraction)
    expect(config.parsing.ocrEnabled).toBe(exp.parsing.ocrEnabled)
  })
})

describe('resolveLibraryPresetLabel', () => {
  const base = defaultLibraryConfig('quick')

  it('returns Chinese name for each preset when applied', () => {
    for (const preset of LIBRARY_PRESETS) {
      const config = applyLibraryPreset(base, preset.id)
      expect(resolveLibraryPresetLabel(config)).toBe(preset.name)
    }
  })

  it('returns 自定义 after manual chunkingStrategy change', () => {
    const config = applyLibraryPreset(base, 'weekly-report-excel')
    config.chunkingStrategy = 'semantic'
    expect(resolveLibraryPresetLabel(config)).toBe('自定义')
  })
})

describe('getLibraryPresetById', () => {
  it('returns null for custom or empty id', () => {
    expect(getLibraryPresetById(null)).toBeNull()
    expect(getLibraryPresetById('custom')).toBeNull()
  })
})
