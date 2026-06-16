import {
  buildIndexPipelinePayload,
  buildParsingPayload,
  buildRetrievalPayload
} from './libraryConfigView'

export const FILE_TYPE_OPTIONS = [
  { value: 'pdf', label: 'PDF' },
  { value: 'word', label: 'Word' },
  { value: 'txt', label: 'TXT' },
  { value: 'markdown', label: 'Markdown' },
  { value: 'excel', label: 'Excel' }
]

/** 一期：与后端 ingest.allowed-mime-types 对齐的系统级支持类型 */
export const SYSTEM_SUPPORTED_FILE_TYPES = FILE_TYPE_OPTIONS.map((o) => o.value)

/**
 * 与 knowbase-service application.yml chunking.* 对齐的系统级分块合并规则（库级不可配置）。
 */
export const SYSTEM_CHUNKING_DEFAULTS = {
  minChunkSize: 80,
  maxChunkSize: 1200,
  minParagraphLength: 30
}

/** 库级分块大小滑块范围（与后端 LibraryIndexPipelineDto / ChunkPreviewRequest 一致） */
export const LIBRARY_CHUNK_SIZE_RANGE = { min: 100, max: 8000, step: 50 }
export const LIBRARY_CHUNK_OVERLAP_RANGE = { min: 0, max: 2000, step: 10 }

/** 库配置表单默认结构（分块参数、向量化、检索） */
export function defaultLibraryConfig() {
  return {
    configVersion: 1,
    metadataDbType: 'postgresql',
    embeddingProvider: 'ollama',
    embeddingModel: 'nomic-embed-text',
    embeddingDimension: 768,
    chunkSize: 500,
    chunkOverlap: 120,
    hierarchicalChunkingEnabled: true,
    chunkDelimiter: '',
    tags: [],
    retrieval: {
      hybridSearchEnabled: true,
      rerankEnabled: true,
      rerankModel: '',
      metadataFilterFields: [
        'periodYear',
        'periodStart',
        'periodEnd',
        'periodMonths',
        'submitter',
        'sectionLabel',
        'hasCompletedWork',
        'docType',
        'fileName',
        'mimeType'
      ],
      similarityThreshold: 0.4,
      defaultTopK: 12
    },
    primaryChunkProfileId: '',
    allowCustomChunkProfiles: true,
    maxActiveChunkProfiles: 5,
    parsing: {
      parserRules: SYSTEM_SUPPORTED_FILE_TYPES.map((fileType) => ({
        fileType,
        parserId: 'auto'
      })),
      defaultLanguage: 'zh-CN',
      autoDetectEncoding: true
    }
  }
}

export function createEmptyLibraryForm() {
  return {
    name: '',
    description: '',
    tags: [],
    config: defaultLibraryConfig()
  }
}

export function buildCreatePayload({ tenantId, name, description, tags }) {
  return {
    tenantId,
    name: name.trim(),
    description: (description || '').trim(),
    tags: tags || []
  }
}

/** 原子创建：basic + indexPipeline + parsing + retrieval 一次提交 */
export function buildAtomicCreatePayload({ tenantId, name, description, tags, config }) {
  const base = buildCreatePayload({ tenantId, name, description, tags })
  if (!config) {
    return base
  }
  return {
    ...base,
    ...buildIndexPipelinePayload(config),
    ...buildParsingPayload(config),
    ...buildRetrievalPayload(config)
  }
}
