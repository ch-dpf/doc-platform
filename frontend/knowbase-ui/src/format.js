export function shortId(value, length = 8) {
  if (!value) {
    return '--';
  }
  const text = String(value);
  return text.length <= length ? text : `${text.slice(0, length)}...`;
}

export function formatDateTime(value) {
  if (!value) {
    return '刚刚';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date);
}

export function formatNumber(value) {
  return new Intl.NumberFormat('zh-CN').format(Number(value) || 0);
}

export function formatPercent(value, digits = 0) {
  const number = Number(value);
  if (!Number.isFinite(number)) {
    return '--';
  }
  return `${(number * 100).toFixed(digits)}%`;
}

export const percent = formatPercent;

export function formatLocationMeta(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return '';
  }
  const parts = [];
  if (metadata.pageNumber != null) {
    parts.push(`P${metadata.pageNumber}`);
  }
  if (metadata.bbox) {
    parts.push(`bbox ${String(metadata.bbox)}`);
  }
  if (metadata.contentFamily) {
    parts.push(String(metadata.contentFamily));
  }
  if (metadata.vectorRank != null || metadata.keywordRank != null) {
    parts.push(`v#${metadata.vectorRank ?? '—'} · k#${metadata.keywordRank ?? '—'}`);
  }
  return parts.join(' · ');
}

export function formatChunkLocationTags(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return [];
  }
  const tags = [];
  if (metadata.pageNumber != null) {
    tags.push({ key: 'page', label: `P${metadata.pageNumber}` });
  }
  if (metadata.bbox) {
    tags.push({ key: 'bbox', label: `bbox ${String(metadata.bbox)}` });
  }
  if (metadata.contentFamily) {
    tags.push({ key: 'family', label: String(metadata.contentFamily) });
  }
  if (metadata.chunkBoundaryType) {
    tags.push({ key: 'boundary', label: String(metadata.chunkBoundaryType) });
  }
  return tags;
}

export function isChunkRetrievalEnabled(metadata) {
  if (!metadata || metadata.retrievalEnabled == null) {
    return true;
  }
  return metadata.retrievalEnabled !== false && String(metadata.retrievalEnabled).toLowerCase() !== 'false';
}

const SUMMARY_CHUNK_ROLES = new Set(['document_summary']);

export function chunkRole(metadata, boundaryType) {
  if (metadata?.chunkRole) {
    return String(metadata.chunkRole);
  }
  return boundaryType || 'flat';
}

export function isSummaryChunk(metadata, boundaryType) {
  const role = chunkRole(metadata, boundaryType);
  return SUMMARY_CHUNK_ROLES.has(role);
}

export function formatChunkRoleLabel(metadata, boundaryType) {
  const role = chunkRole(metadata, boundaryType);
  switch (role) {
    case 'document_summary':
      return '文档摘要';
    case 'table_row_group':
      return '行组';
    case 'flat':
      return '分块';
    case 'child':
      return '子块';
    case 'parent':
      return '父块';
    default:
      return role || '分块';
  }
}

export function chunkRoleTagType(metadata, boundaryType) {
  const role = chunkRole(metadata, boundaryType);
  if (SUMMARY_CHUNK_ROLES.has(role)) {
    return 'success';
  }
  if (role === 'table_row_group') {
    return 'warning';
  }
  return 'info';
}

export function statusTone(status) {
  const normalized = String(status || '').toUpperCase();
  if (['SUCCEEDED', 'ACTIVE', 'PUBLISHED'].includes(normalized)) {
    return '';
  }
  if (['CREATED', 'ROUTING', 'RETRIEVING', 'GENERATING', 'RUNNING', 'TESTING'].includes(normalized)) {
    return 'blue';
  }
  if (['PARTIAL', 'DRAFT', 'PENDING'].includes(normalized)) {
    return 'amber';
  }
  if (['FAILED', 'CANCELLED', 'ERROR'].includes(normalized)) {
    return 'red';
  }
  return '';
}

const PIPELINE_STAGE_LABELS = {
  pipeline: '入库 Pipeline',
  document: '文档',
  load_source: '加载源文件',
  parse_document: '解析',
  normalize_text: '清洗',
  extract_metadata: '元数据增强',
  summarize_document: '文档摘要',
  chunk_document: '分块',
  post_process_chunks: '后处理',
  embed_chunks: '向量化',
  write_index: '写索引',
  publish_index_version: '发布索引'
};

const PIPELINE_STAGE_ORDER = Object.keys(PIPELINE_STAGE_LABELS);

/** Document-level ingestion stages shown in the library document trace stepper (mainline only). */
export const DOCUMENT_PIPELINE_STAGES = [
  'load_source',
  'parse_document',
  'normalize_text',
  'chunk_document',
  'embed_chunks',
  'write_index'
];

export function formatPipelineStageLabel(stage) {
  if (!stage) {
    return '—';
  }
  return PIPELINE_STAGE_LABELS[stage] || stage;
}

export function pipelineSpanStatusType(status) {
  const normalized = String(status || '').toUpperCase();
  if (normalized === 'SUCCEEDED') {
    return 'success';
  }
  if (normalized === 'FAILED') {
    return 'danger';
  }
  if (normalized === 'STARTED') {
    return 'info';
  }
  return 'info';
}

function stageOrder(stage) {
  const index = PIPELINE_STAGE_ORDER.indexOf(stage);
  return index === -1 ? 999 : index;
}

export function sortPipelineSpans(spans) {
  return [...(spans || [])].sort((left, right) => {
    const leftTime = Date.parse(left?.startedAt || '') || 0;
    const rightTime = Date.parse(right?.startedAt || '') || 0;
    if (leftTime !== rightTime) {
      return leftTime - rightTime;
    }
    return stageOrder(left?.stage) - stageOrder(right?.stage);
  });
}

export function filterDocumentPipelineSpans(spans, { documentId, sourceUri, mainlineOnly = true } = {}) {
  if (!Array.isArray(spans)) {
    return [];
  }
  const normalizedDocumentId = documentId ? String(documentId) : '';
  const normalizedSourceUri = sourceUri ? String(sourceUri) : '';
  return spans.filter((span) => {
    const attributes = span?.attributes || {};
    const matchesDocument = (normalizedDocumentId && String(attributes.documentId || '') === normalizedDocumentId)
      || (normalizedSourceUri && String(attributes.sourceUri || '') === normalizedSourceUri);
    if (!matchesDocument) {
      return false;
    }
    if (!mainlineOnly) {
      return true;
    }
    const stage = span?.stage;
    return stage && DOCUMENT_PIPELINE_STAGES.includes(stage);
  });
}

export function formatPipelineSpanSummary(attributes) {
  if (!attributes || typeof attributes !== 'object') {
    return '';
  }
  const parts = [];
  if (attributes.structureAware === true || attributes.structureAware === 'true') {
    parts.push('结构感知');
  }
  if (attributes.parserCode) {
    parts.push(String(attributes.parserCode));
  }
  if (attributes.normalizedCharCount != null) {
    parts.push(`${attributes.normalizedCharCount} 字符`);
  }
  if (attributes.blockCount != null) {
    parts.push(`${attributes.blockCount} 结构块`);
  }
  if (attributes.chunkCount != null) {
    parts.push(`${attributes.chunkCount} 分块`);
  }
  if (attributes.indexableCount != null) {
    parts.push(`${attributes.indexableCount} 可索引`);
  }
  if (attributes.indexableChunks != null) {
    parts.push(`${attributes.indexableChunks} 可索引`);
  }
  if (attributes.beforeCount != null && attributes.afterCount != null) {
    parts.push(`${attributes.beforeCount}→${attributes.afterCount} 块`);
  }
  if (attributes.summariesAdded != null && Number(attributes.summariesAdded) > 0) {
    parts.push(`+${attributes.summariesAdded} 摘要`);
  }
  if (attributes.rowsMerged != null && Number(attributes.rowsMerged) > 0) {
    parts.push(`+${attributes.rowsMerged} 行组`);
  }
  if (attributes.deduplicated != null && Number(attributes.deduplicated) > 0) {
    parts.push(`-${attributes.deduplicated} 去重`);
  }
  if (attributes.vectors != null) {
    parts.push(`${attributes.vectors} 向量`);
  }
  if (attributes.chunksWritten != null) {
    parts.push(`写入 ${attributes.chunksWritten}`);
  }
  if (attributes.error) {
    parts.push(String(attributes.error));
  }
  return parts.join(' · ');
}
