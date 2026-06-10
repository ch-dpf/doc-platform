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

/** POST /api/v1/index/rebuild-library — 按当前库规则批量重索引 */
export function rebuildLibrary(body) {
  return client.post('/api/v1/index/rebuild-library', body)
}
