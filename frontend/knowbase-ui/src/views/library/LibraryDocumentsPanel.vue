<template>
  <div class="library-panel library-documents-panel">
    <div class="library-panel__toolbar">
      <span v-if="library && total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 个文档</span>
      <div class="library-panel__toolbar-actions">
        <el-button round :disabled="!library" @click="openBatchReindexDialog">批量重索引</el-button>
        <el-button type="primary" round @click="goIngest">
          <el-icon class="btn-icon"><Upload /></el-icon>
          上传文档
        </el-button>
      </div>
    </div>

    <el-skeleton v-if="libraryLoading" :rows="6" animated />

    <template v-else-if="library">
      <div class="library-page-content">
        <div class="library-detail-summary">
          <div class="summary-metrics">
            <div class="summary-metric summary-metric--primary">
              <span class="summary-metric__label">文档</span>
              <span class="summary-metric__value">{{ library.documentCount ?? 0 }}</span>
            </div>
            <div class="summary-metric summary-metric--primary">
              <span class="summary-metric__label">分块</span>
              <span class="summary-metric__value">{{ library.chunkCount ?? 0 }}</span>
            </div>
            <div class="summary-metric">
              <span class="summary-metric__label">配置版本</span>
              <span class="summary-metric__value summary-metric__value--sm">
                v{{ libraryConfig?.configVersion ?? 1 }}
              </span>
            </div>
            <div class="summary-metric">
              <span class="summary-metric__label">分块大小/重叠</span>
              <span class="summary-metric__value summary-metric__value--sm">
                {{ libraryConfig?.chunkSize ?? '—' }} / {{ libraryConfig?.chunkOverlap ?? '—' }}
              </span>
            </div>
            <div class="summary-metric">
              <span class="summary-metric__label">Embedding</span>
              <span class="summary-metric__value summary-metric__value--sm">
                {{ libraryConfig?.embeddingModel || '—' }}
              </span>
            </div>
          </div>
          <div v-if="libraryConfig?.tags?.length" class="summary-tags">
            <span class="summary-tags__label">标签</span>
            <el-tag
              v-for="t in libraryConfig.tags"
              :key="t"
              size="small"
              effect="plain"
              class="summary-tags__chip"
            >
              {{ t }}
            </el-tag>
          </div>
          <p v-if="library.description" class="summary-desc">{{ library.description }}</p>
        </div>

        <div class="filter-panel">
          <el-form :inline="true" class="filter-form" @submit.prevent="loadDocs(1)">
            <el-form-item label="关键字">
              <el-input
                v-model="keyword"
                placeholder="文件名 / URL"
                clearable
                style="width: 200px"
                @clear="loadDocs(1)"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" round :loading="docsLoading" @click="loadDocs(1)">查询</el-button>
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
            v-loading="docsLoading"
            :data="documents"
            height="100%"
            stripe
            class="data-table library-table library-table--fixed"
            highlight-current-row
            empty-text="暂无文档，点击「上传文档」"
            table-layout="auto"
            @row-click="openInDocuments"
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
            <el-table-column label="分块档" min-width="88" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="row.chunkProfileId"
                  size="small"
                  :type="row.primaryProfile ? 'success' : 'warning'"
                  effect="plain"
                  :title="row.chunkProfileId"
                >
                  {{ row.primaryProfile ? '主档' : '非主' }}
                </el-tag>
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
            <el-table-column label="操作" min-width="200" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openInDocuments(row)">分块</el-button>
                <el-button
                  v-if="canApproveIndex(row)"
                  link
                  type="success"
                  @click.stop="approveIndex(row)"
                >
                  批准
                </el-button>
                <el-button link type="warning" @click.stop="softDelete(row)">删除</el-button>
                <el-button link type="danger" @click.stop="hardPurge(row)">彻底删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else v-loading="docsLoading" class="library-card-grid">
            <article
              v-for="row in documents"
              :key="row.docId"
              class="library-card"
              tabindex="0"
              @click="openInDocuments(row)"
              @keydown.enter="openInDocuments(row)"
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
                <el-button link type="primary" size="small" @click="openInDocuments(row)">分块</el-button>
                <el-button
                  v-if="canApproveIndex(row)"
                  link
                  type="success"
                  size="small"
                  @click="approveIndex(row)"
                >
                  批准
                </el-button>
                <el-button link type="warning" size="small" @click="softDelete(row)">删除</el-button>
                <el-button link type="danger" size="small" @click="hardPurge(row)">彻底删除</el-button>
              </footer>
            </article>

            <el-empty
              v-if="!docsLoading && documents.length === 0"
              class="library-card-empty"
              description="暂无文档，点击「上传文档」"
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
            :page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            @current-change="onPageChange"
            @size-change="onPageSizeChange"
          />
        </div>
      </div>
    </template>

    <el-result v-else icon="warning" title="知识库不存在或无法加载" />

    <el-dialog
      v-model="batchReindexVisible"
      title="批量补偿重索引"
      width="560px"
      append-to-body
      @closed="onBatchReindexClosed"
    >
      <p class="batch-reindex-desc">按当前知识库配置，对已解析文档重新分块并向量化。</p>
      <el-form v-if="!activeBatchJob" label-width="96px" size="small">
        <el-form-item label="分块档范围">
          <el-select
            v-model="batchReindexProfileId"
            clearable
            placeholder="全库已解析文档"
            style="width: 100%"
            :loading="batchReindexProfilesLoading"
          >
            <el-option label="全库（不限分块档）" value="" />
            <el-option
              v-for="p in batchReindexProfiles"
              :key="p.chunkProfileId"
              :label="`${p.primary ? '【主档】' : ''}${p.chunkProfileId}（${p.docCount} 篇）`"
              :value="p.chunkProfileId"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div v-if="activeBatchJob" class="batch-reindex-progress">
        <p class="batch-reindex-progress__title">
          {{ batchJobTypeLabel(activeBatchJob.jobType) }}
          · {{ batchJobStatusLabel(activeBatchJob.status) }}
        </p>
        <el-progress
          :percentage="activeBatchJob.progressPercent"
          :status="batchJobProgressStatus(activeBatchJob.status)"
        />
        <p class="batch-reindex-progress__meta">
          {{ activeBatchJob.completedCount }} / {{ activeBatchJob.totalCount }} 完成
          <span v-if="activeBatchJob.failedCount">，{{ activeBatchJob.failedCount }} 失败</span>
        </p>
      </div>
      <template #footer>
        <el-button round @click="batchReindexVisible = false">
          {{ activeBatchJob ? '关闭' : '取消' }}
        </el-button>
        <el-button
          v-if="!activeBatchJob"
          type="primary"
          round
          :loading="batchReindexLoading"
          @click="submitBatchReindex"
        >
          提交批量重索引
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Grid, List, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listChunkProfiles } from '../../api/library'
import { listDocuments, deleteDocument, purgeDocument, approveDocumentIndex } from '../../api/ingest'
import { rebuildLibrary } from '../../api/vector'
import { useBatchJobPoll } from '../../composables/useBatchJobPoll'
import { useLibraryContext } from '../../composables/useLibraryContext'
import {
  batchJobProgressStatus,
  batchJobStatusLabel,
  batchJobTypeLabel
} from '../../utils/batchJobDisplay'
import { BATCH_JOB_DONE_EVENT } from '../../utils/batchJobEvents'
import {
  documentStatusType,
  formatListTime,
  indexStatusLabel,
  parseStatusLabel
} from '../../utils/documentDisplay'
import { formatFileSize } from '../../utils/formatFileSize'
import { flattenLibraryConfig } from '../../utils/libraryConfigView'

const route = useRoute()
const router = useRouter()
const { libraryId, tenantId, persist } = useLibraryContext()
const refreshLibrary = inject('refreshLibraryDetail', null)

const libraryIdParam = computed(() => String(route.params.libraryId || ''))
const library = inject('libraryDetail', ref(null))
const libraryLoading = inject('libraryDetailLoading', ref(false))

const docsLoading = ref(false)
const documents = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const batchReindexVisible = ref(false)
const batchReindexLoading = ref(false)
const batchReindexProfileId = ref('')
const batchReindexProfiles = ref([])
const batchReindexProfilesLoading = ref(false)
const { activeJob: activeBatchJob, start: startBatchJobPoll, stop: stopBatchJobPoll } = useBatchJobPoll()

const DOC_VIEW_MODE_KEY = 'docListViewMode'
const viewMode = ref(localStorage.getItem(DOC_VIEW_MODE_KEY) === 'card' ? 'card' : 'list')

const libraryConfig = computed(() => flattenLibraryConfig(library.value))

function onViewModeChange(mode) {
  localStorage.setItem(DOC_VIEW_MODE_KEY, mode)
}

async function loadDocs(p = page.value) {
  if (!libraryIdParam.value || !tenantId.value?.trim()) return
  docsLoading.value = true
  page.value = p
  try {
    const params = {
      libraryId: libraryIdParam.value,
      tenantId: tenantId.value.trim(),
      page: p,
      size: pageSize.value
    }
    if (keyword.value?.trim()) params.keyword = keyword.value.trim()
    const { data } = await listDocuments(params)
    documents.value = data.items || []
    total.value = data.total ?? 0
  } finally {
    docsLoading.value = false
  }
}

function onPageChange(p) {
  loadDocs(p)
}

function onPageSizeChange(s) {
  pageSize.value = s
  loadDocs(1)
}

function goIngest() {
  libraryId.value = libraryIdParam.value
  persist()
  router.push({ path: '/ingest', query: { libraryId: libraryIdParam.value } })
}

function openInDocuments(row) {
  router.push({
    name: 'documentChunks',
    params: { docId: row.docId },
    query: { libraryId: libraryIdParam.value, from: 'library' }
  })
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
  await loadDocs(page.value)
  await refreshLibrary?.()
}

async function softDelete(row) {
  await ElMessageBox.confirm(
    `软删除「${row.fileName}」：从列表隐藏并清理向量索引，原文件与元数据仍保留。`,
    '软删除',
    { type: 'warning' }
  )
  await deleteDocument(row.docId)
  ElMessage.success('已软删除')
  await loadDocs(page.value)
  await refreshLibrary?.()
}

async function hardPurge(row) {
  await ElMessageBox.confirm(
    `彻底删除「${row.fileName}」：将删除存储对象及数据库元数据，并清理向量索引。不可恢复。`,
    '彻底删除',
    { type: 'error', confirmButtonText: '确认彻底删除', confirmButtonClass: 'el-button--danger' }
  )
  await purgeDocument(row.docId)
  ElMessage.success('已彻底删除')
  await loadDocs(page.value)
  await refreshLibrary?.()
}

async function openBatchReindexDialog() {
  batchReindexProfileId.value = ''
  batchReindexVisible.value = true
  if (!libraryIdParam.value) return
  batchReindexProfilesLoading.value = true
  try {
    const { data } = await listChunkProfiles(libraryIdParam.value)
    batchReindexProfiles.value = data || []
  } catch {
    batchReindexProfiles.value = []
  } finally {
    batchReindexProfilesLoading.value = false
  }
}

function onBatchReindexClosed() {
  stopBatchJobPoll()
  activeBatchJob.value = null
  batchReindexProfileId.value = ''
  batchReindexProfiles.value = []
}

async function submitBatchReindex() {
  if (!libraryIdParam.value || !tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID')
    return
  }
  batchReindexLoading.value = true
  try {
    const body = {
      libraryId: libraryIdParam.value,
      tenantId: tenantId.value.trim()
    }
    if (batchReindexProfileId.value) {
      body.chunkProfileId = batchReindexProfileId.value
    }
    const { data } = await rebuildLibrary(body)
    if (data.candidateCount > 0 && data.jobId) {
      ElMessage.success(data.message || `已提交 ${data.candidateCount} 个文档的重索引任务`)
      startBatchJobPoll(data.jobId, {
        onDone: async (job) => {
          if (job.status === 'COMPLETED') {
            ElMessage.success('批量重索引已完成')
          } else if (job.status === 'PARTIAL') {
            ElMessage.warning(`批量重索引部分失败（${job.failedCount} 项）`)
          } else if (job.status === 'FAILED') {
            ElMessage.error(job.lastError || '批量重索引失败')
          }
          await refreshLibrary?.()
          await loadDocs(1)
        }
      })
    } else if (data.candidateCount > 0) {
      ElMessage.success(data.message || `已提交 ${data.candidateCount} 个文档的重索引任务`)
      batchReindexVisible.value = false
      await refreshLibrary?.()
      await loadDocs(1)
    } else {
      ElMessage.warning(data.message || '没有可重索引的文档')
      batchReindexVisible.value = false
    }
  } finally {
    batchReindexLoading.value = false
  }
}

function openDocFromQuery(docId) {
  if (!docId) return
  router.replace({
    name: 'documentChunks',
    params: { docId },
    query: { libraryId: libraryIdParam.value, from: 'library' }
  })
}

watch(
  () => route.query.docId,
  (id) => {
    if (typeof id === 'string' && id.trim() && libraryIdParam.value) {
      openDocFromQuery(id.trim())
    }
  },
  { immediate: true }
)

watch(libraryIdParam, () => {
  if (libraryIdParam.value) loadDocs(1)
})

function onBatchJobDone(event) {
  const job = event?.detail
  if (!job?.libraryId || !libraryIdParam.value) return
  if (job.libraryId !== libraryIdParam.value) return
  if (job.jobType === 'MIGRATE' || job.jobType === 'REBUILD' || job.jobType === 'ARCHIVE') {
    refreshLibrary?.()
    loadDocs(1)
  }
}

onMounted(() => {
  window.addEventListener(BATCH_JOB_DONE_EVENT, onBatchJobDone)
  if (libraryIdParam.value) loadDocs(1)
})

onUnmounted(() => {
  window.removeEventListener(BATCH_JOB_DONE_EVENT, onBatchJobDone)
})
</script>

<style scoped>
.library-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.library-panel__toolbar-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.stat-chip {
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
.batch-reindex-progress {
  margin-top: 8px;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.batch-reindex-progress__title {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.batch-reindex-progress__meta {
  margin: 8px 0 0;
  font-size: 12px;
  color: #64748b;
}
.btn-icon {
  margin-right: 4px;
}
</style>
