import client from './client'

/**
 * GET /api/v1/vector-libraries
 * @param {{ tenantId: string, keyword?: string, tag?: string, page?: number, size?: number }} params
 * @returns {Promise<{ data: { items: object[], total: number, page: number, size: number } }>}
 */
export function listVectorLibraries(params) {
  return client.get('/api/v1/vector-libraries', { params })
}

/** GET /api/v1/vector-libraries/meta/tags — 租户下已使用的知识库标签 */
export function listVectorLibraryTags(tenantId) {
  return client.get('/api/v1/vector-libraries/meta/tags', { params: { tenantId } })
}

export function getVectorLibrary(libraryId) {
  return client.get(`/api/v1/vector-libraries/${libraryId}`)
}

export function createVectorLibrary(body) {
  return client.post('/api/v1/vector-libraries', body)
}

/** PUT /api/v1/vector-libraries/{libraryId} — 名称、描述与知识库配置 */
export function updateVectorLibrarySettings(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}`, body)
}

export function deleteVectorLibrary(libraryId, tenantId) {
  return client.delete(`/api/v1/vector-libraries/${libraryId}`, { params: { tenantId } })
}

export function getUploadTask(taskId) {
  return client.get(`/api/v1/upload-tasks/${taskId}`)
}
