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
        </div>

        <div class="filter-panel">
          <el-form :inline="true" class="filter-form">
            <el-form-item>
              <span v-if="total > 0" class="chunk-page-hint">{{ pageRangeText }}</span>
              <span v-else class="chunk-page-hint">查看已入库的分块文本</span>
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
      indexStatus: doc?.indexStatus
    }
    setPageTitle(summary.value.fileName ? `文档分块 · ${summary.value.fileName}` : '文档分块')
    localStorage.setItem('lastDocId', id)
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

onMounted(() => load(1))
onUnmounted(() => clearPageTitle())
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
</style>
