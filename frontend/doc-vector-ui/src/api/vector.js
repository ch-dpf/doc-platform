import client from './client'

/** POST /api/v1/search */
export function search(body) {
  return client.post('/api/v1/search', body)
}

/** POST /api/v1/rag/chat */
export function ragChat(body) {
  return client.post('/api/v1/rag/chat', body)
}

/** POST /api/v1/index/rebuild */
export function rebuildIndex(body) {
  return client.post('/api/v1/index/rebuild', body)
}

/** DELETE /api/v1/index/{docId} */
export function purgeIndex(docId) {
  return client.delete(`/api/v1/index/${docId}`)
}
