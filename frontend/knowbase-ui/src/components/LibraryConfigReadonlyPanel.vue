<template>
  <div class="library-config-readonly">
    <section class="cfg-section">
      <header class="cfg-section__head">
        <span class="cfg-section__title">基本信息</span>
      </header>
      <div class="settings-rules-grid">
        <div class="settings-kv">
          <span class="settings-kv__label">知识库名称</span>
          <span class="settings-kv__value">{{ summary.name || '—' }}</span>
        </div>
        <div class="settings-kv settings-kv--wide">
          <span class="settings-kv__label">描述</span>
          <span class="settings-kv__value">{{ summary.description || '—' }}</span>
        </div>
        <div class="settings-kv settings-kv--wide">
          <span class="settings-kv__label">标签</span>
          <span class="settings-kv__value">
            <template v-if="summary.tags?.length">
              <el-tag
                v-for="t in summary.tags"
                :key="t"
                size="small"
                effect="plain"
                class="readonly-tag"
              >{{ t }}</el-tag>
            </template>
            <template v-else>—</template>
          </span>
        </div>
      </div>
    </section>

    <section class="cfg-section cfg-section--chunk">
      <header class="cfg-section__head">
        <span class="cfg-section__title">分块配置</span>
      </header>
      <el-collapse v-model="chunkCollapseExpanded" class="strategy-collapse">
        <el-collapse-item name="types">
          <template #title>
            <div class="strategy-collapse__head">
              <span class="chunk-part__label">分块策略</span>
              <span class="strategy-collapse__summary">{{ strategyBrief }}</span>
            </div>
          </template>
          <div v-if="chunkStrategyRows.length" class="strategy-card-grid">
            <div
              v-for="row in chunkStrategyRows"
              :key="row.fileType"
              class="strategy-card"
            >
              <div class="strategy-card__head">
                <span class="strategy-card__type">{{ row.fileTypeLabel }}</span>
                <el-tag
                  :type="strategyTagType(row.chunkingStrategy)"
                  size="small"
                  effect="light"
                  round
                >
                  {{ row.chunkingStrategyLabel }}
                </el-tag>
              </div>
              <p class="strategy-card__note">{{ row.parsingNote }}</p>
              <span
                v-if="row.hierarchicalWhenApplicable"
                class="strategy-card__badge"
              >可启用父子块</span>
            </div>
          </div>
          <p v-else class="chunk-part__empty">{{ strategyEmptyText }}</p>
        </el-collapse-item>

        <el-collapse-item name="params">
          <template #title>
            <div class="strategy-collapse__head">
              <span class="chunk-part__label">分块参数</span>
              <span class="strategy-collapse__summary">{{ chunkParamsBrief }}</span>
            </div>
          </template>
          <div class="settings-rules-grid settings-rules-grid--dense">
            <div class="settings-kv">
              <span class="settings-kv__label">分块大小</span>
              <span class="settings-kv__value">{{ summary.chunkSize }} 字</span>
            </div>
            <div class="settings-kv">
              <span class="settings-kv__label">分块重叠</span>
              <span class="settings-kv__value">{{ summary.chunkOverlap }} 字</span>
            </div>
            <div class="settings-kv">
              <span class="settings-kv__label">父子块</span>
              <span class="settings-kv__value">{{ formatBool(summary.hierarchicalChunkingEnabled !== false) }}</span>
            </div>
            <div class="settings-kv settings-kv--wide">
              <span class="settings-kv__label">自定义分隔符</span>
              <span class="settings-kv__value">{{ formatChunkDelimiter(summary.chunkDelimiter) }}</span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </section>

    <section class="cfg-section">
      <header class="cfg-section__head">
        <span class="cfg-section__title">索引配置</span>
      </header>
      <div class="settings-rules-grid">
        <div class="settings-kv settings-kv--wide">
          <span class="settings-kv__label">Embedding</span>
          <span class="settings-kv__value">
            {{ labelForEmbeddingModel(summary.embeddingModel) }}
            （{{ summary.embeddingDimension }} 维）
          </span>
        </div>
      </div>
    </section>

    <section class="cfg-section">
      <header class="cfg-section__head">
        <span class="cfg-section__title">检索配置</span>
      </header>
      <div class="settings-rules-grid">
        <div class="settings-kv">
          <span class="settings-kv__label">混合检索</span>
          <span class="settings-kv__value">{{ formatRetrievalHybrid(summary) }}</span>
        </div>
        <div class="settings-kv">
          <span class="settings-kv__label">重排序</span>
          <span class="settings-kv__value">{{ formatRetrievalRerank(summary) }}</span>
        </div>
        <div
          v-if="summary.rerankEnabled !== false"
          class="settings-kv settings-kv--wide"
        >
          <span class="settings-kv__label">Rerank 模型</span>
          <span class="settings-kv__value">
            {{ formatRerankModelSummary(summary.rerankModel, summary.embeddingModel) }}
          </span>
        </div>
        <div class="settings-kv">
          <span class="settings-kv__label">相似度阈值</span>
          <span class="settings-kv__value">{{ summary.similarityThreshold }}</span>
        </div>
        <div class="settings-kv">
          <span class="settings-kv__label">默认 Top K</span>
          <span class="settings-kv__value">{{ summary.defaultTopK }}</span>
        </div>
        <div class="settings-kv settings-kv--wide">
          <span class="settings-kv__label">过滤字段</span>
          <span class="settings-kv__value">{{ formatMetadataFilterFields(summary.metadataFilterFields) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { formatBool } from '../utils/libraryConfig'
import { labelForEmbeddingModel } from '../utils/embeddingModels'
import {
  chunkParamsSummaryLine,
  formatChunkDelimiter,
  formatMetadataFilterFields,
  formatRerankModelSummary,
  formatRetrievalHybrid,
  formatRetrievalRerank,
  strategyRowsBrief
} from '../utils/libraryConfigSummary'

const STRATEGY_TAG_TYPES = {
  'paragraph-first': 'info',
  'heading-level': 'primary',
  'fixed-char': 'warning',
  semantic: 'success'
}

const props = defineProps({
  summary: { type: Object, required: true },
  chunkStrategyRows: { type: Array, default: () => [] },
  strategyEmptyText: { type: String, default: '加载策略摘要…' }
})

const chunkCollapseExpanded = ref([])

const strategyBrief = computed(() => {
  if (!props.chunkStrategyRows.length) return props.strategyEmptyText
  return strategyRowsBrief(props.chunkStrategyRows)
})

const chunkParamsBrief = computed(() => chunkParamsSummaryLine(props.summary))

function strategyTagType(strategy) {
  return STRATEGY_TAG_TYPES[strategy] || 'info'
}
</script>

<style scoped>
.library-config-readonly {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cfg-section {
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}

.cfg-section__head {
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

.cfg-section__title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.settings-rules-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.settings-rules-grid--dense {
  margin-top: 4px;
}

.settings-kv--wide {
  grid-column: 1 / -1;
}

.settings-kv__label {
  display: block;
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 2px;
}

.settings-kv__value {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  word-break: break-word;
}

.readonly-tag + .readonly-tag {
  margin-left: 6px;
}

.strategy-collapse {
  border: none;
  --el-collapse-header-height: 36px;
}

.strategy-collapse :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 36px;
  line-height: 1.4;
  padding: 4px 0;
  border: none;
  background: transparent;
}

.strategy-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.strategy-collapse :deep(.el-collapse-item__content) {
  padding: 0 0 4px;
}

.strategy-collapse :deep(.el-collapse-item + .el-collapse-item) {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
}

.strategy-collapse__head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding-right: 8px;
  text-align: left;
}

.strategy-collapse__summary {
  font-size: 11px;
  font-weight: 400;
  color: #94a3b8;
  line-height: 1.35;
}

.chunk-part__label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.chunk-part__empty {
  margin: 0;
  font-size: 12px;
  color: #cbd5e1;
}

.strategy-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.strategy-card {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.strategy-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.strategy-card__type {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.strategy-card__note {
  margin: 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}

.strategy-card__badge {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  color: #0ea5e9;
}
</style>
