<template>
  <el-drawer
    v-model="visible"
    :title="`配置 · ${form.name || '知识库'}`"
    size="600px"
    destroy-on-close
    class="library-settings-drawer"
    @closed="onClosed"
  >
    <el-alert
      v-if="lockPipeline"
      class="lock-alert"
      type="warning"
      :closable="false"
      show-icon
      title="流水线配置已锁定"
      description="库内已有文档，「处理」中的解析、清洗、分块与向量化项不可修改。"
    />

    <el-form v-loading="loading" label-width="100px" label-position="right" class="cfg-form">
      <el-tabs v-model="activeTab" class="settings-tabs">
        <el-tab-pane label="基本" name="basic">
          <div class="tab-pane-body">
            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">基本信息</span>
              </header>
              <el-form-item label="名称" required>
                <el-input v-model="form.name" placeholder="知识库名称" />
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="2" placeholder="可选" />
              </el-form-item>
              <el-form-item label="标签">
                <el-select
                  v-model="form.tags"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  placeholder="输入后回车添加"
                  class="full-width"
                />
              </el-form-item>
            </section>

            <section class="cfg-section cfg-section--muted">
              <header class="cfg-section__head">
                <span class="cfg-section__title">系统信息</span>
                <span class="cfg-section__hint">只读</span>
              </header>
              <div class="meta-tags">
                <el-tag size="small" effect="plain">v{{ form.config.configVersion ?? 1 }}</el-tag>
                <el-tag size="small" effect="plain">{{ wizardModeLabel }}</el-tag>
                <el-tag size="small" type="info" effect="plain">{{ presetLabel }}</el-tag>
                <el-tag size="small" effect="plain">{{ readonly.metadataDbType }}</el-tag>
              </div>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="数据与容量" name="ingest">
          <div class="tab-pane-body">
            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">数据类型</span>
              </header>
              <el-form-item label="支持类型">
                <el-checkbox-group v-model="form.config.ingestAccess.supportedFileTypes" class="type-chips">
                  <el-checkbox
                    v-for="opt in FILE_TYPE_OPTIONS"
                    :key="opt.value"
                    :label="opt.value"
                  >
                    {{ opt.label }}
                  </el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </section>

            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">容量上限</span>
              </header>
              <div class="metric-grid">
                <div class="metric-field">
                  <label class="metric-field__label">文档数</label>
                  <el-input-number
                    v-model="form.config.ingestAccess.capacityLimits.maxDocuments"
                    :min="100"
                    :max="1000000"
                    controls-position="right"
                    class="full-width"
                  />
                </div>
                <div class="metric-field">
                  <label class="metric-field__label">总大小 (GB)</label>
                  <el-input-number
                    v-model="capacityGb"
                    :min="1"
                    :max="1024"
                    controls-position="right"
                    class="full-width"
                  />
                </div>
                <div class="metric-field">
                  <label class="metric-field__label">向量条目</label>
                  <el-input-number
                    v-model="form.config.ingestAccess.capacityLimits.maxChunkEntries"
                    :min="1000"
                    :max="5000000"
                    :step="1000"
                    controls-position="right"
                    class="full-width"
                  />
                </div>
              </div>
            </section>

            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">版本管理</span>
              </header>
              <el-form-item label="启用">
                <el-switch v-model="form.config.ingestAccess.versionPolicy.enabled" />
              </el-form-item>
              <el-form-item v-if="form.config.ingestAccess.versionPolicy.enabled" label="更新策略">
                <el-radio-group v-model="form.config.ingestAccess.versionPolicy.updateStrategy" class="radio-row">
                  <el-radio label="overwrite">覆盖</el-radio>
                  <el-radio label="incremental">增量</el-radio>
                  <el-radio label="keep-history">保留历史</el-radio>
                </el-radio-group>
              </el-form-item>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane name="pipeline">
          <template #label>
            <span class="tab-label">
              处理
              <el-tag v-if="lockPipeline" size="small" type="info" effect="plain" class="tab-lock-tag">锁定</el-tag>
            </span>
          </template>
          <div class="tab-pane-body">
            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">解析</span>
              </header>
              <div class="switch-row">
                <el-form-item label="OCR">
                  <el-switch v-model="form.config.parsing.ocrEnabled" :disabled="lockPipeline" />
                  <p v-if="fieldImpactHint('parsing.ocrEnabled')" class="settings-form__tip">{{ fieldImpactHint('parsing.ocrEnabled') }}</p>
                </el-form-item>
                <el-form-item label="默认语言">
                  <el-select
                    v-model="form.config.parsing.defaultLanguage"
                    :disabled="lockPipeline"
                    class="select-sm"
                  >
                    <el-option label="中文" value="zh-CN" />
                    <el-option label="英文" value="en-US" />
                  </el-select>
                </el-form-item>
                <el-form-item label="自动识别编码">
                  <el-switch v-model="form.config.parsing.autoDetectEncoding" :disabled="lockPipeline" />
                </el-form-item>
              </div>
              <div class="metric-grid">
                <div class="metric-field">
                  <label class="metric-field__label">表格提取</label>
                  <el-select
                    v-model="form.config.parsing.tableExtraction"
                    :disabled="lockPipeline"
                    class="full-width"
                  >
                    <el-option label="纯文本" value="text-only" />
                    <el-option label="结构化" value="structured" />
                    <el-option label="跳过" value="skip" />
                  </el-select>
                  <p v-if="fieldImpactHint('parsing.tableExtraction')" class="settings-form__tip">{{ fieldImpactHint('parsing.tableExtraction') }}</p>
                </div>
                <div class="metric-field">
                  <label class="metric-field__label">图片提取</label>
                  <el-select
                    v-model="form.config.parsing.imageExtraction"
                    :disabled="lockPipeline"
                    class="full-width"
                  >
                    <el-option label="OCR 描述" value="ocr-caption" />
                    <el-option label="跳过" value="skip" />
                  </el-select>
                  <p v-if="fieldImpactHint('parsing.imageExtraction')" class="settings-form__tip">{{ fieldImpactHint('parsing.imageExtraction') }}</p>
                </div>
                <div class="metric-field">
                  <label class="metric-field__label">公式提取</label>
                  <el-select
                    v-model="form.config.parsing.formulaExtraction"
                    :disabled="lockPipeline"
                    class="full-width"
                  >
                    <el-option label="LaTeX" value="latex" />
                    <el-option label="跳过" value="skip" />
                  </el-select>
                </div>
              </div>
            </section>

            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">清洗</span>
              </header>
              <div class="switch-row">
                <el-form-item label="文本清洗">
                  <el-switch v-model="form.config.textNormalizationEnabled" :disabled="lockPipeline" />
                </el-form-item>
                <el-form-item label="去重复段落">
                  <el-switch v-model="form.config.cleaning.removeDuplicateParagraphs" :disabled="lockPipeline" />
                </el-form-item>
              </div>
              <div class="option-grid option-grid--3">
                <el-checkbox v-model="form.config.cleaning.removeHeaderFooter" :disabled="lockPipeline" class="option-grid__item">去页眉页脚</el-checkbox>
                <el-checkbox v-model="form.config.cleaning.removeWatermark" :disabled="lockPipeline" class="option-grid__item">去水印</el-checkbox>
                <el-checkbox v-model="form.config.cleaning.maskPhone" :disabled="lockPipeline" class="option-grid__item">手机号脱敏</el-checkbox>
                <el-checkbox v-model="form.config.cleaning.maskIdCard" :disabled="lockPipeline" class="option-grid__item">身份证脱敏</el-checkbox>
                <el-checkbox v-model="form.config.cleaning.stopwordFilter" :disabled="lockPipeline" class="option-grid__item">停用词过滤</el-checkbox>
              </div>
              <el-form-item v-if="form.config.textNormalizationEnabled" label="行级清洗">
                <el-input
                  v-model="dropPatternsText"
                  type="textarea"
                  :rows="3"
                  :disabled="lockPipeline"
                  placeholder="丢弃行正则，每行一条"
                />
              </el-form-item>
            </section>

            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">分块</span>
              </header>
              <el-form-item label="分块策略">
                <el-select v-model="form.config.chunkingStrategy" :disabled="lockPipeline" class="select-md">
                  <el-option label="按段落" value="paragraph-first" />
                  <el-option label="固定长度" value="fixed-char" />
                  <el-option label="语义分块" value="semantic" />
                  <el-option label="按标题层级" value="heading-level" />
                </el-select>
                <p v-if="fieldImpactHint('chunkingStrategy')" class="settings-form__tip">{{ fieldImpactHint('chunkingStrategy') }}</p>
              </el-form-item>
              <div class="metric-grid">
                <div class="metric-field">
                  <label class="metric-field__label">块大小</label>
                  <el-input-number
                    v-model="form.config.chunkSize"
                    :min="100"
                    :max="8000"
                    :disabled="lockPipeline"
                    controls-position="right"
                    class="full-width"
                  />
                  <p v-if="fieldImpactHint('chunkSize')" class="settings-form__tip">{{ fieldImpactHint('chunkSize') }}</p>
                </div>
                <div class="metric-field">
                  <label class="metric-field__label">重叠</label>
                  <el-input-number
                    v-model="form.config.chunkOverlap"
                    :min="0"
                    :max="2000"
                    :disabled="lockPipeline"
                    controls-position="right"
                    class="full-width"
                  />
                </div>
                <div class="metric-field">
                  <label class="metric-field__label">最大块</label>
                  <el-input-number
                    v-model="form.config.maxChunkSize"
                    :min="200"
                    :max="16000"
                    :disabled="lockPipeline"
                    controls-position="right"
                    class="full-width"
                  />
                </div>
              </div>
            </section>

            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">向量化</span>
              </header>
              <el-form-item label="Embedding">
                <el-input v-model="form.config.embeddingModel" :disabled="lockPipeline" class="select-md" />
              </el-form-item>
              <el-form-item label="向量维度">
                <el-input-number
                  v-model="form.config.embeddingDimension"
                  :min="1"
                  :max="4096"
                  :disabled="lockPipeline"
                  controls-position="right"
                />
              </el-form-item>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="检索" name="retrieval">
          <div class="tab-pane-body">
            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">检索策略</span>
              </header>
              <div class="switch-row">
                <el-form-item label="混合检索">
                  <el-switch v-model="form.config.retrieval.hybridSearchEnabled" />
                </el-form-item>
                <el-form-item label="重排序">
                  <el-switch v-model="form.config.retrieval.rerankEnabled" />
                </el-form-item>
              </div>
              <el-form-item v-if="form.config.retrieval.rerankEnabled" label="Rerank 模型">
                <el-input
                  v-model="form.config.retrieval.rerankModel"
                  placeholder="留空则使用库 Embedding 模型（推荐）"
                  class="select-md"
                />
                <span class="hint-inline">可选独立 Embedding 模型；须已 ollama pull，且与库向量维度一致（默认 768）</span>
              </el-form-item>
              <el-form-item label="相似度阈值">
                <el-slider
                  v-model="form.config.retrieval.similarityThreshold"
                  :min="0"
                  :max="1"
                  :step="0.05"
                  show-input
                  class="cfg-slider"
                />
                <span class="hint-inline">重排开启时建议 0.35~0.45；0 表示不过滤</span>
              </el-form-item>
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="修改分块或检索策略后，请到「文档库」页执行「批量重索引」使向量与规则一致。"
                class="cfg-alert"
              />
              <el-form-item label="过滤字段">
                <el-select
                  v-model="form.config.retrieval.metadataFilterFields"
                  multiple
                  filterable
                  allow-create
                  placeholder="如 department"
                  class="full-width"
                />
              </el-form-item>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="治理" name="governance">
          <div class="tab-pane-body">
            <section class="cfg-section">
              <header class="cfg-section__head">
                <span class="cfg-section__title">治理与安全</span>
              </header>
              <el-form-item label="入库审核">
                <el-radio-group v-model="form.config.governance.ingestReviewMode" class="radio-row">
                  <el-radio label="auto">自动入库</el-radio>
                  <el-radio label="manual-review">人工审核</el-radio>
                </el-radio-group>
              </el-form-item>
              <div class="switch-row">
                <el-form-item label="权限继承">
                  <el-switch v-model="form.config.governance.inheritLibraryPermissions" />
                </el-form-item>
                <el-form-item label="审计日志">
                  <el-switch v-model="form.config.governance.auditLogEnabled" />
                </el-form-item>
              </div>
              <el-form-item label="保留天数">
                <el-input-number v-model="form.config.governance.retentionDays" :min="0" :max="3650" />
                <span class="hint-inline">0 表示不限制</span>
              </el-form-item>
              <el-form-item label="归档规则">
                <el-select v-model="form.config.governance.archivePolicy" class="select-md">
                  <el-option label="不归档" value="none" />
                  <el-option label="冷存储" value="cold-storage" />
                </el-select>
              </el-form-item>
              <el-form-item label="合规标签">
                <el-select
                  v-model="form.config.governance.complianceTags"
                  multiple
                  filterable
                  allow-create
                  placeholder="密级、行业合规等"
                  class="full-width"
                />
              </el-form-item>
            </section>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-form>

    <template #footer>
      <div class="settings-footer">
        <el-button round @click="visible = false">取消</el-button>
        <el-button type="primary" round :loading="saving" @click="submit">保存配置</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVectorLibrary, updateVectorLibrarySettings } from '../api/library'
import { patternsToText, textToPatterns } from '../utils/textPatterns'
import { defaultLibraryConfig, FILE_TYPE_OPTIONS } from '../utils/libraryDefaults'
import { diffLibraryConfig, diffNeedsReindex, hasIngestedContent } from '../utils/libraryConfig'
import { fieldImpactHint } from '../utils/fieldImpactHints'
import { resolveLibraryPresetLabel, syncLibraryPresetIdOnEdit } from '../utils/libraryPresets'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  libraryId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const router = useRouter()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const activeTab = ref('basic')
const loading = ref(false)
const saving = ref(false)
const dropPatternsText = ref('')
const loadedDocCount = ref(0)
const loadedChunkCount = ref(0)
const snapshotPayload = ref(null)

const lockPipeline = computed(() => hasIngestedContent({
  documentCount: loadedDocCount.value,
  chunkCount: loadedChunkCount.value
}))

const wizardModeLabel = computed(() => {
  const m = form.config.wizardMode
  return m === 'advanced' ? '高级配置' : '快速创建'
})

const presetLabel = computed(() => resolveLibraryPresetLabel(form.config))

const readonly = reactive({ metadataDbType: 'postgresql' })

const form = reactive({
  name: '',
  description: '',
  tags: [],
  config: defaultLibraryConfig('quick')
})

const capacityGb = computed({
  get: () => Math.round((form.config.ingestAccess?.capacityLimits?.maxTotalSizeBytes || 0) / (1024 ** 3)),
  set: (gb) => {
    form.config.ingestAccess.capacityLimits.maxTotalSizeBytes = Math.max(1, gb) * 1024 ** 3
  }
})

function normalizedConfigForDiff(cfg) {
  const copy = JSON.parse(JSON.stringify(cfg))
  if (copy.ingestAccess) {
    copy.ingestAccess.accessMode = 'upload-and-folder'
  }
  if (copy.textNormalization) {
    copy.textNormalization.linePatternsToDrop = textToPatterns(dropPatternsText.value)
  }
  copy.embeddingProvider = 'ollama'
  return syncLibraryPresetIdOnEdit(copy)
}

function clonePayload() {
  return {
    name: form.name,
    description: form.description,
    tags: [...form.tags],
    config: normalizedConfigForDiff(form.config)
  }
}

function buildSubmitConfig() {
  return normalizedConfigForDiff(form.config)
}

function resetForm() {
  form.name = ''
  form.description = ''
  form.tags = []
  form.config = defaultLibraryConfig('quick')
  dropPatternsText.value = ''
  snapshotPayload.value = null
  activeTab.value = 'basic'
}

function formatDiffMessage(changes) {
  return changes
    .map((c) => {
      if (c.detail?.length) return `${c.label}：\n  ${c.detail.join('\n  ')}`
      return `${c.label}：${c.before} → ${c.after}`
    })
    .join('\n')
}

async function confirmSave(changes) {
  const needsReindex = diffNeedsReindex(changes)
  const hasChunks = loadedChunkCount.value > 0
  let message = `将保存以下配置变更：\n\n${formatDiffMessage(changes)}`
  if (needsReindex && hasChunks) {
    message += '\n\n部分变更会影响向量索引，保存后建议执行批量补偿重索引。'
  }
  try {
    await ElMessageBox.confirm(message, '确认保存配置', {
      type: needsReindex && hasChunks ? 'warning' : 'info',
      confirmButtonText: '确认保存',
      cancelButtonText: '取消'
    })
    return true
  } catch {
    return false
  }
}

async function offerBatchReindex() {
  try {
    await ElMessageBox.confirm(
      '配置已保存。库内已有向量分块，是否前往文档库执行批量补偿重索引？',
      '建议重索引',
      { type: 'warning', confirmButtonText: '去批量重索引', cancelButtonText: '稍后' }
    )
    router.push({ path: '/documents', query: { batchReindex: '1', libraryId: props.libraryId } })
  } catch {
    /* later */
  }
}

function mergeLoadedConfig(cfg) {
  const base = defaultLibraryConfig(cfg.wizardMode || 'quick')
  const ingest = cfg.ingestAccess || {}
  return {
    ...base,
    ...cfg,
    configVersion: cfg.configVersion ?? 1,
    ingestAccess: {
      ...base.ingestAccess,
      ...ingest,
      accessMode: 'upload-and-folder',
      capacityLimits: { ...base.ingestAccess.capacityLimits, ...(ingest.capacityLimits || {}) },
      versionPolicy: { ...base.ingestAccess.versionPolicy, ...(ingest.versionPolicy || {}) }
    },
    parsing: { ...base.parsing, ...(cfg.parsing || {}) },
    cleaning: { ...base.cleaning, ...(cfg.cleaning || {}) },
    retrieval: { ...base.retrieval, ...(cfg.retrieval || {}) },
    governance: { ...base.governance, ...(cfg.governance || {}) },
    textNormalization: { ...base.textNormalization, ...(cfg.textNormalization || {}) }
  }
}

async function load() {
  if (!props.libraryId) return
  loading.value = true
  resetForm()
  try {
    const { data: lib } = await getVectorLibrary(props.libraryId)
    form.name = lib.name
    form.description = lib.description || ''
    form.tags = [...(lib.config?.tags || [])]
    form.config = mergeLoadedConfig(lib.config || {})
    readonly.metadataDbType = lib.config?.metadataDbType || 'postgresql'
    dropPatternsText.value = patternsToText(form.config.textNormalization?.linePatternsToDrop)
    loadedDocCount.value = lib.documentCount ?? 0
    loadedChunkCount.value = lib.chunkCount ?? 0
    snapshotPayload.value = clonePayload()
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  const types = form.config.ingestAccess?.supportedFileTypes
  if (!types?.length) {
    ElMessage.warning('请至少选择一种数据类型')
    return
  }

  const nextConfig = buildSubmitConfig()
  const beforeCfg = snapshotPayload.value?.config || {}
  // CFG-01: diff gate compares normalized snapshots — nested parsing/chunking/cleaning must match buildSubmitConfig shape
  const changes = diffLibraryConfig(beforeCfg, nextConfig)
  const metaChanged =
    form.name.trim() !== (snapshotPayload.value?.name || '') ||
    form.description !== (snapshotPayload.value?.description || '') ||
    JSON.stringify(form.tags) !== JSON.stringify(snapshotPayload.value?.tags || [])
  if (!changes.length && !metaChanged) {
    ElMessage.info('配置未变更')
    return
  }
  if (changes.length) {
    if (!(await confirmSave(changes))) return
  } else {
    try {
      await ElMessageBox.confirm('将保存名称、描述或标签变更。', '确认保存配置', {
        type: 'info',
        confirmButtonText: '确认保存',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }

  saving.value = true
  try {
    const { data } = await updateVectorLibrarySettings(props.libraryId, {
      name: form.name.trim(),
      description: form.description,
      config: { ...nextConfig, tags: [...form.tags] }
    })
    const warnings = data.warnings || []
    if (warnings.length) ElMessage.warning(warnings[0])
    else ElMessage.success('配置已保存')
    emit('saved', data.library ?? data)
    visible.value = false
    if (diffNeedsReindex(changes) && loadedChunkCount.value > 0) {
      await offerBatchReindex()
    }
  } finally {
    saving.value = false
  }
}

function onClosed() {
  resetForm()
}

watch(
  () => [visible.value, props.libraryId],
  ([open, id]) => {
    if (open && id) load()
  }
)
</script>

<style scoped>
.library-settings-drawer :deep(.el-drawer__body) {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  overflow: hidden;
}
.library-settings-drawer :deep(.el-drawer__footer) {
  padding: 12px 20px 16px;
  border-top: 1px solid #f1f5f9;
}
.lock-alert {
  flex-shrink: 0;
  margin-bottom: 12px;
}
.lock-alert :deep(.el-alert__description) {
  line-height: 1.5;
}
.cfg-form {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.settings-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.settings-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.settings-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #eef2f7;
}
.settings-tabs :deep(.el-tabs__item) {
  height: 40px;
  font-size: 13px;
  color: #64748b;
}
.settings-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}
.settings-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.settings-tabs :deep(.el-tab-pane) {
  height: 100%;
}
.tab-pane-body {
  height: 100%;
  padding-top: 12px;
  overflow-y: auto;
  padding-right: 4px;
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.tab-lock-tag {
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
}
.cfg-section {
  margin-bottom: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #eef2f7;
  border-radius: 10px;
}
.cfg-section:last-child {
  margin-bottom: 0;
}
.cfg-section--muted {
  background: #f8fafc;
}
.cfg-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}
.cfg-section__title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.cfg-section__hint {
  font-size: 12px;
  color: #94a3b8;
}
.cfg-form :deep(.el-form-item) {
  margin-bottom: 12px;
}
.cfg-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
.cfg-form :deep(.el-form-item__label) {
  color: #64748b;
  font-size: 13px;
}
.full-width {
  width: 100%;
}
.select-sm {
  width: 120px;
}
.select-md {
  width: 100%;
  max-width: 280px;
}
.cfg-slider {
  max-width: 100%;
  padding-right: 8px;
}
.radio-row :deep(.el-radio) {
  margin-right: 20px;
}
.switch-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 12px;
}
.switch-row :deep(.el-form-item) {
  margin-bottom: 12px;
}
.option-grid {
  display: grid;
  gap: 8px;
  width: 100%;
  margin-bottom: 12px;
}
.option-grid--3 {
  grid-template-columns: repeat(3, 1fr);
}
.option-grid__item {
  margin: 0 !important;
  padding: 8px 12px;
  height: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fafbfc;
}
.option-grid__item.is-checked {
  border-color: rgba(14, 165, 233, 0.45);
  background: #f0f9ff;
}
.type-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}
.type-chips :deep(.el-checkbox) {
  margin-right: 0;
  height: auto;
  padding: 5px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  transition: border-color 0.15s, background 0.15s;
}
.type-chips :deep(.el-checkbox.is-checked) {
  border-color: rgba(14, 165, 233, 0.45);
  background: #f0f9ff;
}
.type-chips :deep(.el-checkbox__label) {
  font-size: 13px;
  padding-left: 6px;
}
.meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.metric-field {
  padding: 8px 10px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}
.metric-field__label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #64748b;
}
.hint-inline {
  margin-left: 8px;
  font-size: 12px;
  color: #94a3b8;
}
.settings-form__tip {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}
.settings-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
