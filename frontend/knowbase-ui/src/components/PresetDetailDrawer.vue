<template>
  <el-drawer
    :model-value="visible"
    :title="preset?.name || '预设详情'"
    size="820px"
    @close="emit('close')"
  >
    <div v-if="preset" v-loading="loading || guideLoading" class="drawer-stack">
      <div class="meta-bar">
        {{ preset.code }}
        <el-tag size="small" :type="preset.builtIn ? 'info' : 'success'" effect="plain">
          {{ preset.builtIn ? '系统内置' : '租户自定义' }}
        </el-tag>
        <el-tag size="small" :type="preset.enabled ? 'success' : 'warning'" effect="plain">
          {{ preset.enabled ? '启用' : '停用' }}
        </el-tag>
      </div>

      <p class="helper-text">{{ preset.description || '无描述' }}</p>

      <template v-if="kind === 'library'">


        <div class="section-head"><h4>入库与检索参数（L1 默认）</h4></div>
        <div class="kv-grid">
          <div class="kv-item"><span>Embedding</span><strong>{{ librarySummary.embedding }}</strong></div>
          <div class="kv-item"><span>分块上限</span><strong>{{ librarySummary.chunkMaxTokens }}</strong></div>
          <div class="kv-item"><span>重叠 token</span><strong>{{ librarySummary.chunkOverlapTokens }}</strong></div>
          <div class="kv-item"><span>检索 TopK</span><strong>{{ librarySummary.retrievalTopK }}</strong></div>
        </div>
        <p class="helper-text">建仓后可在「库配置」发布新版 Library Profile 调整；换 Embedding 需重建索引。</p>

        <div v-if="guide?.suitableFileTypesZh?.length" class="section-gap">
          <div class="section-head"><h4>适合上传</h4></div>
          <div class="tag-row">
            <el-tag v-for="item in guide.suitableFileTypesZh" :key="item" size="small" type="success" effect="plain">{{ item }}</el-tag>
          </div>
        </div>
        <div v-if="guide?.cautionFileTypesZh?.length" class="section-gap">
          <div class="section-head"><h4>需谨慎</h4></div>
          <div class="tag-row">
            <el-tag v-for="item in guide.cautionFileTypesZh" :key="item" size="small" type="warning" effect="plain">{{ item }}</el-tag>
          </div>
        </div>

        <div class="section-head section-gap"><h4>文档 Profile（L2）· {{ enrichedProfiles.length }} 项</h4></div>
        <el-table v-if="enrichedProfiles.length" :data="enrichedProfiles" size="small" class="data-table" stripe>
          <el-table-column label="类型" min-width="120">
            <template #default="{ row }">
              <strong>{{ row.nameZh || row.code }}</strong>
              <div class="row-meta">{{ row.code }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="contentFamily" label="内容族" width="120" />
          <el-table-column label="解析器" min-width="140">
            <template #default="{ row }">
              {{ row.parserNameZh || parserLabel(row.parserCode) }}
              <el-tag v-if="row.parserExternal" size="small" type="warning" class="mini-tag">外接</el-tag>
              <el-tag v-else size="small" type="info" class="mini-tag">内置</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="切块" min-width="140">
            <template #default="{ row }">
              {{ row.chunkingStrategyLabelZh || row.chunkingStrategy }}
            </template>
          </el-table-column>
          <el-table-column label="扩展名" min-width="120">
            <template #default="{ row }">
              {{ (row.fileExtensions || []).join(', ') || '—' }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="未配置 documentProfiles" :image-size="56" />

        <div class="section-head section-gap"><h4>解析器目录</h4></div>
        <el-table v-if="catalog?.parsers?.length" :data="catalog.parsers" size="small" class="data-table" stripe max-height="280">
          <el-table-column prop="nameZh" label="解析器" min-width="130" />
          <el-table-column prop="code" label="编码" width="130" />
          <el-table-column label="类型" width="72">
            <template #default="{ row }">
              <el-tag size="small" :type="row.external ? 'warning' : 'success'" effect="plain">
                {{ row.external ? '外接' : '内置' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="健康" width="110">
            <template #default="{ row }">
              <el-tooltip :content="row.health?.message || '未返回健康状态'" placement="top">
                <el-tag size="small" :type="healthTagType(row.health?.status)" effect="plain">
                  {{ healthLabel(row.health?.status) }}
                </el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="descriptionZh" label="说明" min-width="160" show-overflow-tooltip />
          <el-table-column label="扩展名" width="120" show-overflow-tooltip>
            <template #default="{ row }">{{ (row.supportedExtensions || []).join(', ') }}</template>
          </el-table-column>
        </el-table>

        <div v-if="guide?.changeImpactHintsZh?.length" class="section-gap">
          <div class="section-head"><h4>库配置变更影响</h4></div>
          <ul class="hint-list">
            <li v-for="(hint, index) in guide.changeImpactHintsZh" :key="index">{{ hint }}</li>
          </ul>
        </div>
      </template>

      <template v-else>
        <div class="section-head"><h4>问答策略摘要</h4></div>
        <div class="kv-grid">
          <div class="kv-item"><span>每库 TopK</span><strong>{{ sceneSummary.topKPerLibrary }}</strong></div>
          <div class="kv-item"><span>候选上限</span><strong>{{ sceneSummary.maxCandidates }}</strong></div>
          <div class="kv-item"><span>证据数</span><strong>{{ sceneSummary.maxEvidence }}</strong></div>
          <div class="kv-item"><span>上下文 token</span><strong>{{ sceneSummary.maxContextTokens }}</strong></div>
          <div class="kv-item"><span>必须引用</span><strong>{{ sceneSummary.citationRequired }}</strong></div>
          <div class="kv-item"><span>低证据拒答</span><strong>{{ sceneSummary.refuseWhenEvidenceLow }}</strong></div>
        </div>

        <div v-if="sceneSummary.systemPrompt" class="prompt-panel">
          <div class="section-head"><h4>System Prompt</h4></div>
          <pre class="json-block">{{ sceneSummary.systemPrompt }}</pre>
        </div>
      </template>

      <div class="section-head section-gap"><h4>完整配置 JSON</h4></div>
      <pre class="json-block">{{ formattedConfig }}</pre>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { getLibraryTypePreset, getLibraryTypePresetGuide, getSceneRulePreset, getIngestionCatalog } from '../api';

const props = defineProps({
  visible: { type: Boolean, default: false },
  kind: { type: String, default: 'library' },
  presetCode: { type: String, default: '' },
  tenantId: { type: String, default: '' },
  fallbackPreset: { type: Object, default: null }
});

const emit = defineEmits(['close']);

const loading = ref(false);
const guideLoading = ref(false);
const preset = ref(null);
const guide = ref(null);
const catalog = ref(null);

const formattedConfig = computed(() => JSON.stringify(preset.value?.config ?? {}, null, 2));

const enrichedProfiles = computed(() => {
  if (guide.value?.documentProfiles?.length) {
    return guide.value.documentProfiles;
  }
  const profiles = preset.value?.config?.documentProfiles;
  return Array.isArray(profiles) ? profiles : [];
});

const librarySummary = computed(() => {
  const config = preset.value?.config ?? {};
  const provider = config.embeddingProvider ?? '--';
  const model = config.embeddingModel ?? '--';
  return {
    embedding: `${provider} / ${model}`,
    chunkMaxTokens: config.chunkMaxTokens ?? '--',
    chunkOverlapTokens: config.chunkOverlapTokens ?? '--',
    retrievalTopK: config.retrievalTopK ?? '--'
  };
});

const sceneSummary = computed(() => {
  const config = preset.value?.config ?? {};
  const retrieval = config.retrieval ?? {};
  const answer = config.answer ?? {};
  return {
    topKPerLibrary: retrieval.topKPerLibrary ?? '--',
    maxCandidates: retrieval.maxCandidates ?? '--',
    maxEvidence: retrieval.maxEvidence ?? '--',
    maxContextTokens: answer.maxContextTokens ?? '--',
    citationRequired: answer.citationRequired === true ? '是' : answer.citationRequired === false ? '否' : '--',
    refuseWhenEvidenceLow: answer.refuseWhenEvidenceLow === true ? '是' : answer.refuseWhenEvidenceLow === false ? '否' : '--',
    systemPrompt: typeof config.systemPrompt === 'string' ? config.systemPrompt : ''
  };
});

function parserLabel(code) {
  const item = catalog.value?.parsers?.find((p) => p.code === code);
  return item?.nameZh || code;
}

function healthTagType(status) {
  if (status === 'READY') return 'success';
  if (status === 'DEGRADED') return 'warning';
  if (status === 'UNCONFIGURED') return 'danger';
  return 'info';
}

function healthLabel(status) {
  if (status === 'READY') return '可用';
  if (status === 'DEGRADED') return '降级';
  if (status === 'UNCONFIGURED') return '未配置';
  return '未知';
}

async function loadPreset() {
  if (!props.presetCode) {
    preset.value = props.fallbackPreset;
    return;
  }
  loading.value = true;
  try {
    const params = props.tenantId ? { tenantId: props.tenantId } : {};
    preset.value = props.kind === 'library'
      ? await getLibraryTypePreset(props.presetCode, params)
      : await getSceneRulePreset(props.presetCode, params);
  } catch {
    preset.value = props.fallbackPreset;
  } finally {
    loading.value = false;
  }
}

async function loadGuideAndCatalog() {
  if (props.kind !== 'library' || !props.presetCode) {
    guide.value = null;
    return;
  }
  guideLoading.value = true;
  try {
    const params = props.tenantId ? { tenantId: props.tenantId } : {};
    const [guideData, catalogData] = await Promise.all([
      getLibraryTypePresetGuide(props.presetCode, params),
      catalog.value ? Promise.resolve(catalog.value) : getIngestionCatalog()
    ]);
    guide.value = guideData;
    catalog.value = catalogData;
  } catch {
    guide.value = null;
  } finally {
    guideLoading.value = false;
  }
}

watch(
  () => [props.visible, props.presetCode, props.kind, props.tenantId],
  ([visible]) => {
    if (visible) {
      loadPreset();
      loadGuideAndCatalog();
    }
  }
);
</script>

<style scoped>
.drawer-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-head h4 {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.section-gap {
  margin-top: 8px;
}

.kv-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.kv-item {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.kv-item span {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hint-list {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}

.mini-tag {
  margin-left: 4px;
  vertical-align: middle;
}

.row-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.json-block {
  margin: 0;
  padding: 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  font-size: 12px;
  overflow: auto;
  max-height: 320px;
}
</style>
