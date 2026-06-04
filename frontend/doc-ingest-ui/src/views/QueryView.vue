<template>
  <div class="page-wrap">
    <el-row :gutter="24">
      <el-col :xs="24" :lg="11">
        <PageCard title="状态查询" subtitle="输入文档 ID，查看解析与向量索引流水线进度">
          <el-form label-width="88px" @submit.prevent="query">
            <el-form-item label="文档 ID" required>
              <el-input
                v-model="docId"
                placeholder="粘贴 UUID"
                clearable
                :prefix-icon="Key"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" round @click="query">
                立即查询
              </el-button>
              <el-button
                v-if="!polling"
                size="large"
                round
                :disabled="!docId?.trim()"
                @click="startPoll"
              >
                自动轮询
              </el-button>
              <el-button v-else type="danger" size="large" round plain @click="stopPoll">
                停止轮询
              </el-button>
            </el-form-item>
          </el-form>
          <span class="api-badge">GET /api/v1/documents/{docId}</span>

          <div v-if="result" class="status-board">
            <div class="status-tile">
              <div class="status-tile__label">解析状态</div>
              <el-tag size="large" :type="statusType(result.parseStatus)" effect="dark" round>
                {{ result.parseStatus }}
              </el-tag>
            </div>
            <div class="status-tile">
              <div class="status-tile__label">索引状态</div>
              <el-tag
                v-if="result.indexStatus"
                size="large"
                :type="statusType(result.indexStatus)"
                effect="dark"
                round
              >
                {{ result.indexStatus }}
              </el-tag>
              <span v-else class="muted">未请求索引</span>
            </div>
            <div class="status-tile">
              <div class="status-tile__label">版本</div>
              <span class="tile-value">v{{ result.version }}</span>
            </div>
            <div class="status-tile">
              <div class="status-tile__label">文件名</div>
              <span class="tile-value tile-ellipsis" :title="result.fileName">{{ result.fileName }}</span>
            </div>
          </div>

          <el-descriptions
            v-if="result"
            class="meta-desc"
            :column="1"
            border
            size="small"
          >
            <el-descriptions-item label="租户">{{ result.tenantId }}</el-descriptions-item>
            <el-descriptions-item label="来源">{{ result.sourceType }}</el-descriptions-item>
            <el-descriptions-item v-if="result.sourceUrl" label="URL">
              {{ result.sourceUrl }}
            </el-descriptions-item>
            <el-descriptions-item label="自动索引">
              {{ result.indexRequested ? '是' : '否' }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ formatTime(result.updatedAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </PageCard>
      </el-col>

      <el-col :xs="24" :lg="13">
        <PageCard title="完整响应" subtitle="接口返回的 DocumentResponse 原始 JSON">
          <JsonPanel title="JSON" :data="result" empty-text="查询后将显示完整元数据" />
          <el-alert
            v-if="polling"
            class="result-alert"
            type="info"
            :closable="false"
            show-icon
          >
            每 3 秒自动刷新，直至手动停止
          </el-alert>
        </PageCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onUnmounted, ref } from 'vue'
import { Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getDocument } from '../api/ingest'
import PageCard from '../components/PageCard.vue'
import JsonPanel from '../components/JsonPanel.vue'

const docId = ref(localStorage.getItem('lastDocId') || '')
const loading = ref(false)
const polling = ref(false)
const result = ref(null)
let timer = null

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

async function query() {
  if (!docId.value?.trim()) {
    ElMessage.warning('请填写文档 ID')
    return
  }
  loading.value = true
  try {
    const { data } = await getDocument(docId.value.trim())
    result.value = data
    localStorage.setItem('lastDocId', docId.value.trim())
  } finally {
    loading.value = false
  }
}

function startPoll() {
  stopPoll()
  polling.value = true
  query()
  timer = setInterval(query, 3000)
}

function stopPoll() {
  polling.value = false
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onUnmounted(stopPoll)
</script>

<style scoped>
.meta-desc {
  margin-top: 20px;
  border-radius: 10px;
  overflow: hidden;
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
.muted {
  color: #94a3b8;
  font-size: 14px;
}
</style>
