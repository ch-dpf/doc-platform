/**
 * 采集 / 入库 API 错误码与友好文案（ProblemDetail.errorCode 或批量项 errorCode）。
 */
export const INGEST_ERROR_CATALOG = {
  LIBRARY_DOCUMENT_LIMIT_EXCEEDED: {
    title: '文档数量已达上限',
    hint: '可在「知识库配置 → 数据与容量」中提高文档数上限，或删除部分文档后再上传。'
  },
  LIBRARY_SIZE_LIMIT_EXCEEDED: {
    title: '知识库总大小已达上限',
    hint: '可在「知识库配置 → 数据与容量」中提高总大小上限，或删除大文件 / 历史版本后再上传。'
  },
  LIBRARY_CHUNK_LIMIT_EXCEEDED: {
    title: '向量条目已达上限',
    hint: '可在「知识库配置 → 数据与容量」中提高向量条目上限，或减少分块（调大块大小）后重试；也可删除部分已索引文档。'
  },
  MIME_TYPE_NOT_ALLOWED: {
    title: '文件类型不在允许范围内',
    hint: '请在「知识库配置 → 数据与容量 → 支持类型」中勾选对应格式，或更换为已支持的文件。'
  },
  FILE_TOO_LARGE: {
    title: '单个文件过大',
    hint: '请压缩或拆分文件；单文件上限见采集页「规则与配置」中的上传限制说明。'
  },
  BATCH_FILE_LIMIT_EXCEEDED: {
    title: '单次上传文件过多',
    hint: '请分批上传；单次数量上限见采集页提示。'
  },
  INGEST_SOURCE_NOT_ALLOWED: {
    title: '当前知识库不支持该采集方式',
    hint: '请使用文档采集页的文件 / 文件夹上传。'
  },
  UPLOAD_FAILED: {
    title: '上传失败',
    hint: '请稍后重试；若持续失败，请查看服务端日志。'
  }
}

const PLAIN_MESSAGE_HINTS = [
  {
    test: (msg) => msg.includes('文档数') && msg.includes('上限'),
    hint: INGEST_ERROR_CATALOG.LIBRARY_DOCUMENT_LIMIT_EXCEEDED.hint
  },
  {
    test: (msg) => msg.includes('总大小') && msg.includes('上限'),
    hint: INGEST_ERROR_CATALOG.LIBRARY_SIZE_LIMIT_EXCEEDED.hint
  },
  {
    test: (msg) => msg.includes('向量条目') && msg.includes('上限'),
    hint: INGEST_ERROR_CATALOG.LIBRARY_CHUNK_LIMIT_EXCEEDED.hint
  }
]

function composeMessage(detail, entry) {
  const core = (detail || entry.title).trim()
  if (core.endsWith('。') || core.endsWith('！') || core.endsWith('!')) {
    return `${core} ${entry.hint}`
  }
  return `${core}。${entry.hint}`
}

export function formatApiErrorPayload(data) {
  if (!data) return null
  if (typeof data === 'string') return enrichPlainIngestMessage(data)

  const errorCode = data.errorCode || data.error_code
  const entry = errorCode ? INGEST_ERROR_CATALOG[errorCode] : null
  const detail = data.detail || data.message || data.error

  if (entry) {
    return composeMessage(detail, entry)
  }
  if (detail) {
    return enrichPlainIngestMessage(String(detail))
  }
  return null
}

export function formatIngestError(err, fallback = '请求失败，请稍后重试') {
  return formatApiErrorPayload(err?.response?.data) || err?.message || fallback
}

export function formatBatchUploadMessage(item) {
  if (!item) return '—'
  if (item.success) return item.message || '—'

  const entry = item.errorCode ? INGEST_ERROR_CATALOG[item.errorCode] : null
  if (entry) {
    return composeMessage(item.message, entry)
  }
  return enrichPlainIngestMessage(item.message) || '上传失败，请稍后重试'
}

export function enrichPlainIngestMessage(message) {
  if (!message || !String(message).trim()) return null
  const text = String(message).trim()
  for (const rule of PLAIN_MESSAGE_HINTS) {
    if (rule.test(text) && !text.includes(rule.hint.slice(0, 8))) {
      return composeMessage(text, { title: text, hint: rule.hint })
    }
  }
  return text
}

export function isCapacityLimitError(errorCode) {
  return (
    errorCode === 'LIBRARY_DOCUMENT_LIMIT_EXCEEDED' ||
    errorCode === 'LIBRARY_SIZE_LIMIT_EXCEEDED' ||
    errorCode === 'LIBRARY_CHUNK_LIMIT_EXCEEDED'
  )
}

export function batchFailureSummary(items) {
  if (!items?.length) return null
  const failed = items.filter((i) => !i.success)
  if (!failed.length) return null
  const first = failed[0]
  return formatBatchUploadMessage(first)
}
