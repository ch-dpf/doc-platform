import client from './client'
import { buildRagChatHistory } from '../utils/ragChat'

/** POST /api/v1/vector-libraries/{libraryId}/search — 混合检索预览 */
export function hybridSearch(libraryId, body) {
  return client.post(`/api/v1/vector-libraries/${libraryId}/search`, body)
}

/** POST /api/v1/rag/retrieval-preview — 与问答同路的 RAG 检索预览 */
export function ragRetrievalPreview(body) {
  return client.post('/api/v1/rag/retrieval-preview', body)
}

export function buildRetrievalPreviewPayload({
  libraryId,
  tenantId,
  question,
  topK,
  minScore,
  docIds,
  messages,
  includeAllChunkProfiles
}) {
  const payload = {
    libraryId,
    tenantId: tenantId.trim(),
    question: question.trim(),
    topK,
  }
  if (minScore > 0) {
    payload.minScore = minScore
  }
  const history = buildRagChatHistory(messages || [])
  if (history.length) payload.history = history
  if (docIds?.length) payload.filter = { docIds }
  if (includeAllChunkProfiles) payload.includeAllChunkProfiles = true
  return payload
}
