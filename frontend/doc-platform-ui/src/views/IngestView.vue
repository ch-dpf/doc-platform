<template>
  <div class="page-wrap">
    <el-row :gutter="24" class="ingest-layout">
      <el-col :xs="24" :lg="13" class="ingest-main-col">
        <PageCard title="文档采集">
          <template #actions>
            <el-button round @click="goBackToLibrary">返回知识库</el-button>
          </template>
          <div class="common-fields">
            <el-form label-width="88px" label-position="left">
              <el-form-item label="当前知识库">
                <span class="library-name-badge">{{ currentLibraryName }}</span>
              </el-form-item>
              <el-steps v-if="pipelineSteps.length" :active="pipelineSteps.length" align-center class="pipeline-steps">
                <el-step v-for="s in pipelineSteps" :key="s.code" :title="s.name" status="process" />
              </el-steps>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="租户 ID" required>
                    <el-input v-model="tenantId" placeholder="demo" @change="persist" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="自动索引">
                    <el-switch v-model="autoIndex" inline-prompt active-text="开" inactive-text="关" />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>

          <el-alert
            v-if="constraints"
            class="constraints-alert"
            type="info"
            :closable="false"
            show-icon
          >
            <template #title>数据源：{{ sourceModeLabel }}</template>
            建仓向导中为该库设定的接入方式；仅展示并允许对应入口。
            单文件 ≤ {{ constraints.maxFileSizeDisplay }}，单次最多 {{ constraints.maxBatchFiles }} 个；
            存储：{{ constraints.storageType }}
          </el-alert>

          <el-tabs v-model="activeTab" class="ingest-tabs">
            <el-tab-pane v-if="uploadAllowed" name="upload">
              <template #label>
                <span class="tab-label"><el-icon><UploadFilled /></el-icon> 文件上传</span>
              </template>
              <el-form label-width="88px" @submit.prevent="submitUpload">
                <el-form-item label="选择文件" required>
                  <el-upload
                    class="upload-zone"
                    drag
                    multiple
                    :auto-upload="false"
                    :limit="uploadLimit"
                    :file-list="fileList"
                    :on-change="onFileChange"
                    :on-remove="onFileRemove"
                    :on-exceed="onExceed"
                  >
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <div class="upload-title">拖拽文件到此处，或点击选择（可多选）</div>
                    <div class="upload-desc">
                      PDF · Word · Excel · TXT · Markdown（不支持图片、音视频）
                    </div>
                  </el-upload>
                </el-form-item>
                <el-form-item v-if="uploading">
                  <el-progress :percentage="uploadPercent" :status="uploadPercent >= 100 ? 'success' : ''" />
                  <span class="hint-text">正在上传…</span>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" :loading="loading" @click="submitUpload">
                    {{ fileList.length > 1 ? '批量上传' : '开始上传' }}
                  </el-button>
                </el-form-item>
              </el-form>
              <span class="api-badge">POST /api/v1/documents/upload/batch</span>
            </el-tab-pane>

            <el-tab-pane v-if="collectAllowed" name="collect">
              <template #label>
                <span class="tab-label"><el-icon><Link /></el-icon> URL 采集</span>
              </template>
              <el-form label-width="88px" @submit.prevent="submitCollect">
                <el-form-item label="页面 URL" required>
                  <el-input v-model="collectUrl" placeholder="https://example.com/doc.html" clearable />
                  <p class="hint-text">
                    按完整 URL 区分文档（含端口）。同一 URL 再次采集将递增版本并刷新内容。
                  </p>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" :loading="loading" @click="submitCollect">
                    开始采集
                  </el-button>
                </el-form-item>
              </el-form>
              <span class="api-badge">POST /api/v1/documents/collect</span>
            </el-tab-pane>
          </el-tabs>
        </PageCard>
      </el-col>

      <el-col :xs="24" :lg="11">
        <PageCard title="接入结果">
          <template v-if="batchResult">
            <el-table :data="batchResult.items" stripe size="small" max-height="360">
              <el-table-column prop="fileName" label="文件" min-width="120" show-overflow-tooltip />
              <el-table-column label="状态" width="80" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.success ? 'success' : 'danger'" size="small">
                    {{ row.success ? '成功' : '失败' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="说明" min-width="160" show-overflow-tooltip />
            </el-table>
            <p class="batch-summary">
              共 {{ batchResult.total }} 个，成功 {{ batchResult.succeeded }}，失败 {{ batchResult.failed }}
            </p>
          </template>
          <JsonPanel v-else title="响应 JSON" :data="result" empty-text="上传或采集成功后，结果将显示在这里" />
          <el-alert
            v-if="lastSuccessDocId"
            class="result-alert"
            type="success"
            :closable="false"
            show-icon
          >
            <template #title>接入成功</template>
            最近文档 ID：<code class="id-code">{{ lastSuccessDocId }}</code><br />
            可在「文档库」查看解析与索引进度。
          </el-alert>
          <el-empty v-if="!batchResult && !result" class="empty-block" description="等待提交" :image-size="80" />
        </PageCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getUploadConstraints,
  uploadDocument,
  uploadDocumentsBatch,
  uploadDocumentAsync,
  collectDocument
} from '../api/ingest'
import { getVectorLibrary, getUploadTask } from '../api/library'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'
import JsonPanel from '../components/JsonPanel.vue'

const route = useRoute()
const router = useRouter()
const { libraryId, tenantId, persist } = useLibraryContext()

function goBackToLibrary() {
  if (libraryId.value) {
    router.push({ name: 'vectorLibraryDetail', params: { libraryId: libraryId.value } })
  } else {
    router.push('/vector-libraries')
  }
}

const activeTab = ref(route.query.tab === 'collect' ? 'collect' : 'upload')
const currentLibraryName = ref('—')

const FIXED_PIPELINE_STEPS = [
  { code: 'INGEST', name: '数据源接入' },
  { code: 'PARSE', name: '文档解析' },
  { code: 'NORMALIZE', name: '文本清洗' },
  { code: 'CHUNK', name: '分块' },
  { code: 'EMBED', name: '向量化' },
  { code: 'INDEX', name: '写入知识库' }
]

const pipelineSteps = ref([...FIXED_PIPELINE_STEPS])
const autoIndex = ref(true)
const fileList = ref([])
const collectUrl = ref('')
const loading = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const result = ref(null)
const batchResult = ref(null)
const constraints = ref(null)
const lastSuccessDocId = ref(null)

const uploadLimit = computed(() => constraints.value?.maxBatchFiles ?? 20)

const uploadAllowed = computed(() => constraints.value?.uploadAllowed !== false)

const collectAllowed = computed(() => constraints.value?.collectAllowed === true)

const sourceModeLabel = computed(() => {
  const mode = constraints.value?.ingestSourceMode
  if (mode === 'crawl') return '线上采集'
  if (mode === 'both') return '本地文件 + 线上采集'
  return '本地文件上传'
})

function syncActiveTab() {
  const c = constraints.value
  if (!c) return
  if (route.query.tab === 'collect' && c.collectAllowed) {
    activeTab.value = 'collect'
  } else if (c.uploadAllowed) {
    activeTab.value = 'upload'
  } else if (c.collectAllowed) {
    activeTab.value = 'collect'
  }
}

async function loadCurrentLibrary() {
  if (!libraryId.value) return
  try {
    const { data } = await getVectorLibrary(libraryId.value)
    currentLibraryName.value = data.name
  } catch {
    currentLibraryName.value = libraryId.value
  }
}

async function loadConstraints() {
  if (!libraryId.value) return
  const { data } = await getUploadConstraints(libraryId.value)
  constraints.value = data
  syncActiveTab()
}

async function initPage() {
  const qLib = route.query.libraryId
  if (qLib && typeof qLib === 'string') {
    libraryId.value = qLib
  }
  persist()
  if (!libraryId.value) {
    ElMessage.warning('请先从知识库详情进入文档采集')
    router.replace('/vector-libraries')
    return
  }
  await Promise.all([loadCurrentLibrary(), loadConstraints()])
}

watch(constraints, syncActiveTab)

onMounted(initPage)

function onFileChange(_uploadFile, uploadFiles) {
  fileList.value = uploadFiles
}

function onFileRemove(_file, uploadFiles) {
  fileList.value = uploadFiles
}

function onExceed() {
  ElMessage.warning(`单次最多选择 ${uploadLimit.value} 个文件`)
}

function onUploadProgress(event) {
  if (!event.total) return
  uploadPercent.value = Math.min(99, Math.round((event.loaded * 100) / event.total))
}

async function submitUpload() {
  if (!uploadAllowed.value) {
    ElMessage.warning('当前知识库不支持文件上传')
    return
  }
  if (!libraryId.value || !tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID')
    return
  }
  const files = fileList.value.map((f) => f.raw).filter(Boolean)
  if (!files.length) {
    ElMessage.warning('请选择文件')
    return
  }
  persist()
  loading.value = true
  uploading.value = true
  uploadPercent.value = 0
  result.value = null
  batchResult.value = null
  lastSuccessDocId.value = null
  try {
    const asyncThreshold = 5 * 1024 * 1024
    if (files.length === 1 && files[0].size >= asyncThreshold) {
        const { data: task } = await uploadDocumentAsync(
          libraryId.value,
          tenantId.value.trim(),
          files[0],
          autoIndex.value,
          onUploadProgress
        )
        ElMessage.success(`大文件已提交异步任务 ${task.taskId}`)
        uploadPercent.value = 100
        pollTask(task.taskId)
        return
    }
    if (files.length === 1) {
      const { data } = await uploadDocument(
        libraryId.value,
        tenantId.value.trim(),
        files[0],
        autoIndex.value,
        onUploadProgress
      )
      result.value = data
      if (data?.docId) {
        lastSuccessDocId.value = data.docId
        localStorage.setItem('lastDocId', data.docId)
      }
      ElMessage.success('上传成功')
    } else {
      const { data } = await uploadDocumentsBatch(
        libraryId.value,
        tenantId.value.trim(),
        files,
        autoIndex.value,
        onUploadProgress
      )
      batchResult.value = data
      const firstOk = data.items?.find((i) => i.success && i.document?.docId)
      if (firstOk?.document?.docId) {
        lastSuccessDocId.value = firstOk.document.docId
        localStorage.setItem('lastDocId', firstOk.document.docId)
      }
      if (data.failed === 0) {
        ElMessage.success(`全部 ${data.succeeded} 个文件上传成功`)
      } else {
        ElMessage.warning(`成功 ${data.succeeded} 个，失败 ${data.failed} 个`)
      }
    }
    uploadPercent.value = 100
  } finally {
    loading.value = false
    uploading.value = false
  }
}

async function pollTask(taskId) {
  const timer = setInterval(async () => {
    try {
      const { data } = await getUploadTask(taskId)
      if (data.status === 'COMPLETED') {
        clearInterval(timer)
        result.value = data.docId ? { docId: data.docId } : null
        ElMessage.success('异步入库完成')
      } else if (data.status === 'FAILED') {
        clearInterval(timer)
        ElMessage.error(data.errorMessage || '异步入库失败')
      }
    } catch {
      clearInterval(timer)
    }
  }, 3000)
}

async function submitCollect() {
  if (!collectAllowed.value) {
    ElMessage.warning('当前知识库不支持 URL 采集')
    return
  }
  if (!libraryId.value || !tenantId.value?.trim() || !collectUrl.value?.trim()) {
    ElMessage.warning('请填写租户与 URL')
    return
  }
  persist()
  loading.value = true
  batchResult.value = null
  try {
    const { data } = await collectDocument({
      libraryId: libraryId.value,
      tenantId: tenantId.value.trim(),
      url: collectUrl.value.trim(),
      autoIndex: autoIndex.value
    })
    result.value = data
    if (data?.docId) {
      lastSuccessDocId.value = data.docId
      localStorage.setItem('lastDocId', data.docId)
    }
    ElMessage.success('采集任务已提交')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.batch-summary {
  margin-top: 12px;
  font-size: 13px;
  color: #64748b;
}
.id-code {
  font-size: 12px;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 6px;
  font-family: ui-monospace, monospace;
}
</style>
