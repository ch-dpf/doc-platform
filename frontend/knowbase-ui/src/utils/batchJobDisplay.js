const JOB_TYPE_LABEL = {
  REBUILD: '重索引',
  MIGRATE: '迁移到主档',
  ARCHIVE: '归档清理'
}

const STATUS_LABEL = {
  QUEUED: '排队中',
  RUNNING: '执行中',
  COMPLETED: '已完成',
  PARTIAL: '部分失败',
  FAILED: '失败'
}

export function batchJobTypeLabel(type) {
  return JOB_TYPE_LABEL[type] || type || '—'
}

export function batchJobStatusLabel(status) {
  return STATUS_LABEL[status] || status || '—'
}

export function batchJobProgressStatus(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'FAILED') return 'exception'
  return undefined
}

export function batchJobAlertType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'FAILED') return 'error'
  return 'info'
}

export function batchJobTagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'PARTIAL') return 'warning'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING' || status === 'QUEUED') return 'info'
  return 'info'
}
