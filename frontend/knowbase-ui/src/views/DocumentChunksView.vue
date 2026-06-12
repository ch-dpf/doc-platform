<template>
  <div class="page-wrap page-wrap--fluid library-list-page library-page-fill">
    <PageCard title="文档分块" :subtitle="pageSubtitle">
      <template #actions>
        <el-button round :icon="ArrowLeft" @click="goBack">返回</el-button>
        <span v-if="total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 块</span>
        <el-button round :loading="loading" @click="load(page)">刷新</el-button>
      </template>

      <div class="library-page-content">
        <div v-if="summary" class="library-detail-summary">
          <p class="summary-file" :title="summary.fileName">{{ summary.fileName }}</p>
          <div class="summary-metrics">
            <div class="summary-metric summary-metric--primary">
              <span class="summary-metric__label">分块总数</span>
              <span class="summary-metric__value">{{ total }}</span>
            </div>
            <div class="summary-metric summary-metric--primary">
              <span class="summary-metric__label">本页字数</span>
              <span class="summary-metric__value">{{ pageCharCount }}</span>
            </div>
            <div class="summary-metric">
              <span class="summary-metric__label">文档版本</span>
              <span class="summary-metric__value summary-metric__value--sm">v{{ summary.version }}</span>
            </div>
            <div class="summary-metric">
              <span class="summary-metric__label">文件大小</span>
              <span class="summary-metric__value summary-metric__value--sm">
                {{ formatFileSize(summary.sizeBytes) }}
              </span>
            </div>
          </div>
          <div v-if="summary.parseStatus || summary.indexStatus" class="summary-tags">
            <span class="summary-tags__label">状态</span>
            <el-tag
              v-if="summary.parseStatus"
              size="small"
              :type="documentStatusType(summary.parseStatus)"
              effect="plain"
              class="summary-tags__chip"
            >
              解析 {{ parseStatusLabel(summary.parseStatus) }}
            </el-tag>
            <el-tag
              v-if="summary.indexStatus"
              size="small"
              :type="documentStatusType(summary.indexStatus)"
              effect="plain"
              class="summary-tags__chip"
            >
              索引 {{ indexStatusLabel(summary.indexStatus) }}
            </el-tag>
          </div>
          <div v-if="ingestProfileSummary" class="summary-ingest-profile">
            <span class="summary-tags__label">分块覆盖</span>
            <el-tag size="small" type="warning" effect="plain" class="summary-tags__chip">
              {{ ingestProfileSummary }}
            </el-tag>
          </div>
          <div v-if="pipelineTraceDisplay" class="summary-pipeline-trace">
            <span class="summary-tags__label">管道路由</span>
            <el-tag size="small" effect="plain" class="summary-tags__chip">
              族群 {{ pipelineTraceDisplay.familyLabel }}
            </el-tag>
            <el-tag size="small" type="info" effect="plain" class="summary-tags__chip">
              策略 {{ pipelineTraceDisplay.strategyLabel }}
            </el-tag>
            <el-tag
              v-if="pipelineTraceDisplay.multiGranularity"
              size="small"
              type="success"
              effect="plain"
              class="summary-tags__chip"
              title="长文档默认启用：子块用于向量检索，父段上下文用于 RAG 生成"
            >
              多粒度索引
            </el-tag>
            <el-tag
              v-if="pipelineTraceDisplay.adjustmentLabel"
              size="small"
              type="warning"
              effect="plain"
              class="summary-tags__chip"
              :title="summary?.ingestReport?.chunkingAdjustmentReason"
            >
              {{ pipelineTraceDisplay.adjustmentLabel }}
            </el-tag>
            <span
              v-if="pipelineTraceDisplay.configVersion"
              class="summary-pipeline-trace__meta"
            >
              管道 v{{ pipelineTraceDisplay.configVersion }}
            </span>
          </div>
          <div v-if="contentSignalsDisplay" class="summary-content-signals">
            <span class="summary-tags__label">内容结构</span>
            <span class="summary-content-signals__meta">
              约 <strong>{{ contentSignalsDisplay.textLength }}</strong> 字
            </span>
            <el-tag
              v-for="hint in contentSignalsDisplay.hints"
              :key="hint"
              size="small"
              effect="plain"
              class="summary-tags__chip"
            >
              {{ hint }}
            </el-tag>
          </div>
          <div v-if="ingestReportDisplay" class="summary-ingest-report">
            <span class="summary-tags__label">入库报告</span>
            <div class="summary-ingest-report__metrics">
              <span class="summary-ingest-metric">
                原始 <strong>{{ ingestReportDisplay.rawChunkCount }}</strong> 块
              </span>
              <span class="summary-ingest-metric">
                过滤 <strong>{{ ingestReportDisplay.filteredOutCount }}</strong> 块
              </span>
              <span class="summary-ingest-metric">
                入库 <strong>{{ ingestReportDisplay.finalChunkCount }}</strong> 块
              </span>
              <span class="summary-ingest-metric">
                均长 <strong>{{ ingestReportDisplay.avgChunkLength }}</strong> 字
              </span>
            </div>
            <el-tag
              v-if="ingestReportDisplay.headerOnlyRatioWarning"
              size="small"
              type="warning"
              effect="dark"
              class="summary-tags__chip"
            >
              GATE-01 表头占比过高
            </el-tag>
          </div>
          <el-alert
            v-if="ingestReportDisplay?.headerOnlyRatioWarning"
            class="summary-gate-alert"
            type="warning"
            :closable="false"
            show-icon
            :title="GATE01_WARNING_TITLE"
            :description="GATE01_WARNING_DESC"
          />
        </div>

        <div class="filter-panel">
          <el-form :inline="true" class="filter-form">
            <el-form-item>
              <span v-if="total > 0" class="chunk-page-hint">{{ pageRangeText }}</span>
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
            empty-text="暂无分块文本（文档可能尚未完成向量化索引）"
            table-layout="auto"
            @row-click="openChunk"
          >
            <el-table-column label="#" min-width="64" align="center">
              <template #default="{ row }">
                <span class="chunk-index">{{ row.index }}</span>
              </template>
            </el-table-column>
            <el-table-column label="字数" min-width="72" align="center">
              <template #default="{ row }">{{ row.length }}</template>
            </el-table-column>
            <el-table-column label="分块内容" min-width="320">
              <template #default="{ row }">
                <p class="chunk-preview">{{ row.content }}</p>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="72" fixed="right" align="center">
              <template #default="{ row }">
                <el-button link type="primary" @click.stop="openChunk(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else v-loading="loading" class="library-card-grid">
            <article
              v-for="row in items"
              :key="row.index"
              class="library-card"
              tabindex="0"
              @click="openChunk(row)"
              @keydown.enter="openChunk(row)"
            >
              <header class="library-card__head">
                <h3 class="library-card__title">分块 #{{ row.index }}</h3>
                <span class="library-card__meta">{{ row.length }} 字</span>
              </header>
              <p class="chunk-card-body">{{ row.content }}</p>
              <footer class="library-card__foot" @click.stop>
                <el-button link type="primary" size="small" @click="openChunk(row)">查看全文</el-button>
                <el-button link type="primary" size="small" @click="copyChunk(row)">复制</el-button>
              </footer>
            </article>

            <el-empty
              v-if="!loading && items.length === 0"
              class="library-card-empty"
              description="暂无分块文本（文档可能尚未完成向量化索引）"
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
            :page-sizes="[10, 20, 50, 100]"
            @current-change="onPageChange"
            @size-change="onPageSizeChange"
          />
        </div>
      </div>
    </PageCard>

    <el-dialog
      v-model="chunkDialogVisible"
      :title="chunkDialogTitle"
      width="720px"
      append-to-body
      destroy-on-close
      class="chunk-dialog"
      @closed="activeChunk = null"
    >
      <template v-if="activeChunk">
        <div class="chunk-dialog__meta">
          <span>序号 <strong>#{{ activeChunk.index }}</strong></span>
          <span>字数 <strong>{{ activeChunk.length }}</strong></span>
        </div>
        <pre class="chunk-dialog__content">{{ activeChunk.content }}</pre>
      </template>
      <template #footer>
        <el-button round @click="chunkDialogVisible = false">关闭</el-button>
        <el-button type="primary" round :disabled="!activeChunk" @click="copyChunk(activeChunk)">
          复制内容
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Grid, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDocument, getDocumentChunks } from '../api/ingest'
import PageCard from '../components/PageCard.vue'
import { usePageTitle } from '../composables/usePageTitle'
import {
  documentStatusType,
  indexStatusLabel,
  parseStatusLabel
} from '../utils/documentDisplay'
import { formatFileSize } from '../utils/formatFileSize'
import { contentSignalsSummary, pipelineTraceSummary } from '../utils/contentPipeline'
import {
  GATE01_WARNING_DESC,
  GATE01_WARNING_TITLE,
  ingestReportSummary
} from '../utils/ingestReport'
import { formatIngestProfileSummary } from '../utils/ingestProfile'
import { BATCH_JOB_DONE_EVENT } from '../utils/batchJobEvents'

const VIEW_MODE_KEY = 'chunkListViewMode'

const route = useRoute()
const router = useRouter()
const { setPageTitle, clearPageTitle } = usePageTitle()

const docId = computed(() => String(route.params.docId || '').trim())
const libraryId = computed(() => {
  const q = route.query.libraryId
  return typeof q === 'string' ? q.trim() : ''
})
const fromSource = computed(() => {
  const q = route.query.from
  return typeof q === 'string' ? q : ''
})

const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const summary = ref(null)
const ingestReportDisplay = computed(() => ingestReportSummary(summary.value?.ingestReport))
const pipelineTraceDisplay = computed(() =>
  pipelineTraceSummary(summary.value?.ingestReport)
)
const contentSignalsDisplay = computed(() =>
  contentSignalsSummary(summary.value?.contentSignals)
)
const ingestProfileSummary = computed(() =>
  formatIngestProfileSummary(summary.value?.ingestProfile)
)
const viewMode = ref(localStorage.getItem(VIEW_MODE_KEY) === 'card' ? 'card' : 'list')
const activeChunk = ref(null)
const chunkDialogVisible = ref(false)

const pageSubtitle = computed(() => {
  if (summary.value?.fileName) {
    return summary.value.fileName
  }
  return '查看已入库的分块文本'
})

const pageCharCount = computed(() =>
  items.value.reduce((sum, row) => sum + (row.length || 0), 0)
)

const pageRangeText = computed(() => {
  if (total.value <= 0) return ''
  const start = (page.value - 1) * pageSize.value + 1
  const end = Math.min(page.value * pageSize.value, total.value)
  return `第 ${start}–${end} 块，共 ${total.value} 块`
})

const chunkDialogTitle = computed(() => {
  if (!activeChunk.value) return '分块详情'
  return `分块 #${activeChunk.value.index}`
})

const highlightChunkIndex = computed(() => {
  const q = route.query.chunkIndex
  if (q == null || q === '') return null
  const n = Number(q)
  return Number.isFinite(n) ? n : null
})

function onViewModeChange(mode) {
  localStorage.setItem(VIEW_MODE_KEY, mode)
}

async function load(p = page.value) {
  const id = docId.value
  if (!id) return
  loading.value = true
  try {
    const [docRes, chunkRes] = await Promise.all([
      getDocument(id).catch(() => ({ data: null })),
      getDocumentChunks(id, { page: p, size: pageSize.value })
    ])
    const doc = docRes.data
    const data = chunkRes.data
    items.value = data.items || []
    total.value = data.total ?? 0
    page.value = data.page ?? p
    pageSize.value = data.size ?? pageSize.value
    summary.value = {
      fileName: data.fileName || doc?.fileName || id,
      version: data.version ?? doc?.version ?? 0,
      sizeBytes: doc?.sizeBytes,
      parseStatus: doc?.parseStatus,
      indexStatus: doc?.indexStatus,
      ingestReport: doc?.ingestReport ?? null,
      ingestProfile: doc?.ingestProfile ?? null,
      contentSignals: doc?.contentSignals ?? null
    }
    setPageTitle(summary.value.fileName ? `文档分块 · ${summary.value.fileName}` : '文档分块')
    localStorage.setItem('lastDocId', id)
    await maybeHighlightChunk()
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  load(p)
}

function onPageSizeChange(size) {
  pageSize.value = size
  load(1)
}

function openChunk(row) {
  if (!row) return
  activeChunk.value = row
  chunkDialogVisible.value = true
}

async function maybeHighlightChunk() {
  const idx = highlightChunkIndex.value
  if (idx == null || total.value <= 0) return
  const targetPage = Math.floor(idx / pageSize.value) + 1
  if (targetPage !== page.value) {
    await load(targetPage)
    return
  }
  const row = items.value.find((r) => r.index === idx)
  if (row) openChunk(row)
}

async function copyChunk(row) {
  if (!row?.content) return
  try {
    await navigator.clipboard.writeText(row.content)
    ElMessage.success('已复制分块内容')
  } catch {
    ElMessage.error('复制失败')
  }
}

function goBack() {
  if (fromSource.value === 'qa') {
    router.push({ name: 'qa', query: libraryId.value ? { libraryId: libraryId.value } : undefined })
    return
  }
  if (fromSource.value === 'library' && libraryId.value) {
    router.push({ name: 'vectorLibraryDetail', params: { libraryId: libraryId.value } })
    return
  }
  router.push({
    path: '/documents',
    query: libraryId.value ? { libraryId: libraryId.value } : undefined
  })
}

watch(docId, () => {
  page.value = 1
  load(1)
})

function onBatchJobDone(event) {
  const job = event?.detail
  if (!job) return
  if (job.jobType === 'MIGRATE' || job.jobType === 'REBUILD') {
    load(page.value)
  }
}

onMounted(() => {
  window.addEventListener(BATCH_JOB_DONE_EVENT, onBatchJobDone)
  load(1)
})
onUnmounted(() => {
  window.removeEventListener(BATCH_JOB_DONE_EVENT, onBatchJobDone)
  clearPageTitle()
})
</script>

<style scoped>
.library-table--fixed {
  width: 100%;
}
.library-table--fixed :deep(.el-table__body-wrapper) {
  overflow-y: auto;
}
.library-table--fixed :deep(.el-scrollbar__bar.is-horizontal) {
  height: 6px;
}
.library-table--fixed :deep(.el-scrollbar__bar.is-vertical) {
  width: 6px;
}

.summary-ingest-profile,
.summary-ingest-report,
.summary-pipeline-trace,
.summary-content-signals {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-top: 8px;
}

.summary-pipeline-trace__meta,
.summary-content-signals__meta {
  font-size: 12px;
  color: var(--dp-text-secondary);
}

.summary-content-signals__meta strong {
  color: var(--dp-text);
  font-weight: 600;
}

.summary-ingest-report__metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.summary-ingest-metric {
  font-size: 12px;
  color: var(--dp-text-secondary);
}

.summary-ingest-metric strong {
  color: var(--dp-text);
  font-weight: 600;
}

.summary-gate-alert {
  margin-top: 8px;
}
</style>
