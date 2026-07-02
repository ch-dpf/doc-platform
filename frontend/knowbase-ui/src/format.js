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

/** Documents below this parseConfidence should show a review banner in the ingest wizard. */
export const PARSE_CONFIDENCE_THRESHOLD = 0.7;

export const DOCUMENT_METADATA_DISPLAY_KEYS = [
  ['parseConfidence', '解析置信度'],
  ['parseConfidenceSource', '置信来源'],
  ['tableRegionCount', '表区数'],
  ['lowConfidenceReasons', '低置信原因'],
  ['pageNumber', '页'],
  ['bbox', 'bbox'],
  ['readingOrder', '序'],
  ['columnIndex', '列'],
  ['columnCount', '列数'],
  ['tableRegionId', '表区'],
  ['tableRegionLabel', '表区名'],
  ['sheetName', 'Sheet'],
  ['rowRange', '行'],
  ['columnRange', '列域'],
  ['headerPath', '表头'],
  ['ocrConfidence', '置信度'],
  ['bboxSource', 'bbox源'],
  ['parser', 'Parser'],
  ['parserEngine', '引擎'],
  ['tableFormat', '表格'],
  ['rowGroupCount', '行组']
];

export const BLOCK_METADATA_TAG_KEYS = [
  ['rowRole', '角色'],
  ['indexableHint', '索引'],
  ['pageNumber', 'P'],
  ['bbox', 'bbox'],
  ['readingOrder', '#'],
  ['columnIndex', 'col'],
  ['tableRegionId', 'table'],
  ['tableRegionLabel', '表区'],
  ['sheetName', 'sheet'],
  ['rowRange', 'row'],
  ['columnRange', 'col'],
  ['ocrConfidence', 'conf'],
  ['lowConfidenceOcr', '低置信'],
  ['reviewRequired', '复核'],
  ['headerPath', 'head'],
  ['ocrFilterReason', '过滤']
];

export const CHUNK_METADATA_TAG_KEYS = [
  ['chunkRole', 'role'],
  ['sourceStructure', 'structure'],
  ['rowRole', '角色'],
  ['pageNumber', 'P'],
  ['bbox', 'bbox'],
  ['tableRegionId', 'table'],
  ['tableRegionLabel', '表区'],
  ['sheetName', 'sheet'],
  ['rowRange', 'row'],
  ['columnRange', 'col'],
  ['headerPath', 'head'],
  ['ocrConfidence', 'conf'],
  ['ocrFilterReason', '过滤'],
  ['fallback', 'fallback']
];

export function formatMetadataValue(value) {
  if (value === true) {
    return '是';
  }
  if (value === false) {
    return '否';
  }
  if (Array.isArray(value)) {
    if (value.length > 4 && value.every(item => typeof item === 'number')) {
      return value.slice(0, 4).join(',');
    }
    if (value.length && typeof value[0] === 'object') {
      return `${value.length} 项`;
    }
    return value.slice(0, 3).join(' > ');
  }
  if (typeof value === 'object' && value !== null) {
    return `${Object.keys(value).length} 项`;
  }
  if (typeof value === 'number' && value > 0 && value < 1) {
    return formatPercent(value);
  }
  return String(value);
}

export function formatIndexableHint(value) {
  if (value === true || value === 'true') {
    return '可索引';
  }
  if (value === false || value === 'false') {
    return '仅上下文';
  }
  return formatMetadataValue(value);
}

export function buildMetadataTags(metadata, keys, max = 8) {
  if (!metadata || typeof metadata !== 'object') {
    return [];
  }
  return keys
    .filter(([key]) => metadata[key] !== undefined && metadata[key] !== null && metadata[key] !== '')
    .map(([key, label]) => ({
      key,
      label,
      value: key === 'indexableHint' ? formatIndexableHint(metadata[key]) : formatMetadataValue(metadata[key])
    }))
    .slice(0, max);
}

export function parseConfidenceValue(metadata) {
  if (!metadata || metadata.parseConfidence == null) {
    return null;
  }
  const value = Number(metadata.parseConfidence);
  return Number.isFinite(value) ? value : null;
}

export function isLowParseConfidence(metadata, threshold = PARSE_CONFIDENCE_THRESHOLD) {
  const confidence = parseConfidenceValue(metadata);
  return confidence != null && confidence < threshold;
}

export function hasAnyLowParseConfidence(documents, threshold = PARSE_CONFIDENCE_THRESHOLD) {
  return (documents || []).some(doc => isLowParseConfidence(doc?.parse?.metadata, threshold));
}

export function collectLowConfidenceReasons(documents, threshold = PARSE_CONFIDENCE_THRESHOLD) {
  const reasons = new Set();
  for (const doc of documents || []) {
    const metadata = doc?.parse?.metadata || {};
    if (!isLowParseConfidence(metadata, threshold)) {
      continue;
    }
    const list = metadata.lowConfidenceReasons;
    if (Array.isArray(list)) {
      list.forEach(reason => reasons.add(String(reason)));
    }
  }
  return [...reasons];
}

export function summarizeParseQuality(documents) {
  const confidences = [];
  let tableRegions = 0;
  for (const doc of documents || []) {
    const metadata = doc?.parse?.metadata || {};
    const confidence = parseConfidenceValue(metadata);
    if (confidence != null) {
      confidences.push(confidence);
    }
    const regions = Number(metadata.tableRegionCount);
    if (Number.isFinite(regions) && regions > 0) {
      tableRegions += regions;
    }
  }
  const parts = [];
  if (confidences.length) {
    const average = confidences.reduce((sum, value) => sum + value, 0) / confidences.length;
    parts.push(formatPercent(average));
  }
  if (tableRegions > 0) {
    parts.push(`${tableRegions} 表区`);
  }
  return parts.length ? parts.join(' · ') : '—';
}

export function formatLocationMeta(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return '';
  }
  const parts = [];
  if (metadata.pageNumber != null) {
    parts.push(`P${metadata.pageNumber}`);
  }
  if (metadata.sheetName) {
    parts.push(String(metadata.sheetName));
  }
  const cellRef = metadata.primaryCellRef
    || (Array.isArray(metadata.cellRefs) && metadata.cellRefs.length ? metadata.cellRefs[0] : null);
  if (cellRef) {
    parts.push(String(cellRef));
  }
  if (metadata.tableRegionLabel) {
    parts.push(String(metadata.tableRegionLabel));
  } else if (metadata.tableRegionId) {
    parts.push(`表区 ${metadata.tableRegionId}`);
  }
  if (metadata.headerPath) {
    parts.push(Array.isArray(metadata.headerPath) ? metadata.headerPath.join(' > ') : String(metadata.headerPath));
  }
  if (metadata.rowRole) {
    parts.push(String(metadata.rowRole));
  }
  if (metadata.rowRange) {
    parts.push(`行 ${metadata.rowRange}`);
  }
  if (metadata.bbox) {
    parts.push(`bbox ${String(metadata.bbox)}`);
  }
  if (metadata.ocrConfidence != null) {
    parts.push(`conf ${formatPercent(metadata.ocrConfidence)}`);
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
  if (metadata.sheetName) {
    tags.push({ key: 'sheet', label: String(metadata.sheetName) });
  }
  const cellRef = metadata.primaryCellRef
    || (Array.isArray(metadata.cellRefs) && metadata.cellRefs.length ? metadata.cellRefs[0] : null);
  if (cellRef) {
    tags.push({ key: 'cellRef', label: String(cellRef) });
  }
  if (metadata.tableRegionLabel) {
    tags.push({ key: 'tableRegion', label: String(metadata.tableRegionLabel) });
  } else if (metadata.tableRegionId) {
    tags.push({ key: 'tableRegion', label: `表区 ${metadata.tableRegionId}` });
  }
  if (metadata.headerPath) {
    const path = Array.isArray(metadata.headerPath) ? metadata.headerPath.join(' > ') : String(metadata.headerPath);
    tags.push({ key: 'headerPath', label: path });
  }
  if (metadata.rowRole) {
    tags.push({ key: 'rowRole', label: String(metadata.rowRole) });
  }
  if (metadata.rowRange) {
    tags.push({ key: 'rowRange', label: `行 ${metadata.rowRange}` });
  }
  if (metadata.bbox) {
    tags.push({ key: 'bbox', label: `bbox ${String(metadata.bbox)}` });
  }
  if (metadata.ocrConfidence != null) {
    tags.push({ key: 'ocrConfidence', label: `conf ${formatPercent(metadata.ocrConfidence)}` });
  }
  if (metadata.contentFamily) {
    tags.push({ key: 'family', label: String(metadata.contentFamily) });
  }
  if (metadata.chunkBoundaryType) {
    tags.push({ key: 'boundary', label: String(metadata.chunkBoundaryType) });
  }
  return tags;
}

/** Tags for citations / evidence cards (same location contract as chunks). */
export function formatCitationLocationTags(metadata) {
  return formatChunkLocationTags(metadata);
}

export function buildLibraryDocumentLocateRoute(libraryId, documentId, metadata = {}) {
  if (!libraryId || !documentId) {
    return null;
  }
  const query = {};
  if (metadata.pageNumber != null) {
    query.page = String(metadata.pageNumber);
  }
  if (metadata.chunkId) {
    query.chunkId = String(metadata.chunkId);
  }
  if (metadata.sheetName) {
    query.sheet = String(metadata.sheetName);
  }
  if (metadata.rowIndex != null) {
    query.row = String(metadata.rowIndex);
  }
  if (metadata.columnIndex != null) {
    query.col = String(metadata.columnIndex);
  }
  const cellRef = metadata.primaryCellRef
    || (Array.isArray(metadata.cellRefs) && metadata.cellRefs.length ? metadata.cellRefs[0] : null);
  if (cellRef) {
    query.cellRef = String(cellRef);
  }
  return {
    name: 'library-document-detail',
    params: { libraryId: String(libraryId), documentId: String(documentId) },
    query
  };
}

export function canLocateCitation(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return false;
  }
  const hasExcelLocate = metadata.sheetName != null
    && (metadata.rowIndex != null
      || metadata.primaryCellRef != null
      || (Array.isArray(metadata.cellCoordinates) && metadata.cellCoordinates.length > 0));
  return metadata.pageNumber != null
    || hasExcelLocate
    || metadata.tableRegionLabel != null
    || metadata.bbox != null
    || metadata.wordSectionPath != null;
}

export function isChunkRetrievalEnabled(metadata) {
  if (!metadata || metadata.retrievalEnabled == null) {
    return true;
  }
  return metadata.retrievalEnabled !== false && String(metadata.retrievalEnabled).toLowerCase() !== 'false';
}

/** PDF page schematic overlay from chunk/citation bbox metadata (PDF points, origin bottom-left). */
export function parseBboxOverlay(metadata, pageWidth = 612, pageHeight = 792) {
  if (!metadata || typeof metadata !== 'object') {
    return null;
  }
  const width = Number(metadata.pageWidth) > 0 ? Number(metadata.pageWidth) : pageWidth;
  const height = Number(metadata.pageHeight) > 0 ? Number(metadata.pageHeight) : pageHeight;
  const raw = metadata.tableRegionBbox || metadata.bbox;
  if (!Array.isArray(raw) || raw.length < 4) {
    return null;
  }
  const [x, y, bboxWidth, bboxHeight] = raw.map((value) => Number(value));
  if (![x, y, bboxWidth, bboxHeight].every((value) => Number.isFinite(value))) {
    return null;
  }
  return {
    pageNumber: metadata.pageNumber == null ? null : Number(metadata.pageNumber),
    left: `${(x / width) * 100}%`,
    top: `${((height - y - bboxHeight) / height) * 100}%`,
    width: `${(bboxWidth / width) * 100}%`,
    height: `${(bboxHeight / height) * 100}%`,
    label: metadata.tableRegionLabel || metadata.tableRegionId || '区域'
  };
}

/** Word-level OCR overlays for schematic preview. */
export function parseOcrWordOverlays(metadata, pageWidth = 612, pageHeight = 792) {
  if (!metadata || typeof metadata !== 'object' || !Array.isArray(metadata.ocrWords)) {
    return [];
  }
  const width = Number(metadata.pageWidth) > 0 ? Number(metadata.pageWidth) : pageWidth;
  const height = Number(metadata.pageHeight) > 0 ? Number(metadata.pageHeight) : pageHeight;
  return metadata.ocrWords
    .map((word, index) => {
      if (!word || !Array.isArray(word.bbox) || word.bbox.length < 4) {
        return null;
      }
      const [x, y, bboxWidth, bboxHeight] = word.bbox.map((value) => Number(value));
      if (![x, y, bboxWidth, bboxHeight].every((value) => Number.isFinite(value))) {
        return null;
      }
      return {
        key: `ocr-word-${index}`,
        text: word.text || '',
        left: `${(x / width) * 100}%`,
        top: `${((height - y - bboxHeight) / height) * 100}%`,
        width: `${(bboxWidth / width) * 100}%`,
        height: `${(bboxHeight / height) * 100}%`
      };
    })
    .filter(Boolean);
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
