<template>
  <el-dialog
    v-model="visible"
    title="创建知识库"
    width="920px"
    destroy-on-close
    class="library-wizard"
    @closed="onClosed"
  >
    <div class="wizard-mode-bar">
      <span class="wizard-mode-bar__label">创建模式</span>
      <el-radio-group v-model="wizardMode" size="small" @change="onModeChange">
        <el-radio-button label="quick">快速创建</el-radio-button>
        <el-radio-button label="advanced">高级配置</el-radio-button>
      </el-radio-group>
      <span class="wizard-mode-bar__hint">
        {{ wizardMode === 'quick' ? '使用推荐默认规则，仅填基础信息' : '五步向导设定治理边界与默认规则' }}
      </span>
    </div>

    <el-steps
      v-if="wizardMode === 'advanced'"
      :active="step"
      align-center
      finish-status="success"
      class="wizard-steps"
    >
      <el-step v-for="(s, i) in WIZARD_STEPS" :key="i" :title="s.title" />
    </el-steps>

    <!-- Step 1: 基础信息 -->
    <div v-show="wizardMode === 'quick' || step === 0" class="step-body">
      <div v-if="wizardMode === 'advanced'" class="step-head">
        <h3 class="step-head__title">{{ WIZARD_STEPS[0].title }}</h3>
        <p class="step-head__desc">{{ STEP_DESCRIPTIONS[0] }}</p>
      </div>
      <div :class="['wizard-section', { 'wizard-section--solo': wizardMode === 'quick' }]">
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="库类型预设">
            <el-radio-group v-model="selectedPresetId" class="preset-grid" @change="onPresetChange">
              <el-radio
                v-for="preset in LIBRARY_PRESETS"
                :key="preset.id"
                :label="preset.id"
                class="preset-grid__item"
              >
                <span class="preset-grid__name">{{ preset.name }}</span>
                <span class="preset-grid__summary">{{ preset.summary }}</span>
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="知识库名称" required>
            <el-input v-model="form.name" placeholder="例如：产品手册库" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简要说明知识库用途" />
          </el-form-item>
          <el-form-item label="标签">
            <el-select
              v-model="form.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="输入后回车添加标签"
              class="wizard-full-width"
            />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- Step 2: 数据类型与容量 -->
    <div v-show="wizardMode === 'advanced' && step === 1" class="step-body">
      <div class="step-head">
        <h3 class="step-head__title">{{ WIZARD_STEPS[1].title }}</h3>
        <p class="step-head__desc">{{ STEP_DESCRIPTIONS[1] }}</p>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">数据类型</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="支持类型" required>
            <el-checkbox-group v-model="form.config.ingestAccess.supportedFileTypes" class="option-grid option-grid--5">
              <el-checkbox v-for="opt in FILE_TYPE_OPTIONS" :key="opt.value" :label="opt.value" class="option-grid__item">
                {{ opt.label }}
              </el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-form>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">单库容量上限</span>
        </div>
        <div class="metric-grid">
          <div class="metric-field">
            <label class="metric-field__label">文档数</label>
            <el-input-number
              v-model="form.config.ingestAccess.capacityLimits.maxDocuments"
              :min="100"
              :max="1000000"
              controls-position="right"
              class="wizard-full-width"
            />
          </div>
          <div class="metric-field">
            <label class="metric-field__label">总大小 (GB)</label>
            <el-input-number
              v-model="capacityGb"
              :min="1"
              :max="1024"
              :step="1"
              controls-position="right"
              class="wizard-full-width"
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
              class="wizard-full-width"
            />
          </div>
        </div>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">版本管理</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="启用版本管理">
            <el-switch v-model="form.config.ingestAccess.versionPolicy.enabled" />
          </el-form-item>
          <el-form-item v-if="form.config.ingestAccess.versionPolicy.enabled" label="更新策略">
            <el-radio-group v-model="form.config.ingestAccess.versionPolicy.updateStrategy" class="radio-row">
              <el-radio label="overwrite">覆盖</el-radio>
              <el-radio label="incremental">增量</el-radio>
              <el-radio label="keep-history">保留历史版本</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- Step 3: 文档处理规则 -->
    <div v-show="wizardMode === 'advanced' && step === 2" class="step-body step-body--scroll">
      <div class="step-head">
        <h3 class="step-head__title">{{ WIZARD_STEPS[2].title }}</h3>
        <p class="step-head__desc">{{ STEP_DESCRIPTIONS[2] }}</p>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">解析规则</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item>
            <template #label>
              <span>OCR</span>
              <el-tooltip
                content="适用于扫描件 PDF：库开启后，Tika 抽取不足时将回退 Tesseract OCR（需服务端 ingest.ocr.enabled=true）"
                placement="top"
              >
                <el-icon class="tip-icon"><QuestionFilled /></el-icon>
              </el-tooltip>
            </template>
            <el-switch v-model="form.config.parsing.ocrEnabled" />
          </el-form-item>
          <div class="field-grid field-grid--3">
            <el-form-item label="表格提取">
              <el-select v-model="form.config.parsing.tableExtraction" class="wizard-full-width">
                <el-option label="纯文本" value="text-only" />
                <el-option label="结构化" value="structured" />
                <el-option label="跳过" value="skip" />
              </el-select>
            </el-form-item>
            <el-form-item label="图片提取">
              <el-select v-model="form.config.parsing.imageExtraction" class="wizard-full-width">
                <el-option label="OCR 描述" value="ocr-caption" />
                <el-option label="跳过" value="skip" />
              </el-select>
            </el-form-item>
            <el-form-item label="公式提取">
              <el-select v-model="form.config.parsing.formulaExtraction" class="wizard-full-width">
                <el-option label="LaTeX" value="latex" />
                <el-option label="跳过" value="skip" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="编码与语言">
            <div class="inline-controls">
              <el-checkbox v-model="form.config.parsing.autoDetectEncoding">自动识别编码</el-checkbox>
              <el-select v-model="form.config.parsing.defaultLanguage" class="wizard-select-sm">
                <el-option label="中文" value="zh-CN" />
                <el-option label="英文" value="en-US" />
              </el-select>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">清洗规则</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="文本清洗">
            <el-switch v-model="form.config.textNormalizationEnabled" />
          </el-form-item>
          <el-form-item label="内容清洗">
            <div class="option-grid option-grid--3">
              <el-checkbox v-model="form.config.cleaning.removeHeaderFooter" class="option-grid__item">去页眉页脚</el-checkbox>
              <el-checkbox v-model="form.config.cleaning.removeWatermark" class="option-grid__item">去水印</el-checkbox>
              <el-checkbox v-model="form.config.cleaning.removeDuplicateParagraphs" class="option-grid__item">去重复段落</el-checkbox>
              <el-checkbox v-model="form.config.cleaning.maskPhone" class="option-grid__item">手机号脱敏</el-checkbox>
              <el-checkbox v-model="form.config.cleaning.maskIdCard" class="option-grid__item">身份证脱敏</el-checkbox>
              <el-checkbox v-model="form.config.cleaning.stopwordFilter" class="option-grid__item">停用词过滤</el-checkbox>
            </div>
          </el-form-item>
          <el-form-item v-if="form.config.textNormalizationEnabled" label="行级清洗">
            <el-input v-model="dropPatternsText" type="textarea" :rows="4" placeholder="丢弃行正则，每行一条" />
          </el-form-item>
        </el-form>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">分块规则</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="分块策略">
            <el-select v-model="form.config.chunkingStrategy" class="wizard-select-md">
              <el-option label="按段落" value="paragraph-first" />
              <el-option label="固定长度" value="fixed-char" />
              <el-option label="语义分块" value="semantic" />
              <el-option label="按标题层级" value="heading-level" />
            </el-select>
          </el-form-item>
          <div class="metric-grid">
            <div class="metric-field">
              <label class="metric-field__label">块大小</label>
              <el-input-number v-model="form.config.chunkSize" :min="100" :max="8000" controls-position="right" class="wizard-full-width" />
            </div>
            <div class="metric-field">
              <label class="metric-field__label">重叠</label>
              <el-input-number v-model="form.config.chunkOverlap" :min="0" :max="2000" controls-position="right" class="wizard-full-width" />
            </div>
            <div class="metric-field">
              <label class="metric-field__label">最大块</label>
              <el-input-number v-model="form.config.maxChunkSize" :min="200" :max="16000" controls-position="right" class="wizard-full-width" />
            </div>
          </div>
        </el-form>
      </div>

      <div class="wizard-section wizard-section--preview">
        <div class="wizard-section__head">
          <span class="wizard-section__title">分块预览</span>
          <span class="wizard-section__hint">高级模式须完成预览后方可进入下一步</span>
        </div>
        <p class="preview-strategy-hint">{{ strategyPreviewHint }}</p>
        <el-input v-model="previewText" type="textarea" :rows="8" placeholder="粘贴样本文本，或点击下方加载对比示例" />
        <div class="preview-actions">
          <el-button plain @click="loadPreviewSample">加载对比示例</el-button>
          <el-button type="primary" plain :loading="previewLoading" @click="runPreview">预览分块</el-button>
          <el-tag v-if="previewDone" type="success" size="small">已预览</el-tag>
          <el-tag v-else type="warning" size="small">待预览</el-tag>
          <span v-if="previewSummary" class="preview-summary">{{ previewSummary }}</span>
        </div>
        <p v-if="previewAdjustHint" class="preview-adjust-hint">{{ previewAdjustHint }}</p>
        <el-table v-if="previewChunkItems.length" :data="previewChunkItems" size="small" max-height="240" stripe class="preview-table">
          <el-table-column prop="index" label="#" width="48" align="center" />
          <el-table-column prop="length" label="字数" width="72" align="center" />
          <el-table-column prop="content" label="摘录" show-overflow-tooltip />
        </el-table>
      </div>
    </div>

    <!-- Step 4: 索引与检索 -->
    <div v-show="wizardMode === 'advanced' && step === 3" class="step-body">
      <div class="step-head">
        <h3 class="step-head__title">{{ WIZARD_STEPS[3].title }}</h3>
        <p class="step-head__desc">{{ STEP_DESCRIPTIONS[3] }}</p>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">向量化</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="Embedding">
            <div class="inline-controls">
              <el-input v-model="form.config.embeddingProvider" disabled class="wizard-input-xs" />
              <el-input v-model="form.config.embeddingModel" class="wizard-input-md" />
              <span class="hint-inline">维度 {{ form.config.embeddingDimension }}</span>
            </div>
          </el-form-item>
        </el-form>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">检索策略</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="混合检索">
            <div class="inline-controls">
              <el-switch v-model="form.config.retrieval.hybridSearchEnabled" />
              <span class="hint-inline">向量 + BM25 关键词检索</span>
            </div>
          </el-form-item>
          <el-form-item label="重排序">
            <div class="inline-controls inline-controls--wrap">
              <el-switch v-model="form.config.retrieval.rerankEnabled" />
              <el-input
                v-if="form.config.retrieval.rerankEnabled"
                v-model="form.config.retrieval.rerankModel"
                placeholder="留空则使用库 Embedding 模型"
                class="wizard-input-md"
              />
            </div>
          </el-form-item>
          <el-form-item label="过滤字段">
            <el-select
              v-model="form.config.retrieval.metadataFilterFields"
              multiple
              filterable
              allow-create
              placeholder="如 department、docType"
              class="wizard-full-width"
            />
          </el-form-item>
          <el-form-item label="相似度阈值">
            <el-slider
              v-model="form.config.retrieval.similarityThreshold"
              :min="0"
              :max="1"
              :step="0.05"
              show-input
              class="wizard-slider"
            />
            <span class="hint-inline">重排开启时建议 0.35~0.45</span>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- Step 5: 治理与安全 -->
    <div v-show="wizardMode === 'advanced' && step === 4" class="step-body">
      <div class="step-head">
        <h3 class="step-head__title">{{ WIZARD_STEPS[4].title }}</h3>
        <p class="step-head__desc">{{ STEP_DESCRIPTIONS[4] }}</p>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">治理策略</span>
        </div>
        <el-form class="wizard-form" label-width="112px" label-position="right">
          <el-form-item label="入库审核">
            <el-radio-group v-model="form.config.governance.ingestReviewMode" class="radio-row">
              <el-radio label="auto">自动入库</el-radio>
              <el-radio label="manual-review">人工审核后生效</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="权限继承">
            <div class="inline-controls">
              <el-switch v-model="form.config.governance.inheritLibraryPermissions" />
              <span class="hint-inline">文档继承知识库权限</span>
            </div>
          </el-form-item>
          <div class="field-grid field-grid--2">
            <el-form-item label="保留天数">
              <div class="inline-controls">
                <el-input-number
                  v-model="form.config.governance.retentionDays"
                  :min="0"
                  :max="3650"
                  controls-position="right"
                />
                <span class="hint-inline">0 表示不限制</span>
              </div>
            </el-form-item>
            <el-form-item label="归档规则">
              <el-select v-model="form.config.governance.archivePolicy" class="wizard-full-width">
                <el-option label="不归档" value="none" />
                <el-option label="冷存储归档" value="cold-storage" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="合规标签">
            <el-select
              v-model="form.config.governance.complianceTags"
              multiple
              filterable
              allow-create
              placeholder="密级、行业合规等"
              class="wizard-full-width"
            />
          </el-form-item>
          <el-form-item label="审计日志">
            <el-switch v-model="form.config.governance.auditLogEnabled" />
          </el-form-item>
        </el-form>
      </div>

      <div class="wizard-section">
        <div class="wizard-section__head">
          <span class="wizard-section__title">可选：示例文档</span>
        </div>
        <el-upload
          class="sample-upload"
          drag
          :auto-upload="false"
          :limit="1"
          :file-list="sampleFileList"
          :on-change="onSampleFileChange"
          :on-remove="onSampleFileRemove"
        >
          <div class="hint">创建完成后可选上传 1 个示例文件（非必填）</div>
        </el-upload>
      </div>

      <el-alert class="wizard-outcome" type="success" :closable="false" show-icon>
        <template #title>创建产出</template>
        知识库 ID · 默认配置快照（config_version=1）· 空文档列表 · 可在详情页进入文档采集
      </el-alert>
    </div>

    <template #footer>
      <el-button round @click="visible = false">取消</el-button>
      <template v-if="wizardMode === 'advanced'">
        <el-button v-if="step > 0" round @click="step--">上一步</el-button>
        <el-button v-if="step < 4" type="primary" round @click="nextStep">下一步</el-button>
        <el-button v-else type="primary" round :loading="submitting" @click="submit">创建知识库</el-button>
      </template>
      <el-button v-else type="primary" round :loading="submitting" @click="submitQuick">快速创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { QuestionFilled } from '@element-plus/icons-vue'
import { createVectorLibrary } from '../api/library'
import { previewChunks as fetchChunkPreview } from '../api/chunk'
import { uploadDocument } from '../api/ingest'
import { buildCreatePayload, defaultLibraryConfig, FILE_TYPE_OPTIONS, WIZARD_STEPS } from '../utils/libraryDefaults'
import { applyLibraryPreset, LIBRARY_PRESETS } from '../utils/libraryPresets'
import {
  CHUNK_PREVIEW_COMPARISON_SAMPLE,
  CHUNK_STRATEGY_PREVIEW_HINTS,
  resolvePreviewChunkParams
} from '../utils/chunkPreviewSample'
import { DEFAULT_LINE_DROP_PATTERNS, patternsToText } from '../utils/textPatterns'
import { usePageTitle } from '../composables/usePageTitle'

const STEP_DESCRIPTIONS = [
  '填写名称、描述与标签，便于团队识别与管理。',
  '定义支持的数据类型与单库容量边界。',
  '配置解析、清洗与分块规则，并完成分块预览确认。',
  '设定 Embedding 与检索相关默认参数。',
  '配置入库治理策略，可选上传示例文档。'
]

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  tenantId: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'created'])

const router = useRouter()
const { setPageTitle, clearPageTitle } = usePageTitle()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const wizardMode = ref('quick')
const selectedPresetId = ref('general-mixed')
const step = ref(0)
const submitting = ref(false)
const previewLoading = ref(false)
const previewDone = ref(false)
const previewChunkItems = ref([])
const previewSummary = ref('')
const previewAdjustHint = ref('')
const dropPatternsText = ref(patternsToText(DEFAULT_LINE_DROP_PATTERNS))

const strategyPreviewHint = computed(
  () => CHUNK_STRATEGY_PREVIEW_HINTS[form.config.chunkingStrategy] || ''
)
const previewText = ref('示例段落一。\n\n示例段落二，用于预览分块效果。')
const sampleFileList = ref([])
const sampleFile = ref(null)

const form = reactive({
  name: '',
  description: '',
  tags: [],
  config: defaultLibraryConfig('quick')
})

const capacityGb = computed({
  get: () => Math.round((form.config.ingestAccess.capacityLimits.maxTotalSizeBytes || 0) / (1024 ** 3)),
  set: (gb) => {
    form.config.ingestAccess.capacityLimits.maxTotalSizeBytes = Math.max(1, gb) * 1024 ** 3
  }
})

function syncDropPatterns() {
  form.config.textNormalization.linePatternsToDrop = dropPatternsText.value
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function onModeChange(mode) {
  step.value = 0
  form.config = applyLibraryPreset(defaultLibraryConfig(mode), selectedPresetId.value, mode)
  previewDone.value = false
}

function onPresetChange(presetId) {
  form.config = applyLibraryPreset(form.config, presetId, wizardMode.value)
  previewDone.value = false
}

function onSampleFileChange(_f, list) {
  sampleFileList.value = list
  sampleFile.value = list[0]?.raw || null
}

function onSampleFileRemove(_f, list) {
  sampleFileList.value = list
  sampleFile.value = list[0]?.raw || null
}

function validateStep(s) {
  if (s === 0 && !form.name?.trim()) {
    ElMessage.warning('请填写知识库名称')
    return false
  }
  if (s === 1) {
    const types = form.config.ingestAccess.supportedFileTypes
    if (!types?.length) {
      ElMessage.warning('请至少选择一种数据类型')
      return false
    }
  }
  if (s === 2) {
    if (!previewText.value?.trim()) {
      ElMessage.warning('请填写预览样本文本')
      return false
    }
    if (!previewDone.value) {
      ElMessage.warning('请先预览分块结果')
      return false
    }
  }
  return true
}

async function nextStep() {
  if (!validateStep(step.value)) return
  step.value++
}

async function runPreview() {
  if (!previewText.value?.trim()) {
    ElMessage.warning('请填写样本文本或加载对比示例')
    return
  }
  syncDropPatterns()
  previewLoading.value = true
  previewChunkItems.value = []
  previewSummary.value = ''
  previewAdjustHint.value = ''
  const textLen = previewText.value.trim().length
  const strategy = form.config.chunkingStrategy
  const sizing = resolvePreviewChunkParams(textLen, form.config, strategy)
  const hints = [sizing.expectedHint, sizing.previewHint].filter(Boolean)
  if (hints.length) {
    previewAdjustHint.value = hints.join(' ')
  }
  try {
    const norm = { ...form.config.textNormalization, enabled: true }
    const { data } = await fetchChunkPreview({
      sampleText: previewText.value,
      chunkingStrategy: form.config.chunkingStrategy,
      chunkSize: sizing.chunkSize,
      chunkOverlap: sizing.chunkOverlap,
      minChunkSize: sizing.minChunkSize,
      maxChunkSize: sizing.maxChunkSize,
      minParagraphLength: sizing.minParagraphLength,
      normalizeBeforeChunk: form.config.normalizeBeforeChunk,
      textNormalizationEnabled: form.config.textNormalizationEnabled,
      textNormalization: form.config.textNormalizationEnabled ? norm : null,
      cleaning: form.config.cleaning || {},
      libraryId: null
    })
    previewChunkItems.value = data.chunks || []
    const filtered = data.filteredOutCount ?? 0
    previewSummary.value = filtered > 0
      ? `共 ${previewChunkItems.value.length} 块（入库预览，已过滤 ${filtered} 个表头块）`
      : `共 ${previewChunkItems.value.length} 块（入库预览）`
    previewDone.value = true
    if (sizing.expectedHint) {
      const expectedMatch = sizing.expectedHint.match(/约\s*(\d+)/)
      if (expectedMatch) {
        const expected = Number(expectedMatch[1])
        const actual = previewChunkItems.value.length
        if (Math.abs(actual - expected) > 2 && strategy === 'semantic') {
          ElMessage.info(`语义分块 ${actual} 块：若远高于 2，请确认 Ollama embedding 可用，或两段主题差异是否足够大`)
        }
      }
    } else if (previewChunkItems.value.length <= 1 && textLen > 200) {
      ElMessage.info('仅分出 1 块：可加载对比示例，或调小块大小后再预览')
    }
  } catch (e) {
    const msg =
      e?.response?.data?.detail ||
      e?.response?.data?.message ||
      e?.response?.data?.error ||
      e?.message ||
      '预览失败'
    if (form.config.chunkingStrategy === 'semantic') {
      ElMessage.error(`语义分块预览失败（需 Ollama embedding 可用）：${msg}`)
    } else {
      ElMessage.error(msg)
    }
  } finally {
    previewLoading.value = false
  }
}

function loadPreviewSample() {
  previewText.value = CHUNK_PREVIEW_COMPARISON_SAMPLE
  previewDone.value = false
  previewChunkItems.value = []
  previewSummary.value = ''
  previewAdjustHint.value = ''
  ElMessage.success('已加载对比示例，请切换分块策略后分别预览')
}

async function doCreate(mode) {
  syncDropPatterns()
  form.config.tags = [...form.tags]
  submitting.value = true
  try {
    const payload = buildCreatePayload({
      tenantId: props.tenantId,
      name: form.name,
      description: form.description,
      tags: form.tags,
      config: form.config,
      wizardMode: mode
    })
    const { data: lib } = await createVectorLibrary(payload)
    emit('created', lib)

    if (sampleFile.value) {
      await uploadDocument(lib.libraryId, props.tenantId, sampleFile.value, true)
      ElMessage.success('知识库已创建，示例文档已开始入库')
    } else {
      ElMessage.success(mode === 'quick' ? '知识库已快速创建（默认规则）' : '知识库已创建，配置快照 v1 已保存')
    }

    visible.value = false
    router.push({ name: 'vectorLibraryDetail', params: { libraryId: lib.libraryId } })
  } finally {
    submitting.value = false
  }
}

function submitQuick() {
  if (!validateStep(0)) return
  doCreate('quick')
}

function submit() {
  if (!validateStep(0) || !validateStep(1) || !validateStep(2)) return
  doCreate('advanced')
}

function resetForm() {
  wizardMode.value = 'quick'
  selectedPresetId.value = 'general-mixed'
  step.value = 0
  form.name = ''
  form.description = ''
  form.tags = []
  form.config = applyLibraryPreset(defaultLibraryConfig('quick'), 'general-mixed', 'quick')
  sampleFileList.value = []
  sampleFile.value = null
  previewChunkItems.value = []
  previewDone.value = false
  dropPatternsText.value = patternsToText(DEFAULT_LINE_DROP_PATTERNS)
}

function onClosed() {
  clearPageTitle()
  resetForm()
}

watch(visible, (v) => {
  if (v) {
    resetForm()
    setPageTitle('创建知识库')
  } else {
    clearPageTitle()
  }
})
</script>

<style scoped>
.wizard-mode-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid var(--dp-border);
  border-radius: 10px;
}
.wizard-mode-bar__label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.wizard-mode-bar__hint {
  font-size: 12px;
  color: #64748b;
}
.wizard-steps {
  margin-bottom: 18px;
}
.wizard-steps :deep(.el-step__title) {
  font-size: 13px;
}
.step-body {
  min-height: 320px;
  padding: 4px 2px 0;
}
.step-body--scroll {
  max-height: 52vh;
  overflow-y: auto;
  padding-right: 6px;
}
.step-head {
  margin-bottom: 14px;
}
.step-head__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.01em;
}
.step-head__desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}
.wizard-section {
  margin-bottom: 12px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid var(--dp-border);
  border-radius: 10px;
}
.wizard-section--solo {
  margin-bottom: 0;
}
.wizard-section--preview {
  background: #f8fafc;
}
.wizard-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eef2f7;
}
.wizard-section__title {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.wizard-section__hint {
  font-size: 12px;
  color: #94a3b8;
}
.wizard-form :deep(.el-form-item) {
  margin-bottom: 14px;
}
.wizard-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}
.wizard-full-width {
  width: 100%;
}
.wizard-select-sm {
  width: 120px;
}
.wizard-select-md {
  width: 200px;
}
.wizard-input-xs {
  width: 88px;
}
.wizard-input-md {
  width: 200px;
}
.wizard-slider {
  max-width: 480px;
  padding-right: 12px;
}
.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 14px;
}
.metric-field {
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}
.metric-field__label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
}
.field-grid {
  display: grid;
  gap: 0 16px;
  margin-bottom: 4px;
}
.field-grid--2 {
  grid-template-columns: repeat(2, 1fr);
}
.field-grid--3 {
  grid-template-columns: repeat(3, 1fr);
}
.field-grid :deep(.el-form-item) {
  margin-bottom: 14px;
}
.preset-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  width: 100%;
}
.preset-grid__item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  height: auto;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  white-space: normal;
}
.preset-grid__item :deep(.el-radio__label) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 8px;
  white-space: normal;
}
.preset-grid__name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.preset-grid__summary {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}
.option-grid {
  display: grid;
  gap: 8px;
  width: 100%;
}
.option-grid--5 {
  grid-template-columns: repeat(5, 1fr);
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
  transition: border-color 0.15s, background 0.15s;
}
.option-grid__item:hover {
  border-color: #cbd5e1;
}
.option-grid__item.is-checked {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}
.radio-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
}
.inline-controls {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.inline-controls--wrap {
  row-gap: 8px;
}
.preview-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}
.preview-strategy-hint,
.preview-adjust-hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}
.preview-summary {
  font-size: 13px;
  font-weight: 500;
  color: #0f172a;
}
.preview-table {
  margin-top: 10px;
  border-radius: 8px;
  overflow: hidden;
}
.sample-upload :deep(.el-upload-dragger) {
  padding: 16px;
  border-radius: 8px;
}
.wizard-outcome {
  margin-top: 4px;
  border-radius: 10px;
}
.hint,
.hint-inline {
  font-size: 12px;
  color: #64748b;
}
.tip-icon {
  margin-left: 4px;
  color: #94a3b8;
  vertical-align: middle;
  cursor: help;
}
.library-wizard :deep(.el-dialog__body) {
  padding-top: 16px;
  padding-bottom: 8px;
}
</style>
