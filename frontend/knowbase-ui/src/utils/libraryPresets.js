import { defaultLibraryConfig } from './libraryDefaults'

/**
 * Phase 3 库类型预设 — 与 FILE-TYPE-PROCESSING.md §2 / 附录 A 对齐。
 * `libraryPresetId` 写入 config_json，供编辑页展示来源（PRESET-04）。
 */
export const LIBRARY_PRESET_CUSTOM = 'custom'

export const LIBRARY_PRESETS = [
  {
    id: 'weekly-report-excel',
    name: '周报 Excel 库',
    summary: '同质周报 xlsx：paragraph-first + text-only，禁止 semantic/structured',
    supportedFileTypes: ['excel'],
    overrides: {
      chunkingStrategy: 'paragraph-first',
      chunkSize: 500,
      chunkOverlap: 120,
      parsing: {
        tableExtraction: 'text-only',
        ocrEnabled: false,
        autoDetectEncoding: true
      },
      cleaning: {
        removeDuplicateParagraphs: true
      }
    }
  },
  {
    id: 'policy-longform',
    name: '制度 / 长文库',
    summary: '制度 docx / 长 PDF：heading-level + Word structured 表格',
    supportedFileTypes: ['word', 'pdf'],
    overrides: {
      chunkingStrategy: 'heading-level',
      chunkSize: 500,
      chunkOverlap: 120,
      parsing: {
        tableExtraction: 'structured',
        ocrEnabled: false,
        autoDetectEncoding: true,
        defaultLanguage: 'zh-CN'
      },
      cleaning: {
        removeHeaderFooter: true,
        removeDuplicateParagraphs: true
      }
    }
  },
  {
    id: 'scan-reimbursement',
    name: '报销扫描库',
    summary: '扫描 PDF / 图片型单据：开启 OCR，paragraph-first',
    supportedFileTypes: ['pdf'],
    overrides: {
      chunkingStrategy: 'paragraph-first',
      chunkSize: 500,
      chunkOverlap: 120,
      parsing: {
        ocrEnabled: true,
        tableExtraction: 'text-only',
        autoDetectEncoding: true,
        defaultLanguage: 'zh-CN'
      },
      cleaning: {
        removeHeaderFooter: true,
        removeDuplicateParagraphs: true
      }
    }
  },
  {
    id: 'general-mixed',
    name: '通用混合库',
    summary: '五种类型均衡默认，适合多格式混合入库',
    supportedFileTypes: ['pdf', 'word', 'txt', 'markdown', 'excel'],
    overrides: {
      chunkingStrategy: 'paragraph-first',
      chunkSize: 500,
      chunkOverlap: 120,
      parsing: {
        ocrEnabled: false,
        tableExtraction: 'text-only',
        autoDetectEncoding: true,
        defaultLanguage: 'zh-CN'
      }
    }
  }
]

export function getLibraryPresetById(id) {
  if (!id || id === LIBRARY_PRESET_CUSTOM) return null
  return LIBRARY_PRESETS.find((p) => p.id === id) || null
}

function mergeNested(base, patch) {
  if (!patch) return base
  const out = { ...base }
  for (const [k, v] of Object.entries(patch)) {
    if (v && typeof v === 'object' && !Array.isArray(v)) {
      out[k] = mergeNested(out[k] || {}, v)
    } else {
      out[k] = v
    }
  }
  return out
}

/** 将预设覆盖到向导 config（保留 embedding / governance 等未列字段） */
export function applyLibraryPreset(baseConfig, presetId, wizardMode = 'quick') {
  const preset = getLibraryPresetById(presetId)
  const seed = baseConfig || defaultLibraryConfig(wizardMode)
  if (!preset) {
    return { ...seed, libraryPresetId: LIBRARY_PRESET_CUSTOM }
  }
  const merged = mergeNested(seed, preset.overrides)
  merged.libraryPresetId = preset.id
  merged.ingestAccess = {
    ...merged.ingestAccess,
    supportedFileTypes: [...preset.supportedFileTypes]
  }
  if (merged.parsing) {
    merged.parsing = { ...merged.parsing, ...preset.overrides.parsing }
  }
  if (merged.cleaning && preset.overrides.cleaning) {
    merged.cleaning = { ...merged.cleaning, ...preset.overrides.cleaning }
  }
  return merged
}

/** 编辑页：按 libraryPresetId 或关键字段匹配展示来源 */
export function resolveLibraryPresetLabel(config) {
  const id = config?.libraryPresetId
  if (id && id !== LIBRARY_PRESET_CUSTOM) {
    const p = getLibraryPresetById(id)
    if (p) return p.name
  }
  for (const preset of LIBRARY_PRESETS) {
    if (configMatchesPreset(config, preset)) return preset.name
  }
  return '自定义'
}

function configMatchesPreset(config, preset) {
  if (!config) return false
  const types = config.ingestAccess?.supportedFileTypes || []
  const presetTypes = [...preset.supportedFileTypes].sort().join(',')
  if ([...types].sort().join(',') !== presetTypes) return false
  const o = preset.overrides
  if (config.chunkingStrategy !== o.chunkingStrategy) return false
  if (o.chunkSize != null && config.chunkSize !== o.chunkSize) return false
  if (o.parsing?.ocrEnabled != null && config.parsing?.ocrEnabled !== o.parsing.ocrEnabled) return false
  if (o.parsing?.tableExtraction != null && config.parsing?.tableExtraction !== o.parsing.tableExtraction) return false
  return true
}
