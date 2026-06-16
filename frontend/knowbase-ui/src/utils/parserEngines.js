import { FILE_TYPE_OPTIONS, SYSTEM_SUPPORTED_FILE_TYPES } from './libraryDefaults'

export const FILE_TYPE_LABELS = Object.fromEntries(
  FILE_TYPE_OPTIONS.map((o) => [o.value, o.label])
)

/** 与后端 BuiltinParserId 对齐的内置解析器（API 不可用时的回退） */
export const PARSER_ENGINE_OPTIONS = [
  {
    parserId: 'auto',
    label: '自动（MIME 默认）',
    description: '按文件类型应用系统默认解析策略'
  },
  {
    parserId: 'tika-plain',
    label: '纯文本',
    description: 'Tika 纯文本抽取，不做 HTML 结构化管道'
  },
  {
    parserId: 'tika-structured',
    label: '结构化文档',
    description: 'HTML 管道 + 表格 structured，适合 Word/PDF'
  },
  {
    parserId: 'tika-ocr-auto',
    label: '扫描友好',
    description: 'Tika 抽取不足时自动 OCR 回退'
  },
  {
    parserId: 'excel-structured',
    label: '表格结构化',
    description: 'Excel 优先 Markdown 表格'
  },
  {
    parserId: 'tika-table-text',
    label: '表格转文本',
    description: '表格扁平为 tab 分隔纯文本'
  }
]

export function defaultParserRules(fileTypes = SYSTEM_SUPPORTED_FILE_TYPES) {
  return fileTypes.map((fileType) => ({
    fileType,
    parserId: 'auto'
  }))
}

export function defaultParsingConfig() {
  return {
    parserRules: defaultParserRules(),
    defaultLanguage: 'zh-CN',
    autoDetectEncoding: true
  }
}

/** 合并 API 返回规则与系统支持类型，保证每种类型都有一行 */
export function normalizeParserRules(rules, fileTypes = SYSTEM_SUPPORTED_FILE_TYPES) {
  const byType = new Map(
    (rules || [])
      .filter((r) => r?.fileType)
      .map((r) => [r.fileType, r.parserId || 'auto'])
  )
  return fileTypes.map((fileType) => ({
    fileType,
    parserId: byType.get(fileType) || 'auto'
  }))
}

export function parserLabel(parserId, options = PARSER_ENGINE_OPTIONS) {
  const found = options.find((o) => o.parserId === parserId)
  return found?.label || parserId || '—'
}

export function optionsForFileType(fileType, engines = PARSER_ENGINE_OPTIONS) {
  return engines.filter(
    (e) =>
      e.parserId === 'auto' ||
      !e.recommendedFileTypes?.length ||
      e.recommendedFileTypes.includes(fileType)
  )
}

// 与后端 ParserEngineDescriptor.recommendedFileTypes 对齐（静态回退）
for (const opt of PARSER_ENGINE_OPTIONS) {
  if (opt.parserId === 'auto') {
    opt.recommendedFileTypes = [...SYSTEM_SUPPORTED_FILE_TYPES]
  } else if (opt.parserId === 'tika-plain') {
    opt.recommendedFileTypes = ['txt', 'markdown']
  } else if (opt.parserId === 'tika-structured') {
    opt.recommendedFileTypes = ['pdf', 'word']
  } else if (opt.parserId === 'tika-ocr-auto') {
    opt.recommendedFileTypes = ['pdf']
  } else if (opt.parserId === 'excel-structured') {
    opt.recommendedFileTypes = ['excel']
  } else if (opt.parserId === 'tika-table-text') {
    opt.recommendedFileTypes = ['pdf', 'word', 'excel']
  }
}
