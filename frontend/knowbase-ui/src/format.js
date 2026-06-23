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
