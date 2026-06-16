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

/** GET 按文件类型的入库分块策略（只读） */
export function getChunkStrategySummary(libraryId) {
  return client.get(`/api/v1/vector-libraries/${libraryId}/chunk-strategy-summary`)
}

/** GET 知识库活跃分块档列表 */
export function listChunkProfiles(libraryId) {
  return client.get(`/api/v1/vector-libraries/${libraryId}/chunk-profiles`)
}

export function setPrimaryChunkProfile(libraryId, chunkProfileId) {
  return client.put(`/api/v1/vector-libraries/${libraryId}/chunk-profiles/primary`, {
    chunkProfileId
  })
}

export function updateChunkGovernance(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}/chunk-governance`, body)
}

export function backfillChunkProfiles(libraryId) {
  return client.post(`/api/v1/vector-libraries/${libraryId}/chunk-profiles/backfill`)
}

/** GET 迁移到主档候选统计 */
export function getMigrationCandidates(libraryId, params) {
  return client.get(`/api/v1/vector-libraries/${libraryId}/chunk-profiles/migration-candidates`, {
    params
  })
}

/** POST 一键迁移到主档 */
export function migrateToPrimary(libraryId, body) {
  return client.post(`/api/v1/vector-libraries/${libraryId}/chunk-profiles/migrate-to-primary`, body)
}

/** GET 归档候选预览 */
export function getArchiveCandidates(libraryId, params) {
  return client.get(`/api/v1/vector-libraries/${libraryId}/chunk-profiles/archive-candidates`, {
    params
  })
}

/** POST 归档分块档（软删除该档文档并清理向量） */
export function archiveChunkProfile(libraryId, body) {
  return client.post(`/api/v1/vector-libraries/${libraryId}/chunk-profiles/archive`, body)
}

/** POST /api/v1/vector-libraries — 原子创建（basic + 可选 indexPipeline / parsing / retrieval） */
export function createVectorLibrary(body) {
  return client.post('/api/v1/vector-libraries', body)
}

export function updateLibraryBasic(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}/basic`, body)
}

export function updateLibraryIndexPipeline(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}/index-pipeline`, body)
}

export function listParserEngines() {
  return client.get('/api/v1/parser-engines')
}

export function updateLibraryParsing(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}/parsing`, body)
}

export function updateLibraryRetrieval(libraryId, body) {
  return client.put(`/api/v1/vector-libraries/${libraryId}/retrieval`, body)
}

export function deleteVectorLibrary(libraryId, tenantId) {
  return client.delete(`/api/v1/vector-libraries/${libraryId}`, { params: { tenantId } })
}

export function getUploadTask(taskId) {
  return client.get(`/api/v1/upload-tasks/${taskId}`)
}
