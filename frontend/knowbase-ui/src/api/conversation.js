import client from './client'

const base = import.meta.env.VITE_API_BASE || ''

export function createConversation(libraryId, body) {
  return client.post(`/api/v1/vector-libraries/${libraryId}/conversations`, body)
}

export function listConversations({ libraryId, tenantId, page = 1, size = 50 }) {
  return client.get('/api/v1/conversations', {
    params: { libraryId, tenantId, page, size }
  })
}

export function getConversation(conversationId, tenantId) {
  return client.get(`/api/v1/conversations/${conversationId}`, {
    params: { tenantId }
  })
}

export function listConversationMessages(conversationId, tenantId) {
  return client.get(`/api/v1/conversations/${conversationId}/messages`, {
    params: { tenantId }
  })
}

export function deleteConversation(conversationId, tenantId) {
  return client.delete(`/api/v1/conversations/${conversationId}`, {
    params: { tenantId }
  })
}

export function conversationChat(conversationId, body) {
  return client.post(`/api/v1/conversations/${conversationId}/chat`, body)
}

/**
 * SSE 流式对话（POST + fetch ReadableStream）
 * @param {string} conversationId
 * @param {object} body
 * @param {{ onChunk?: (text: string) => void, onDone?: (event: object) => void, onError?: (err: Error) => void }} handlers
 */
export async function conversationChatStream(conversationId, body, handlers = {}) {
  const url = `${base}/api/v1/conversations/${conversationId}/chat/stream`
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
  if (!reader) {
    const err = new Error('浏览器不支持流式响应')
    handlers.onError?.(err)
    throw err
  }
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
        if (event.type === 'chunk' && event.content) {
          handlers.onChunk?.(event.content)
        } else if (event.type === 'done') {
          handlers.onDone?.(event)
        } else if (event.type === 'error') {
          const err = new Error(event.content || '流式问答失败')
          handlers.onError?.(err)
          throw err
        }
      } catch (e) {
        if (e instanceof SyntaxError) continue
        throw e
      }
    }
  }
}
