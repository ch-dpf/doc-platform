<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-card>
        <template #header>POST /api/v1/index/rebuild</template>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="补偿重索引：根据已解析文本 URL 重新分块并向量化（HTTP 202）"
          style="margin-bottom: 16px"
        />
        <el-form label-width="120px">
          <el-form-item label="文档 ID" required>
            <el-input v-model="form.docId" />
          </el-form-item>
          <el-form-item label="租户 ID" required>
            <el-input v-model="form.tenantId" />
          </el-form-item>
          <el-form-item label="版本号">
            <el-input-number v-model="form.version" :min="1" />
          </el-form-item>
          <el-form-item label="解析文本 URL" required>
            <el-input v-model="form.parsedTextUrl" placeholder="MinIO 预签名或内部 URL" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="submit">提交重索引</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card v-if="accepted">
        <el-result icon="success" title="已接受" sub-title="HTTP 202 Accepted，后台异步执行" />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { rebuildIndex } from '../api/vector'

const form = reactive({
  docId: localStorage.getItem('lastDocId') || '',
  tenantId: localStorage.getItem('tenantId') || 'demo',
  version: 1,
  parsedTextUrl: ''
})
const loading = ref(false)
const accepted = ref(false)

async function submit() {
  if (!form.docId?.trim() || !form.tenantId?.trim() || !form.parsedTextUrl?.trim()) {
    ElMessage.warning('请填写必填项')
    return
  }
  localStorage.setItem('tenantId', form.tenantId.trim())
  localStorage.setItem('lastDocId', form.docId.trim())
  loading.value = true
  accepted.value = false
  try {
    await rebuildIndex({
      docId: form.docId.trim(),
      tenantId: form.tenantId.trim(),
      version: form.version,
      parsedTextUrl: form.parsedTextUrl.trim()
    })
    accepted.value = true
    ElMessage.success('重索引任务已提交')
  } finally {
    loading.value = false
  }
}
</script>
