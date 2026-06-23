<template>
  <PageCard
    title="召回与评测"
  >
    <el-tabs v-model="activeTab" class="eval-tabs">
      <el-tab-pane label="召回预览" name="preview">
        <el-form label-position="top" @submit.prevent="submitRetrievalTest">
          <el-form-item label="测试问题" required>
            <el-input
              v-model="form.question"
              type="textarea"
              :rows="3"
              placeholder="输入要在本知识库中检索的问题"
            />
          </el-form-item>
          <el-form-item label="检索模式">
            <el-select v-model="form.retrievalMode" class="full-width">
              <el-option label="混合检索（向量 + 全文）" value="hybrid" />
              <el-option label="向量检索" value="vector" />
              <el-option label="全文检索" value="keyword" />
            </el-select>
            <p class="helper-text">控制候选召回路径；混合模式会合并向量与关键词两路结果。</p>
          </el-form-item>
          <el-form-item label="召回条数 Top-K">
            <el-input-number v-model="form.topK" :min="1" :max="50" class="full-width" />
            <p class="helper-text">融合与重排沿用库 Profile 默认值，多库编排请在智能体版本中配置。</p>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round :loading="testing" native-type="submit">
              {{ testing ? '检索中…' : '运行召回预览' }}
            </el-button>
            <el-button v-if="form.question?.trim()" round @click="addToEvalSetFromPreview">
              加入评测集…
            </el-button>
          </el-form-item>
        </el-form>

        <template v-if="result">
          <el-alert
            v-if="result.evidenceLow"
            type="warning"
            :closable="false"
            show-icon
            class="result-summary"
            title="证据数量低于阈值，请检查文档是否已 INDEXED 或调大 Top-K。"
          />
          <div class="meta-bar result-summary">
            候选 {{ result.candidateCount }} 个 · 证据 {{ result.evidence?.length || 0 }} 个 ·
            context {{ result.contextTokens }} token
            <span v-if="result.trace?.retrievalMode"> · {{ formatRetrievalMode(result.trace.retrievalMode) }}</span>
            <span v-if="isNonDefaultPostProcess(result.trace)">
              · 后处理 {{ result.trace.fusion }}/{{ result.trace.rerank }}
            </span>
          </div>
          <div v-for="evidence in result.evidence || []" :key="evidence.evidenceId" class="evidence-item">
            <div class="row-title">{{ shortId(evidence.documentId, 10) }} · chunk {{ shortId(evidence.chunkId, 8) }}</div>
            <div class="row-meta">
              score {{ percent(evidence.score, 0) }} · 库 {{ shortId(evidence.libraryId, 10) }}
              <span v-if="formatLocationMeta(evidence.metadata)" class="meta-tag">{{ formatLocationMeta(evidence.metadata) }}</span>
            </div>
            <p class="muted evidence-copy">{{ evidence.content }}</p>
          </div>
          <template v-if="result.trace?.explain?.length">
            <h4 class="section-title">召回明细 Top-{{ result.trace.explain.length }}</h4>
            <el-table :data="result.trace.explain" size="small" stripe class="data-table">
              <el-table-column prop="rank" label="#" width="48" />
              <el-table-column label="文档" width="120">
                <template #default="{ row }">{{ shortId(row.documentId, 10) }}</template>
              </el-table-column>
              <el-table-column label="得分" width="88">
                <template #default="{ row }">{{ percent(row.score, 0) }}</template>
              </el-table-column>
              <el-table-column label="向量/关键词" width="120">
                <template #default="{ row }">v#{{ row.vectorRank ?? '—' }} · k#{{ row.keywordRank ?? '—' }}</template>
              </el-table-column>
              <el-table-column label="定位" width="120">
                <template #default="{ row }">{{ row.pageNumber != null ? `P${row.pageNumber}` : '—' }}</template>
              </el-table-column>
              <el-table-column prop="contentPreview" label="预览" min-width="200" show-overflow-tooltip />
            </el-table>
          </template>
          <el-empty v-if="!(result.evidence?.length)" description="未召回任何证据，请检查文档是否已 INDEXED。" />
        </template>
      </el-tab-pane>

      <el-tab-pane label="评测集" name="samples">
        <div class="toolbar">
          <el-button type="primary" round @click="openSampleDialog()">新增样本</el-button>
          <el-button round :loading="autoGeneratingSamples" @click="handleAutoGenerateSamples">样本自动生成</el-button>
          <el-button round :loading="loadingSamples" @click="loadSamples">刷新</el-button>
        </div>
        <el-table v-if="samples.length" :data="samples" size="small" stripe class="data-table">
          <el-table-column prop="question" label="问题" min-width="200" show-overflow-tooltip />
          <el-table-column label="来源" width="88">
            <template #default="{ row }">
              <el-tag v-if="row.autoDraft" size="small" type="warning">自动草稿</el-tag>
              <el-tag v-else size="small" type="info">手工</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Hit@K" width="72" prop="hitRank" />
          <el-table-column label="期望命中" min-width="160">
            <template #default="{ row }">{{ formatExpectations(row) }}</template>
          </el-table-column>
          <el-table-column label="启用" width="72">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openSampleDialog(row)">编辑</el-button>
              <el-button link type="danger" @click="removeSample(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!loadingSamples" description="暂无评测样本。可点击「样本自动生成」，或在召回预览调试后加入。" />
      </el-tab-pane>

      <el-tab-pane label="评测报告" name="report">
        <p class="helper-text section-intro">
          对评测集中已启用的样本批量计算 Recall@K、MRR 等指标，用于发版回归与索引 Promote 门禁。
          评测使用「召回预览」中的检索模式与 Top-K（{{ formatRetrievalMode(form.retrievalMode) }} · K={{ form.topK }}）。
        </p>
        <el-alert
          v-if="!samples.filter(s => s.enabled).length && !loadingSamples"
          type="info"
          :closable="false"
          show-icon
          class="result-summary"
          title="尚无启用的评测样本"
          description="请先在「评测集」中审核自动草稿或新增样本，勾选启用后再运行评测。"
        />
        <div class="toolbar">
          <el-form inline @submit.prevent="runBatchEval">
            <el-form-item label="评测 Hit@K">
              <el-input-number v-model="batchHitK" :min="1" :max="50" placeholder="默认" />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                round
                :loading="runningBatch"
                :disabled="!enabledSampleCount"
                native-type="submit"
              >
                运行评测
              </el-button>
            </el-form-item>
          </el-form>
          <el-button round :loading="loadingRuns" @click="loadEvalRuns">刷新历史</el-button>
        </div>

        <el-alert
          v-if="latestRun"
          :type="latestRun.recallAtK >= 1 ? 'success' : latestRun.recallAtK >= 0.8 ? 'warning' : 'error'"
          :closable="false"
          show-icon
          class="result-summary"
          :title="batchSummary(latestRun)"
        />

        <el-table v-if="evalRuns.length" :data="evalRuns" size="small" stripe class="data-table">
          <el-table-column label="时间" width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="Recall@K" width="120">
            <template #default="{ row }">
              {{ row.recallAtK == null ? '—' : percent(row.recallAtK, 1) }}
            </template>
          </el-table-column>
          <el-table-column label="MRR" width="88">
            <template #default="{ row }">{{ row.mrr == null ? '—' : percent(row.mrr, 1) }}</template>
          </el-table-column>
          <el-table-column label="CP@K" width="88">
            <template #default="{ row }">
              {{ row.contextPrecisionAtK == null ? '—' : percent(row.contextPrecisionAtK, 1) }}
            </template>
          </el-table-column>
          <el-table-column label="检索模式" width="100">
            <template #default="{ row }">
              {{ formatRetrievalMode(row.retrievalPolicy?.retrievalMode) }}
            </template>
          </el-table-column>
          <el-table-column prop="message" label="摘要" min-width="160" show-overflow-tooltip />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewRun(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else-if="!loadingRuns" description="尚无评测记录。" />

        <template v-if="selectedRun?.stratifiedRecall && Object.keys(selectedRun.stratifiedRecall).length">
          <h4 class="section-title">分层 Recall（contentFamily）</h4>
          <el-table
            :data="Object.entries(selectedRun.stratifiedRecall).map(([family, recall]) => ({ family, recall }))"
            size="small"
            stripe
            class="data-table"
          >
            <el-table-column prop="family" label="类型" width="160" />
            <el-table-column label="Recall">
              <template #default="{ row }">{{ percent(row.recall, 1) }}</template>
            </el-table-column>
          </el-table>
        </template>

        <template v-if="selectedRun?.results?.length">
          <h4 class="section-title">样本明细 · {{ shortId(selectedRun.evalRunId, 8) }}</h4>
          <el-table :data="selectedRun.results" size="small" stripe class="data-table">
            <el-table-column prop="question" label="问题" min-width="180" show-overflow-tooltip />
            <el-table-column label="命中" width="72">
              <template #default="{ row }">
                <el-tag size="small" :type="row.hit ? 'success' : 'danger'">{{ row.hit ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Rank" width="72">
              <template #default="{ row }">{{ row.firstHitRank ?? '—' }}</template>
            </el-table-column>
            <el-table-column prop="matchType" label="匹配类型" width="140" />
            <el-table-column prop="failureReason" label="说明" min-width="160" show-overflow-tooltip />
          </el-table>
        </template>
      </el-tab-pane>
    </el-tabs>
  </PageCard>

  <el-dialog
    v-model="sampleDialogVisible"
    :title="editingSample ? '编辑评测样本' : '新增评测样本'"
    width="560px"
  >
    <el-form label-position="top">
      <el-form-item label="问题" required>
        <el-input v-model="sampleForm.question" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="Hit@K（判定窗口）">
        <el-input-number v-model="sampleForm.hitRank" :min="1" :max="50" class="full-width" />
        <p class="helper-text">Top-K 内命中期望文档或片段即计为命中。</p>
      </el-form-item>
      <el-form-item label="期望来源（文件名 / 路径）">
        <el-input v-model="sampleForm.expectedSourceUri" placeholder="如 guide.pdf，与文档列表 sourceUri 对应" clearable />
      </el-form-item>
      <el-form-item label="期望片段（ground truth）">
        <el-input
          v-model="sampleForm.groundTruthContext"
          type="textarea"
          :rows="2"
          placeholder="块内容包含该片段即命中（≥8 字符）；与来源文件名二选一或同时填写"
        />
      </el-form-item>
      <el-collapse>
        <el-collapse-item title="高级：期望 documentId" name="docId">
          <el-form-item label="documentId">
            <el-input v-model="sampleForm.expectedDocumentId" placeholder="可选 UUID" clearable />
          </el-form-item>
        </el-collapse-item>
      </el-collapse>
      <el-form-item label="备注">
        <el-input v-model="sampleForm.notes" />
      </el-form-item>
      <el-form-item label="参与评测">
        <el-switch v-model="sampleForm.enabled" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="sampleDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingSample" @click="saveSample">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import PageCard from '../../components/PageCard.vue';
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import {
  createRetrievalEvalSample,
  deleteRetrievalEvalSample,
  generateRetrievalEvalDrafts,
  getRetrievalEvaluation,
  listRetrievalEvalSamples,
  listRetrievalEvaluations,
  runLibraryRetrievalTest,
  runRetrievalEvaluation,
  updateRetrievalEvalSample
} from '../../api';
import { formatDateTime, formatLocationMeta, percent, shortId } from '../../format';

const route = useRoute();
const { libraryId, showMessage } = useLibraryWorkspace();

function resolveInitialTab() {
  const tab = route.query.tab;
  if (tab === 'batch' || tab === 'report') return 'report';
  if (tab === 'samples') return 'samples';
  return 'preview';
}

const activeTab = ref(resolveInitialTab());
const testing = ref(false);
const savingSample = ref(false);
const loadingSamples = ref(false);
const loadingRuns = ref(false);
const runningBatch = ref(false);
const autoGeneratingSamples = ref(false);
const result = ref(null);
const samples = ref([]);
const evalRuns = ref([]);
const selectedRun = ref(null);
const sampleDialogVisible = ref(false);
const editingSample = ref(null);
const batchHitK = ref(null);

const form = ref({
  question: '',
  retrievalMode: 'hybrid',
  topK: 8
});

const RETRIEVAL_MODE_LABELS = {
  vector: '向量检索',
  keyword: '全文检索',
  hybrid: '混合检索'
};

const sampleForm = ref(emptySampleForm());

const latestRun = computed(() => evalRuns.value[0] || null);
const enabledSampleCount = computed(() => samples.value.filter(sample => sample.enabled).length);

function emptySampleForm() {
  return {
    question: '',
    hitRank: 8,
    expectedDocumentId: '',
    expectedSourceUri: '',
    groundTruthContext: '',
    notes: '',
    enabled: true
  };
}

function formatRetrievalMode(mode) {
  return RETRIEVAL_MODE_LABELS[mode] || mode || RETRIEVAL_MODE_LABELS.hybrid;
}

function isNonDefaultPostProcess(trace) {
  if (!trace) return false;
  const fusion = trace.fusion || 'score';
  const rerank = trace.rerank || 'none';
  return fusion !== 'score' || rerank !== 'none';
}

function buildRetrievalPolicyOverride() {
  return {
    topKPerLibrary: form.value.topK,
    retrievalMode: form.value.retrievalMode || 'hybrid'
  };
}

async function submitRetrievalTest() {
  if (!form.value.question?.trim()) {
    showMessage('请输入测试问题', 'error');
    return;
  }
  testing.value = true;
  try {
    result.value = await runLibraryRetrievalTest(libraryId.value, {
      question: form.value.question.trim(),
      retrievalPolicyOverride: buildRetrievalPolicyOverride()
    });
    showMessage('召回预览完成', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    testing.value = false;
  }
}

function addToEvalSetFromPreview() {
  editingSample.value = null;
  sampleForm.value = {
    ...emptySampleForm(),
    question: form.value.question.trim(),
    hitRank: form.value.topK
  };
  sampleDialogVisible.value = true;
}

function batchSummary(run) {
  const recall = run.recallAtK == null ? '—' : percent(run.recallAtK, 1);
  const mrr = run.mrr == null ? '' : ` · MRR ${percent(run.mrr, 1)}`;
  const cp = run.contextPrecisionAtK == null ? '' : ` · CP@${run.hitK} ${percent(run.contextPrecisionAtK, 1)}`;
  return `Recall@${run.hitK} = ${recall}（${run.passedSamples}/${run.totalSamples}）${mrr}${cp}`;
}

function formatExpectations(row) {
  const parts = [];
  if (row.expectedDocumentIds?.length) {
    parts.push(`doc ${shortId(row.expectedDocumentIds[0], 8)}`);
  }
  if (row.expectedSourceUris?.length) parts.push(row.expectedSourceUris[0]);
  if (row.groundTruthContexts?.length) parts.push('期望片段');
  return parts.join(' · ') || '—';
}

async function handleAutoGenerateSamples() {
  autoGeneratingSamples.value = true;
  try {
    const created = await generateRetrievalEvalDrafts(libraryId.value, {
      replaceExistingAutoDrafts: true
    });
    await loadSamples();
    showMessage(
      created.length
        ? `已自动生成 ${created.length} 条评测样本（前 ${Math.min(5, created.length)} 条已启用）`
        : '未生成新样本（可能已有同名草稿或分块不满足条件）',
      created.length ? 'success' : 'info'
    );
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    autoGeneratingSamples.value = false;
  }
}

async function loadSamples() {
  loadingSamples.value = true;
  try {
    samples.value = await listRetrievalEvalSamples(libraryId.value);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingSamples.value = false;
  }
}

async function loadEvalRuns() {
  loadingRuns.value = true;
  try {
    evalRuns.value = await listRetrievalEvaluations(libraryId.value);
    if (evalRuns.value.length && !selectedRun.value) {
      await viewRun(evalRuns.value[0]);
    }
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingRuns.value = false;
  }
}

function openSampleDialog(sample = null) {
  editingSample.value = sample;
  if (sample) {
    sampleForm.value = {
      question: sample.question,
      hitRank: sample.hitRank,
      expectedDocumentId: sample.expectedDocumentIds?.[0] || '',
      expectedSourceUri: sample.expectedSourceUris?.[0] || '',
      groundTruthContext: sample.groundTruthContexts?.[0] || '',
      notes: sample.notes || '',
      enabled: sample.enabled
    };
  } else {
    sampleForm.value = emptySampleForm();
  }
  sampleDialogVisible.value = true;
}

function buildSamplePayload(source) {
  const payload = {
    question: source.question?.trim(),
    hitRank: source.hitRank,
    notes: source.notes || undefined,
    enabled: source.enabled
  };
  if (source.expectedDocumentId?.trim()) {
    payload.expectedDocumentIds = [source.expectedDocumentId.trim()];
  }
  if (source.expectedSourceUri?.trim()) {
    payload.expectedSourceUris = [source.expectedSourceUri.trim()];
  }
  if (source.groundTruthContext?.trim()) {
    payload.groundTruthContexts = [source.groundTruthContext.trim()];
  }
  return payload;
}

async function saveSample() {
  if (!sampleForm.value.question?.trim()) {
    showMessage('请填写问题', 'error');
    return;
  }
  const hasExpectation = sampleForm.value.expectedDocumentId?.trim()
    || sampleForm.value.expectedSourceUri?.trim()
    || sampleForm.value.groundTruthContext?.trim();
  if (!hasExpectation) {
    showMessage('请至少填写期望来源或期望片段', 'error');
    return;
  }
  savingSample.value = true;
  try {
    const payload = buildSamplePayload(sampleForm.value);
    if (editingSample.value) {
      await updateRetrievalEvalSample(libraryId.value, editingSample.value.sampleId, payload);
    } else {
      await createRetrievalEvalSample(libraryId.value, payload);
    }
    sampleDialogVisible.value = false;
    await loadSamples();
    showMessage('评测样本已保存', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    savingSample.value = false;
  }
}

async function removeSample(sample) {
  try {
    await deleteRetrievalEvalSample(libraryId.value, sample.sampleId);
    await loadSamples();
    showMessage('已删除', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function runBatchEval() {
  runningBatch.value = true;
  try {
    const payload = {
      enabledOnly: true,
      retrievalPolicyOverride: buildRetrievalPolicyOverride()
    };
    if (batchHitK.value) payload.hitK = batchHitK.value;
    const run = await runRetrievalEvaluation(libraryId.value, payload);
    selectedRun.value = run;
    await loadEvalRuns();
    showMessage(run.message || '评测完成', run.recallAtK >= 1 ? 'success' : 'warning');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    runningBatch.value = false;
  }
}


async function viewRun(run) {
  try {
    selectedRun.value = await getRetrievalEvaluation(libraryId.value, run.evalRunId);
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

onMounted(() => {
  loadSamples();
  loadEvalRuns();
});
</script>

<style scoped>
.eval-tabs {
  margin-top: 4px;
}
.section-intro {
  margin: 0 0 16px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}
.result-summary {
  margin: 20px 0 12px;
}
.section-title {
  margin: 24px 0 12px;
  font-size: 14px;
  font-weight: 600;
}
.helper-text {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--dp-text-secondary);
  line-height: 1.5;
}
</style>
