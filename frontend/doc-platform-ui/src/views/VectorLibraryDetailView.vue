<template>
  <div class="page-wrap">
    <PageCard :title="libraryTitle">
      <template #actions>
        <el-button round @click="goBack">返回列表</el-button>
        <el-button round @click="openEdit">编辑规则</el-button>
        <el-button type="primary" round @click="goIngest">
          <el-icon class="btn-icon"><Upload /></el-icon>
          文档采集
        </el-button>
      </template>

      <el-skeleton v-if="libraryLoading" :rows="4" animated />

      <template v-else-if="library">
        <div class="stat-grid">
          <div class="stat-card stat-card--primary">
            <span class="stat-card__label">文档数</span>
            <span class="stat-card__value">{{ library.documentCount }}</span>
          </div>
          <div class="stat-card stat-card--primary">
            <span class="stat-card__label">分块数</span>
            <span class="stat-card__value">{{ library.chunkCount }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-card__label">数据源</span>
            <span class="stat-card__value stat-card__value--sm">
              {{ sourceLabel(library.config?.ingestSourceMode) }}
            </span>
          </div>
          <div class="stat-card">
            <span class="stat-card__label">存储</span>
            <span class="stat-card__value stat-card__value--sm">{{ library.config?.storageType || '—' }}</span>
          </div>
          <div class="stat-card">
            <span class="stat-card__label">Embedding</span>
            <span class="stat-card__value stat-card__value--sm">{{ library.config?.embeddingModel || '—' }}</span>
          </div>
        </div>

        <div class="meta-bar">
          <span class="meta-bar__label">流水线</span>{{ fixedPipeline }}
        </div>

        <div class="section-block">
          <div class="section-head">
            <h3>库内文档</h3>
            <span v-if="total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 个文件</span>
          </div>

          <el-form :inline="true" class="filter-panel" @submit.prevent="loadDocs(1)">
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
              <el-button round :loading="docsLoading" @click="loadDocs(page)">刷新</el-button>
            </el-form-item>
          </el-form>

          <el-table v-loading="docsLoading" :data="documents" stripe class="data-table">
            <el-table-column prop="fileName" label="文件名" min-width="160" show-overflow-tooltip />
            <el-table-column label="来源" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.sourceType === 'CRAWL' ? 'warning' : 'primary'" effect="light">
                  {{ row.sourceType === 'CRAWL' ? '采集' : '上传' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="解析" width="92" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.parseStatus)" effect="dark" round>
                  {{ row.parseStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="索引" width="92" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="row.indexStatus"
                  size="small"
                  :type="statusType(row.indexStatus)"
                  effect="dark"
                  round
                >
                  {{ row.indexStatus }}
                </el-tag>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="168">
              <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openInDocuments(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="total > 0"
            class="pager"
            background
            layout="total, prev, pager, next"
            :total="total"
            :current-page="page"
            :page-size="size"
            @current-change="loadDocs"
          />

          <el-empty v-else-if="!docsLoading" description="暂无文档，点击下方开始采集" :image-size="96">
            <el-button type="primary" round @click="goIngest">文档采集</el-button>
          </el-empty>
        </div>
      </template>

      <el-result v-else icon="warning" title="知识库不存在或无法加载">
        <template #extra>
          <el-button type="primary" round @click="goBack">返回列表</el-button>
        </template>
      </el-result>
    </PageCard>

    <EditLibrarySettingsDrawer
      v-model="editVisible"
      :library-id="libraryIdParam"
      @saved="onRulesSaved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getVectorLibrary } from '../api/library'
import { listDocuments } from '../api/ingest'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'
import EditLibrarySettingsDrawer from '../components/EditLibrarySettingsDrawer.vue'

const FIXED_PIPELINE =
  '数据源接入 → 文档解析 → 文本清洗 → 分块 → 向量化 → 写入知识库'

const route = useRoute()
const router = useRouter()
const { libraryId, tenantId, persist } = useLibraryContext()

const libraryIdParam = computed(() => String(route.params.libraryId || ''))
const fixedPipeline = FIXED_PIPELINE

const libraryLoading = ref(false)
const library = ref(null)
const docsLoading = ref(false)
const documents = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const editVisible = ref(false)

const libraryTitle = computed(() => library.value?.name || '知识库详情')

function sourceLabel(mode) {
  if (mode === 'crawl') return '线上采集'
  if (mode === 'both') return '本地+线上'
  return '本地文件'
}

function statusType(s) {
  if (!s) return 'info'
  if (s === 'PARSED' || s === 'INDEXED') return 'success'
  if (s === 'FAILED') return 'danger'
  return 'warning'
}

function formatTime(iso) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

async function loadLibrary() {
  if (!libraryIdParam.value) return
  libraryLoading.value = true
  try {
    const { data } = await getVectorLibrary(libraryIdParam.value)
    library.value = data
    libraryId.value = data.libraryId
    persist()
  } catch {
    library.value = null
  } finally {
    libraryLoading.value = false
  }
}

async function loadDocs(p = 1) {
  if (!libraryIdParam.value || !tenantId.value?.trim()) return
  docsLoading.value = true
  page.value = p
  try {
    const params = {
      libraryId: libraryIdParam.value,
      tenantId: tenantId.value.trim(),
      page: p,
      size: size.value
    }
    if (keyword.value?.trim()) params.keyword = keyword.value.trim()
    const { data } = await listDocuments(params)
    documents.value = data.items || []
    total.value = data.total ?? 0
  } finally {
    docsLoading.value = false
  }
}

function goBack() {
  router.push('/vector-libraries')
}

function goIngest() {
  libraryId.value = libraryIdParam.value
  persist()
  router.push({ path: '/ingest', query: { libraryId: libraryIdParam.value } })
}

function openEdit() {
  editVisible.value = true
}

function openInDocuments(row) {
  libraryId.value = libraryIdParam.value
  persist()
  router.push({ path: '/documents', query: { docId: row.docId, poll: '1' } })
}

async function onRulesSaved() {
  await loadLibrary()
}

async function refreshAll() {
  await loadLibrary()
  await loadDocs(1)
}

watch(libraryIdParam, () => {
  if (libraryIdParam.value) refreshAll()
})

onMounted(refreshAll)
</script>

