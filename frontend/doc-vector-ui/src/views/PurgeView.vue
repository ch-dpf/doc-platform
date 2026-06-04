<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-card>
        <template #header>DELETE /api/v1/index/{docId}</template>
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="清理该文档在 vector_idx 中的全部分块与索引任务记录"
          style="margin-bottom: 16px"
        />
        <el-form label-width="100px" @submit.prevent="submit">
          <el-form-item label="文档 ID" required>
            <el-input v-model="docId" placeholder="UUID" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="danger" :loading="loading" @click="submit">清理向量</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card v-if="done">
        <el-result icon="success" title="清理完成" sub-title="HTTP 204 No Content" />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purgeIndex } from '../api/vector'

const docId = ref(localStorage.getItem('lastDocId') || '')
const loading = ref(false)
const done = ref(false)

async function submit() {
  if (!docId.value?.trim()) {
    ElMessage.warning('请填写文档 ID')
    return
  }
  await ElMessageBox.confirm('确定清理该文档的向量数据？', '确认', { type: 'warning' })
  loading.value = true
  done.value = false
  try {
    await purgeIndex(docId.value.trim())
    done.value = true
    ElMessage.success('已清理')
  } finally {
    loading.value = false
  }
}
</script>
