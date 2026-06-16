import client from './client'

/** GET /api/v1/embedding-models — 本地 Ollama Embedding 模型目录 */
export function listEmbeddingModels() {
  return client.get('/api/v1/embedding-models')
}
