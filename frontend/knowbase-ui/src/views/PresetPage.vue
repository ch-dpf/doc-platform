<template>
  <div class="page-wrap page-wrap--fluid page-stack">
    <el-alert
      v-if="message"
      :title="message"
      :type="messageType === 'success' ? 'success' : 'error'"
      show-icon
      closable
      @close="message = ''"
    />

    <div class="stat-grid">
      <article class="stat-card stat-card--primary">
        <span class="stat-card__label">库类型预设</span>
        <span class="stat-card__value">{{ formatNumber(libraryTotal) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">场景规则预设</span>
        <span class="stat-card__value">{{ formatNumber(sceneTotal) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">租户自定义</span>
        <span class="stat-card__value">{{ formatNumber(customPresetCount) }}</span>
      </article>
    </div>

    <PageCard title="预设管理" subtitle="查看、创建与删除库类型预设和场景规则预设，浏览完整配置。">
      <template #actions>
        <el-button round @click="refreshAll">刷新</el-button>
      </template>

      <div class="filter-panel">
        <el-form :inline="true" class="filter-form" @submit.prevent="search">
          <el-form-item label="租户 ID">
            <el-input v-model="tenantId" style="width: 140px" clearable @change="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round @click="search">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="库类型预设" name="library">
          <el-table v-if="libraryPresets.length" :data="libraryPresets" class="data-table" stripe>
            <el-table-column prop="name" label="名称" min-width="150">
              <template #default="{ row }">
                {{ row.name }}
                <div class="row-meta">{{ row.code }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.builtIn ? 'info' : 'success'" effect="plain">
                  {{ row.builtIn ? '内置' : '自定义' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="chunk / overlap" width="130">
              <template #default="{ row }">
                {{ row.config?.chunkMaxTokens ?? '--' }} / {{ row.config?.chunkOverlapTokens ?? '--' }}
              </template>
            </el-table-column>
            <el-table-column label="TopK" width="70">
              <template #default="{ row }">{{ row.config?.retrievalTopK ?? '--' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail('library', row)">详情</el-button>
                <el-button
                  link
                  type="danger"
                  :disabled="row.builtIn"
                  @click="confirmDelete('library', row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="!libraryLoading" description="暂无库类型预设" />
          <div v-if="libraryTotal > 0" class="table-pagination">
            <el-pagination
              v-model:current-page="libraryPagination.page"
              v-model:page-size="libraryPagination.size"
              :total="libraryTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="loadLibraryPresets"
              @size-change="handleLibraryPageSizeChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="场景规则预设" name="scene">
          <el-table v-if="scenePresets.length" :data="scenePresets" class="data-table" stripe>
            <el-table-column prop="name" label="名称" min-width="150">
              <template #default="{ row }">
                {{ row.name }}
                <div class="row-meta">{{ row.code }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="类型" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.builtIn ? 'info' : 'success'" effect="plain">
                  {{ row.builtIn ? '内置' : '自定义' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="证据 / 上下文" width="130">
              <template #default="{ row }">
                {{ row.config?.retrieval?.maxEvidence ?? '--' }} / {{ row.config?.answer?.maxContextTokens ?? '--' }}
              </template>
            </el-table-column>
            <el-table-column label="引用" width="80">
              <template #default="{ row }">
                {{ row.config?.answer?.citationRequired ? '必须' : '可选' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail('scene', row)">详情</el-button>
                <el-button
                  link
                  type="danger"
                  :disabled="row.builtIn"
                  @click="confirmDelete('scene', row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else-if="!sceneLoading" description="暂无场景规则预设" />
          <div v-if="sceneTotal > 0" class="table-pagination">
            <el-pagination
              v-model:current-page="scenePagination.page"
              v-model:page-size="scenePagination.size"
              :total="sceneTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="loadScenePresets"
              @size-change="handleScenePageSizeChange"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </PageCard>

    <div class="grid cols-2">
      <PageCard :title="createTitle" :subtitle="createSubtitle">
        <el-form label-position="top" @submit.prevent="submitCreate">
          <el-form-item label="预设编码" required>
            <el-input v-model="createForm.code" placeholder="例如 custom_docs" />
          </el-form-item>
          <el-form-item label="预设名称" required>
            <el-input v-model="createForm.name" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="createForm.description" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="配置 JSON" required>
            <el-input
              v-model="createForm.configText"
              type="textarea"
              :rows="12"
              class="config-editor"
              spellcheck="false"
            />
          </el-form-item>
          <el-form-item>
            <el-button round @click="resetCreateForm">重置模板</el-button>
            <el-button type="primary" round :loading="creating" native-type="submit">创建预设</el-button>
          </el-form-item>
        </el-form>
      </PageCard>

      <PageCard title="配置说明" subtitle="创建租户自定义预设时，可参考内置预设详情中的 JSON 结构。">
        <div v-if="activeTab === 'library'" class="guide-stack">
          <div class="guide-item">
            <strong>embeddingProvider / embeddingModel</strong>
            <p>指定向量模型提供方与模型名，需与 Tokenizer Profile 保持一致。</p>
          </div>
          <div class="guide-item">
            <strong>chunkMaxTokens / chunkOverlapTokens</strong>
            <p>控制入库分块大小与重叠，影响召回粒度与索引体积。</p>
          </div>
          <div class="guide-item">
            <strong>documentProfiles</strong>
            <p>按内容族声明 parserCode、chunkingStrategy 与 metadataSchema，支持异构文档混库。</p>
          </div>
        </div>
        <div v-else class="guide-stack">
          <div class="guide-item">
            <strong>routing</strong>
            <p>定义多库路由模式，默认 selected_libraries 表示使用智能体绑定的库集合。</p>
          </div>
          <div class="guide-item">
            <strong>retrieval</strong>
            <p>控制每库 TopK、候选融合（RRF/MMR）、跨库均衡与 contentFamily 权重。</p>
          </div>
          <div class="guide-item">
            <strong>answer</strong>
            <p>定义引用要求、低证据拒答策略与上下文 token 预算。</p>
          </div>
        </div>
      </PageCard>
    </div>

    <PresetDetailDrawer
      :visible="detailVisible"
      :kind="detailKind"
      :preset-code="detailCode"
      :tenant-id="tenantId"
      :fallback-preset="detailFallback"
      @close="detailVisible = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { ElMessageBox } from 'element-plus';
import PageCard from '../components/PageCard.vue';
import PresetDetailDrawer from '../components/PresetDetailDrawer.vue';
import {
  createLibraryTypePreset,
  createSceneRulePreset,
  deleteLibraryTypePreset,
  deleteSceneRulePreset,
  pageLibraryTypePresets,
  pageSceneRulePresets
} from '../api';
import { requestContext } from '../context';
import { formatNumber } from '../format';

const LIBRARY_CONFIG_TEMPLATE = `{
  "embeddingProvider": "ollama",
  "embeddingModel": "bge-m3",
  "embeddingDimension": 1024,
  "chunkMaxTokens": 512,
  "chunkOverlapTokens": 64,
  "retrievalTopK": 8,
  "documentProfiles": [
    {
      "code": "default_markdown",
      "contentFamily": "RICH_TEXT",
      "parserCode": "markdown-structure",
      "chunkingStrategy": "structure_token_window",
      "metadataSchema": {},
      "options": {}
    }
  ]
}`;

const SCENE_CONFIG_TEMPLATE = `{
  "systemPrompt": "你是企业知识助手，请基于检索到的证据准确回答。",
  "routing": {
    "mode": "selected_libraries",
    "maxLibraries": 8
  },
  "retrieval": {
    "topKPerLibrary": 6,
    "maxCandidates": 24,
    "maxEvidence": 8,
    "fusion": "rrf",
    "rerank": "mmr"
  },
  "answer": {
    "citationRequired": true,
    "refuseWhenEvidenceLow": true,
    "minEvidenceCount": 1,
    "maxContextTokens": 2048
  },
  "citation": {
    "granularity": "chunk",
    "groupByLibrary": true
  }
}`;

const activeTab = ref('library');
const tenantId = ref(requestContext.tenantId || 'default');
const message = ref('');
const messageType = ref('success');

const libraryPresets = ref([]);
const libraryTotal = ref(0);
const libraryLoading = ref(false);
const libraryPagination = ref({ page: 1, size: 10 });

const scenePresets = ref([]);
const sceneTotal = ref(0);
const sceneLoading = ref(false);
const scenePagination = ref({ page: 1, size: 10 });

const creating = ref(false);
const createForm = ref({
  code: '',
  name: '',
  description: '',
  configText: LIBRARY_CONFIG_TEMPLATE
});

const detailVisible = ref(false);
const detailKind = ref('library');
const detailCode = ref('');
const detailFallback = ref(null);

const createTitle = computed(() => (activeTab.value === 'library' ? '创建库类型预设' : '创建场景规则预设'));
const createSubtitle = computed(() => (
  activeTab.value === 'library'
    ? '为当前租户新增自定义库类型模板，供建库时选择。'
    : '为当前租户新增自定义场景规则，供智能体版本绑定。'
));
const customPresetCount = computed(() => {
  const libraryCustom = libraryPresets.value.filter(item => !item.builtIn).length;
  const sceneCustom = scenePresets.value.filter(item => !item.builtIn).length;
  return libraryCustom + sceneCustom;
});

watch(activeTab, () => {
  resetCreateForm();
});

function showMessage(text, type = 'success') {
  message.value = text || '操作失败';
  messageType.value = type;
}

function resetCreateForm() {
  createForm.value = {
    code: activeTab.value === 'library' ? `custom_library_${Date.now().toString().slice(-4)}` : `custom_scene_${Date.now().toString().slice(-4)}`,
    name: activeTab.value === 'library' ? '自定义文档库' : '自定义问答场景',
    description: '',
    configText: activeTab.value === 'library' ? LIBRARY_CONFIG_TEMPLATE : SCENE_CONFIG_TEMPLATE
  };
}

async function loadLibraryPresets() {
  libraryLoading.value = true;
  try {
    const data = await pageLibraryTypePresets({
      tenantId: tenantId.value,
      page: libraryPagination.value.page,
      size: libraryPagination.value.size
    });
    libraryPresets.value = data.items ?? [];
    libraryTotal.value = data.total ?? 0;
    libraryPagination.value.page = data.page ?? libraryPagination.value.page;
    libraryPagination.value.size = data.size ?? libraryPagination.value.size;
  } catch (error) {
    showMessage(`库类型预设加载失败：${error.message}`, 'error');
  } finally {
    libraryLoading.value = false;
  }
}

async function loadScenePresets() {
  sceneLoading.value = true;
  try {
    const data = await pageSceneRulePresets({
      tenantId: tenantId.value,
      page: scenePagination.value.page,
      size: scenePagination.value.size
    });
    scenePresets.value = data.items ?? [];
    sceneTotal.value = data.total ?? 0;
    scenePagination.value.page = data.page ?? scenePagination.value.page;
    scenePagination.value.size = data.size ?? scenePagination.value.size;
  } catch (error) {
    showMessage(`场景规则预设加载失败：${error.message}`, 'error');
  } finally {
    sceneLoading.value = false;
  }
}

async function refreshAll() {
  await Promise.all([loadLibraryPresets(), loadScenePresets()]);
}

function search() {
  libraryPagination.value.page = 1;
  scenePagination.value.page = 1;
  refreshAll();
}

function handleTabChange() {
  resetCreateForm();
}

function handleLibraryPageSizeChange() {
  libraryPagination.value.page = 1;
  loadLibraryPresets();
}

function handleScenePageSizeChange() {
  scenePagination.value.page = 1;
  loadScenePresets();
}

function openDetail(kind, row) {
  detailKind.value = kind;
  detailCode.value = row.code;
  detailFallback.value = row;
  detailVisible.value = true;
}

async function confirmDelete(kind, row) {
  try {
    await ElMessageBox.confirm(
      `确定删除预设「${row.name}（${row.code}）」吗？此操作不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    );
  } catch {
    return;
  }

  try {
    if (kind === 'library') {
      await deleteLibraryTypePreset(row.code, tenantId.value);
    } else {
      await deleteSceneRulePreset(row.code, tenantId.value);
    }
    showMessage('预设已删除', 'success');
    if (detailVisible.value && detailCode.value === row.code) {
      detailVisible.value = false;
    }
    if (kind === 'library') {
      if (libraryPresets.value.length === 1 && libraryPagination.value.page > 1) {
        libraryPagination.value.page -= 1;
      }
      await loadLibraryPresets();
    } else {
      if (scenePresets.value.length === 1 && scenePagination.value.page > 1) {
        scenePagination.value.page -= 1;
      }
      await loadScenePresets();
    }
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function submitCreate() {
  let config;
  try {
    config = JSON.parse(createForm.value.configText);
  } catch {
    showMessage('配置 JSON 格式不正确，请检查后重试', 'error');
    return;
  }

  creating.value = true;
  try {
    const payload = {
      tenantId: tenantId.value,
      code: createForm.value.code.trim(),
      name: createForm.value.name.trim(),
      description: createForm.value.description?.trim() || '',
      config
    };
    if (activeTab.value === 'library') {
      await createLibraryTypePreset(payload);
      libraryPagination.value.page = 1;
      await loadLibraryPresets();
    } else {
      await createSceneRulePreset(payload);
      scenePagination.value.page = 1;
      await loadScenePresets();
    }
    showMessage('预设创建成功', 'success');
    resetCreateForm();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    creating.value = false;
  }
}

onMounted(async () => {
  resetCreateForm();
  await refreshAll();
});
</script>

<style scoped>
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.config-editor :deep(textarea) {
  font-family: Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
}

.guide-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.guide-item strong {
  display: block;
  margin-bottom: 6px;
  color: var(--dp-text);
}

.guide-item p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--dp-text-secondary);
}
</style>
