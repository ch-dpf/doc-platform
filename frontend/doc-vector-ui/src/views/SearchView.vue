<template>
  <el-row :gutter="20">
    <el-col :span="12">
      <el-card>
        <template #header>POST /api/v1/search</template>
        <el-form label-width="110px">
          <el-form-item label="租户 ID" required>
            <el-input v-model="form.tenantId" />
          </el-form-item>
          <el-form-item label="检索问题" required>
            <el-input v-model="form.query" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="Top K">
            <el-slider v-model="form.topK" :min="1" :max="50" show-input />
          </el-form-item>
          <el-form-item label="限定 docId">
            <el-input
              v-model="docIdsText"
              type="textarea"
              :rows="2"
              placeholder="可选，每行一个 UUID，留空表示全库检索"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="submit">检索</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :span="12">
      <el-card v-if="hits.length" shadow="never">
        <template #header>命中 {{ hits.length }} 条</template>
        <el-table :data="hits" stripe max-height="480">
          <el-table-column prop="score" label="分数" width="90">
            <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
          </el-table-column>
          <el-table-column prop="docId" label="docId" width="120" show-overflow-tooltip />
          <el-table-column prop="chunkIndex" label="块" width="60" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
        </el-table>
      </el-card>
      <JsonPanel v-else title="SearchResponse" :data="raw" />
    </el-col>
  </el-row>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { search } from '../api/vector'
import JsonPanel from '../components/JsonPanel.vue'

const form = reactive({
  tenantId: localStorage.getItem('tenantId') || 'demo',
  query: 'pgvector 语义检索',
  topK: 5
})
const docIdsText = ref('')
const loading = ref(false)
const raw = ref(null)

const hits = computed(() => raw.value?.hits || [])

async function submit() {
  if (!form.tenantId?.trim() || !form.query?.trim()) {
    ElMessage.warning('请填写租户与检索问题')
    return
  }
  localStorage.setItem('tenantId', form.tenantId.trim())
  const docIds = docIdsText.value
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
  const body = {
    tenantId: form.tenantId.trim(),
    query: form.query.trim(),
    topK: form.topK,
    filter: docIds.length ? { docIds } : null
  }
  loading.value = true
  try {
    const { data } = await search(body)
    raw.value = data
    ElMessage.success(`返回 ${data.hits?.length || 0} 条结果`)
  } finally {
    loading.value = false
  }
}
</script>
