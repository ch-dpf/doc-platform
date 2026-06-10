/**
 * 附录 A / ROADMAP §2 锚点期望值 — 供 libraryPresets.test.js 审计预设套用结果。
 * 来源：FILE-TYPE-PROCESSING.md 附录 A L233–240、§2 L88/L101/L111。
 */

/** 按 fileTypeKey 的 parsing/chunking 片段（附录 A） */
export const APPENDIX_A_EXPECTATIONS = {
  pdf: {
    parsing: { ocrEnabled: false, tableExtraction: 'text-only' },
    chunkingStrategy: 'paragraph-first'
  },
  word: {
    parsing: { tableExtraction: 'structured', ocrEnabled: false },
    chunkingStrategy: 'heading-level'
  },
  excel: {
    parsing: { tableExtraction: 'text-only' },
    chunkingStrategy: 'paragraph-first'
  },
  txt: {
    parsing: { autoDetectEncoding: true },
    chunkingStrategy: 'paragraph-first'
  },
  markdown: {
    parsing: { autoDetectEncoding: true },
    chunkingStrategy: 'paragraph-first'
  }
}

/** ROADMAP 三锚点 presetId → 关键字段（§2） */
export const ROADMAP_ANCHOR_EXPECTATIONS = {
  'weekly-report-excel': {
    chunkingStrategy: 'paragraph-first',
    parsing: { tableExtraction: 'text-only', ocrEnabled: false },
    supportedFileTypes: ['excel']
  },
  'scan-reimbursement': {
    chunkingStrategy: 'paragraph-first',
    parsing: { ocrEnabled: true, tableExtraction: 'text-only' },
    supportedFileTypes: ['pdf']
  },
  'policy-longform': {
    chunkingStrategy: 'heading-level',
    parsing: { tableExtraction: 'structured', ocrEnabled: false },
    supportedFileTypes: ['word', 'pdf']
  }
}
