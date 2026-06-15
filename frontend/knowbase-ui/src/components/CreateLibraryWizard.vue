<template>
  <el-dialog
    v-model="visible"
    title="创建知识库"
    width="720px"
    destroy-on-close
    append-to-body
    lock-scroll
    class="library-config-dialog"
    @closed="onClosed"
  >
    <el-form class="create-form" label-width="100px" label-position="right">
      <LibraryConfigTabs
        ref="tabsRef"
        v-model:active-tab="activeTab"
        :form="form"
        :chunk-strategy-rows="chunkStrategyRows"
        strategy-empty-text="系统默认策略"
      />
    </el-form>

    <template #footer>
      <el-button round @click="visible = false">取消</el-button>
      <el-button type="primary" round :loading="submitting" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import LibraryConfigTabs from './LibraryConfigTabs.vue'
import {
  createVectorLibrary,
  updateLibraryIndexPipeline,
  updateLibraryRetrieval
} from '../api/library'
import { buildDefaultChunkStrategyRows } from '../utils/chunkStrategyDefaults'
import {
  buildCreatePayload,
  createEmptyLibraryForm
} from '../utils/libraryDefaults'
import {
  buildIndexPipelinePayload,
  buildRetrievalPayload,
  normalizeSubmitConfig
} from '../utils/libraryConfigView'
import { usePageTitle } from '../composables/usePageTitle'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  tenantId: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'created'])

const router = useRouter()
const { setPageTitle, clearPageTitle } = usePageTitle()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const submitting = ref(false)
const activeTab = ref('basic')
const tabsRef = ref(null)

const form = reactive(createEmptyLibraryForm())

const chunkStrategyRows = computed(() =>
  buildDefaultChunkStrategyRows({
    hierarchicalChunkingEnabled: form.config.hierarchicalChunkingEnabled,
    chunkDelimiter: form.config.chunkDelimiter
  })
)

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写知识库名称')
    return
  }
  if (!form.description?.trim()) {
    ElMessage.warning('请填写知识库描述')
    return
  }
  submitting.value = true
  try {
    const payload = buildCreatePayload({
      tenantId: props.tenantId,
      name: form.name,
      description: form.description,
      tags: form.tags
    })
    const { data: lib } = await createVectorLibrary(payload)
    if (!lib?.libraryId) {
      ElMessage.error(
        '创建接口未返回 libraryId。若嵌入 kanhai 宿主，请在 application.yml 设置 knowbase.web.expose-controllers: true'
      )
      return
    }
    const config = normalizeSubmitConfig(form.config)
    await Promise.all([
      updateLibraryIndexPipeline(lib.libraryId, buildIndexPipelinePayload(config)),
      updateLibraryRetrieval(lib.libraryId, buildRetrievalPayload(config))
    ])
    emit('created', lib)
    ElMessage.success('知识库已创建')
    visible.value = false
    router.push({ name: 'vectorLibraryDetail', params: { libraryId: lib.libraryId } })
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  const empty = createEmptyLibraryForm()
  form.name = empty.name
  form.description = empty.description
  form.tags = empty.tags
  form.config = empty.config
  activeTab.value = 'basic'
  tabsRef.value?.resetUi?.()
}

function onClosed() {
  clearPageTitle()
  resetForm()
}

watch(visible, (v) => {
  if (v) {
    resetForm()
    setPageTitle('创建知识库')
  } else {
    clearPageTitle()
  }
})
</script>

