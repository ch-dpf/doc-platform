<template>
  <el-drawer
    v-model="visible"
    :title="`编辑规则 · ${form.name || '知识库'}`"
    size="520px"
    destroy-on-close
    @closed="onClosed"
  >
    <el-form v-loading="loading" label-width="108px" class="edit-form">
      <el-divider content-position="left">基本信息</el-divider>
      <el-form-item label="名称" required>
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" />
      </el-form-item>

      <el-divider content-position="left">只读 · 创建时设定</el-divider>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="存储">{{ readonly.storageType }}</el-descriptions-item>
        <el-descriptions-item label="元数据库">{{ readonly.metadataDbType }}</el-descriptions-item>
        <el-descriptions-item label="数据源">{{ readonly.ingestSourceMode }}</el-descriptions-item>
        <el-descriptions-item label="建仓流水线">{{ fixedPipeline }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">文档预处理</el-divider>
      <el-form-item label="文本清洗">
        <el-switch v-model="form.config.textNormalizationEnabled" />
      </el-form-item>
      <el-collapse v-if="form.config.textNormalizationEnabled">
        <el-collapse-item title="清洗规则" name="norm">
          <el-row :gutter="8">
            <el-col :span="12"><el-checkbox v-model="form.config.textNormalization.trimLines">行首尾去空白</el-checkbox></el-col>
            <el-col :span="12"><el-checkbox v-model="form.config.textNormalization.removeControlChars">移除控制字符</el-checkbox></el-col>
            <el-col :span="12"><el-checkbox v-model="form.config.textNormalization.normalizeUnicodeSpaces">统一空格</el-checkbox></el-col>
            <el-col :span="12"><el-checkbox v-model="form.config.textNormalization.collapseBlankLines">合并空行</el-checkbox></el-col>
            <el-col :span="12"><el-checkbox v-model="form.config.textNormalization.dropNoiseLines">丢弃噪声行</el-checkbox></el-col>
            <el-col :span="12">
              <el-form-item label="最短行" label-width="64px">
                <el-input-number v-model="form.config.textNormalization.minLineLength" :min="0" :max="20" size="small" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-input v-model="dropPatternsText" type="textarea" :rows="3" placeholder="每行一条丢弃行正则" />
        </el-collapse-item>
      </el-collapse>

      <el-divider content-position="left">分块</el-divider>
      <el-form-item label="分块策略">
        <el-select v-model="form.config.chunkingStrategy" style="width: 180px" size="small">
          <el-option label="段落优先" value="paragraph-first" />
          <el-option label="固定长度" value="fixed-char" />
        </el-select>
      </el-form-item>
      <el-row :gutter="8">
        <el-col :span="8">
          <el-form-item label="块大小" label-width="72px">
            <el-input-number v-model="form.config.chunkSize" :min="100" :max="8000" size="small" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="重叠" label-width="48px">
            <el-input-number v-model="form.config.chunkOverlap" :min="0" :max="2000" size="small" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="最大块" label-width="56px">
            <el-input-number v-model="form.config.maxChunkSize" :min="200" :max="16000" size="small" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="分块前规范化">
        <el-switch v-model="form.config.normalizeBeforeChunk" />
      </el-form-item>

      <el-divider content-position="left">向量化</el-divider>
      <el-form-item label="提供方">
        <el-input v-model="form.config.embeddingProvider" disabled />
      </el-form-item>
      <el-form-item label="Embedding 模型">
        <el-input v-model="form.config.embeddingModel" placeholder="nomic-embed-text" />
        <p class="field-hint">入库与检索均使用该库配置；变更后须对已有文档补偿重索引</p>
      </el-form-item>
      <el-form-item label="向量维度">
        <el-input-number v-model="form.config.embeddingDimension" :min="1" :max="4096" />
        <p class="field-hint">须与所选 Embedding 模型输出维度一致</p>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button round @click="visible = false">取消</el-button>
      <el-button type="primary" round :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-drawer>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVectorLibrary, updateVectorLibrarySettings } from '../api/library'
import { patternsToText, textToPatterns } from '../utils/textPatterns'

const FIXED_PIPELINE =
  '数据源接入 → 文档解析 → 文本清洗 → 分块 → 向量化 → 写入知识库'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  libraryId: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'saved'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const fixedPipeline = FIXED_PIPELINE
const loading = ref(false)
const saving = ref(false)
const dropPatternsText = ref('')
const snapshotEmbedding = ref({ model: '', dimension: 0, provider: 'ollama' })
const loadedChunkCount = ref(0)

const readonly = reactive({
  storageType: '—',
  metadataDbType: '—',
  ingestSourceMode: '—'
})

const form = reactive({
  name: '',
  description: '',
  config: {
    chunkingStrategy: 'paragraph-first',
    chunkSize: 600,
    chunkOverlap: 100,
    minChunkSize: 80,
    maxChunkSize: 1200,
    minParagraphLength: 30,
    normalizeBeforeChunk: true,
    textNormalizationEnabled: true,
    embeddingProvider: 'ollama',
    embeddingModel: 'nomic-embed-text',
    embeddingDimension: 768,
    textNormalization: {
      enabled: true,
      collapseBlankLines: true,
      trimLines: true,
      removeControlChars: true,
      normalizeUnicodeSpaces: true,
      dropNoiseLines: true,
      minLineLength: 2,
      linePatternsToDrop: []
    }
  }
})

function resetForm() {
  form.name = ''
  form.description = ''
  dropPatternsText.value = ''
}

async function load() {
  if (!props.libraryId) return
  loading.value = true
  resetForm()
  try {
    const { data: lib } = await getVectorLibrary(props.libraryId)
    const cfg = lib.config || {}
    form.name = lib.name
    form.description = lib.description || ''
    Object.assign(form.config, {
      chunkingStrategy: cfg.chunkingStrategy || 'paragraph-first',
      chunkSize: cfg.chunkSize ?? 600,
      chunkOverlap: cfg.chunkOverlap ?? 100,
      minChunkSize: cfg.minChunkSize ?? 80,
      maxChunkSize: cfg.maxChunkSize ?? 1200,
      minParagraphLength: cfg.minParagraphLength ?? 30,
      normalizeBeforeChunk: cfg.normalizeBeforeChunk !== false,
      textNormalizationEnabled: cfg.textNormalizationEnabled !== false,
      embeddingProvider: cfg.embeddingProvider || 'ollama',
      embeddingModel: cfg.embeddingModel || 'nomic-embed-text',
      embeddingDimension: cfg.embeddingDimension ?? 768,
      textNormalization: { ...form.config.textNormalization, ...(cfg.textNormalization || {}) }
    })
    readonly.storageType = cfg.storageType || '—'
    readonly.metadataDbType = cfg.metadataDbType || 'postgresql'
    readonly.ingestSourceMode = cfg.ingestSourceMode || 'upload'
    dropPatternsText.value = patternsToText(form.config.textNormalization.linePatternsToDrop)
    loadedChunkCount.value = lib.chunkCount ?? 0
    snapshotEmbedding.value = {
      model: form.config.embeddingModel,
      dimension: form.config.embeddingDimension,
      provider: form.config.embeddingProvider
    }
  } finally {
    loading.value = false
  }
}

function embeddingConfigChanged() {
  const s = snapshotEmbedding.value
  return (
    s.model !== form.config.embeddingModel ||
    s.dimension !== form.config.embeddingDimension ||
    s.provider !== form.config.embeddingProvider
  )
}

async function submit() {
  if (!form.name?.trim()) {
    ElMessage.warning('请填写名称')
    return
  }
  if (embeddingConfigChanged() && loadedChunkCount.value > 0) {
    try {
      await ElMessageBox.confirm(
        'Embedding 模型、维度或提供方已变更。已有向量不会自动更新，请在文档库中对相关文档执行补偿重索引。是否继续保存？',
        '向量化配置变更',
        { type: 'warning', confirmButtonText: '继续保存', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  form.config.textNormalization.linePatternsToDrop = textToPatterns(dropPatternsText.value)
  form.config.embeddingProvider = 'ollama'
  saving.value = true
  try {
    const { data } = await updateVectorLibrarySettings(props.libraryId, {
      name: form.name.trim(),
      description: form.description,
      config: form.config
    })
    const lib = data.library ?? data
    const warnings = data.warnings || []
    if (warnings.length) {
      ElMessage.warning(warnings[0])
    } else {
      ElMessage.success('规则已保存')
    }
    emit('saved', lib)
    visible.value = false
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
.edit-form {
  padding-right: 8px;
}
.field-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}
</style>
