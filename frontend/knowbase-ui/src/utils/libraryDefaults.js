export const FILE_TYPE_OPTIONS = [
  { value: 'pdf', label: 'PDF' },
  { value: 'word', label: 'Word' },
  { value: 'txt', label: 'TXT' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'excel', label: 'Excel' }
]

import { DEFAULT_LINE_DROP_PATTERNS } from './textPatterns'

export const WIZARD_STEPS = [
  { title: '基础信息' },
  { title: '数据类型与容量' },
  { title: '文档处理规则' },
  { title: '索引与检索' },
  { title: '治理与安全' }
]

const defaultTextNormalization = () => ({
  enabled: true,
  collapseBlankLines: true,
  trimLines: true,
  removeControlChars: true,
  normalizeUnicodeSpaces: true,
  dropNoiseLines: true,
  minLineLength: 2,
  linePatternsToDrop: [...DEFAULT_LINE_DROP_PATTERNS]
})

export function defaultLibraryConfig(mode = 'quick') {
  return {
    configVersion: 1,
    wizardMode: mode,
    metadataDbType: 'postgresql',
    ingestSourceMode: 'upload',
    embeddingProvider: 'ollama',
    embeddingModel: 'nomic-embed-text',
    embeddingDimension: 768,
    chunkingStrategy: 'paragraph-first',
    chunkSize: 500,
    chunkOverlap: 120,
    minChunkSize: 80,
    maxChunkSize: 1200,
    minParagraphLength: 30,
    normalizeBeforeChunk: true,
    textNormalizationEnabled: true,
    textNormalization: defaultTextNormalization(),
    tags: [],
    ingestAccess: {
      accessMode: 'upload-and-folder',
      supportedFileTypes: ['pdf', 'word', 'txt', 'markdown', 'excel'],
      capacityLimits: {
        maxDocuments: 10000,
        maxTotalSizeBytes: 10737418240,
        maxChunkEntries: 500000
      },
      versionPolicy: {
        enabled: true,
        updateStrategy: 'keep-history'
      }
    },
    parsing: {
      ocrEnabled: false,
      tableExtraction: 'text-only',
      imageExtraction: 'skip',
      formulaExtraction: 'skip',
      autoDetectEncoding: true,
      defaultLanguage: 'zh-CN'
    },
    cleaning: {
      removeHeaderFooter: true,
      removeWatermark: true,
      removeDuplicateParagraphs: true,
      maskPhone: false,
      maskIdCard: false,
      stopwordFilter: false
    },
    retrieval: {
      hybridSearchEnabled: true,
      rerankEnabled: true,
      rerankModel: '',
      metadataFilterFields: [],
      similarityThreshold: 0.4
    },
    governance: {
      ingestReviewMode: 'auto',
      inheritLibraryPermissions: true,
      retentionDays: 0,
      archivePolicy: 'none',
      complianceTags: [],
      auditLogEnabled: true
    }
  }
}

export function buildCreatePayload({ tenantId, name, description, tags, config, wizardMode }) {
  const cfg = { ...defaultLibraryConfig(wizardMode), ...config, wizardMode }
  return {
    tenantId,
    name: name.trim(),
    description,
    tags: tags || [],
    config: cfg
  }
}
