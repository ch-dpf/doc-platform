import client from './client'

/** POST /api/v1/documents/upload */
export function uploadDocument(tenantId, file, autoIndex = true) {
  const form = new FormData()
  form.append('file', file)
  return client.post('/api/v1/documents/upload', form, {
    params: { tenantId, autoIndex },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** POST /api/v1/documents/collect */
export function collectDocument(body) {
  return client.post('/api/v1/documents/collect', body)
}

/** GET /api/v1/documents */
export function listDocuments(params) {
  return client.get('/api/v1/documents', { params })
}

/** GET /api/v1/documents/{docId} */
export function getDocument(docId) {
  return client.get(`/api/v1/documents/${docId}`)
}

/** DELETE /api/v1/documents/{docId} — 软删除 */
export function deleteDocument(docId) {
  return client.delete(`/api/v1/documents/${docId}`)
}

/** DELETE /api/v1/documents/{docId}/purge — 物理删除 */
export function purgeDocument(docId) {
  return client.delete(`/api/v1/documents/${docId}/purge`)
}
