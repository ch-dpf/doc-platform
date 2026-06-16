<template>
  <el-dialog
    v-model="visible"
    title="创建知识库"
    width="480px"
    destroy-on-close
    append-to-body
    lock-scroll
    class="quick-create-library-dialog"
    @closed="onClosed"
  >
    <el-form class="quick-create-form" label-position="top" @submit.prevent="submit">
      <LibraryBasicFields :form="form" layout="top" />
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
import LibraryBasicFields from './LibraryBasicFields.vue'
import { createVectorLibrary } from '../api/library'
import { buildCreatePayload, createEmptyLibraryForm } from '../utils/libraryDefaults'
import { librarySettingsRoute } from '../utils/libraryOnboarding'
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
const form = reactive(createEmptyLibraryForm())

function resetForm() {
  const empty = createEmptyLibraryForm()
  form.name = empty.name
  form.description = empty.description
  form.tags = empty.tags
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写知识库名称')
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
    emit('created', lib)
    ElMessage.success('知识库已创建')
    visible.value = false
    router.push(
      librarySettingsRoute(lib.libraryId, {
        onboarding: true,
        tab: 'parsing'
      })
    )
  } finally {
    submitting.value = false
  }
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

<style scoped>
.quick-create-library-dialog__hint {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--dp-text-secondary);
}
</style>
