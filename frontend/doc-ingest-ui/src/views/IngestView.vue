<template>
  <div class="page-wrap">
    <el-row :gutter="24">
      <el-col :xs="24" :lg="13">
        <PageCard
          title="文档收集上传"
          subtitle="支持本地文件上传与 URL 远程采集，共用租户与索引配置"
        >
          <div class="common-fields">
            <el-form label-width="88px" label-position="left">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="租户 ID" required>
                    <el-input v-model="tenantId" placeholder="demo" />
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

          <el-tabs v-model="activeTab" class="ingest-tabs">
            <el-tab-pane name="upload">
              <template #label>
                <span class="tab-label"><el-icon><UploadFilled /></el-icon> 文件上传</span>
              </template>
              <el-form label-width="88px" @submit.prevent="submitUpload">
                <el-form-item label="选择文件" required>
                  <el-upload
                    class="upload-zone"
                    drag
                    :auto-upload="false"
                    :limit="1"
                    :on-change="onFileChange"
                    :on-remove="() => (file = null)"
                  >
                    <el-icon class="upload-icon"><UploadFilled /></el-icon>
                    <div class="upload-title">拖拽文件到此处，或点击选择</div>
                    <div class="upload-desc">PDF · Word · Excel · TXT · Markdown · 图片（PNG/JPG/GIF/WebP 等）</div>
                  </el-upload>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" size="large" :loading="loading" @click="submitUpload">
                    开始上传
                  </el-button>
                </el-form-item>
              </el-form>
              <span class="api-badge">POST /api/v1/documents/upload</span>
            </el-tab-pane>

            <el-tab-pane name="collect">
              <template #label>
                <span class="tab-label"><el-icon><Link /></el-icon> URL 采集</span>
              </template>
              <el-form label-width="88px" @submit.prevent="submitCollect">
                <el-form-item label="页面 URL" required>
                  <el-input
                    v-model="collectUrl"
                    placeholder="http://localhost:8081/doc.html"
                    clearable
                  />
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
        <PageCard title="接入结果" subtitle="提交成功后返回 DocumentResponse">
          <JsonPanel title="响应 JSON" :data="result" empty-text="上传或采集成功后，结果将显示在这里" />
          <el-alert
            v-if="result?.docId"
            class="result-alert"
            type="success"
            :closable="false"
            show-icon
          >
            <template #title>接入成功</template>
            文档 ID：<code class="id-code">{{ result.docId }}</code><br />
            可在「文档管理」查看列表，或在「查询状态」轮询解析 / 索引进度。
          </el-alert>
          <el-empty v-else class="empty-block" description="等待提交" :image-size="80" />
        </PageCard>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { uploadDocument, collectDocument } from '../api/ingest'
import PageCard from '../components/PageCard.vue'
import JsonPanel from '../components/JsonPanel.vue'

const route = useRoute()

const activeTab = ref(route.query.tab === 'collect' ? 'collect' : 'upload')
const tenantId = ref(localStorage.getItem('tenantId') || 'demo')
const autoIndex = ref(true)
const file = ref(null)
const collectUrl = ref('')
const loading = ref(false)
const result = ref(null)

function onFileChange(uploadFile) {
  file.value = uploadFile.raw
}

function persistTenant() {
  localStorage.setItem('tenantId', tenantId.value.trim())
}

async function submitUpload() {
  if (!tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID')
    return
  }
  if (!file.value) {
    ElMessage.warning('请选择文件')
    return
  }
  persistTenant()
  loading.value = true
  try {
    const { data } = await uploadDocument(tenantId.value.trim(), file.value, autoIndex.value)
    result.value = data
    if (data?.docId) localStorage.setItem('lastDocId', data.docId)
    ElMessage.success('上传成功')
  } finally {
    loading.value = false
  }
}

async function submitCollect() {
  if (!tenantId.value?.trim() || !collectUrl.value?.trim()) {
    ElMessage.warning('请填写租户 ID 与 URL')
    return
  }
  persistTenant()
  loading.value = true
  try {
    const { data } = await collectDocument({
      tenantId: tenantId.value.trim(),
      url: collectUrl.value.trim(),
      autoIndex: autoIndex.value
    })
    result.value = data
    if (data?.docId) localStorage.setItem('lastDocId', data.docId)
    ElMessage.success('采集任务已提交')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.upload-icon {
  font-size: 48px;
  color: #38bdf8;
  margin-bottom: 8px;
}
.upload-title {
  font-size: 15px;
  color: #334155;
  font-weight: 500;
}
.upload-desc {
  margin-top: 6px;
  font-size: 12px;
  color: #94a3b8;
}
.id-code {
  font-size: 12px;
  background: rgba(255, 255, 255, 0.6);
  padding: 2px 6px;
  border-radius: 4px;
}
</style>
