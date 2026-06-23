<template>
  <el-drawer
    :model-value="visible"
    :title="preset?.name || '预设详情'"
    size="760px"
    @close="emit('close')"
  >
    <div v-if="preset" v-loading="loading" class="drawer-stack">
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
        <div class="section-head"><h4>入库与检索参数</h4></div>
        <div class="kv-grid">
          <div class="kv-item"><span>Embedding</span><strong>{{ librarySummary.embedding }}</strong></div>
          <div class="kv-item"><span>分块上限</span><strong>{{ librarySummary.chunkMaxTokens }}</strong></div>
          <div class="kv-item"><span>重叠 token</span><strong>{{ librarySummary.chunkOverlapTokens }}</strong></div>
          <div class="kv-item"><span>检索 TopK</span><strong>{{ librarySummary.retrievalTopK }}</strong></div>
        </div>

        <div class="section-head"><h4>文档 Profile（{{ documentProfiles.length }}）</h4></div>
        <el-table v-if="documentProfiles.length" :data="documentProfiles" size="small" class="data-table">
          <el-table-column prop="code" label="编码" min-width="140" show-overflow-tooltip />
          <el-table-column prop="contentFamily" label="内容族" width="130" />
          <el-table-column prop="parserCode" label="解析器" width="130" />
          <el-table-column prop="chunkingStrategy" label="切块策略" min-width="160" show-overflow-tooltip />
        </el-table>
        <el-empty v-else description="未配置 documentProfiles" :image-size="56" />
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

      <div class="section-head"><h4>完整配置 JSON</h4></div>
      <pre class="json-block">{{ formattedConfig }}</pre>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { getLibraryTypePreset, getSceneRulePreset } from '../api';

const props = defineProps({
  visible: { type: Boolean, default: false },
  kind: { type: String, default: 'library' },
  presetCode: { type: String, default: '' },
  tenantId: { type: String, default: '' },
  fallbackPreset: { type: Object, default: null }
});

const emit = defineEmits(['close']);

const loading = ref(false);
const preset = ref(null);

const formattedConfig = computed(() => JSON.stringify(preset.value?.config ?? {}, null, 2));

const documentProfiles = computed(() => {
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

watch(
  () => [props.visible, props.presetCode, props.kind, props.tenantId],
  ([visible]) => {
    if (visible) {
      loadPreset();
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
}

.kv-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.kv-item {
  padding: 12px 14px;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: #f8fafc;
}

.kv-item span {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--dp-text-secondary);
}

.kv-item strong {
  font-size: 14px;
  color: var(--dp-text);
}

.json-block {
  margin: 0;
  padding: 14px;
  border-radius: var(--dp-radius);
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.55;
  overflow: auto;
  max-height: 360px;
  white-space: pre-wrap;
  word-break: break-word;
}

.prompt-panel {
  margin-bottom: 16px;
}
</style>
