<template>
  <div class="page-wrap page-wrap--fluid page-stack">
    <el-alert v-if="message" :title="message" :type="messageType === 'success' ? 'success' : 'error'" show-icon closable @close="message = ''" />

    <div class="stat-grid">
      <article class="stat-card stat-card--primary">
        <span class="stat-card__label">智能体</span>
        <span class="stat-card__value">{{ formatNumber(agents.length) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">已发布版本</span>
        <span class="stat-card__value">{{ formatNumber(publishedCount) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">可绑定知识库</span>
        <span class="stat-card__value">{{ formatNumber(availableLibraries.length) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">Tokenizer Profile</span>
        <span class="stat-card__value">{{ formatNumber(tokenizerProfiles.length) }}</span>
      </article>
    </div>

    <div class="grid cols-2">
      <PageCard title="创建知识智能体">
        <template #actions>
          <el-button round @click="refresh">刷新</el-button>
        </template>

        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="租户 ID">
            <el-input v-model="form.tenantId" />
          </el-form-item>
          <el-form-item label="名称" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="场景规则预设" required>
            <el-select v-model="form.scenePresetCode" class="full-width">
              <el-option
                v-for="preset in sceneRulePresets"
                :key="preset.code"
                :label="`${preset.name}（${preset.code}）`"
                :value="preset.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="Chat Tokenizer Profile">
            <el-select v-model="form.chatTokenizerProfileId" clearable class="full-width" placeholder="使用模型默认 tokenizer">
              <el-option
                v-for="profile in tokenizerProfiles"
                :key="profile.tokenizerProfileId"
                :label="`${profile.provider}/${profile.modelName} · ${profile.tokenizerId}${profile.approximate ? '（近似）' : ''}`"
                :value="profile.tokenizerProfileId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="系统提示词">
            <el-input v-model="form.systemPrompt" type="textarea" :rows="3" />
          </el-form-item>
          <p v-if="selectedPreset" class="helper-text">
            {{ selectedPreset.description }}。默认证据数 {{ selectedPreset.config?.retrieval?.maxEvidence ?? '--' }}，
            上下文 {{ selectedPreset.config?.answer?.maxContextTokens ?? '--' }} token。
          </p>

          <el-form-item v-if="availableLibraries.length" label="绑定知识库">
            <el-checkbox-group v-model="selectedLibraryIds">
              <el-checkbox v-for="library in availableLibraries" :key="library.libraryId" :label="String(library.libraryId)">
                {{ library.name }}（{{ shortId(library.libraryId, 6) }}）
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" round :loading="loading" native-type="submit">创建智能体</el-button>
          </el-form-item>
        </el-form>
      </PageCard>

      <PageCard title="智能体版本">
        <template #actions>
          <el-select v-model="versionAgentId" size="small" placeholder="选择智能体" style="width: 220px" @change="loadVersions">
            <el-option
              v-for="item in agents"
              :key="item.agentId"
              :label="item.name"
              :value="item.agentId"
            />
          </el-select>
          <el-button size="small" round :loading="loadingVersions" @click="loadVersions">刷新</el-button>
        </template>

        <el-table v-if="agentVersions.length" :data="agentVersions" class="data-table" size="small">
          <el-table-column label="版本" width="70">
            <template #default="{ row }">v{{ row.version }}</template>
          </el-table-column>
          <el-table-column prop="scenePresetCode" label="场景" width="150" show-overflow-tooltip />
          <el-table-column label="知识库" width="80">
            <template #default="{ row }">{{ row.libraryIds?.length || 0 }} 个</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.published ? 'success' : row.status === 'DRAFT' ? 'warning' : 'info'">
                {{ row.published ? 'PUBLISHED' : row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" min-width="160">
            <template #default="{ row }">
              <el-button v-if="!row.published" link type="primary" @click="publishVersion(row)">发布</el-button>
              <el-button v-if="row.published || row.status !== 'DISABLED'" link type="warning" @click="disableVersion(row)">禁用</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="选择智能体后查看版本列表。" />

        <el-divider />

        <el-form label-position="top" size="small" @submit.prevent="submitVersion">
          <div class="section-head"><h4>创建新版本（DRAFT）</h4></div>
          <el-form-item label="场景规则预设">
            <el-select v-model="versionForm.scenePresetCode" class="full-width">
              <el-option
                v-for="preset in sceneRulePresets"
                :key="preset.code"
                :label="preset.name"
                :value="preset.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="绑定知识库">
            <el-checkbox-group v-model="versionForm.libraryIds">
              <el-checkbox v-for="library in availableLibraries" :key="library.libraryId" :label="String(library.libraryId)">
                {{ library.name }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round :loading="creatingVersion" :disabled="!versionAgentId" native-type="submit">
              创建版本
            </el-button>
          </el-form-item>
        </el-form>
      </PageCard>
    </div>

    <PageCard title="场景规则预设">
      <div class="grid cols-3">
        <div v-for="preset in sceneRulePresets" :key="preset.code" class="step">
          <div class="step-name">{{ preset.name }}</div>
          <div class="step-desc">{{ preset.description }}</div>
          <div class="meta-line" style="margin-top: 10px">
            maxEvidence {{ preset.config?.retrieval?.maxEvidence ?? '--' }} · context {{ preset.config?.answer?.maxContextTokens ?? '--' }}
          </div>
        </div>
      </div>
    </PageCard>

    <PageCard title="检索测试" subtitle="正式问答前验证多库路由、证据召回与上下文预算。">
      <el-form label-position="top" @submit.prevent="submitRetrievalTest">
        <div class="grid cols-2 compact-grid">
          <el-form-item label="智能体" required>
            <el-select v-model="retrievalTest.agentId" class="full-width" placeholder="请选择智能体">
              <el-option
                v-for="item in agents"
                :key="item.agentId"
                :value="item.agentId"
                :label="`${item.name} · ${shortId(item.agentId, 8)}`"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="上下文 Token 上限">
            <el-input-number v-model="retrievalTest.maxContextTokens" :min="256" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item label="问题">
          <el-input v-model="retrievalTest.question" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="调试知识库 ID">
          <el-input v-model="retrievalTest.debugLibraryIdsText" placeholder="可选，逗号分隔 UUID" />
        </el-form-item>
        <div class="grid cols-3 compact-grid">
          <el-form-item label="融合策略">
            <el-select v-model="retrievalTest.fusion" clearable class="full-width" placeholder="沿用智能体配置">
              <el-option label="RRF 融合" value="rrf" />
              <el-option label="分数排序" value="score" />
            </el-select>
          </el-form-item>
          <el-form-item label="重排策略">
            <el-select v-model="retrievalTest.rerank" clearable class="full-width" placeholder="沿用智能体配置">
              <el-option label="MMR 多样性重排" value="mmr" />
              <el-option label="不重排" value="none" />
            </el-select>
          </el-form-item>
          <el-form-item label="候选上限">
            <el-input-number v-model="retrievalTest.maxCandidates" :min="1" class="full-width" />
          </el-form-item>
        </div>
        <el-form-item>
          <el-button type="primary" round :loading="retrievalTesting" native-type="submit">
            {{ retrievalTesting ? '测试中...' : '运行检索测试' }}
          </el-button>
        </el-form-item>
      </el-form>

      <template v-if="retrievalResult">
        <div class="meta-bar" style="margin-top: 16px">
          路由库 {{ retrievalResult.routedLibraryIds?.length || 0 }} 个 · 候选 {{ retrievalResult.candidateCount }} 个 ·
          证据 {{ retrievalResult.evidence?.length || 0 }} 个 · context {{ retrievalResult.contextTokens }} token
        </div>
        <div v-for="evidence in retrievalResult.evidence || []" :key="evidence.evidenceId" class="evidence-item" style="margin-top: 12px">
          <div class="row-title">{{ shortId(evidence.documentId, 10) }} · chunk {{ shortId(evidence.chunkId, 8) }}</div>
          <div class="row-meta">score {{ percent(evidence.score, 0) }} · 库 {{ shortId(evidence.libraryId, 10) }}</div>
          <p class="muted evidence-copy">{{ evidence.content }}</p>
        </div>
      </template>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import PageCard from '../components/PageCard.vue';
import {
  createAgent,
  createAgentVersion,
  disableAgentVersion,
  listAgentVersions,
  listAgents,
  listLibraries,
  listSceneRulePresets,
  listTokenizerProfiles,
  publishAgentVersion,
  runRetrievalTest
} from '../api';
import { formatNumber, percent, shortId } from '../format';

const agents = ref([]);
const agentVersions = ref([]);
const versionAgentId = ref('');
const loadingVersions = ref(false);
const creatingVersion = ref(false);
const availableLibraries = ref([]);
const tokenizerProfiles = ref([]);
const sceneRulePresets = ref([
  { code: 'technical_support', name: '技术支持', description: '步骤化排障、版本与环境信息说明', config: { retrieval: { maxEvidence: 12 }, answer: { maxContextTokens: 4096 } } },
  { code: 'internal_knowledge_assistant', name: '内部知识助手', description: '面向内部员工的知识问答场景', config: { retrieval: { maxEvidence: 12 }, answer: { maxContextTokens: 4096 } } }
]);
const loading = ref(false);
const retrievalTesting = ref(false);
const message = ref('');
const messageType = ref('success');
const selectedLibraryIds = ref([]);
const retrievalResult = ref(null);
const retrievalTest = ref({
  agentId: '',
  question: '如何安装 PostgreSQL 和 pgvector？',
  debugLibraryIdsText: '',
  maxContextTokens: 4096,
  fusion: 'rrf',
  rerank: 'mmr',
  maxCandidates: 24,
  balanceAcrossLibraries: true
});
const form = ref({
  tenantId: 'default',
  name: '研发支持助手',
  scenePresetCode: 'technical_support',
  chatTokenizerProfileId: '',
  systemPrompt: '请基于证据回答，并保留引用。'
});
const versionForm = ref({
  scenePresetCode: 'technical_support',
  libraryIds: []
});

const selectedPreset = computed(() => sceneRulePresets.value.find(item => item.code === form.value.scenePresetCode));
const publishedCount = computed(() => agents.value.filter(item => item.published).length);

async function loadPresets() {
  try {
    const presets = await listSceneRulePresets();
    if (Array.isArray(presets) && presets.length > 0) {
      sceneRulePresets.value = presets;
    }
    if (!sceneRulePresets.value.some(item => item.code === form.value.scenePresetCode)) {
      form.value.scenePresetCode = sceneRulePresets.value[0]?.code || 'internal_knowledge_assistant';
    }
    versionForm.value.scenePresetCode = form.value.scenePresetCode;
  } catch (error) {
    showMessage(`场景规则预设加载失败：${error.message}`, 'error');
  }
}

async function refresh() {
  try {
    const [agentList, libraries, profiles] = await Promise.all([
      listAgents({ tenantId: form.value.tenantId }),
      listLibraries({ tenantId: form.value.tenantId }),
      listTokenizerProfiles()
    ]);
    agents.value = agentList;
    availableLibraries.value = libraries;
    tokenizerProfiles.value = profiles;
    if (!retrievalTest.value.agentId && agentList.length > 0) {
      retrievalTest.value.agentId = agentList[0].agentId;
    }
    if (!versionAgentId.value && agentList.length > 0) {
      versionAgentId.value = agentList[0].agentId;
      await loadVersions();
    }
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function loadVersions() {
  if (!versionAgentId.value) {
    agentVersions.value = [];
    return;
  }
  loadingVersions.value = true;
  try {
    agentVersions.value = await listAgentVersions(versionAgentId.value);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingVersions.value = false;
  }
}

async function submitVersion() {
  if (!versionAgentId.value) {
    showMessage('请先选择智能体', 'error');
    return;
  }
  creatingVersion.value = true;
  try {
    await createAgentVersion(versionAgentId.value, {
      scenePresetCode: versionForm.value.scenePresetCode,
      libraryIds: versionForm.value.libraryIds,
      routingPolicy: { mode: 'selected_libraries' },
      retrievalPolicy: { topKPerLibrary: 8 },
      answerPolicy: { citationRequired: true, refuseWhenEvidenceLow: true },
      systemPrompt: form.value.systemPrompt,
      chatTokenizerProfileId: form.value.chatTokenizerProfileId || null
    });
    showMessage('智能体版本已创建', 'success');
    await loadVersions();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    creatingVersion.value = false;
  }
}

async function publishVersion(row) {
  try {
    await publishAgentVersion(versionAgentId.value, row.agentVersionId);
    showMessage(`版本 v${row.version} 已发布`, 'success');
    await Promise.all([refresh(), loadVersions()]);
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function disableVersion(row) {
  try {
    await disableAgentVersion(versionAgentId.value, row.agentVersionId);
    showMessage(`版本 v${row.version} 已禁用`, 'success');
    await loadVersions();
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function submit() {
  loading.value = true;
  try {
    await createAgent({
      ...form.value,
      chatTokenizerProfileId: form.value.chatTokenizerProfileId || null,
      libraryIds: selectedLibraryIds.value,
      routingPolicy: { mode: 'selected_libraries' },
      retrievalPolicy: { topKPerLibrary: 8 },
      answerPolicy: { citationRequired: true, refuseWhenEvidenceLow: true }
    });
    showMessage('知识智能体创建成功', 'success');
    await refresh();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function submitRetrievalTest() {
  if (!retrievalTest.value.agentId) {
    showMessage('请先选择智能体', 'error');
    return;
  }
  retrievalTesting.value = true;
  try {
    retrievalResult.value = await runRetrievalTest(retrievalTest.value.agentId, {
      question: retrievalTest.value.question,
      debugLibraryIds: retrievalTest.value.debugLibraryIdsText.split(',').map(item => item.trim()).filter(Boolean),
      retrievalPolicyOverride: buildRetrievalPolicyOverride(),
      answerPolicyOverride: { maxContextTokens: retrievalTest.value.maxContextTokens }
    });
    showMessage('检索测试完成', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    retrievalTesting.value = false;
  }
}

function showMessage(text, type) {
  message.value = text || '操作失败';
  messageType.value = type;
}

function buildRetrievalPolicyOverride() {
  const policy = {};
  if (retrievalTest.value.fusion) policy.fusion = retrievalTest.value.fusion;
  if (retrievalTest.value.rerank) policy.rerank = retrievalTest.value.rerank;
  if (retrievalTest.value.maxCandidates) policy.maxCandidates = retrievalTest.value.maxCandidates;
  if (retrievalTest.value.balanceAcrossLibraries !== null) {
    policy.balanceAcrossLibraries = retrievalTest.value.balanceAcrossLibraries;
  }
  return policy;
}

onMounted(async () => {
  await loadPresets();
  await refresh();
});
</script>
