<template>
  <div class="page-wrap">
    <PageCard title="文档库">
      <template #actions>
        <span v-if="total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 条</span>
        <el-button type="primary" :icon="Refresh" :loading="loading" round @click="load(1)">
          刷新
        </el-button>
      </template>

      <div class="filter-panel">
        <el-form :inline="true" @submit.prevent="load(1)">
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
          <el-form-item label="来源">
            <el-select v-model="filters.sourceType" clearable placeholder="全部" style="width: 100px">
              <el-option label="上传" value="UPLOAD" />
              <el-option label="采集" value="CRAWL" />
            </el-select>
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
            <el-button type="primary" round @click="load(1)">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :data="items"
        class="data-table"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="fileName" label="文件名" min-width="150" show-overflow-tooltip />
        <el-table-column label="来源" width="88" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'CRAWL' ? 'warning' : 'primary'" effect="light">
              {{ row.sourceType === 'CRAWL' ? '采集' : '上传' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sourceUrl" label="来源 URL" min-width="160" show-overflow-tooltip />
        <el-table-column label="解析" width="96" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.parseStatus)" effect="dark" round>
              {{ row.parseStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="索引" width="96" align="center">
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
        <el-table-column prop="version" label="版本" width="64" align="center" />
        <el-table-column label="更新时间" width="168">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="copyDocId(row.docId)">复制 ID</el-button>
            <el-button link type="warning" @click="softDelete(row)">删除</el-button>
            <el-button link type="danger" @click="hardPurge(row)">彻底删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="total > 0"
        class="pager"
        background
        layout="total, prev, pager, next, sizes"
        :total="total"
        :current-page="page"
        :page-size="size"
        :page-sizes="[10, 20, 50]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
      <el-empty
        v-else-if="!loading"
        class="empty-block"
        description="暂无文档，请从知识库详情进入文档采集"
        :image-size="100"
      >
        <el-button type="primary" round @click="goToLibraryForIngest">去知识库</el-button>
      </el-empty>
    </PageCard>

    <el-dialog
      v-model="detailVisible"
      title="文档详情与入库进度"
      width="720px"
      destroy-on-close
      class="detail-dialog"
      @closed="stopPoll"
    >
      <div v-if="detail" class="status-board">
        <div class="status-tile">
          <div class="status-tile__label">解析</div>
          <el-tag size="large" :type="statusType(detail.parseStatus)" effect="dark" round>
            {{ detail.parseStatus }}
          </el-tag>
        </div>
        <div class="status-tile">
          <div class="status-tile__label">索引</div>
          <el-tag
            v-if="detail.indexStatus"
            size="large"
            :type="statusType(detail.indexStatus)"
            effect="dark"
            round
          >
            {{ detail.indexStatus }}
          </el-tag>
          <span v-else class="muted">未请求索引</span>
        </div>
        <div class="status-tile">
          <div class="status-tile__label">版本</div>
          <span class="tile-value">v{{ detail.version }}</span>
        </div>
        <div class="status-tile">
          <div class="status-tile__label">文件名</div>
          <span class="tile-value tile-ellipsis" :title="detail.fileName">{{ detail.fileName }}</span>
        </div>
      </div>

      <el-alert
        v-if="polling"
        class="poll-alert"
        type="info"
        :closable="false"
        show-icon
      >
        每 3 秒刷新入库进度（解析 → 向量索引）
      </el-alert>

      <JsonPanel v-if="detail" title="元数据" :data="detail" />

      <template #footer>
        <el-button
          v-if="!polling"
          round
          :disabled="!detail"
          @click="startPoll"
        >
          自动刷新
        </el-button>
        <el-button v-else round type="danger" plain @click="stopPoll">停止刷新</el-button>
        <el-button round :disabled="!detail" @click="openRebuildDialog">补偿重索引</el-button>
        <el-button round type="warning" :disabled="!detail" @click="purgeVectors">清理向量</el-button>
        <el-button type="primary" round @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rebuildVisible" title="补偿重索引" width="520px" append-to-body>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="异步任务：根据解析文本 URL 重新分块并向量化（HTTP 202）"
        style="margin-bottom: 12px"
      />
      <el-form label-width="110px">
        <el-form-item label="解析文本 URL" required>
          <el-input
            v-model="rebuildForm.parsedTextUrl"
            placeholder="MinIO 预签名或内部可访问 URL"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button round @click="rebuildVisible = false">取消</el-button>
        <el-button type="primary" round :loading="rebuildLoading" @click="submitRebuild">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDocuments, getDocument, deleteDocument, purgeDocument } from '../api/ingest'
import { listVectorLibraries } from '../api/library'
import { rebuildIndex, purgeIndex } from '../api/vector'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'
import JsonPanel from '../components/JsonPanel.vue'

const router = useRouter()
const route = useRoute()
const { libraryId, persist } = useLibraryContext()

const libraries = ref([])
const filters = reactive({
  tenantId: localStorage.getItem('tenantId') || 'demo',
  keyword: '',
  sourceType: '',
  parseStatus: '',
  indexStatus: ''
})

const loading = ref(false)
const items = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const detailVisible = ref(false)
const detail = ref(null)
const polling = ref(false)
let pollTimer = null

const rebuildVisible = ref(false)
const rebuildLoading = ref(false)
const rebuildForm = reactive({ parsedTextUrl: '' })

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

async function loadLibraries() {
  const { data } = await listVectorLibraries(filters.tenantId.trim())
  libraries.value = data
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
  if (filters.sourceType) params.sourceType = filters.sourceType
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

async function refreshDetail() {
  if (!detail.value?.docId) return
  const { data } = await getDocument(detail.value.docId)
  detail.value = data
}

async function openDetail(row) {
  localStorage.setItem('lastDocId', row.docId)
  const { data } = await getDocument(row.docId)
  detail.value = data
  detailVisible.value = true
}

function copyDocId(id) {
  navigator.clipboard.writeText(id)
  localStorage.setItem('lastDocId', id)
  ElMessage.success('已复制文档 ID')
}

function startPoll() {
  stopPoll()
  polling.value = true
  refreshDetail()
  pollTimer = setInterval(refreshDetail, 3000)
}

function stopPoll() {
  polling.value = false
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function openRebuildDialog() {
  if (!detail.value) return
  rebuildForm.parsedTextUrl = ''
  rebuildVisible.value = true
}

async function submitRebuild() {
  const d = detail.value
  if (!d?.docId || !rebuildForm.parsedTextUrl?.trim()) {
    ElMessage.warning('请填写解析文本 URL')
    return
  }
  rebuildLoading.value = true
  try {
    await rebuildIndex({
      libraryId: d.libraryId || libraryId.value,
      docId: d.docId,
      tenantId: d.tenantId,
      version: d.version,
      parsedTextUrl: rebuildForm.parsedTextUrl.trim()
    })
    ElMessage.success('重索引任务已提交（异步）')
    rebuildVisible.value = false
    await refreshDetail()
  } finally {
    rebuildLoading.value = false
  }
}

async function purgeVectors() {
  const id = detail.value?.docId
  if (!id) return
  await ElMessageBox.confirm(
    '仅清理该文档的向量分块与索引任务记录，不删除 MinIO 原文与元数据。',
    '清理向量',
    { type: 'warning' }
  )
  await purgeIndex(id)
  ElMessage.success('向量数据已清理')
  await refreshDetail()
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

watch(
  () => route.query.docId,
  async (id) => {
    if (!id || typeof id !== 'string') return
    try {
      const { data } = await getDocument(id.trim())
      detail.value = data
      detailVisible.value = true
      if (route.query.poll === '1') startPoll()
    } catch {
      /* handled by client interceptor */
    }
  },
  { immediate: true }
)

onMounted(async () => {
  await loadLibraries()
  await load(1)
})
onUnmounted(stopPoll)
</script>

<style scoped>
.pager {
  margin-top: 20px;
  justify-content: flex-end;
}
.muted {
  color: #94a3b8;
}
.poll-alert {
  margin: 12px 0;
}
.tile-value {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}
.tile-ellipsis {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
