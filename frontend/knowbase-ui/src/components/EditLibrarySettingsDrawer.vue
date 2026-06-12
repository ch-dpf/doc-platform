<template>
  <el-dialog
    v-model="visible"
    :title="`库配置 · ${form.name || '知识库'}`"
    width="720px"
    destroy-on-close
    append-to-body
    lock-scroll
    class="library-config-dialog"
    @closed="onClosed"
  >
    <el-form
      v-loading="loading"
      class="cfg-form"
      label-width="100px"
      label-position="right"
    >
      <LibraryConfigTabs
        ref="tabsRef"
        v-model:active-tab="activeTab"
        :form="form"
        :library-id="props.libraryId"
        :chunk-strategy-rows="chunkStrategyRows"
        :indexed-chunk-count="loadedChunkCount"
        strategy-empty-text="加载策略摘要…"
      />
    </el-form>

    <template #footer>
      <el-button round @click="visible = false">取消</el-button>
      <el-button type="primary" round :loading="saving" @click="submit">保存库配置</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import LibraryConfigTabs from './LibraryConfigTabs.vue'
import {
  getChunkStrategySummary,
  getMigrationCandidates,
  getVectorLibrary,
  migrateToPrimary,
  updateChunkGovernance,
  updateLibraryBasic,
  updateLibraryIndexPipeline,
  updateLibraryRetrieval
} from '../api/library'
import { useLibraryContext } from '../composables/useLibraryContext'
import { defaultLibraryConfig } from '../utils/libraryDefaults'
import { diffLibraryConfig, diffNeedsReindex } from '../utils/libraryConfig'
import {
  buildIndexPipelinePayload,
  buildRetrievalPayload,
  flattenLibraryConfig,
  hasPipelineChanges,
  hasRetrievalChanges,
  libraryWithFlatConfig,
  normalizeSubmitConfig
} from '../utils/libraryConfigView'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  libraryId: { type: String, default: '' },
  /** 打开时定位到的 Tab：basic | pipeline | index | retrieval */
  initialTab: { type: String, default: 'basic' }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const router = useRouter()
const { tenantId } = useLibraryContext()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

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
  activeTab.value = props.initialTab || 'basic'
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
  if (tid) {
    try {
      const { data } = await getMigrationCandidates(props.libraryId, { tenantId: tid })
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
          const { data: mig } = await migrateToPrimary(props.libraryId, { tenantId: tid })
          if (mig.candidateCount > 0) {
            ElMessage.success(mig.message || `已提交 ${mig.candidateCount} 个文档的迁移任务`)
          }
          return
        } catch (action) {
          if (action === 'cancel') return
        }
      }
    } catch {
      // fall through to batch reindex offer
    }
  }
  try {
    await ElMessageBox.confirm(
      '配置已保存。库内已有向量分块，是否前往文档库执行批量补偿重索引？',
      '建议重索引',
      { type: 'warning', confirmButtonText: '去批量重索引', cancelButtonText: '稍后' }
    )
    router.push({ path: '/documents', query: { batchReindex: '1', libraryId: props.libraryId } })
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
  if (!props.libraryId) return
  try {
    const { data } = await getChunkStrategySummary(props.libraryId)
    chunkStrategyRows.value = data || []
  } catch {
    chunkStrategyRows.value = []
  }
}

async function load() {
  if (!props.libraryId) return
  loading.value = true
  resetForm()
  try {
    const [{ data: lib }] = await Promise.all([
      getVectorLibrary(props.libraryId),
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
    const id = props.libraryId
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
    emit('saved', libraryWithFlatConfig(savedLib))
    visible.value = false
    if (diffNeedsReindex(changes) && loadedChunkCount.value > 0) {
      await offerMigrationOrReindex()
    }
  } finally {
    saving.value = false
  }
}

function onClosed() {
  resetForm()
}

watch(
  () => [visible.value, props.libraryId, props.initialTab],
  ([open, id]) => {
    if (open && id) {
      activeTab.value = props.initialTab || 'basic'
      load()
    }
  }
)
</script>

