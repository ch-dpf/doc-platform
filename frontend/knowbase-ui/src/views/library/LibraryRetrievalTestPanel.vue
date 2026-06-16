<template>
  <div class="library-panel library-retrieval-panel">
    <p class="library-retrieval-panel__intro">
      在本知识库上测试召回检索效果，与智能问答使用相同的检索链路（含改写、混合检索与重排序）。
    </p>

    <el-form label-position="top" size="default" class="retrieval-test-form">
      <el-form-item label="测试问题" required>
        <el-input
          v-model="question"
          type="textarea"
          :rows="3"
          placeholder="输入要检索的问题或关键词"
          @keydown.ctrl.enter="runRetrieval"
        />
      </el-form-item>

      <div class="retrieval-test-form__row">
        <el-form-item label="租户 ID" required class="retrieval-test-form__tenant">
          <el-input v-model="tenantInput" clearable />
        </el-form-item>
        <el-form-item label="Top K">
          <el-slider v-model="topK" :min="1" :max="20" show-input />
        </el-form-item>
        <el-form-item label="最低相似度">
          <el-input-number
            v-model="minScore"
            :min="0"
            :max="1"
            :step="0.05"
            :precision="2"
            controls-position="right"
          />
        </el-form-item>
      </div>

      <el-form-item label="检索范围">
        <el-switch
          v-model="includeAllChunkProfiles"
          inline-prompt
          active-text="全部分块档"
          inactive-text="仅主档"
        />
        <p class="retrieval-hint">默认仅检索库主分块档</p>
      </el-form-item>

      <el-form-item label="限定文档（可选）">
        <el-input
          v-model="docIdsText"
          type="textarea"
          :rows="2"
          placeholder="每行一个 docId"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" round :loading="loading" :disabled="!question?.trim()" @click="runRetrieval">
          执行检索测试
        </el-button>
        <el-button round :disabled="!result" @click="clearResult">清空结果</el-button>
      </el-form-item>
    </el-form>

    <div v-if="result" class="retrieval-result">
      <div class="retrieval-result__head">
        <span>检索结果</span>
        <el-tag size="small" type="info">召回测试</el-tag>
        <el-tag v-if="result.rerankEnabled" size="small" type="warning">重排序已启用</el-tag>
        <el-tag v-else size="small">未重排序</el-tag>
      </div>
      <p class="retrieval-result__meta">
        原始/追问语句：{{ result.conversationQuery || result.searchQuery || '—' }}
        <br />
        改写后向量检索：{{ result.searchQuery || '—' }}
        <br />
        关键字检索：{{ result.keywordQuery || '—' }}
        <br />
        有效 Top-K：{{ result.effectiveTopK }}，最终命中 {{ result.hitCount }} 条
        <br />
        <template v-if="result.temporalScopeSummary">
          时间范围：{{ result.temporalScopeSummary }}
          <br />
        </template>
        <template v-if="result.parseConfidence">
          解析置信度：{{ result.parseConfidence }}
          <br />
        </template>
        重排模型：{{
          result.rerankEnabled ? (result.rerankModel || '库级 Embedding 模型') : '—'
        }}
      </p>
      <el-alert
        v-if="headerWarning"
        type="warning"
        :closable="false"
        show-icon
        :title="headerWarning"
        class="retrieval-result__warn"
      />
      <el-tabs v-model="previewTab" class="retrieval-result__tabs" size="small">
        <el-tab-pane label="重排后结果" name="after">
          <el-table :data="result.hits" size="small" stripe max-height="420">
            <el-table-column prop="rank" label="#" width="48" />
            <el-table-column prop="score" :label="result.finalScoreLabel || '分数'" width="96">
              <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件" min-width="140" show-overflow-tooltip />
            <el-table-column prop="chunkIndex" label="块" width="56" />
            <el-table-column label="表头" width="60">
              <template #default="{ row }">
                <el-tag v-if="row.headerOnlyChunk" size="small" type="warning">是</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="excerpt" label="摘录" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
        <el-tab-pane :label="`重排前候选 (${result.preRerankHitCount || 0})`" name="before">
          <el-table :data="result.preRerankHits || []" size="small" stripe max-height="420">
            <el-table-column prop="rank" label="#" width="48" />
            <el-table-column prop="score" :label="result.preRerankScoreLabel || '分数'" width="96">
              <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
            </el-table-column>
            <el-table-column prop="fileName" label="文件" min-width="140" show-overflow-tooltip />
            <el-table-column prop="chunkIndex" label="块" width="56" />
            <el-table-column label="表头" width="60">
              <template #default="{ row }">
                <el-tag v-if="row.headerOnlyChunk" size="small" type="warning">是</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="excerpt" label="摘录" min-width="200" show-overflow-tooltip />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getVectorLibrary } from '../../api/library'
import { ragRetrievalPreview, buildRetrievalPreviewPayload } from '../../api/search'
import { useLibraryContext } from '../../composables/useLibraryContext'
import { flattenLibraryConfig } from '../../utils/libraryConfigView'

const route = useRoute()
const { tenantId, persist } = useLibraryContext()

const libraryIdParam = computed(() => String(route.params.libraryId || ''))
const library = inject('libraryDetail', ref(null))

const question = ref('')
const tenantInput = ref(tenantId.value)
const topK = ref(12)
const minScore = ref(0)
const includeAllChunkProfiles = ref(false)
const docIdsText = ref('')
const loading = ref(false)
const result = ref(null)
const previewTab = ref('after')

const headerWarning = computed(() => {
  const note = result.value?.retrievalNote
  if (note) return note
  const hits = result.value?.hits
  if (!hits?.length) return ''
  const headerCount = hits.filter((h) => h.headerOnlyChunk).length
  if (headerCount === 0) return ''
  if (headerCount >= Math.ceil(hits.length / 2)) {
    return `命中 ${headerCount}/${hits.length} 条为表头块，建议在「设置」中调低相似度阈值并执行批量重索引。`
  }
  return ''
})

watch(
  () => tenantInput.value,
  (v) => {
    if (v?.trim()) {
      tenantId.value = v.trim()
      persist()
    }
  }
)

function parseDocIds(text) {
  return text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

async function syncDefaultsFromLibrary() {
  if (!libraryIdParam.value) return
  try {
    const { data: lib } = await getVectorLibrary(libraryIdParam.value)
    const flat = flattenLibraryConfig(lib)
    const defaultTopK = flat.retrieval?.defaultTopK
    if (defaultTopK > 0) topK.value = Math.min(defaultTopK, 20)
    const threshold = flat.retrieval?.similarityThreshold
    if (threshold > 0) minScore.value = threshold
  } catch {
    // ignore
  }
}

async function runRetrieval() {
  const q = question.value?.trim()
  if (!libraryIdParam.value || !tenantInput.value?.trim() || !q) {
    ElMessage.warning('请填写租户 ID 与测试问题')
    return
  }
  loading.value = true
  try {
    const { data } = await ragRetrievalPreview(
      buildRetrievalPreviewPayload({
        libraryId: libraryIdParam.value,
        tenantId: tenantInput.value,
        question: q,
        topK: topK.value,
        minScore: minScore.value,
        docIds: parseDocIds(docIdsText.value),
        messages: [],
        includeAllChunkProfiles: includeAllChunkProfiles.value
      })
    )
    result.value = data
    previewTab.value = 'after'
  } catch (e) {
    ElMessage.error(e?.message || '检索测试失败')
  } finally {
    loading.value = false
  }
}

function clearResult() {
  result.value = null
}

watch(libraryIdParam, () => {
  clearResult()
  syncDefaultsFromLibrary()
})

onMounted(() => {
  tenantInput.value = tenantId.value
  syncDefaultsFromLibrary()
})
</script>

<style scoped>
.library-retrieval-panel__intro {
  margin: 0 0 16px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}
.retrieval-test-form {
  max-width: 720px;
}
.retrieval-test-form__row {
  display: grid;
  grid-template-columns: 1fr 1fr 200px;
  gap: 16px;
}
@media (max-width: 900px) {
  .retrieval-test-form__row {
    grid-template-columns: 1fr;
  }
}
.retrieval-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}
.retrieval-result {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
}
.retrieval-result__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}
.retrieval-result__meta {
  margin: 0 0 12px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}
.retrieval-result__warn {
  margin-bottom: 12px;
}
.retrieval-result__tabs {
  margin-top: 4px;
}
</style>
