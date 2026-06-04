<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-card>
        <template #header>POST /api/v1/rag/chat</template>
        <el-form label-width="110px">
          <el-form-item label="租户 ID" required>
            <el-input v-model="form.tenantId" />
          </el-form-item>
          <el-form-item label="用户问题" required>
            <el-input v-model="form.question" type="textarea" :rows="4" placeholder="基于知识库生成回答" />
          </el-form-item>
          <el-form-item label="检索 Top K">
            <el-slider v-model="form.topK" :min="1" :max="20" show-input />
          </el-form-item>
          <el-form-item label="最低分数">
            <el-input-number v-model="form.minScore" :min="0" :max="1" :step="0.05" :precision="2" />
            <span class="hint-inline">0 表示不过滤</span>
          </el-form-item>
          <el-form-item label="限定 docId">
            <el-input
              v-model="docIdsText"
              type="textarea"
              :rows="2"
              placeholder="可选，每行一个 UUID"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="submit">生成回答</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card v-if="result" shadow="never">
        <template #header>
          RAG 回答
          <el-tag v-if="result.found === false" type="warning" size="small" style="margin-left: 8px">未找到</el-tag>
          <el-tag v-else-if="result.usedLlm" type="success" size="small" style="margin-left: 8px">有据作答</el-tag>
          <el-tag v-else type="info" size="small" style="margin-left: 8px">未调用 LLM</el-tag>
        </template>
        <div class="answer">{{ result.answer }}</div>
        <el-divider v-if="result.citations?.length" content-position="left">
          引用片段（{{ result.retrievedCount }}）
        </el-divider>
        <el-table v-if="result.citations?.length" :data="result.citations" stripe max-height="320">
          <el-table-column type="index" label="#" width="48" />
          <el-table-column prop="score" label="分数" width="80">
            <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
          </el-table-column>
          <el-table-column prop="docId" label="docId" width="120" show-overflow-tooltip />
          <el-table-column prop="excerpt" label="摘录" show-overflow-tooltip />
        </el-table>
      </el-card>
      <JsonPanel v-else title="RagChatResponse" :data="raw" />
    </el-col>
  </el-row>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ragChat } from '../api/vector'
import JsonPanel from '../components/JsonPanel.vue'

const form = reactive({
  tenantId: localStorage.getItem('tenantId') || 'demo',
  question: '请简要说明本项目中 pgvector 的用途',
  topK: 5,
  minScore: 0
})
const docIdsText = ref('')
const loading = ref(false)
const raw = ref(null)
const result = ref(null)

async function submit() {
  if (!form.tenantId?.trim() || !form.question?.trim()) {
    ElMessage.warning('请填写租户与用户问题')
    return
  }
  localStorage.setItem('tenantId', form.tenantId.trim())
  const docIds = docIdsText.value
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
  const body = {
    tenantId: form.tenantId.trim(),
    question: form.question.trim(),
    topK: form.topK,
    minScore: form.minScore > 0 ? form.minScore : null,
    filter: docIds.length ? { docIds } : null
  }
  loading.value = true
  result.value = null
  try {
    const { data } = await ragChat(body)
    raw.value = data
    result.value = data
    ElMessage.success(data.found === false ? '未找到相关资料' : '已生成 RAG 回答')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.answer {
  white-space: pre-wrap;
  line-height: 1.6;
  color: #1e293b;
}
.hint-inline {
  margin-left: 8px;
  font-size: 12px;
  color: #64748b;
}
</style>
