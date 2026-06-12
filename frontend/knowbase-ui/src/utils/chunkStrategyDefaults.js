import { FILE_TYPE_OPTIONS } from './libraryDefaults'

/** 与后端 ContentFamilyPipelineDefaults 对齐的创建态策略预览（无 libraryId） */
const MIME_STRATEGY = {
  pdf: { strategy: 'paragraph-first', label: '按段落', parsingNote: '解析随 MIME 默认' },
  word: { strategy: 'heading-level', label: '按标题层级', parsingNote: '解析随 MIME 默认' },
  txt: { strategy: 'paragraph-first', label: '按段落', parsingNote: '解析随 MIME 默认' },
  markdown: { strategy: 'heading-level', label: '按标题层级', parsingNote: '解析随 MIME 默认' },
  excel: { strategy: 'paragraph-first', label: '按段落', parsingNote: '表格:text-only' }
}

/**
 * @param {{ hierarchicalChunkingEnabled?: boolean, chunkDelimiter?: string }} opts
 */
export function buildDefaultChunkStrategyRows(opts = {}) {
  const hierarchical = opts.hierarchicalChunkingEnabled !== false
  const delimiterNote =
    opts.chunkDelimiter && String(opts.chunkDelimiter).trim()
      ? '；库级自定义分隔符优先'
      : ''

  return FILE_TYPE_OPTIONS.map(({ value, label }) => {
    const meta = MIME_STRATEGY[value] || MIME_STRATEGY.pdf
    return {
      fileType: value,
      fileTypeLabel: label,
      chunkingStrategy: meta.strategy,
      chunkingStrategyLabel: meta.label,
      hierarchicalWhenApplicable:
        hierarchical && meta.strategy === 'heading-level',
      parsingNote: meta.parsingNote + delimiterNote
    }
  })
}
