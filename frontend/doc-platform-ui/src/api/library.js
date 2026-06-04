import client from './client'

export function listVectorLibraries(tenantId) {
  return client.get('/api/v1/vector-libraries', { params: { tenantId } })
}

export function getVectorLibrary(libraryId) {
  return client.get(`/api/v1/vector-libraries/${libraryId}`)
}

export function createVectorLibrary(body) {
  return client.post('/api/v1/vector-libraries', body)
}

/** PUT /api/v1/vector-libraries/{libraryId} — 名称、预处理、分块与向量化配置 */
export function updateVectorLibrarySettings(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}`, body)
}

export function getUploadTask(taskId) {
  return client.get(`/api/v1/upload-tasks/${taskId}`)
}
