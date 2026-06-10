<template>
  <div class="page-wrap page-wrap--fluid library-list-page library-page-fill">
    <PageCard title="文档库">
      <template #actions>
        <span v-if="total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 条</span>
        <el-button round :disabled="!libraryId" @click="openBatchReindexDialog">批量重索引</el-button>
        <el-button type="primary" :icon="Refresh" :loading="loading" round @click="load(1)">
          刷新
        </el-button>
      </template>

      <div class="library-page-content">
        <div class="filter-panel">
          <el-form :inline="true" class="filter-form" @submit.prevent="load(1)">
            <el-form-item label="知识库" required>
              <el-select v-model="libraryId" filterable style="width: 200px" @change="onLibraryChange">
                <el-option
                  v-for="lib in libraries"
                  :key="lib.libraryId"
                  :label="lib.name"
                  :value="lib.libraryId"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="租户 ID" required>
              <el-input v-model="filters.tenantId" style="width: 120px" clearable />
            </el-form-item>
            <el-form-item label="关键字">
              <el-input
                v-model="filters.keyword"
                placeholder="文件名 / URL"
                clearable
                style="width: 180px"
              />
            </el-form-item>
            <el-form-item label="解析">
              <el-select v-model="filters.parseStatus" clearable placeholder="全部" style="width: 100px">
                <el-option label="待处理" value="PENDING" />
                <el-option label="解析中" value="PARSING" />
                <el-option label="已解析" value="PARSED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
            </el-form-item>
            <el-form-item label="索引">
              <el-select v-model="filters.indexStatus" clearable placeholder="全部" style="width: 100px">
                <el-option label="待索引" value="PENDING" />
                <el-option label="已索引" value="INDEXED" />
                <el-option label="失败" value="FAILED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" round :loading="loading" @click="load(1)">查询</el-button>
            </el-form-item>
            <el-form-item class="filter-form__view">
              <el-radio-group v-model="viewMode" size="small" @change="onViewModeChange">
                <el-radio-button value="list">
                  <el-icon><List /></el-icon>
                  <span class="view-label">列表</span>
                </el-radio-button>
                <el-radio-button value="card">
                  <el-icon><Grid /></el-icon>
                  <span class="view-label">卡片</span>
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>

        <div class="library-list-body">
          <el-table
            v-if="viewMode === 'list'"
            v-loading="loading"
            :data="items"
            height="100%"
            stripe
            class="data-table library-table library-table--fixed"
            highlight-current-row
            empty-text="暂无文档，请从知识库详情上传"
            table-layout="auto"
            @row-click="openDetail"
          >
            <el-table-column label="文件名" min-width="200">
              <template #default="{ row }">
                <span class="name-link">{{ row.fileName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="大小" min-width="88" align="center">
              <template #default="{ row }">{{ formatFileSize(row.sizeBytes) }}</template>
            </el-table-column>
            <el-table-column label="分块" min-width="64" align="center">
              <template #default="{ row }">
                <span v-if="row.indexStatus === 'INDEXED'">{{ row.chunkCount ?? 0 }}</span>
                <span v-else class="list-empty">—</span>
              </template>
            </el-table-column>
            <el-table-column label="版本" min-width="64" align="center">
              <template #default="{ row }">v{{ row.version }}</template>
            </el-table-column>
            <el-table-column label="解析" min-width="88" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="documentStatusType(row.parseStatus)" effect="plain">
                  {{ parseStatusLabel(row.parseStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="索引" min-width="88" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="row.indexStatus"
                  size="small"
                  :type="documentStatusType(row.indexStatus)"
                  effect="plain"
                >
                  {{ indexStatusLabel(row.indexStatus) }}
                </el-tag>
                <span v-else class="list-empty">—</span>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="140" align="center">
              <template #default="{ row }">{{ formatListTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="220" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openDetail(row)">分块</el-button>
                <el-button
                  v-if="canApproveIndex(row)"
                  link
                  type="success"
                  @click.stop="approveIndex(row)"
                >
                  批准
                </el-button>
                <el-button link type="primary" @click.stop="copyDocId(row.docId)">复制 ID</el-button>
                <el-button link type="warning" @click.stop="softDelete(row)">删除</el-button>
                <el-button link type="danger" @click.stop="hardPurge(row)">彻底删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else v-loading="loading" class="library-card-grid">
            <article
              v-for="row in items"
              :key="row.docId"
              class="library-card"
              tabindex="0"
              @click="openDetail(row)"
              @keydown.enter="openDetail(row)"
            >
              <header class="library-card__head">
                <h3 class="library-card__title" :title="row.fileName">{{ row.fileName }}</h3>
              </header>
              <p class="library-card__summary">
                <span>{{ formatFileSize(row.sizeBytes) }}</span>
                <span class="library-card__dot">·</span>
                <span>分块 {{ row.indexStatus === 'INDEXED' ? (row.chunkCount ?? 0) : '—' }}</span>
                <span class="library-card__dot">·</span>
                <span>v{{ row.version }}</span>
              </p>
              <div class="library-card__status" @click.stop>
                <el-tag size="small" :type="documentStatusType(row.parseStatus)" effect="plain">
                  {{ parseStatusLabel(row.parseStatus) }}
                </el-tag>
                <el-tag
                  v-if="row.indexStatus"
                  size="small"
                  :type="documentStatusType(row.indexStatus)"
                  effect="plain"
                >
                  {{ indexStatusLabel(row.indexStatus) }}
                </el-tag>
              </div>
              <p class="library-card__meta">{{ formatListTime(row.updatedAt) }}</p>
              <footer class="library-card__foot" @click.stop>
                <el-button link type="primary" size="small" @click="openDetail(row)">分块</el-button>
                <el-button
                  v-if="canApproveIndex(row)"
                  link
                  type="success"
                  size="small"
                  @click="approveIndex(row)"
                >
                  批准
                </el-button>
                <el-button link type="primary" size="small" @click="copyDocId(row.docId)">复制 ID</el-button>
                <el-button link type="warning" size="small" @click="softDelete(row)">删除</el-button>
                <el-button link type="danger" size="small" @click="hardPurge(row)">彻底删除</el-button>
              </footer>
            </article>

            <el-empty
              v-if="!loading && items.length === 0"
              class="library-card-empty"
              description="暂无文档，请从知识库详情上传"
              :image-size="80"
            />
          </div>
        </div>

        <div class="library-list-footer">
          <el-pagination
            v-if="total > 0"
            class="pager-row"
            background
            layout="total, sizes, prev, pager, next"
            :total="total"
            :current-page="page"
            :page-size="size"
            :page-sizes="[10, 20, 50]"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </div>
    </PageCard>

    <el-dialog
      v-model="batchReindexVisible"
      title="批量补偿重索引"
      width="560px"
      append-to-body
      @closed="onBatchReindexClosed"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="将按当前知识库配置，对已解析文档重新分块并向量化（异步任务）"
        style="margin-bottom: 12px"
      />
      <p class="batch-reindex-desc">
        适用场景：变更知识库配置（清洗、分块、Embedding）后，使库内已有文档与最新配置保持一致。
      </p>
      <el-descriptions v-if="batchReindexSummary" :column="1" border size="small">
        <el-descriptions-item label="知识库">{{ batchReindexSummary.libraryName }}</el-descriptions-item>
        <el-descriptions-item label="租户 ID">{{ filters.tenantId }}</el-descriptions-item>
        <el-descriptions-item label="可重索引文档">
          已解析且存有 parsed.txt 的文档（预估由服务端统计）
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button round @click="batchReindexVisible = false">取消</el-button>
        <el-button type="primary" round :loading="batchReindexLoading" @click="submitBatchReindex">
          提交批量重索引
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Grid, List, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDocuments, deleteDocument, purgeDocument, approveDocumentIndex } from '../api/ingest'
import { listVectorLibraries } from '../api/library'
import { rebuildLibrary } from '../api/vector'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'
import {
  documentStatusType,
  formatListTime,
  indexStatusLabel,
  parseStatusLabel
} from '../utils/documentDisplay'
import { formatFileSize } from '../utils/formatFileSize'

const router = useRouter()
const route = useRoute()
const { libraryId, persist } = useLibraryContext()

const libraries = ref([])
const filters = reactive({
  tenantId: localStorage.getItem('tenantId') || 'demo',
  keyword: '',
  parseStatus: '',
  indexStatus: ''
})

const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const batchReindexVisible = ref(false)
const batchReindexLoading = ref(false)
const batchReindexSummary = ref(null)

const DOC_VIEW_MODE_KEY = 'docListViewMode'
const viewMode = ref(localStorage.getItem(DOC_VIEW_MODE_KEY) === 'card' ? 'card' : 'list')

function onViewModeChange(mode) {
  localStorage.setItem(DOC_VIEW_MODE_KEY, mode)
}

function canApproveIndex(row) {
  return row.parseStatus === 'PARSED' && row.indexStatus === 'PENDING'
}

async function approveIndex(row) {
  await ElMessageBox.confirm(
    `批准「${row.fileName}」进入向量索引？`,
    '批准索引',
    { type: 'info', confirmButtonText: '批准' }
  )
  await approveDocumentIndex(row.docId)
  ElMessage.success('已提交索引')
  load(page.value)
}

async function loadLibraries() {
  const { data } = await listVectorLibraries({
    tenantId: filters.tenantId.trim(),
    page: 1,
    size: 200
  })
  libraries.value = data.items || []
}

function onLibraryChange() {
  persist()
  load(1)
}

function goToLibraryForIngest() {
  if (libraryId.value) {
    router.push({ name: 'vectorLibraryDetail', params: { libraryId: libraryId.value } })
  } else {
    router.push('/vector-libraries')
  }
}

function buildParams(p) {
  const params = {
    libraryId: libraryId.value,
    tenantId: filters.tenantId.trim(),
    page: p,
    size: size.value
  }
  if (filters.keyword?.trim()) params.keyword = filters.keyword.trim()
  if (filters.parseStatus) params.parseStatus = filters.parseStatus
  if (filters.indexStatus) params.indexStatus = filters.indexStatus
  return params
}

async function load(p = page.value) {
  if (!libraryId.value || !filters.tenantId?.trim()) {
    ElMessage.warning('请选择知识库并填写租户 ID')
    return
  }
  persist()
  localStorage.setItem('tenantId', filters.tenantId.trim())
  loading.value = true
  page.value = p
  try {
    const { data } = await listDocuments(buildParams(p))
    items.value = data.items || []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  load(p)
}

function onSizeChange(s) {
  size.value = s
  load(1)
}

function openDetail(row) {
  localStorage.setItem('lastDocId', row.docId)
  router.push({
    name: 'documentChunks',
    params: { docId: row.docId },
    query: { libraryId: libraryId.value || undefined, from: 'documents' }
  })
}

function copyDocId(id) {
  navigator.clipboard.writeText(id)
  localStorage.setItem('lastDocId', id)
  ElMessage.success('已复制文档 ID')
}

async function softDelete(row) {
  await ElMessageBox.confirm(
    `软删除「${row.fileName}」：从列表隐藏并清理向量索引，MinIO 原文件与元数据行仍保留。`,
    '软删除',
    { type: 'warning' }
  )
  await deleteDocument(row.docId)
  ElMessage.success('已软删除')
  load(page.value)
}

async function hardPurge(row) {
  await ElMessageBox.confirm(
    `彻底删除「${row.fileName}」：将删除 MinIO 对象及数据库元数据，并清理向量索引。不可恢复。`,
    '彻底删除',
    { type: 'error', confirmButtonText: '确认彻底删除', confirmButtonClass: 'el-button--danger' }
  )
  await purgeDocument(row.docId)
  ElMessage.success('已彻底删除')
  load(page.value)
}

function openBatchReindexDialog() {
  const lib = libraries.value.find((l) => l.libraryId === libraryId.value)
  batchReindexSummary.value = {
    libraryName: lib?.name || libraryId.value || '—'
  }
  batchReindexVisible.value = true
}

function onBatchReindexClosed() {
  batchReindexSummary.value = null
}

async function submitBatchReindex() {
  if (!libraryId.value || !filters.tenantId?.trim()) {
    ElMessage.warning('请选择知识库并填写租户 ID')
    return
  }
  batchReindexLoading.value = true
  try {
    const { data } = await rebuildLibrary({
      libraryId: libraryId.value,
      tenantId: filters.tenantId.trim()
    })
    if (data.candidateCount > 0) {
      ElMessage.success(data.message || `已提交 ${data.candidateCount} 个文档的重索引任务`)
    } else {
      ElMessage.warning(data.message || '没有可重索引的文档')
    }
    batchReindexVisible.value = false
    router.replace({ path: '/documents' })
    await load(page.value)
  } finally {
    batchReindexLoading.value = false
  }
}

watch(
  () => route.query.docId,
  (id) => {
    if (!id || typeof id !== 'string') return
    if (route.query.libraryId && typeof route.query.libraryId === 'string') {
      libraryId.value = route.query.libraryId
      persist()
    }
    router.replace({
      name: 'documentChunks',
      params: { docId: id.trim() },
      query: { libraryId: libraryId.value || undefined, from: 'documents' }
    })
  },
  { immediate: true }
)

watch(
  () => [route.query.batchReindex, route.query.libraryId, libraries.value.length],
  ([flag, qLib]) => {
    if (flag !== '1') return
    if (qLib && typeof qLib === 'string') {
      libraryId.value = qLib
      persist()
    }
    if (!libraryId.value) return
    openBatchReindexDialog()
    router.replace({ path: '/documents', query: { libraryId: libraryId.value } })
  },
  { immediate: true }
)

onMounted(async () => {
  await loadLibraries()
  if (route.query.libraryId && typeof route.query.libraryId === 'string') {
    libraryId.value = route.query.libraryId
    persist()
  }
  await load(1)
})
</script>

<style scoped>
.stat-chip {
  margin-right: 12px;
  font-size: 13px;
  color: #64748b;
}
.stat-chip strong {
  color: #0f172a;
}
.library-table--fixed {
  width: 100%;
}
.library-table--fixed :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}
.batch-reindex-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
</style>
