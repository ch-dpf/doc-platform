<template>
  <div class="page-wrap">
    <PageCard title="智能问答">
      <el-tabs v-model="activeTab" class="qa-tabs">
        <el-tab-pane label="问答" name="chat">
          <el-row :gutter="24">
            <el-col :xs="24" :lg="11">
              <el-form label-width="96px" @submit.prevent="submitChat">
                <el-form-item label="知识库" required>
                  <el-select v-model="libraryId" filterable style="width: 100%">
                    <el-option
                      v-for="lib in libraries"
                      :key="lib.libraryId"
                      :label="lib.name"
                      :value="lib.libraryId"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="租户 ID" required>
                  <el-input v-model="chatForm.tenantId" clearable />
                </el-form-item>
                <el-form-item label="问题" required>
                  <el-input
                    v-model="chatForm.question"
                    type="textarea"
                    :rows="4"
                    placeholder="基于知识库生成回答"
                  />
                </el-form-item>
                <el-form-item label="对话模型">
                  <el-input
                    v-model="chatForm.chatModel"
                    clearable
                    placeholder="留空使用服务端默认模型"
                  />
                </el-form-item>
                <el-form-item label="Top K">
                  <el-slider v-model="chatForm.topK" :min="1" :max="20" show-input />
                </el-form-item>
                <el-form-item label="最低分数">
                  <el-input-number
                    v-model="chatForm.minScore"
                    :min="0"
                    :max="1"
                    :step="0.05"
                    :precision="2"
                  />
                  <span class="hint-inline">0 表示不过滤</span>
                </el-form-item>
                <el-form-item label="限定文档">
                  <el-input
                    v-model="docIdsText"
                    type="textarea"
                    :rows="2"
                    placeholder="可选，每行一个 docId"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="chatLoading" round @click="submitChat">
                    提问
                  </el-button>
                </el-form-item>
              </el-form>
            </el-col>
            <el-col :xs="24" :lg="13">
              <div v-if="chatResult" class="answer-panel">
                <div class="answer-header">
                  <span class="answer-title">回答</span>
                  <el-tag v-if="chatResult.found === false" type="warning" size="small">未找到</el-tag>
                  <el-tag v-else-if="chatResult.usedLlm" type="success" size="small">有据作答</el-tag>
                  <el-tag v-else type="info" size="small">未调用 LLM</el-tag>
                </div>
                <div class="answer-body">{{ chatResult.answer }}</div>
                <el-divider v-if="chatResult.citations?.length" content-position="left">
                  引用（{{ chatResult.retrievedCount }}）
                </el-divider>
                <el-table
                  v-if="chatResult.citations?.length"
                  :data="chatResult.citations"
                  stripe
                  max-height="280"
                  size="small"
                >
                  <el-table-column type="index" label="#" width="44" />
                  <el-table-column prop="score" label="分数" width="72">
                    <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
                  </el-table-column>
                  <el-table-column prop="docId" label="docId" width="110" show-overflow-tooltip />
                  <el-table-column prop="excerpt" label="摘录" show-overflow-tooltip />
                </el-table>
              </div>
              <el-empty v-else description="输入问题后点击「提问」" :image-size="80" />
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="检索片段" name="search">
          <el-row :gutter="24">
            <el-col :xs="24" :lg="11">
              <el-form label-width="96px" @submit.prevent="submitSearch">
                <el-form-item label="知识库" required>
                  <el-select v-model="libraryId" filterable style="width: 100%">
                    <el-option
                      v-for="lib in libraries"
                      :key="lib.libraryId"
                      :label="lib.name"
                      :value="lib.libraryId"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="租户 ID" required>
                  <el-input v-model="searchForm.tenantId" clearable />
                </el-form-item>
                <el-form-item label="检索语句" required>
                  <el-input v-model="searchForm.query" type="textarea" :rows="3" />
                </el-form-item>
                <el-form-item label="Top K">
                  <el-slider v-model="searchForm.topK" :min="1" :max="50" show-input />
                </el-form-item>
                <el-form-item label="限定文档">
                  <el-input
                    v-model="searchDocIdsText"
                    type="textarea"
                    :rows="2"
                    placeholder="可选，每行一个 docId"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="searchLoading" round @click="submitSearch">
                    检索
                  </el-button>
                </el-form-item>
              </el-form>
            </el-col>
            <el-col :xs="24" :lg="13">
              <el-table
                v-if="searchHits.length"
                :data="searchHits"
                stripe
                class="data-table"
                max-height="420"
                size="small"
              >
                <el-table-column prop="score" label="分数" width="80">
                  <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
                </el-table-column>
                <el-table-column prop="docId" label="docId" width="110" show-overflow-tooltip />
                <el-table-column prop="chunkIndex" label="块" width="52" />
                <el-table-column prop="content" label="内容" show-overflow-tooltip />
              </el-table>
              <el-empty v-else description="检索后将显示 TopK 片段" :image-size="80" />
            </el-col>
          </el-row>
        </el-tab-pane>
      </el-tabs>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ragChat, search } from '../api/vector'
import { listVectorLibraries } from '../api/library'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'

const route = useRoute()
const { libraryId, tenantId, persist } = useLibraryContext()
const libraries = ref([])
const activeTab = ref(route.query.tab === 'search' ? 'search' : 'chat')

const chatForm = reactive({
  tenantId: localStorage.getItem('tenantId') || 'demo',
  question: '',
  topK: 5,
  minScore: 0,
  chatModel: ''
})
const searchForm = reactive({
  tenantId: localStorage.getItem('tenantId') || 'demo',
  query: '',
  topK: 5
})
const docIdsText = ref('')
const searchDocIdsText = ref('')
const chatLoading = ref(false)
const searchLoading = ref(false)
const chatResult = ref(null)
const searchRaw = ref(null)

const searchHits = computed(() => searchRaw.value?.hits || [])

watch(
  () => route.query.tab,
  (tab) => {
    if (tab === 'search') activeTab.value = 'search'
    else if (tab === 'chat') activeTab.value = 'chat'
  }
)

function parseDocIds(text) {
  return text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

async function submitChat() {
  if (!libraryId.value || !chatForm.tenantId?.trim() || !chatForm.question?.trim()) {
    ElMessage.warning('请选择知识库并填写租户与问题')
    return
  }
  persist()
  localStorage.setItem('tenantId', chatForm.tenantId.trim())
  const docIds = parseDocIds(docIdsText.value)
  chatLoading.value = true
  chatResult.value = null
  try {
    const { data } = await ragChat({
      libraryId: libraryId.value,
      tenantId: chatForm.tenantId.trim(),
      question: chatForm.question.trim(),
      topK: chatForm.topK,
      minScore: chatForm.minScore > 0 ? chatForm.minScore : null,
      filter: docIds.length ? { docIds } : null,
      chatModel: chatForm.chatModel?.trim() || null
    })
    chatResult.value = data
    ElMessage.success(data.found === false ? '未找到相关资料' : '已生成回答')
  } finally {
    chatLoading.value = false
  }
}

async function submitSearch() {
  if (!libraryId.value || !searchForm.tenantId?.trim() || !searchForm.query?.trim()) {
    ElMessage.warning('请选择知识库并填写租户与检索语句')
    return
  }
  persist()
  localStorage.setItem('tenantId', searchForm.tenantId.trim())
  const docIds = parseDocIds(searchDocIdsText.value)
  searchLoading.value = true
  try {
    const { data } = await search({
      libraryId: libraryId.value,
      tenantId: searchForm.tenantId.trim(),
      query: searchForm.query.trim(),
      topK: searchForm.topK,
      filter: docIds.length ? { docIds } : null
    })
    searchRaw.value = data
    ElMessage.success(`返回 ${data.hits?.length || 0} 条结果`)
  } finally {
    searchLoading.value = false
  }
}

onMounted(async () => {
  persist()
  const { data } = await listVectorLibraries(tenantId.value)
  libraries.value = data
  if (route.query.tab === 'search' && !searchForm.query) {
    searchForm.query = chatForm.question || '知识库检索'
  }
})
</script>

