<template>
  <div class="tab-pane-body parser-config-tab">
    <section class="cfg-section">
      <header class="cfg-section__head">
        <span class="cfg-section__title">内置解析器</span>
      </header>
      <p class="cfg-section__intro">
        按文件类型选择入库解析引擎。变更后已有文档需重新解析并重索引才会生效。
      </p>

      <el-table
        :data="form.config.parsing.parserRules"
        size="small"
        stripe
        class="parser-rules-table"
        max-height="280"
      >
        <el-table-column label="文件类型" width="108">
          <template #default="{ row }">
            {{ fileTypeLabel(row.fileType) }}
          </template>
        </el-table-column>
        <el-table-column label="解析器" min-width="200">
          <template #default="{ row }">
            <el-select
              v-model="row.parserId"
              filterable
              class="full-width"
              :placeholder="'选择 ' + fileTypeLabel(row.fileType) + ' 解析器'"
            >
              <el-option
                v-for="opt in selectableOptions(row.fileType)"
                :key="opt.parserId"
                :label="opt.label"
                :value="opt.parserId"
              >
                <div class="parser-option">
                  <span class="parser-option__label">{{ opt.label }}</span>
                  <span class="parser-option__desc">{{ opt.description }}</span>
                </div>
              </el-option>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="parser-rule-desc">{{ parserDescription(row.parserId) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-collapse v-model="advancedExpanded" class="parser-advanced-collapse">
      <el-collapse-item name="advanced">
        <template #title>
          <div class="parser-advanced-collapse__head">
            <span class="parser-advanced-collapse__title">高级选项</span>
            <span class="parser-advanced-collapse__summary">{{ advancedSummary }}</span>
          </div>
        </template>
        <div class="parser-advanced-body">
          <div class="parser-advanced-row">
            <label class="parser-advanced-row__label">默认语言</label>
            <div class="parser-advanced-row__control">
              <el-input
                v-model="form.config.parsing.defaultLanguage"
                placeholder="zh-CN"
                class="parser-advanced-row__input"
              />
              <p class="field-hint">用于 OCR 与 Tika 语言提示</p>
            </div>
          </div>
          <div class="parser-advanced-row parser-advanced-row--switch">
            <label class="parser-advanced-row__label">自动检测编码</label>
            <div class="parser-advanced-row__control">
              <el-switch v-model="form.config.parsing.autoDetectEncoding" />
              <p class="field-hint">
                TXT/Markdown 等纯文本建议开启；关闭时按默认语言固定字符集（中文 GB18030，其他 UTF-8）
              </p>
            </div>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { listParserEngines } from '../api/library'
import {
  FILE_TYPE_LABELS,
  PARSER_ENGINE_OPTIONS,
  optionsForFileType,
  parserLabel
} from '../utils/parserEngines'

const props = defineProps({
  form: { type: Object, required: true }
})

/** 默认折叠；展开后内容在折叠面板内滚动，避免被弹窗裁切 */
const advancedExpanded = ref([])
const engineOptions = ref([...PARSER_ENGINE_OPTIONS])

const advancedSummary = computed(() => {
  const parsing = props.form?.config?.parsing || {}
  const lang = parsing.defaultLanguage?.trim() || 'zh-CN'
  const encoding = parsing.autoDetectEncoding !== false ? '自动检测编码' : '固定字符集'
  return `${lang} · ${encoding}`
})

onMounted(async () => {
  try {
    const { data } = await listParserEngines()
    if (Array.isArray(data) && data.length) {
      engineOptions.value = data.map((item) => ({
        parserId: item.parserId,
        label: item.label,
        description: item.description,
        recommendedFileTypes: item.recommendedFileTypes || []
      }))
    }
  } catch {
    engineOptions.value = [...PARSER_ENGINE_OPTIONS]
  }
})

function fileTypeLabel(fileType) {
  return FILE_TYPE_LABELS[fileType] || fileType || '—'
}

function selectableOptions(fileType) {
  return optionsForFileType(fileType, engineOptions.value)
}

function parserDescription(parserId) {
  const found = engineOptions.value.find((o) => o.parserId === parserId)
  return found?.description || parserLabel(parserId, engineOptions.value)
}
</script>

<style scoped>
.parser-config-tab {
  padding-bottom: 8px;
}

.parser-rules-table {
  width: 100%;
}

.parser-option {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
  padding: 2px 0;
}

.parser-option__label {
  font-size: 13px;
  color: #334155;
}

.parser-option__desc {
  font-size: 11px;
  color: #94a3b8;
}

.parser-rule-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.parser-advanced-collapse {
  margin-top: 12px;
  border: 1px solid #eef2f7;
  border-radius: 10px;
  background: #fff;
  --el-collapse-header-height: 40px;
}

.parser-advanced-collapse :deep(.el-collapse-item__header) {
  height: auto;
  min-height: 40px;
  line-height: 1.4;
  padding: 8px 12px;
  border: none;
  background: transparent;
}

.parser-advanced-collapse :deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

.parser-advanced-collapse :deep(.el-collapse-item__content) {
  padding: 0 12px 12px;
}

.parser-advanced-collapse__head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  width: 100%;
  padding-right: 8px;
}

.parser-advanced-collapse__title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.parser-advanced-collapse__summary {
  font-size: 12px;
  color: #94a3b8;
  font-weight: 400;
}

.parser-advanced-body {
  padding-top: 4px;
}

.parser-advanced-row {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 12px;
  align-items: start;
  padding: 8px 0;
}

.parser-advanced-row + .parser-advanced-row {
  border-top: 1px solid #f1f5f9;
}

.parser-advanced-row__label {
  font-size: 13px;
  color: #64748b;
  line-height: 32px;
}

.parser-advanced-row--switch .parser-advanced-row__label {
  line-height: 24px;
}

.parser-advanced-row__input {
  max-width: 220px;
}

.field-hint {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.55;
  color: #94a3b8;
}
</style>
