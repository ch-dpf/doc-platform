<template>
  <div class="library-panel library-settings-panel">
    <el-form
      v-loading="loading"
      class="cfg-form library-settings-form"
      label-width="100px"
      label-position="right"
    >
      <LibraryConfigTabs
        ref="tabsRef"
        v-model:active-tab="activeTab"
        :form="form"
        :library-id="libraryIdParam"
        :chunk-strategy-rows="chunkStrategyRows"
        :indexed-chunk-count="loadedChunkCount"
        strategy-empty-text="加载策略摘要…"
      />
    </el-form>

    <div class="library-settings-panel__footer">
      <el-button type="primary" round :loading="saving" @click="submit">保存库配置</el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LibraryConfigTabs from '../../components/LibraryConfigTabs.vue'
import {
  getChunkStrategySummary,
  getMigrationCandidates,
  getVectorLibrary,
  migrateToPrimary,
  updateChunkGovernance,
  updateLibraryBasic,
  updateLibraryIndexPipeline,
  updateLibraryRetrieval
} from '../../api/library'
import { useLibraryContext } from '../../composables/useLibraryContext'
import { defaultLibraryConfig } from '../../utils/libraryDefaults'
import { diffLibraryConfig, diffNeedsReindex } from '../../utils/libraryConfig'
import {
  buildIndexPipelinePayload,
  buildRetrievalPayload,
  flattenLibraryConfig,
  hasPipelineChanges,
  hasRetrievalChanges,
  libraryWithFlatConfig,
  normalizeSubmitConfig
} from '../../utils/libraryConfigView'

const route = useRoute()
const router = useRouter()
const { tenantId } = useLibraryContext()

const libraryIdParam = computed(() => String(route.params.libraryId || ''))
const activeTab = ref('basic')
const loading = ref(false)
const saving = ref(false)
const loadedDocCount = ref(0)
const loadedChunkCount = ref(0)
const snapshotPayload = ref(null)
const chunkStrategyRows = ref([])
const tabsRef = ref(null)

const form = reactive({
  name: '',
  description: '',
  tags: [],
  config: defaultLibraryConfig()
})

function clonePayload() {
  return {
    name: form.name,
    description: form.description,
    tags: [...form.tags],
    config: normalizeSubmitConfig(form.config)
  }
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.tags = []
  form.config = defaultLibraryConfig()
  snapshotPayload.value = null
  chunkStrategyRows.value = []
  tabsRef.value?.resetUi?.()
}

function formatDiffMessage(changes) {
  return changes
    .map((c) => {
      if (c.detail?.length) return `${c.label}：\n  ${c.detail.join('\n  ')}`
      return `${c.label}：${c.before} → ${c.after}`
    })
    .join('\n')
}

async function confirmSave(changes) {
  const needsReindex = diffNeedsReindex(changes)
  const hasChunks = loadedChunkCount.value > 0
  let message = `将保存以下库配置变更：\n\n${formatDiffMessage(changes)}`
  if (needsReindex && hasChunks) {
    message += '\n\n部分变更会影响向量索引，保存后建议执行批量补偿重索引。'
  }
  try {
    await ElMessageBox.confirm(message, '确认保存库配置', {
      type: needsReindex && hasChunks ? 'warning' : 'info',
      confirmButtonText: '确认保存',
      cancelButtonText: '取消'
    })
    return true
  } catch {
    return false
  }
}

async function offerMigrationOrReindex() {
  const tid = tenantId.value?.trim()
  const id = libraryIdParam.value
  if (tid && id) {
    try {
      const { data } = await getMigrationCandidates(id, { tenantId: tid })
      if (data?.candidateCount > 0) {
        try {
          await ElMessageBox.confirm(
            `主档已更新。尚有 ${data.candidateCount} 篇文档在非主档，默认问答将无法检索。是否一键迁移到主档 ${data.primaryChunkProfileId}？`,
            '建议迁移到主档',
            {
              type: 'warning',
              confirmButtonText: '一键迁移',
              cancelButtonText: '稍后',
              distinguishCancelAndClose: true
            }
          )
          const { data: mig } = await migrateToPrimary(id, { tenantId: tid })
          if (mig.candidateCount > 0) {
            ElMessage.success(mig.message || `已提交 ${mig.candidateCount} 个文档的迁移任务`)
          }
          return
        } catch (action) {
          if (action === 'cancel') return
        }
      }
    } catch {
      // fall through
    }
  }
  try {
    await ElMessageBox.confirm(
      '配置已保存。库内已有向量分块，是否前往文档列表执行批量补偿重索引？',
      '建议重索引',
      { type: 'warning', confirmButtonText: '去批量重索引', cancelButtonText: '稍后' }
    )
    router.push({
      name: 'libraryDocuments',
      params: { libraryId: id }
    })
  } catch {
    /* later */
  }
}

function mergeLoadedConfig(cfg) {
  const base = defaultLibraryConfig()
  return {
    ...base,
    ...cfg,
    configVersion: cfg.configVersion ?? 1,
    retrieval: { ...base.retrieval, ...(cfg.retrieval || {}) }
  }
}

async function loadChunkStrategySummary() {
  if (!libraryIdParam.value) return
  try {
    const { data } = await getChunkStrategySummary(libraryIdParam.value)
    chunkStrategyRows.value = data || []
  } catch {
    chunkStrategyRows.value = []
  }
}

async function load() {
  if (!libraryIdParam.value) return
  loading.value = true
  resetForm()
  try {
    const [{ data: lib }] = await Promise.all([
      getVectorLibrary(libraryIdParam.value),
      loadChunkStrategySummary()
    ])
    const flat = flattenLibraryConfig(lib)
    form.name = lib.name
    form.description = lib.description || ''
    form.tags = [...(flat.tags || [])]
    form.config = mergeLoadedConfig(flat)
    if (!form.config.retrieval.defaultTopK) {
      form.config.retrieval.defaultTopK = 12
    }
    loadedDocCount.value = lib.documentCount ?? 0
    loadedChunkCount.value = lib.chunkCount ?? 0
    snapshotPayload.value = clonePayload()
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写知识库名称')
    return
  }
  if (!form.description?.trim()) {
    ElMessage.warning('请填写知识库描述')
    return
  }
  const nextConfig = normalizeSubmitConfig(form.config)
  const beforeCfg = snapshotPayload.value?.config || {}
  const changes = diffLibraryConfig(beforeCfg, nextConfig)
  const metaChanged =
    form.name.trim() !== (snapshotPayload.value?.name || '') ||
    form.description !== (snapshotPayload.value?.description || '') ||
    JSON.stringify(form.tags) !== JSON.stringify(snapshotPayload.value?.tags || [])
  const governanceChanged =
    beforeCfg.allowCustomChunkProfiles !== nextConfig.allowCustomChunkProfiles ||
    beforeCfg.maxActiveChunkProfiles !== nextConfig.maxActiveChunkProfiles
  if (!changes.length && !metaChanged && !governanceChanged) {
    ElMessage.info('库配置未变更')
    return
  }
  if (changes.length) {
    if (!(await confirmSave(changes))) return
  } else {
    try {
      await ElMessageBox.confirm('将保存名称、描述或标签变更。', '确认保存库配置', {
        type: 'info',
        confirmButtonText: '确认保存',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }

  saving.value = true
  try {
    const id = libraryIdParam.value
    if (!id) {
      ElMessage.error('未指定知识库 ID，无法保存配置')
      return
    }
    const calls = []
    if (metaChanged) {
      calls.push(updateLibraryBasic(id, {
        name: form.name.trim(),
        description: form.description.trim(),
        tags: [...form.tags]
      }))
    }
    if (hasPipelineChanges(changes)) {
      calls.push(updateLibraryIndexPipeline(id, buildIndexPipelinePayload(nextConfig)))
    }
    if (hasRetrievalChanges(changes)) {
      calls.push(updateLibraryRetrieval(id, buildRetrievalPayload(nextConfig)))
    }
    if (governanceChanged) {
      calls.push(
        updateChunkGovernance(id, {
          allowCustomChunkProfiles: nextConfig.allowCustomChunkProfiles !== false,
          maxActiveChunkProfiles: nextConfig.maxActiveChunkProfiles > 0
            ? nextConfig.maxActiveChunkProfiles
            : 5
        })
      )
    }
    const results = await Promise.all(calls)
    const last = results[results.length - 1]?.data
    const warnings = results.flatMap((r) => r.data?.warnings || [])
    if (warnings.length) ElMessage.warning(warnings[0])
    else ElMessage.success('库配置已保存')
    const savedLib = last?.library ?? last
    libraryWithFlatConfig(savedLib)
    snapshotPayload.value = clonePayload()
    if (diffNeedsReindex(changes) && loadedChunkCount.value > 0) {
      await offerMigrationOrReindex()
    }
  } finally {
    saving.value = false
  }
}

watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab === 'string' && tab) activeTab.value = tab
  },
  { immediate: true }
)

watch(libraryIdParam, () => {
  if (libraryIdParam.value) load()
})

onMounted(() => {
  if (libraryIdParam.value) load()
})
</script>

<style scoped>
.library-settings-panel {
  max-width: 800px;
}
.library-settings-form {
  min-height: 320px;
}
.library-settings-panel__footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}
</style>
