/**
 * 构建 RAG 多轮对话历史（不含当前待发送问题）。
 * @param {Array<{ role: string, content: string, loading?: boolean }>} messages
 */
export function buildRagChatHistory(messages) {
  return (messages || [])
    .filter((m) => !m.loading && m.content?.trim() && (m.role === 'user' || m.role === 'assistant'))
    .map((m) => ({ role: m.role, content: m.content.trim() }))
}

/**
 * 构建 RAG 请求体。
 */
export function buildRagChatPayload({
  libraryId,
  tenantId,
  question,
  history,
  topK,
  minScore,
  docIds,
  chatModel,
  includeAllChunkProfiles
}) {
  const payload = {
    libraryId,
    tenantId: tenantId.trim(),
    question: question.trim(),
    topK,
    minScore: minScore > 0 ? minScore : null,
    chatModel: chatModel?.trim() || null
  }
  if (history?.length) {
    payload.history = history
  }
  if (docIds?.length) {
    payload.filter = { docIds }
  }
  if (includeAllChunkProfiles) {
    payload.includeAllChunkProfiles = true
  }
  return payload
}
