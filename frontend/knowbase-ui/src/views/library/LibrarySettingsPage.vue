<template>
  <PageCard title="库配置与运维" subtitle="管理本库实例的 L1 索引参数与 L2 文档路由；配置自建仓预设快照而来，修改预设模板不会自动同步。">
    <template #actions>
      <el-button round @click="$router.push({ name: 'library-retrieval-test' })">召回预览</el-button>
      <el-button round :loading="loading" @click="loadAll">刷新</el-button>
    </template>

    <el-skeleton v-if="loading && !profile" :rows="8" animated />

    <template v-else-if="profile">
      <el-alert
        v-if="library?.libraryTypePresetCode"
        type="info"
        :closable="false"
        show-icon
        class="section-gap"
        title="来源模板"
        :description="`本库创建于「${presetGuide?.name || library.libraryTypePresetCode}」预设（${library.libraryTypePresetCode}）。下方为实例副本，与预设管理中的模板独立演化。`"
      />

      <div class="section-head">
        <h4 class="section-title">L1 · Library Profile · v{{ profile.version }}</h4>
        <el-button type="primary" round @click="openProfileDialog">发布新版本</el-button>
      </div>
      <p class="helper-text">索引不变量：Embedding 模型/维度、默认 chunk 上限。变更 Embedding 将触发索引漂移，需重建索引代次。</p>
      <el-descriptions :column="2" border size="small" class="profile-block">
        <el-descriptions-item label="Embedding">{{ profile.embeddingProvider }} / {{ profile.embeddingModel }}</el-descriptions-item>
        <el-descriptions-item label="维度">{{ profile.embeddingDimension }}</el-descriptions-item>
        <el-descriptions-item label="检索 TopK">{{ profile.retrievalTopK }}</el-descriptions-item>
        <el-descriptions-item label="分块">{{ profile.chunkMaxTokens }} / overlap {{ profile.chunkOverlapTokens }}</el-descriptions-item>
        <el-descriptions-item label="promoteRecallAtK">{{ formatOption('promoteRecallAtK', '0.85') }}</el-descriptions-item>
        <el-descriptions-item label="回归允许回落">{{ formatOption('promoteRecallRegressionDeltaMax', '0.02') }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="profile.l1DriftDetected"
        type="warning"
        :closable="false"
        show-icon
        class="section-gap"
        title="检测到 L1 索引不变量漂移"
        :description="`${profile.driftMessage}（字段：${(profile.driftFields || []).join('、')}）`"
      />

      <div class="section-gap">
        <h4 class="section-title">Profile 版本历史</h4>
        <el-table v-if="profileVersions.length" :data="profileVersions" size="small" stripe>
          <el-table-column label="版本" width="72">
            <template #default="{ row }">v{{ row.version }}</template>
          </el-table-column>
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="变更" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.l1Changed" size="small" type="danger">L1</el-tag>
              <el-tag v-if="row.l2Changed" size="small" type="warning">L2</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="changedFields" label="字段" min-width="160">
            <template #default="{ row }">{{ (row.changedFields || []).join('、') || '—' }}</template>
          </el-table-column>
          <el-table-column label="建议" min-width="220">
            <template #default="{ row }">{{ (row.suggestedActions || []).join('；') || '—' }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="section-gap">
        <h4 class="section-title">Promote 评测门禁（Recall@K + 回归 delta）</h4>
        <el-alert
          v-if="evalGate"
          :type="gateAlertType"
          :closable="false"
          show-icon
          :title="gateTitle"
          :description="gateDescription"
        />
        <div v-if="baseline" class="meta-bar">回归基线 Recall@{{ baseline.hitK }} = {{ percent(baseline.recallAtK, 1) }}</div>
        <div class="action-row section-gap">
          <el-button round :loading="bootstrapping" @click="handleBootstrapSamples">导入示例评测集</el-button>
          <el-button round @click="$router.push({ name: 'library-retrieval-test', query: { tab: 'report' } })">查看评测报告</el-button>
        </div>
      </div>

      <div class="section-gap">
        <div class="section-head">
          <h4 class="section-title">L2 · Document Profile（文档类型路由）</h4>
          <el-button round @click="openDocProfileDialog()">新增</el-button>
        </div>
        <p class="helper-text">按文件扩展名与内容族选择解析器与切块策略。修改 parser/切块后请对命中文档执行「重索引」。</p>
        <el-table v-if="documentProfiles.length" :data="enrichedDocumentProfiles" size="small" stripe>
          <el-table-column label="类型" min-width="120">
            <template #default="{ row }">
              <strong>{{ row.nameZh || row.code }}</strong>
              <div class="row-meta">{{ row.code }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="contentFamily" label="族" width="110" />
          <el-table-column label="解析器" min-width="130">
            <template #default="{ row }">
              {{ parserLabel(row.parserCode) }}
              <el-tag v-if="findParser(row.parserCode)?.external" size="small" type="warning" class="mini-tag">外接</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="切块" min-width="130">
            <template #default="{ row }">
              {{ chunkingLabel(row.chunkingStrategy) }}
            </template>
          </el-table-column>
          <el-table-column label="扩展名" min-width="100" show-overflow-tooltip>
            <template #default="{ row }">{{ (row.fileExtensions || []).join(', ') || '—' }}</template>
          </el-table-column>
          <el-table-column label="启用" width="72">
            <template #default="{ row }">
              <el-tag size="small" :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDocProfileDialog(row)">编辑</el-button>
              <el-button link type="warning" @click="handleReindexProfile(row.code)">重索引</el-button>
              <el-button link type="danger" @click="handleDeleteDocProfile(row.code)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无 Document Profile" />
        <ul v-if="presetGuide?.changeImpactHintsZh?.length" class="hint-list section-gap">
          <li v-for="(hint, i) in presetGuide.changeImpactHintsZh" :key="i">{{ hint }}</li>
        </ul>
      </div>

      <div class="section-gap">
        <h4 class="section-title">批量重索引与重复检测</h4>
        <div class="action-row">
          <el-button type="warning" round :loading="reindexingFailed" @click="handleReindexFailed">重试失败文档</el-button>
        </div>
        <el-table v-if="duplicates.length" :data="duplicates" size="small" stripe class="section-gap">
          <el-table-column prop="contentHash" label="Hash" min-width="140" show-overflow-tooltip />
          <el-table-column prop="count" label="数量" width="72" />
        </el-table>
      </div>
    </template>
  </PageCard>

  <el-dialog v-model="profileDialogVisible" title="发布 Library Profile 新版本" width="620px">
    <el-form label-position="top">
      <div class="grid cols-2 compact-grid">
        <el-form-item label="Embedding Provider"><el-input v-model="profileForm.embeddingProvider" /></el-form-item>
        <el-form-item label="Embedding Model"><el-input v-model="profileForm.embeddingModel" /></el-form-item>
      </div>
      <div class="grid cols-3 compact-grid">
        <el-form-item label="维度"><el-input-number v-model="profileForm.embeddingDimension" :min="1" class="full-width" /></el-form-item>
        <el-form-item label="TopK"><el-input-number v-model="profileForm.retrievalTopK" :min="1" class="full-width" /></el-form-item>
        <el-form-item label="Chunk Max"><el-input-number v-model="profileForm.chunkMaxTokens" :min="64" class="full-width" /></el-form-item>
      </div>
      <el-form-item label="Chunk Overlap"><el-input-number v-model="profileForm.chunkOverlapTokens" :min="0" class="full-width" /></el-form-item>
      <div class="grid cols-2 compact-grid">
        <el-form-item label="promoteRecallAtK"><el-input-number v-model="profileForm.promoteRecallAtK" :min="0" :max="1" :step="0.05" class="full-width" /></el-form-item>
        <el-form-item label="回归允许回落"><el-input-number v-model="profileForm.promoteRecallRegressionDeltaMax" :min="0" :max="1" :step="0.01" class="full-width" /></el-form-item>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="profileDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingProfile" @click="saveProfileVersion">保存新版本</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="docProfileDialogVisible" :title="editingDocProfile ? '编辑 Document Profile' : '新增 Document Profile'" width="620px">
    <el-form label-position="top">
      <el-form-item label="Code（不可变）">
        <el-input v-model="docProfileForm.code" :disabled="!!editingDocProfile" placeholder="如 default_docx" />
      </el-form-item>
      <el-form-item label="Content Family（不可变）">
        <el-input v-model="docProfileForm.contentFamily" :disabled="!!editingDocProfile" placeholder="RICH_TEXT" />
      </el-form-item>
      <el-form-item label="解析器（可更换）">
        <el-select v-model="docProfileForm.parserCode" filterable class="full-width">
          <el-option
            v-for="p in catalog?.parsers || []"
            :key="p.code"
            :label="`${p.nameZh}（${p.code}）`"
            :value="p.code"
          >
            <span>{{ p.nameZh }}</span>
            <el-tag size="small" :type="p.external ? 'warning' : 'info'" class="mini-tag">{{ p.external ? '外接' : '内置' }}</el-tag>
          </el-option>
        </el-select>
        <p v-if="selectedParser" class="helper-text">{{ selectedParser.descriptionZh }}</p>
      </el-form-item>
      <el-form-item label="切块策略（可更换）">
        <el-select v-model="docProfileForm.chunkingStrategy" filterable class="full-width">
          <el-option
            v-for="s in chunkingOptions"
            :key="s.value"
            :label="s.label"
            :value="s.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="启用">
        <el-switch v-model="docProfileForm.enabled" />
      </el-form-item>
      <el-alert type="warning" :closable="false" show-icon title="变更影响" description="保存后请对命中该 Profile 的文档执行「重索引」，新上传文件将使用新策略。" />
    </el-form>
    <template #footer>
      <el-button @click="docProfileDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="savingDocProfile" @click="saveDocProfile">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import PageCard from '../../components/PageCard.vue';
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import { useIngestionCatalog } from '../../composables/useIngestionCatalog';
import {
  bootstrapRetrievalEvalSamples,
  createDocumentProfile,
  createLibraryProfileVersion,
  deleteDocumentProfile,
  getLibraryProfile,
  getLibraryTypePresetGuide,
  getPromoteEvalGate,
  getRetrievalEvalBaseline,
  listDocumentDuplicates,
  listDocumentProfiles,
  listLibraryProfileVersions,
  reindexByDocumentProfile,
  reindexFailedDocuments,
  updateDocumentProfile
} from '../../api';
import { formatDateTime, percent } from '../../format';

const { libraryId, library, reloadIndexHealth, showMessage } = useLibraryWorkspace();
const { catalog, ensureCatalog, parserLabel, chunkingLabel, findParser, findProfileTemplate } = useIngestionCatalog();

const profile = ref(null);
const presetGuide = ref(null);
const profileVersions = ref([]);
const documentProfiles = ref([]);
const evalGate = ref(null);
const baseline = ref(null);
const duplicates = ref([]);
const loading = ref(false);
const reindexingFailed = ref(false);
const bootstrapping = ref(false);
const profileDialogVisible = ref(false);
const savingProfile = ref(false);
const docProfileDialogVisible = ref(false);
const savingDocProfile = ref(false);
const editingDocProfile = ref(null);
const profileForm = ref(emptyProfileForm());
const docProfileForm = ref(emptyDocProfileForm());

const gateAlertType = computed(() => {
  if (!evalGate.value?.enabled) return 'info';
  return evalGate.value.passed ? 'success' : 'error';
});
const gateTitle = computed(() => {
  if (!evalGate.value?.enabled) return '门禁未启用';
  return evalGate.value.passed ? '评测通过，可 promote' : '评测未通过，promote 将被阻断';
});
const gateDescription = computed(() => {
  const parts = [...(evalGate.value?.messages || []), ...(evalGate.value?.failures || [])];
  if (evalGate.value?.currentRecallAtK != null) {
    parts.unshift(`当前 Recall@K = ${percent(evalGate.value.currentRecallAtK, 1)}`);
  }
  if (evalGate.value?.baselineRecallAtK != null) {
    parts.push(`基线 ${percent(evalGate.value.baselineRecallAtK, 1)}，回落 ${percent(evalGate.value.regressionDelta || 0, 1)}`);
  }
  return parts.join('；') || '请配置评测集并运行评测报告';
});

const enrichedDocumentProfiles = computed(() =>
  documentProfiles.value.map((row) => {
    const template = findProfileTemplate(row.code);
    return {
      ...row,
      nameZh: template?.nameZh,
      fileExtensions: template?.fileExtensions
    };
  })
);

const selectedParser = computed(() => findParser(docProfileForm.value.parserCode));

const chunkingOptions = computed(() => {
  const fromCatalog = (catalog.value?.documentProfiles || []).map((p) => ({
    value: p.defaultChunkingStrategy,
    label: p.chunkingStrategyLabelZh || p.defaultChunkingStrategy
  }));
  const seen = new Set();
  const options = [];
  for (const item of fromCatalog) {
    if (!seen.has(item.value)) {
      seen.add(item.value);
      options.push(item);
    }
  }
  if (docProfileForm.value.chunkingStrategy && !seen.has(docProfileForm.value.chunkingStrategy)) {
    options.push({ value: docProfileForm.value.chunkingStrategy, label: docProfileForm.value.chunkingStrategy });
  }
  return options;
});

function emptyProfileForm() {
  return {
    embeddingProvider: 'ollama',
    embeddingModel: 'bge-m3',
    embeddingDimension: 1024,
    chunkMaxTokens: 512,
    chunkOverlapTokens: 64,
    retrievalTopK: 8,
    promoteRecallAtK: 0.85,
    promoteRecallRegressionDeltaMax: 0.02
  };
}

function emptyDocProfileForm() {
  return { code: '', contentFamily: 'RICH_TEXT', parserCode: 'markdown-structure', chunkingStrategy: 'structure_token_window', enabled: true };
}

function formatOption(key, fallback) {
  const value = profile.value?.options?.[key];
  return value == null ? fallback : String(value);
}

async function loadAll() {
  loading.value = true;
  try {
    await ensureCatalog();
    const presetCode = library.value?.libraryTypePresetCode;
    const guidePromise = presetCode
      ? getLibraryTypePresetGuide(presetCode).catch(() => null)
      : Promise.resolve(null);
    const [profileData, versions, gateData, baselineData, docProfiles, dupData, guideData] = await Promise.all([
      getLibraryProfile(libraryId.value),
      listLibraryProfileVersions(libraryId.value),
      getPromoteEvalGate(libraryId.value),
      getRetrievalEvalBaseline(libraryId.value).catch(() => null),
      listDocumentProfiles(libraryId.value),
      listDocumentDuplicates(libraryId.value),
      guidePromise
    ]);
    profile.value = profileData;
    profileVersions.value = versions;
    evalGate.value = gateData;
    baseline.value = baselineData;
    documentProfiles.value = docProfiles;
    duplicates.value = dupData;
    presetGuide.value = guideData;
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

function openProfileDialog() {
  profileForm.value = {
    embeddingProvider: profile.value.embeddingProvider,
    embeddingModel: profile.value.embeddingModel,
    embeddingDimension: profile.value.embeddingDimension,
    chunkMaxTokens: profile.value.chunkMaxTokens,
    chunkOverlapTokens: profile.value.chunkOverlapTokens,
    retrievalTopK: profile.value.retrievalTopK,
    promoteRecallAtK: Number(formatOption('promoteRecallAtK', '0.85')),
    promoteRecallRegressionDeltaMax: Number(formatOption('promoteRecallRegressionDeltaMax', '0.02'))
  };
  profileDialogVisible.value = true;
}

async function saveProfileVersion() {
  savingProfile.value = true;
  try {
    const { promoteRecallAtK, promoteRecallRegressionDeltaMax, ...rest } = profileForm.value;
    await createLibraryProfileVersion(libraryId.value, {
      ...rest,
      embeddingTokenizerProfileId: profile.value.embeddingTokenizerProfileId,
      options: {
        ...(profile.value.options || {}),
        promoteRecallAtK,
        promoteRecallRegressionDeltaMax
      }
    });
    profileDialogVisible.value = false;
    showMessage('已发布 Library Profile 新版本', 'success');
    await Promise.all([loadAll(), reloadIndexHealth()]);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    savingProfile.value = false;
  }
}

function openDocProfileDialog(row = null) {
  editingDocProfile.value = row;
  docProfileForm.value = row
    ? { code: row.code, contentFamily: row.contentFamily, parserCode: row.parserCode, chunkingStrategy: row.chunkingStrategy, enabled: row.enabled }
    : emptyDocProfileForm();
  docProfileDialogVisible.value = true;
}

async function saveDocProfile() {
  savingDocProfile.value = true;
  try {
    const payload = {
      contentFamily: docProfileForm.value.contentFamily,
      parserCode: docProfileForm.value.parserCode,
      chunkingStrategy: docProfileForm.value.chunkingStrategy,
      enabled: docProfileForm.value.enabled
    };
    if (editingDocProfile.value) {
      await updateDocumentProfile(libraryId.value, docProfileForm.value.code, payload);
    } else {
      await createDocumentProfile(libraryId.value, { ...docProfileForm.value, metadataSchema: {}, options: {} });
    }
    docProfileDialogVisible.value = false;
    showMessage('Document Profile 已保存', 'success');
    await loadAll();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    savingDocProfile.value = false;
  }
}

async function handleDeleteDocProfile(code) {
  try {
    await deleteDocumentProfile(libraryId.value, code);
    showMessage('已删除', 'success');
    await loadAll();
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function handleReindexProfile(code) {
  try {
    const result = await reindexByDocumentProfile(libraryId.value, code);
    showMessage(`已提交 ${result.documentCount} 个文档重索引（${code}）`, 'success');
    await reloadIndexHealth();
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function handleReindexFailed() {
  reindexingFailed.value = true;
  try {
    const result = await reindexFailedDocuments(libraryId.value);
    showMessage(`已提交 ${result.documentCount} 个失败文档重索引`, 'success');
    await Promise.all([loadAll(), reloadIndexHealth()]);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    reindexingFailed.value = false;
  }
}

async function handleBootstrapSamples() {
  bootstrapping.value = true;
  try {
    const samples = await bootstrapRetrievalEvalSamples(libraryId.value, false);
    showMessage(`已导入 ${samples.length} 条评测样本`, 'success');
    await loadAll();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    bootstrapping.value = false;
  }
}

onMounted(loadAll);
</script>

<style scoped>
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.profile-block { margin-bottom: 16px; }
.section-gap { margin-top: 24px; }
.section-title { margin: 0; font-size: 14px; font-weight: 600; }
.action-row { display: flex; flex-wrap: wrap; gap: 12px; align-items: center; }
.meta-bar { margin-top: 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.helper-text { margin: 0 0 8px; font-size: 13px; color: var(--el-text-color-secondary); }
.row-meta { font-size: 12px; color: var(--el-text-color-secondary); }
.hint-list { margin: 8px 0 0; padding-left: 18px; font-size: 13px; color: var(--el-text-color-regular); line-height: 1.6; }
.mini-tag { margin-left: 6px; }
.full-width { width: 100%; }
</style>
