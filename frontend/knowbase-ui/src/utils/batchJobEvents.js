export const BATCH_JOB_DONE_EVENT = 'knowbase:batch-job-done'

export function notifyBatchJobDone(job) {
  if (typeof window === 'undefined' || !job) return
  window.dispatchEvent(new CustomEvent(BATCH_JOB_DONE_EVENT, { detail: job }))
}
