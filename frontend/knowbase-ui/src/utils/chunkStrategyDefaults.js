import { FILE_TYPE_OPTIONS, SYSTEM_SUPPORTED_FILE_TYPES } from './libraryDefaults'
import { chunkingStrategyLabel } from './libraryConfig'

/** 与后端 FileTypeChunkStrategyDefaults 对齐 */
export const FILE_TYPE_DEFAULT_STRATEGIES = {
  pdf: 'paragraph-first',
  word: 'heading-level',
  txt: 'paragraph-first',
  markdown: 'heading-level',
  excel: 'paragraph-first'
}

export const CHUNKING_STRATEGY_OPTIONS = [
  {
    value: 'auto',
    label: '自动（按文件类型）',
    description: 'Word/Markdown 默认按标题；PDF/TXT/Excel 默认按段落'
  },
  {
    value: 'paragraph-first',
    label: '按段落',
    description: '空行分段，超长段按字符窗口重叠切分（推荐通用文档）'
  },
  {
    value: 'heading-level',
    label: '按标题层级',
    description: '按 Markdown/Word 标题切段，适合长文档与制度'
  },
  {
    value: 'semantic',
    label: '语义分块',
    description: '按 Embedding 语义相似度聚合（需 Ollama 可用）'
  },
  {
    value: 'fixed-char',
    label: '固定长度',
    description: '字符滑动窗口，适合结构弱文本'
  }
]

/**
 * @param {string} libraryStrategy
 * @param {string[]} [fileTypes]
 */
export function buildChunkStrategyPreviewRows(
  libraryStrategy = 'auto',
  fileTypes = SYSTEM_SUPPORTED_FILE_TYPES
) {
  const strategy = libraryStrategy || 'auto'
  const typeLabels = Object.fromEntries(FILE_TYPE_OPTIONS.map((o) => [o.value, o.label]))

  return fileTypes.map((fileType) => {
    const effective =
      strategy === 'auto'
        ? FILE_TYPE_DEFAULT_STRATEGIES[fileType] || 'paragraph-first'
        : strategy
    return {
      fileType,
      fileTypeLabel: typeLabels[fileType] || fileType,
      chunkingStrategy: effective,
      chunkingStrategyLabel: chunkingStrategyLabel(effective)
    }
  })
}

/** @deprecated 使用 buildChunkStrategyPreviewRows */
export function buildDefaultChunkStrategyRows() {
  return buildChunkStrategyPreviewRows('auto')
}
