<template>
  <div class="page-wrap page-wrap--fluid qa-page">
    <PageCard title="智能问答">
      <template #actions>
        <template v-if="orchestrationLibraryIds.length">
          <el-tag
            v-for="(id, idx) in orchestrationLibraryIds"
            :key="id"
            size="small"
            :type="idx === 0 ? 'primary' : 'info'"
            effect="plain"
            class="qa-orchestration-tag"
          >
            {{ libraryName(id) }}{{ idx === 0 ? '（主）' : '' }}
          </el-tag>
        </template>
        <el-button round size="small" :loading="creatingConversation" @click="startNewChat">
          新对话
        </el-button>
      </template>

      <div class="qa-layout">
            <aside class="qa-sidebar">
              <el-form label-position="top" size="small" class="qa-sidebar-form">
                <el-form-item label="知识库编排" required>
                  <el-select
                    v-model="orchestrationLibraryIds"
                    multiple
                    filterable
                    collapse-tags
                    collapse-tags-tooltip
                    placeholder="选择一个或多个知识库"
                    class="full-width"
                  >
                    <el-option
                      v-for="lib in libraries"
                      :key="lib.libraryId"
                      :label="lib.name"
                      :value="lib.libraryId"
                    />
                  </el-select>
                  <p class="qa-field-hint">首个知识库为主检索源，会话绑定主库；检索测试请前往各知识库详情</p>
                </el-form-item>
                <el-form-item label="租户 ID" required>
                  <el-input v-model="chatSettings.tenantId" clearable />
                </el-form-item>
              </el-form>

              <el-collapse v-model="settingsOpen" class="qa-settings-collapse">
                <el-collapse-item title="检索与模型" name="advanced">
                  <el-form label-position="top" size="small">
                    <el-form-item label="对话模型">
                      <el-input
                        v-model="chatSettings.chatModel"
                        clearable
                        placeholder="留空使用服务端默认"
                      />
                    </el-form-item>
                    <el-form-item label="Top K">
                      <el-slider v-model="chatSettings.topK" :min="1" :max="20" show-input />
                      <p v-if="libraryDefaultTopK" class="qa-field-hint">
                        库默认 {{ libraryDefaultTopK }}，本会话可覆盖
                      </p>
                    </el-form-item>
                    <el-form-item label="最低相似度">
                      <el-input-number
                        v-model="chatSettings.minScore"
                        :min="0"
                        :max="1"
                        :step="0.05"
                        :precision="2"
                        controls-position="right"
                        class="full-width"
                      />
                    </el-form-item>
                    <el-form-item label="检索范围">
                      <el-switch
                        v-model="chatSettings.includeAllChunkProfiles"
                        inline-prompt
                        active-text="全部分块档"
                        inactive-text="仅主档"
                      />
                      <p class="qa-field-hint">默认仅检索库主分块档；开启后可跨历史分块档检索</p>
                    </el-form-item>
                    <el-form-item label="限定文档">
                      <el-input
                        v-model="docIdsText"
                        type="textarea"
                        :rows="2"
                        placeholder="可选，每行一个 docId"
                      />
                    </el-form-item>
                  </el-form>
                </el-collapse-item>
              </el-collapse>

              <div class="qa-conv-list">
                <div class="qa-conv-list__head">
                  <span>会话历史</span>
                  <el-button link type="primary" size="small" :disabled="!primaryLibraryId" @click="loadConversations">
                    刷新
                  </el-button>
                </div>
                <div v-if="!conversations.length" class="qa-conv-list__empty">暂无会话，发送消息将自动创建</div>
                <button
                  v-for="conv in conversations"
                  :key="conv.conversationId"
                  type="button"
                  class="qa-conv-item"
                  :class="{ 'is-active': currentConversationId === conv.conversationId }"
                  @click="selectConversation(conv.conversationId)"
                >
                  <span class="qa-conv-item__title">{{ conv.title || '新对话' }}</span>
                  <span class="qa-conv-item__meta">{{ conv.messageCount }} 条</span>
                </button>
              </div>

            </aside>

            <main class="qa-chat">
              <div ref="chatScrollRef" class="qa-chat__messages">
                <div v-if="!messages.length" class="qa-chat__empty">
                  <el-empty description="选择知识库编排后开始提问" :image-size="96" />
                </div>

                <div
                  v-for="msg in messages"
                  :key="msg.id"
                  class="qa-msg"
                  :class="`qa-msg--${msg.role}`"
                >
                  <div class="qa-msg__avatar">
                    <el-icon v-if="msg.role === 'user'"><User /></el-icon>
                    <el-icon v-else><ChatDotRound /></el-icon>
                  </div>
                  <div class="qa-msg__body">
                    <header class="qa-msg__head">
                      <span class="qa-msg__role">{{ msg.role === 'user' ? '我' : '助手' }}</span>
                      <el-tag v-if="msg.loading" size="small" type="info" effect="plain">生成中…</el-tag>
                      <el-tag v-else-if="msg.role === 'assistant' && msg.conversational" size="small" type="info">
                        对话
                      </el-tag>
                      <el-tag
                        v-else-if="msg.role === 'assistant' && (msg.found === false || isNotFoundAnswer(msg))"
                        size="small"
                        type="warning"
                      >
                        未找到
                      </el-tag>
                      <el-tag
                        v-else-if="msg.role === 'assistant' && msg.usedLlm && msg.found !== false"
                        size="small"
                        type="success"
                      >
                        有据作答
                      </el-tag>
                      <el-tag
                        v-if="msg.role === 'assistant' && msg.searchQuery && msg.searchQuery !== msg.userQuestion && !msg.loading"
                        size="small"
                        type="info"
                        effect="plain"
                        :title="`检索语句：${msg.searchQuery}`"
                      >
                        追问检索
                      </el-tag>
                    </header>
                    <div v-if="msg.loading && !msg.content" class="qa-msg__loading">
                      <el-skeleton :rows="3" animated />
                    </div>
                    <div v-else class="qa-msg__content">{{ msg.content }}<span v-if="msg.streaming" class="qa-cursor">▍</span></div>

                    <div
                      v-if="msg.role === 'assistant' && msg.citations?.length && !msg.loading"
                      class="qa-citations"
                    >
                      <button type="button" class="qa-citations__toggle" @click="toggleCitations(msg.id)">
                        <el-icon><Document /></el-icon>
                        引用 {{ msg.citations.length }} 条
                        <el-icon class="qa-citations__chevron" :class="{ 'is-open': expandedCitations.has(msg.id) }">
                          <ArrowDown />
                        </el-icon>
                      </button>
                      <el-table
                        v-show="expandedCitations.has(msg.id)"
                        :data="msg.citations"
                        stripe
                        size="small"
                        max-height="220"
                        class="qa-citations__table"
                      >
                        <el-table-column type="index" label="#" width="40" />
                        <el-table-column prop="score" label="分数" width="72">
                          <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
                        </el-table-column>
                        <el-table-column label="文档" min-width="120" show-overflow-tooltip>
                          <template #default="{ row }">
                            <button
                              v-if="row.docId"
                              type="button"
                              class="qa-link-btn"
                              @click="openCitation(row)"
                            >
                              {{ row.fileName || shortId(row.docId) }}
                            </button>
                            <span v-else>—</span>
                          </template>
                        </el-table-column>
                        <el-table-column label="块" width="56">
                          <template #default="{ row }">
                            <button
                              v-if="row.docId"
                              type="button"
                              class="qa-link-btn"
                              @click="openCitation(row)"
                            >
                              #{{ row.chunkIndex }}
                            </button>
                            <span v-else>—</span>
                          </template>
                        </el-table-column>
                        <el-table-column label="分块档" width="88" show-overflow-tooltip>
                          <template #default="{ row }">
                            <el-tag
                              v-if="row.chunkProfileId"
                              size="small"
                              :type="row.primaryProfile ? 'success' : 'warning'"
                              effect="plain"
                              :title="row.chunkProfileId"
                            >
                              {{ row.primaryProfile ? '主档' : '非主' }}
                            </el-tag>
                            <span v-else>—</span>
                          </template>
                        </el-table-column>
                        <el-table-column prop="excerpt" label="摘录" show-overflow-tooltip />
                      </el-table>
                    </div>

                    <div
                      v-if="msg.role === 'assistant' && msg.retrievalTrace && !msg.loading"
                      class="qa-trace"
                    >
                      <button type="button" class="qa-trace__toggle" @click="toggleTrace(msg.id)">
                        <el-icon><DataAnalysis /></el-icon>
                        检索 trace
                        <el-tag v-if="msg.retrievalTrace.cacheHit" size="small" type="info">缓存</el-tag>
                        <el-icon class="qa-citations__chevron" :class="{ 'is-open': expandedTrace.has(msg.id) }">
                          <ArrowDown />
                        </el-icon>
                      </button>
                      <div v-show="expandedTrace.has(msg.id)" class="qa-trace__panel">
                        <p class="qa-trace__meta">
                          追问语句：{{ msg.retrievalTrace.conversationQuery || '—' }}
                          <br />
                          向量检索：{{ msg.retrievalTrace.searchQuery || '—' }}
                          <br />
                          关键字：{{ msg.retrievalTrace.keywordQuery || '—' }}
                          <br />
                          Top-K {{ msg.retrievalTrace.effectiveTopK }}，命中 {{ msg.retrievalTrace.hitCount }} 条
                          <span v-if="msg.retrievalTrace.rerankEnabled">
                            · 重排 {{ msg.retrievalTrace.rerankModel || '库级模型' }}
                          </span>
                        </p>
                        <el-alert
                          v-if="traceHeaderWarning(msg.retrievalTrace)"
                          type="warning"
                          :closable="false"
                          show-icon
                          :title="traceHeaderWarning(msg.retrievalTrace)"
                          class="qa-trace__warn"
                        />
                        <el-table
                          :data="msg.retrievalTrace.hits || []"
                          size="small"
                          stripe
                          max-height="180"
                        >
                          <el-table-column prop="rank" label="#" width="36" />
                          <el-table-column
                            prop="score"
                            :label="msg.retrievalTrace.finalScoreLabel || '分数'"
                            width="88"
                          >
                            <template #default="{ row }">{{ row.score?.toFixed(4) }}</template>
                          </el-table-column>
                          <el-table-column label="文档" min-width="100" show-overflow-tooltip>
                            <template #default="{ row }">
                              <button
                                v-if="row.docId"
                                type="button"
                                class="qa-link-btn"
                                @click="openTraceHit(row)"
                              >
                                {{ row.fileName || shortId(row.docId) }}
                              </button>
                            </template>
                          </el-table-column>
                          <el-table-column label="块" width="52">
                            <template #default="{ row }">
                              <button
                                v-if="row.docId"
                                type="button"
                                class="qa-link-btn"
                                @click="openTraceHit(row)"
                              >
                                #{{ row.chunkIndex }}
                              </button>
                            </template>
                          </el-table-column>
                          <el-table-column label="档" width="72" show-overflow-tooltip>
                            <template #default="{ row }">
                              <el-tag
                                v-if="row.chunkProfileId"
                                size="small"
                                :type="row.primaryProfile ? 'success' : 'warning'"
                                effect="plain"
                                :title="row.chunkProfileId"
                              >
                                {{ row.primaryProfile ? '主' : '非主' }}
                              </el-tag>
                              <span v-else>—</span>
                            </template>
                          </el-table-column>
                          <el-table-column label="表头" width="52">
                            <template #default="{ row }">
                              <el-tag v-if="row.headerOnlyChunk" size="small" type="warning">是</el-tag>
                            </template>
                          </el-table-column>
                          <el-table-column prop="excerpt" label="摘录" min-width="100" show-overflow-tooltip />
                        </el-table>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <footer class="qa-composer">
                <el-input
                  v-model="inputText"
                  type="textarea"
                  :rows="2"
                  :autosize="{ minRows: 2, maxRows: 6 }"
                  placeholder="输入问题，Enter 发送，Shift+Enter 换行"
                  :disabled="chatLoading"
                  @keydown.enter="onEnterKey"
                />
                <div class="qa-composer__actions">
                  <el-button
                    type="primary"
                    round
                    :loading="chatLoading"
                    :disabled="!canSend"
                    @click="sendMessage"
                  >
                    发送
                  </el-button>
                </div>
              </footer>
            </main>
          </div>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ChatDotRound, DataAnalysis, Document, User } from '@element-plus/icons-vue'
import {
  createConversation,
  conversationChatStream,
  listConversationMessages,
  listConversations
} from '../api/conversation'
import { getVectorLibrary, listVectorLibraries } from '../api/library'
import { flattenLibraryConfig } from '../utils/libraryConfigView'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'

const QA_ORCHESTRATION_KEY = 'qaOrchestrationLibraryIds'

const route = useRoute()
const router = useRouter()
const { libraryId, tenantId, persist } = useLibraryContext()
const libraries = ref([])
const orchestrationLibraryIds = ref([])
const settingsOpen = ref(['advanced'])
const libraryDefaultTopK = ref(null)

const chatSettings = reactive({
  tenantId: tenantId.value,
  topK: 12,
  minScore: 0,
  chatModel: '',
  includeAllChunkProfiles: false
})
const docIdsText = ref('')
const chatLoading = ref(false)
const creatingConversation = ref(false)
const messages = ref([])
const conversations = ref([])
const currentConversationId = ref(null)
const inputText = ref('')
const chatScrollRef = ref(null)
const expandedCitations = ref(new Set())
const expandedTrace = ref(new Set())
const skipOrchestrationWatch = ref(false)

const primaryLibraryId = computed(() => orchestrationLibraryIds.value[0] || '')

function libraryName(id) {
  return libraries.value.find((l) => l.libraryId === id)?.name || id
}

function persistOrchestration() {
  localStorage.setItem(QA_ORCHESTRATION_KEY, JSON.stringify(orchestrationLibraryIds.value))
  if (primaryLibraryId.value) {
    libraryId.value = primaryLibraryId.value
    persist()
  }
}

const canSend = computed(
  () =>
    !!primaryLibraryId.value
    && !!chatSettings.tenantId?.trim()
    && !!inputText.value?.trim()
    && !chatLoading.value
)

function parseDocIds(text) {
  return text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function newMessageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function isNotFoundAnswer(msg) {
  return !!msg.content?.trim().startsWith('未找到')
}

watch(
  () => chatSettings.tenantId,
  (v) => {
    if (v?.trim()) tenantId.value = v.trim()
  }
)

async function syncRetrievalDefaultsFromLibrary(id) {
  if (!id) {
    libraryDefaultTopK.value = null
    return
  }
  try {
    const { data: lib } = await getVectorLibrary(id)
    const flat = flattenLibraryConfig(lib)
    const topK = flat.retrieval?.defaultTopK
    if (topK > 0) {
      libraryDefaultTopK.value = topK
      chatSettings.topK = Math.min(topK, 20)
    } else {
      libraryDefaultTopK.value = null
    }
    const threshold = flat.retrieval?.similarityThreshold
    if (threshold > 0) {
      chatSettings.minScore = threshold
    }
  } catch {
    libraryDefaultTopK.value = null
  }
}

watch(orchestrationLibraryIds, async (next, prev) => {
  persistOrchestration()
  await syncRetrievalDefaultsFromLibrary(primaryLibraryId.value)
  if (skipOrchestrationWatch.value || !prev?.length || JSON.stringify(next) === JSON.stringify(prev) || !messages.value.length) {
    return
  }
  try {
    await ElMessageBox.confirm('变更知识库编排将清空当前对话，是否继续？', '变更编排', {
      type: 'warning',
      confirmButtonText: '继续',
      cancelButtonText: '取消'
    })
    await startNewChat()
  } catch {
    skipOrchestrationWatch.value = true
    orchestrationLibraryIds.value = [...prev]
    skipOrchestrationWatch.value = false
  }
}, { deep: true })

async function scrollToBottom() {
  await nextTick()
  const el = chatScrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

function toggleCitations(msgId) {
  const next = new Set(expandedCitations.value)
  if (next.has(msgId)) next.delete(msgId)
  else next.add(msgId)
  expandedCitations.value = next
}

function toggleTrace(msgId) {
  const next = new Set(expandedTrace.value)
  if (next.has(msgId)) next.delete(msgId)
  else next.add(msgId)
  expandedTrace.value = next
}

function shortId(id) {
  if (!id) return '—'
  const s = String(id)
  return s.length > 10 ? `${s.slice(0, 8)}…` : s
}

function shortChunkProfile(id) {
  if (!id) return '—'
  const s = String(id)
  return s.length > 14 ? `${s.slice(0, 12)}…` : s
}

function openCitation(citation) {
  if (!citation?.docId) return
  router.push({
    name: 'documentChunks',
    params: { docId: citation.docId },
    query: {
      libraryId: primaryLibraryId.value || undefined,
      from: 'qa',
      chunkIndex: citation.chunkIndex != null ? String(citation.chunkIndex) : undefined
    }
  })
}

function openTraceHit(hit) {
  openCitation({ docId: hit.docId, chunkIndex: hit.chunkIndex })
}

function traceHeaderWarning(trace) {
  const note = trace?.retrievalNote
  if (note) return note
  const hits = trace?.hits
  if (!hits?.length) return ''
  const headerCount = hits.filter((h) => h.headerOnlyChunk).length
  if (headerCount === 0) return ''
  if (headerCount >= Math.ceil(hits.length / 2)) {
    return `命中 ${headerCount}/${hits.length} 条为表头块，建议调低相似度阈值并重索引。`
  }
  return ''
}

async function loadConversations() {
  if (!primaryLibraryId.value || !chatSettings.tenantId?.trim()) return
  try {
    const { data } = await listConversations({
      libraryId: primaryLibraryId.value,
      tenantId: chatSettings.tenantId.trim(),
      page: 1,
      size: 50
    })
    conversations.value = data.items || []
  } catch {
    conversations.value = []
  }
}

async function ensureConversation() {
  if (currentConversationId.value) return currentConversationId.value
  creatingConversation.value = true
  try {
    const { data } = await createConversation(primaryLibraryId.value, {
      tenantId: chatSettings.tenantId.trim(),
      title: '新对话'
    })
    currentConversationId.value = data.conversationId
    await loadConversations()
    return data.conversationId
  } finally {
    creatingConversation.value = false
  }
}

async function selectConversation(conversationId) {
  if (chatLoading.value) return
  currentConversationId.value = conversationId
  messages.value = []
  expandedCitations.value = new Set()
  expandedTrace.value = new Set()
  try {
    const { data } = await listConversationMessages(conversationId, chatSettings.tenantId.trim())
    messages.value = (data || []).map((m) => ({
      id: m.messageId || newMessageId(),
      role: m.role,
      content: m.content,
      citations: m.citations || [],
      searchQuery: m.searchQuery,
      loading: false,
      streaming: false,
      retrievalTrace: null
    }))
    await scrollToBottom()
  } catch (e) {
    ElMessage.error(e?.message || '加载会话失败')
  }
}

async function startNewChat() {
  messages.value = []
  inputText.value = ''
  expandedCitations.value = new Set()
  expandedTrace.value = new Set()
  currentConversationId.value = null
}

function onEnterKey(event) {
  if (event.shiftKey) return
  event.preventDefault()
  sendMessage()
}

async function sendMessage() {
  const question = inputText.value?.trim()
  if (!primaryLibraryId.value || !chatSettings.tenantId?.trim() || !question) {
    ElMessage.warning('请选择知识库编排并填写租户与问题')
    return
  }

  persistOrchestration()
  tenantId.value = chatSettings.tenantId.trim()
  localStorage.setItem('tenantId', chatSettings.tenantId.trim())

  const userMsg = {
    id: newMessageId(),
    role: 'user',
    content: question
  }
  const assistantMsg = {
    id: newMessageId(),
    role: 'assistant',
    content: '',
    userQuestion: question,
    loading: true,
    streaming: true,
    citations: [],
    found: null,
    usedLlm: false,
    searchQuery: null,
    historyUsed: 0,
    retrievalTrace: null
  }

  messages.value.push(userMsg, assistantMsg)
  inputText.value = ''
  chatLoading.value = true
  await scrollToBottom()

  const docIds = parseDocIds(docIdsText.value)
  const payload = {
    tenantId: chatSettings.tenantId.trim(),
    question,
    topK: chatSettings.topK,
    chatModel: chatSettings.chatModel?.trim() || null
  }
  if (chatSettings.minScore > 0) {
    payload.minScore = chatSettings.minScore
  }
  if (docIds.length) {
    payload.filter = { docIds }
  }
  if (chatSettings.includeAllChunkProfiles) {
    payload.includeAllChunkProfiles = true
  }

  try {
    const conversationId = await ensureConversation()
    await conversationChatStream(conversationId, payload, {
      onChunk: (text) => {
        assistantMsg.content += text
        scrollToBottom()
      },
      onDone: (event) => {
        assistantMsg.loading = false
        assistantMsg.streaming = false
        assistantMsg.content = event.content || assistantMsg.content
        assistantMsg.citations = event.citations || []
        assistantMsg.found = event.found
        assistantMsg.usedLlm = event.usedLlm
        assistantMsg.conversational = event.conversational === true
        assistantMsg.searchQuery = event.searchQuery || question
        assistantMsg.historyUsed = event.historyUsed ?? 0
        assistantMsg.retrievalTrace = event.retrievalTrace || null
        if (assistantMsg.retrievalTrace) {
          expandedTrace.value = new Set([...expandedTrace.value, assistantMsg.id])
        }
        if (event.found === false && !event.conversational) {
          ElMessage.warning('未找到相关资料')
        }
      },
      onError: (err) => {
        throw err
      }
    })
    await loadConversations()
  } catch (e) {
    assistantMsg.loading = false
    assistantMsg.streaming = false
    assistantMsg.content = e?.message || '问答失败，请稍后重试'
    assistantMsg.found = false
  } finally {
    chatLoading.value = false
    await scrollToBottom()
  }
}

onMounted(async () => {
  chatSettings.tenantId = tenantId.value
  persist()
  const { data } = await listVectorLibraries({ tenantId: tenantId.value, page: 1, size: 200 })
  libraries.value = data.items || []

  let savedIds = []
  try {
    const raw = localStorage.getItem(QA_ORCHESTRATION_KEY)
    if (raw) savedIds = JSON.parse(raw)
  } catch {
    savedIds = []
  }
  if (Array.isArray(savedIds) && savedIds.length) {
    orchestrationLibraryIds.value = savedIds.filter((id) =>
      libraries.value.some((l) => l.libraryId === id)
    )
  }
  if (!orchestrationLibraryIds.value.length) {
    if (route.query.libraryId && typeof route.query.libraryId === 'string') {
      orchestrationLibraryIds.value = [route.query.libraryId]
    } else if (libraryId.value) {
      orchestrationLibraryIds.value = [libraryId.value]
    }
  }
  persistOrchestration()
  if (primaryLibraryId.value) {
    await syncRetrievalDefaultsFromLibrary(primaryLibraryId.value)
  }
  if (route.query.new === '1') {
    await startNewChat()
    router.replace({ name: 'qa' })
  } else {
    await loadConversations()
  }
})

watch(primaryLibraryId, () => {
  loadConversations()
})
</script>

<style scoped>
.qa-page {
  min-height: calc(100vh - 120px);
}

.qa-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 20px;
  min-height: 560px;
}

.qa-sidebar {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  background: #f8fafc;
  align-self: start;
}

.qa-sidebar-form {
  margin-bottom: 8px;
}

.qa-field-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.qa-settings-collapse {
  border: none;
  margin-bottom: 12px;
}

.qa-settings-collapse :deep(.el-collapse-item__header) {
  background: transparent;
  border: none;
  font-size: 13px;
  color: #475569;
  height: 36px;
}

.qa-settings-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.qa-conv-list {
  margin: 12px 0;
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
}

.qa-conv-list__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.qa-conv-list__empty {
  font-size: 12px;
  color: #94a3b8;
  padding: 8px 0;
}

.qa-conv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 8px 10px;
  margin-bottom: 4px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}

.qa-conv-item:hover,
.qa-conv-item.is-active {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.qa-conv-item__title {
  font-size: 12px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 150px;
}

.qa-conv-item__meta {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
}

.qa-cursor {
  display: inline-block;
  animation: qa-blink 1s step-end infinite;
  color: #2563eb;
}

@keyframes qa-blink {
  50% { opacity: 0; }
}

.qa-orchestration-tag {
  margin-right: 6px;
}

.qa-sidebar-tip {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.full-width {
  width: 100%;
}

.hint-inline {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #94a3b8;
}

.qa-chat {
  display: flex;
  flex-direction: column;
  min-height: 560px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
}

.qa-chat__messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  min-height: 360px;
  max-height: calc(100vh - 320px);
}

.qa-chat__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 320px;
}

.qa-empty-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.qa-empty-desc {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  max-width: 360px;
}

.qa-msg {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.qa-msg--user {
  flex-direction: row-reverse;
}

.qa-msg__avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e2e8f0;
  color: #475569;
}

.qa-msg--assistant .qa-msg__avatar {
  background: #dbeafe;
  color: #2563eb;
}

.qa-msg--user .qa-msg__avatar {
  background: #dcfce7;
  color: #16a34a;
}

.qa-msg__body {
  max-width: min(720px, 85%);
  min-width: 0;
}

.qa-msg--user .qa-msg__body {
  text-align: right;
}

.qa-msg__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.qa-msg--user .qa-msg__head {
  justify-content: flex-end;
}

.qa-msg__role {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.qa-msg__content {
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  text-align: left;
}

.qa-msg--assistant .qa-msg__content {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #0f172a;
}

.qa-msg--user .qa-msg__content {
  background: #2563eb;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.qa-msg__loading {
  padding: 8px 0;
}

.qa-citations {
  margin-top: 10px;
  text-align: left;
}

.qa-citations__toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: #2563eb;
  font-size: 12px;
  cursor: pointer;
}

.qa-citations__chevron {
  transition: transform 0.2s;
}

.qa-citations__chevron.is-open {
  transform: rotate(180deg);
}

.qa-citations__table {
  margin-top: 8px;
}

.qa-link-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: #2563eb;
  font-size: inherit;
  cursor: pointer;
  text-align: left;
}

.qa-link-btn:hover {
  text-decoration: underline;
}

.qa-trace {
  margin-top: 10px;
  text-align: left;
}

.qa-trace__toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 8px;
  border: none;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.qa-trace__panel {
  margin-top: 8px;
  padding: 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
}

.qa-trace__meta {
  margin: 0 0 8px;
  font-size: 11px;
  line-height: 1.5;
  color: #64748b;
}

.qa-trace__warn {
  margin-bottom: 8px;
}

.qa-composer {
  border-top: 1px solid #e2e8f0;
  padding: 14px 16px;
  background: #fafafa;
}

.qa-composer__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  gap: 12px;
}

.qa-composer__hint {
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 960px) {
  .qa-layout {
    grid-template-columns: 1fr;
  }

  .qa-chat__messages {
    max-height: 50vh;
  }

  .qa-msg__body {
    max-width: 92%;
  }
}
</style>
