import { ref } from 'vue'
import { listEmbeddingModels } from '../api/embedding'
import {
  EMBEDDING_MODEL_FALLBACKS,
  INDEX_PROVIDER_LABEL,
  INDEX_VECTOR_STORE_LABEL,
  mapEmbeddingCatalogModels
} from '../utils/embeddingModels'

/** 加载本地 Ollama Embedding 模型目录（索引 / Rerank 共用） */
export function useEmbeddingCatalog() {
  const provider = ref('ollama')
  const vectorStoreType = ref('pgvector')
  const models = ref([...EMBEDDING_MODEL_FALLBACKS])
  const loading = ref(false)
  const loaded = ref(false)
  const error = ref('')

  async function load(force = false) {
    if (loaded.value && !force) return
    loading.value = true
    error.value = ''
    try {
      const { data } = await listEmbeddingModels()
      provider.value = data?.provider || 'ollama'
      vectorStoreType.value = data?.vectorStoreType || 'pgvector'
      const mapped = mapEmbeddingCatalogModels(data?.models)
      if (mapped.length) {
        models.value = mapped
      }
      loaded.value = true
    } catch (e) {
      error.value = e?.message || '无法获取本地 Embedding 模型'
      models.value = [...EMBEDDING_MODEL_FALLBACKS]
      loaded.value = true
    } finally {
      loading.value = false
    }
  }

  function providerLabel() {
    return provider.value === 'ollama' ? INDEX_PROVIDER_LABEL : provider.value
  }

  function vectorStoreLabel() {
    return vectorStoreType.value === 'pgvector' ? INDEX_VECTOR_STORE_LABEL : vectorStoreType.value
  }

  return {
    provider,
    vectorStoreType,
    models,
    loading,
    loaded,
    error,
    load,
    providerLabel,
    vectorStoreLabel
  }
}
