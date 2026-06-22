<template>
  <div class="page-wrap page-wrap--fluid page-stack">
    <el-alert v-if="message" :title="message" :type="messageType === 'success' ? 'success' : 'error'" show-icon closable @close="message = ''" />

    <div class="grid cols-2">
      <PageCard title="Pipeline Trace 查询">
        <el-form label-position="top" @submit.prevent="loadTrace">
          <el-form-item label="Trace ID">
            <el-input v-model="traceId" placeholder="UUID" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round :loading="loadingTrace" native-type="submit">查询 Trace</el-button>
          </el-form-item>
        </el-form>
        <el-table v-if="traceSpans.length" :data="traceSpans" size="small" class="data-table">
          <el-table-column prop="stage" label="阶段" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">{{ row.durationMs ?? '--' }} ms</template>
          </el-table-column>
          <el-table-column prop="pipeline" label="Pipeline" width="100" />
        </el-table>
        <el-empty v-else description="输入 Trace ID 查询 Pipeline Span。" />
      </PageCard>

      <PageCard title="Pipeline Run 查询">
        <el-form label-position="top" @submit.prevent="loadPipelineRun">
          <div class="grid cols-2 compact-grid">
            <el-form-item label="Pipeline">
              <el-select v-model="pipelineName" class="full-width">
                <el-option label="query" value="query" />
                <el-option label="ingestion" value="ingestion" />
              </el-select>
            </el-form-item>
            <el-form-item label="Run ID">
              <el-input v-model="pipelineRunId" placeholder="UUID" />
            </el-form-item>
          </div>
          <el-form-item>
            <el-button type="primary" round :loading="loadingPipelineRun" native-type="submit">查询 Run</el-button>
          </el-form-item>
        </el-form>
        <el-table v-if="runSpans.length" :data="runSpans" size="small" class="data-table">
          <el-table-column prop="stage" label="阶段" width="120" />
          <el-table-column prop="status" label="状态" width="100" />
          <el-table-column label="耗时" width="90">
            <template #default="{ row }">{{ row.durationMs ?? '--' }} ms</template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="按 pipeline + runId 查询 Span。" />
      </PageCard>
    </div>

    <PageCard title="评测运行">
      <template #actions>
        <el-button round :loading="loadingEvalRuns" @click="refreshEvalRuns">刷新列表</el-button>
      </template>

      <el-form label-position="top" @submit.prevent="submitEvalRun">
        <div class="grid cols-2 compact-grid">
          <el-form-item label="租户 ID">
            <el-input v-model="evalForm.tenantId" />
          </el-form-item>
          <el-form-item label="智能体">
            <el-select v-model="evalForm.agentId" clearable class="full-width" placeholder="可选">
              <el-option
                v-for="agent in agents"
                :key="agent.agentId"
                :label="agent.name"
                :value="agent.agentId"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="评测样例（每行：问题|期望答案）">
          <el-input v-model="evalSamplesText" type="textarea" :rows="4" placeholder="如何安装 PostgreSQL|PostgreSQL 安装步骤" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" round :loading="creatingEval" native-type="submit">创建评测运行</el-button>
        </el-form-item>
      </el-form>

      <el-table v-if="evalRuns.length" :data="evalRuns" size="small" class="data-table">
        <el-table-column label="ID" min-width="120">
          <template #default="{ row }">{{ shortId(row.evalRunId, 12) }}</template>
        </el-table-column>
        <el-table-column prop="evalType" label="类型" width="120" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="均分" width="90">
          <template #default="{ row }">{{ formatPercent(row.metrics?.averageScore, 0) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEvalDetail(row.evalRunId)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无评测运行。" />

      <div v-if="evalDetail" class="eval-detail">
        <div class="section-head"><h4>评测详情</h4></div>
        <div class="meta-bar">
          样例 {{ evalDetail.samples?.length || 0 }} 个 · 均分 {{ formatPercent(evalDetail.metrics?.averageScore, 0) }}
        </div>
        <div v-for="sample in evalDetail.samples || []" :key="sample.sampleId" class="evidence-item">
          <div class="row-title">Q: {{ sample.question }}</div>
          <div class="row-meta">score {{ sample.score ?? '--' }}</div>
          <p class="muted evidence-copy">A: {{ sample.actualAnswer || '无回答' }}</p>
        </div>
      </div>
    </PageCard>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import PageCard from '../components/PageCard.vue';
import {
  createEvalRun,
  getEvalRun,
  listAgents,
  listEvalRuns,
  listPipelineRun,
  listPipelineTrace
} from '../api';
import { requestContext } from '../context';
import { formatPercent, shortId } from '../format';

const message = ref('');
const messageType = ref('success');
const traceId = ref('');
const pipelineName = ref('query');
const pipelineRunId = ref('');
const traceSpans = ref([]);
const runSpans = ref([]);
const loadingTrace = ref(false);
const loadingPipelineRun = ref(false);
const agents = ref([]);
const evalRuns = ref([]);
const evalDetail = ref(null);
const loadingEvalRuns = ref(false);
const creatingEval = ref(false);
const evalSamplesText = ref('如何安装 PostgreSQL|PostgreSQL 安装\n如何排查 502|502 网关错误');
const evalForm = ref({
  tenantId: requestContext.tenantId,
  agentId: ''
});

function showMessage(text, type) {
  message.value = text || '操作失败';
  messageType.value = type;
}

async function loadTrace() {
  if (!traceId.value) {
    showMessage('请输入 Trace ID', 'error');
    return;
  }
  loadingTrace.value = true;
  try {
    traceSpans.value = await listPipelineTrace(traceId.value.trim());
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingTrace.value = false;
  }
}

async function loadPipelineRun() {
  if (!pipelineRunId.value) {
    showMessage('请输入 Run ID', 'error');
    return;
  }
  loadingPipelineRun.value = true;
  try {
    runSpans.value = await listPipelineRun(pipelineName.value, pipelineRunId.value.trim());
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingPipelineRun.value = false;
  }
}

async function refreshEvalRuns() {
  loadingEvalRuns.value = true;
  try {
    evalRuns.value = await listEvalRuns({
      tenantId: evalForm.value.tenantId,
      agentId: evalForm.value.agentId || undefined
    });
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingEvalRuns.value = false;
  }
}

async function submitEvalRun() {
  creatingEval.value = true;
  try {
    const samples = evalSamplesText.value
      .split('\n')
      .map(line => line.trim())
      .filter(Boolean)
      .map(line => {
        const [question, expectedAnswer = ''] = line.split('|').map(item => item.trim());
        return { question, expectedAnswer };
      });
    evalDetail.value = await createEvalRun({
      tenantId: evalForm.value.tenantId,
      agentId: evalForm.value.agentId || null,
      evalType: 'qa-regression',
      samples
    });
    showMessage('评测运行已完成', 'success');
    await refreshEvalRuns();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    creatingEval.value = false;
  }
}

async function openEvalDetail(evalRunId) {
  try {
    evalDetail.value = await getEvalRun(evalRunId);
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

onMounted(async () => {
  evalForm.value.tenantId = requestContext.tenantId;
  try {
    agents.value = await listAgents({ tenantId: requestContext.tenantId });
    if (agents.value.length > 0) {
      evalForm.value.agentId = agents.value[0].agentId;
    }
  } catch (error) {
    showMessage(error.message, 'error');
  }
  await refreshEvalRuns();
});
</script>

<style scoped>
.eval-detail {
  margin-top: 16px;
}

.section-head h4 {
  margin: 0 0 8px;
  font-size: 14px;
}
</style>
