<template>
  <div class="page-wrap page-wrap--fluid qa-page">
    <el-alert v-if="message" :title="message" :type="messageType === 'success' ? 'success' : 'error'" show-icon closable @close="message = ''" />

    <PageCard title="智能问答" class="qa-workbench">
      <template #actions>
        <el-tag v-if="queryResult" size="small" :type="statusTagType(queryResult.status)">{{ queryResult.status }}</el-tag>
        <span class="qa-inline-hint">证据与引用会在结果返回后自动展开</span>
      </template>

      <div class="qa-layout">
        <aside class="qa-sidebar">
          <div class="qa-sidebar__section">
            <div class="section-head">
              <h3>问答配置</h3>
            </div>
            <el-form label-position="top" size="default" @submit.prevent="submit">
              <el-form-item label="智能体 ID" required>
                <el-select v-model="form.agentId" filterable class="full-width" placeholder="选择智能体">
                  <el-option
                    v-for="agent in agents"
                    :key="agent.agentId"
                    :label="`${agent.name} · ${shortId(agent.agentId, 8)}`"
                    :value="agent.agentId"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="智能体版本 ID">
                <el-input v-model="form.agentVersionId" placeholder="留空使用已发布版本" />
              </el-form-item>
              <el-form-item label="调试知识库 ID">
                <el-input v-model="debugLibraryIdsText" placeholder="可选，逗号分隔" />
              </el-form-item>
              <el-form-item label="问题" required>
                <el-input v-model="form.question" type="textarea" :rows="4" />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" round :loading="loading" native-type="submit" class="full-width">
                  {{ loading ? '问答中...' : '发起问答' }}
                </el-button>
              </el-form-item>
            </el-form>
          </div>

          <div class="qa-sidebar__section">
            <div class="section-head"><h3>Token 摘要</h3></div>
            <div class="summary-metrics">
              <div class="summary-metric">
                <span class="summary-metric__label">Prompt</span>
                <span class="summary-metric__value">{{ formatNumber(queryResult?.tokenUsage?.promptTokens || 0) }}</span>
              </div>
              <div class="summary-metric">
                <span class="summary-metric__label">Completion</span>
                <span class="summary-metric__value">{{ formatNumber(queryResult?.tokenUsage?.completionTokens || 0) }}</span>
              </div>
              <div class="summary-metric summary-metric--primary">
                <span class="summary-metric__label">Total</span>
                <span class="summary-metric__value">{{ formatNumber(queryResult?.tokenUsage?.totalTokens || 0) }}</span>
              </div>
            </div>
          </div>
        </aside>

        <main class="qa-chat">
          <div class="qa-chat__messages">
            <div class="message user">{{ form.question }}</div>
            <div v-if="queryResult?.answer" class="message answer-panel">
              <div class="answer-body">{{ queryResult.answer }}</div>
            </div>
            <el-empty v-else description="提交后这里会显示结构化回答" :image-size="80" />
          </div>

          <div class="qa-evidence-panel">
            <div class="section-head">
              <h3>证据与轨迹</h3>
            </div>
            <el-tabs v-model="activeTab" class="qa-tabs">
              <el-tab-pane label="证据" name="evidence">
                <div v-if="queryResult?.evidence?.length" class="evidence">
                  <div v-for="item in queryResult.evidence" :key="item.evidenceId" class="evidence-item">
                    <div class="row-title">{{ shortId(item.documentId, 12) }} · chunk {{ shortId(item.chunkId, 8) }}</div>
                    <div class="row-meta">
                      score {{ formatPercent(item.score, 0) }} · 库 {{ shortId(item.libraryId, 10) }}
                      <span v-if="formatLocationMeta(item.metadata)" class="meta-tag">{{ formatLocationMeta(item.metadata) }}</span>
                    </div>
                    <div v-if="formatCitationLocationTags(item.metadata).length" class="citation-tags">
                      <el-tag
                        v-for="tag in formatCitationLocationTags(item.metadata)"
                        :key="`${item.evidenceId}-${tag.key}`"
                        size="small"
                        type="warning"
                        effect="plain"
                      >
                        {{ tag.label }}
                      </el-tag>
                    </div>
                    <p class="muted evidence-copy">{{ item.content }}</p>
                  </div>
                </div>
                <el-empty v-else description="暂无证据数据" :image-size="64" />
              </el-tab-pane>
              <el-tab-pane label="引用" name="citations">
                <div v-if="queryResult?.citations?.length" class="evidence">
                  <div v-for="item in queryResult.citations" :key="item.citationId" class="evidence-item">
                    <div class="row-title">{{ item.sourceTitle || shortId(item.documentId, 12) }}</div>
                    <div class="row-meta">
                      {{ item.sourceUri || '无来源 URI' }}
                      <span v-if="formatLocationMeta(item.metadata)" class="meta-tag">{{ formatLocationMeta(item.metadata) }}</span>
                    </div>
                    <div v-if="formatCitationLocationTags(item.metadata).length" class="citation-tags">
                      <el-tag
                        v-for="tag in formatCitationLocationTags(item.metadata)"
                        :key="`${item.citationId}-${tag.key}`"
                        size="small"
                        type="warning"
                        effect="plain"
                      >
                        {{ tag.label }}
                      </el-tag>
                      <el-button
                        v-if="item.libraryId && item.documentId && canLocateCitation(item.metadata)"
                        link
                        type="primary"
                        @click="openCitationDocument(item)"
                      >
                        查看原文定位
                      </el-button>
                    </div>
                    <p class="muted evidence-copy">{{ item.snippet || '暂无引用摘要' }}</p>
                  </div>
                </div>
                <el-empty v-else description="暂无引用数据" :image-size="64" />
              </el-tab-pane>
              <el-tab-pane label="Token / Trace" name="token">
                <div class="meta-bar">
                  Trace {{ queryResult?.traceId || '--' }} ·
                  {{ queryResult?.completedAt ? formatDateTime(queryResult.completedAt) : '等待执行' }}
                </div>
                <div class="drawer-actions" style="margin: 12px 0">
                  <el-button size="small" round :loading="loadingTrace" :disabled="!queryResult?.traceId" @click="loadTrace">
                    加载 Pipeline Span
                  </el-button>
                </div>
                <el-table v-if="traceSpans.length" :data="traceSpans" size="small" class="data-table">
                  <el-table-column prop="stage" label="阶段" width="120" />
                  <el-table-column prop="status" label="状态" width="100" />
                  <el-table-column label="耗时" width="90">
                    <template #default="{ row }">{{ row.durationMs ?? '--' }} ms</template>
                  </el-table-column>
                </el-table>
                <pre class="pre-block">{{ resultText }}</pre>
              </el-tab-pane>
            </el-tabs>
          </div>
        </main>
      </div>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import PageCard from '../components/PageCard.vue';
import { askQuestion, listAgents, listPipelineTrace } from '../api';
import { requestContext } from '../context';
import {
  buildLibraryDocumentLocateRoute,
  canLocateCitation,
  formatCitationLocationTags,
  formatDateTime,
  formatLocationMeta,
  formatNumber,
  formatPercent,
  shortId
} from '../format';

const router = useRouter();

const debugLibraryIdsText = ref('');
const agents = ref([]);
const traceSpans = ref([]);
const loadingTrace = ref(false);
const form = ref({
  agentId: '',
  agentVersionId: '',
  question: '如何排查接口 502？',
  sessionId: '',
  stream: false
});
const queryResult = ref(null);
const loading = ref(false);
const message = ref('');
const messageType = ref('success');
const activeTab = ref('evidence');
const resultText = computed(() => (queryResult.value ? JSON.stringify(queryResult.value, null, 2) : '等待提问'));

function statusTagType(status) {
  const value = String(status || '').toUpperCase();
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  return 'info';
}

async function submit() {
  loading.value = true;
  try {
    const data = await askQuestion({
      ...form.value,
      agentVersionId: form.value.agentVersionId || null,
      debugLibraryIds: debugLibraryIdsText.value.split(',').map(item => item.trim()).filter(Boolean),
      variables: {}
    });
    queryResult.value = data;
    traceSpans.value = [];
    showMessage('问答完成', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function loadTrace() {
  if (!queryResult.value?.traceId) return;
  loadingTrace.value = true;
  try {
    traceSpans.value = await listPipelineTrace(queryResult.value.traceId);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingTrace.value = false;
  }
}

async function loadAgents() {
  try {
    agents.value = await listAgents({ tenantId: requestContext.tenantId });
    if (!form.value.agentId && agents.value.length > 0) {
      form.value.agentId = agents.value[0].agentId;
    }
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

loadAgents();

function openCitationDocument(citation) {
  const routeLocation = buildLibraryDocumentLocateRoute(
    citation.libraryId,
    citation.documentId,
    { ...citation.metadata, chunkId: citation.chunkId }
  );
  if (routeLocation) {
    router.push(routeLocation);
  }
}

function showMessage(text, type) {
  message.value = text || '操作失败';
  messageType.value = type;
}
</script>
