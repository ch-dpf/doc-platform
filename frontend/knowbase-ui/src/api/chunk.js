import client from './client'

/** POST /api/v1/index/chunk-preview */
export function previewChunks(body) {
  return client.post('/api/v1/index/chunk-preview', body)
}
