import { onUnmounted, ref } from 'vue'
import { getBatchJob } from '../api/vector'
import { notifyBatchJobDone } from '../utils/batchJobEvents'

const TERMINAL = new Set(['COMPLETED', 'PARTIAL', 'FAILED'])

export function useBatchJobPoll() {
  const activeJob = ref(null)
  let timer = null

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  async function pollOnce(jobId) {
    const { data } = await getBatchJob(jobId)
    activeJob.value = data
    return data
  }

  function start(jobId, { intervalMs = 2000, onDone } = {}) {
    stop()
    activeJob.value = null
    pollOnce(jobId).catch(() => {})
    timer = setInterval(async () => {
      try {
        const data = await pollOnce(jobId)
        if (TERMINAL.has(data.status)) {
          stop()
          notifyBatchJobDone(data)
          onDone?.(data)
        }
      } catch {
        stop()
      }
    }, intervalMs)
  }

  onUnmounted(stop)

  return { activeJob, start, stop, pollOnce }
}

export function isBatchJobTerminal(status) {
  return TERMINAL.has(status)
}
