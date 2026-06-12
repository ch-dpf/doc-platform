import client from './client'

const base = import.meta.env.VITE_API_BASE || ''

/** POST /api/v1/rag/chat — 支持 history 多轮上下文 */
export function ragChat(body, config = {}) {
  return client.post('/api/v1/rag/chat', body, config)
}

/** SSE 流式 RAG（无会话持久化，客户端维护 history） */
export async function ragChatStream(body, handlers = {}) {
  const url = `${base}/api/v1/rag/chat/stream`
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'text/event-stream' },
    body: JSON.stringify(body)
  })
  if (!response.ok) {
    let detail = `HTTP ${response.status}`
    try {
      const json = await response.json()
      detail = json.detail || json.error || detail
    } catch {
      // ignore
    }
    const err = new Error(detail)
    handlers.onError?.(err)
    throw err
  }
  const reader = response.body?.getReader()
  if (!reader) throw new Error('浏览器不支持流式响应')
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const parts = buffer.split('\n\n')
    buffer = parts.pop() || ''
    for (const part of parts) {
      const dataLine = part.split('\n').find((l) => l.startsWith('data:'))
      if (!dataLine) continue
      const jsonText = dataLine.slice(5).trim()
      if (!jsonText) continue
      try {
        const event = JSON.parse(jsonText)
        if (event.type === 'chunk' && event.content) handlers.onChunk?.(event.content)
        else if (event.type === 'done') handlers.onDone?.(event)
        else if (event.type === 'error') throw new Error(event.content || '流式问答失败')
      } catch (e) {
        if (e instanceof SyntaxError) continue
        handlers.onError?.(e)
        throw e
      }
    }
  }
}

/** GET /api/v1/index/rebuild-library/candidates — 批量重索引候选统计 */
export function getRebuildCandidates(params) {
  return client.get('/api/v1/index/rebuild-library/candidates', { params })
}

/** POST /api/v1/index/rebuild-library — 按当前库规则批量重索引；可选 chunkProfileId 仅处理该档 */
export function rebuildLibrary(body) {
  return client.post('/api/v1/index/rebuild-library', body)
}

/** GET /api/v1/index/batch-jobs/{jobId} — 批量任务进度 */
export function getBatchJob(jobId) {
  return client.get(`/api/v1/index/batch-jobs/${jobId}`)
}

/** GET /api/v1/index/batch-jobs — 知识库最近批量任务 */
export function listBatchJobs(params) {
  return client.get('/api/v1/index/batch-jobs', { params })
}

/** POST /api/v1/index/batch-jobs/{jobId}/retry — 重试失败项 */
export function retryBatchJob(jobId) {
  return client.post(`/api/v1/index/batch-jobs/${jobId}/retry`)
}

/** GET /api/v1/index/batch-jobs/{jobId}/failed-items — 失败文档列表 */
export function getBatchJobFailedItems(jobId) {
  return client.get(`/api/v1/index/batch-jobs/${jobId}/failed-items`)
}
