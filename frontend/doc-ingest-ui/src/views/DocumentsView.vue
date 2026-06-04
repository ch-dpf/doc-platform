<template>
  <div class="page-wrap">
    <PageCard title="文档库" subtitle="按租户查看已接入文档，支持筛选与生命周期管理">
      <template #actions>
        <span v-if="total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 条</span>
        <el-button type="primary" :icon="Refresh" :loading="loading" round @click="load(1)">
          刷新
        </el-button>
      </template>

      <div class="filter-panel">
        <el-form :inline="true" @submit.prevent="load(1)">
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
        <el-table-column label="操作" width="260" fixed="right">
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
        description="暂无文档，请前往「文档收集上传」添加"
        :image-size="100"
      >
        <el-button type="primary" round @click="router.push('/ingest')">去收集上传</el-button>
      </el-empty>
    </PageCard>

    <el-dialog
      v-model="detailVisible"
      title="文档详情"
      width="680px"
      destroy-on-close
      class="detail-dialog"
    >
      <JsonPanel v-if="detail" title="元数据" :data="detail" />
      <template #footer>
        <el-button round @click="goQuery(detail?.docId)">查询状态</el-button>
        <el-button type="primary" round @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDocuments, getDocument, deleteDocument, purgeDocument } from '../api/ingest'
import PageCard from '../components/PageCard.vue'
import JsonPanel from '../components/JsonPanel.vue'

const router = useRouter()

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

function buildParams(p) {
  const params = { tenantId: filters.tenantId.trim(), page: p, size: size.value }
  if (filters.keyword?.trim()) params.keyword = filters.keyword.trim()
  if (filters.sourceType) params.sourceType = filters.sourceType
  if (filters.parseStatus) params.parseStatus = filters.parseStatus
  if (filters.indexStatus) params.indexStatus = filters.indexStatus
  return params
}

async function load(p = page.value) {
  if (!filters.tenantId?.trim()) {
    ElMessage.warning('请填写租户 ID')
    return
  }
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

async function openDetail(row) {
  const { data } = await getDocument(row.docId)
  detail.value = data
  detailVisible.value = true
}

function copyDocId(id) {
  navigator.clipboard.writeText(id)
  localStorage.setItem('lastDocId', id)
  ElMessage.success('已复制文档 ID')
}

function goQuery(docId) {
  if (docId) {
    localStorage.setItem('lastDocId', docId)
    detailVisible.value = false
    router.push('/query')
  }
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

onMounted(() => load(1))
</script>

<style scoped>
.pager {
  margin-top: 20px;
  justify-content: flex-end;
}
.muted {
  color: #94a3b8;
}
</style>
