const PARSE_LABELS = {
  PENDING: '待处理',
  PARSING: '解析中',
  PARSED: '已解析',
  FAILED: '失败'
}

const INDEX_LABELS = {
  PENDING: '待索引',
  INDEXED: '已索引',
  FAILED: '失败'
}

export function documentStatusType(status) {
  if (!status) return 'info'
  if (status === 'PARSED' || status === 'INDEXED') return 'success'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

export function parseStatusLabel(status) {
  return PARSE_LABELS[status] || status || '—'
}

export function indexStatusLabel(status) {
  return INDEX_LABELS[status] || status || '—'
}

export function formatListTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '—'
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
