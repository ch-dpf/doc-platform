<template>
  <el-dialog
    v-model="visible"
    title="新建知识库"
    width="820px"
    destroy-on-close
    class="library-wizard"
    @closed="onClosed"
  >
    <el-steps :active="step" align-center finish-status="success" class="wizard-steps">
      <el-step title="数据源" />
      <el-step title="数据存储" />
      <el-step title="文档预处理" />
      <el-step title="向量化入库" />
    </el-steps>

    <div v-show="step === 0" class="step-body">
      <el-form label-width="108px">
        <el-form-item label="库名称" required>
          <el-input v-model="form.name" placeholder="例如：产品手册库" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="数据源" required>
          <el-radio-group v-model="form.ingestSourceMode">
            <el-radio label="upload">导入本地文档</el-radio>
            <el-radio label="crawl">接入线上网站</el-radio>
            <el-radio label="both">两者均支持</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="form.ingestSourceMode === 'upload' || form.ingestSourceMode === 'both'"
          label="本地文件"
        >
          <el-upload
            drag
            multiple
            :auto-upload="false"
            :file-list="fileList"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div>选择 PDF / Word / TXT / Markdown（创建完成后自动开始上传）</div>
          </el-upload>
          <p v-if="sampleHint" class="hint">{{ sampleHint }}</p>
          <el-button
            v-if="stagedFiles.length"
            type="primary"
            link
            :loading="parseLoading"
            @click="extractFromFirstFile"
          >
            Tika 解析首个文件为预览样本
          </el-button>
        </el-form-item>
        <el-form-item
          v-if="form.ingestSourceMode === 'crawl' || form.ingestSourceMode === 'both'"
          label="采集 URL"
          :required="form.ingestSourceMode === 'crawl'"
        >
          <el-input
            v-model="form.seedUrl"
            placeholder="线上采集必填；both 模式可与本地文件二选一或同时填写"
            clearable
          />
        </el-form-item>
      </el-form>
    </div>

    <div v-show="step === 1" class="step-body">
      <el-form label-width="120px">
        <el-form-item label="非结构化存储" required>
          <el-radio-group v-model="form.config.storageType">
            <el-radio label="minio">MinIO 对象存储</el-radio>
            <el-radio label="local-fs">本地文件系统 (FS)</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.config.storageType === 'minio'" label="存储路径前缀">
          <el-input v-model="form.config.storagePathPrefix" placeholder="留空使用默认桶路径" />
        </el-form-item>
        <el-form-item v-if="form.config.storageType === 'local-fs'" label="本地目录" required>
          <el-input v-model="form.config.localBasePath" placeholder="./data/documents/{libraryId}" />
        </el-form-item>
        <el-form-item label="元数据存储" required>
          <el-select v-model="form.config.metadataDbType" style="width: 100%">
            <el-option label="PostgreSQL（当前）" value="postgresql" />
          </el-select>
          <p class="hint">关系型元数据库可扩展，一期已对接 PostgreSQL。</p>
        </el-form-item>
      </el-form>
    </div>

    <div v-show="step === 2" class="step-body">
      <p class="step-note">建仓将固定执行：文档解析（Tika）→ 文本清洗 → 分块。以下配置清洗规则与分块参数。</p>
      <el-form label-width="120px">
        <el-form-item label="文本清洗">
          <el-switch v-model="form.config.textNormalizationEnabled" active-text="启用" inactive-text="关闭" />
          <span class="hint-inline">去噪、统一编码、页眉页脚等</span>
        </el-form-item>
        <el-collapse v-if="form.config.textNormalizationEnabled">
          <el-collapse-item title="清洗规则（知识库级，可持久化）" name="norm">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-checkbox v-model="form.config.textNormalization.trimLines">行首尾去空白</el-checkbox>
              </el-col>
              <el-col :span="12">
                <el-checkbox v-model="form.config.textNormalization.removeControlChars">移除控制字符</el-checkbox>
              </el-col>
              <el-col :span="12">
                <el-checkbox v-model="form.config.textNormalization.normalizeUnicodeSpaces">统一空格</el-checkbox>
              </el-col>
              <el-col :span="12">
                <el-checkbox v-model="form.config.textNormalization.collapseBlankLines">合并多余空行</el-checkbox>
              </el-col>
              <el-col :span="12">
                <el-checkbox v-model="form.config.textNormalization.dropNoiseLines">丢弃噪声行</el-checkbox>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最短行" label-width="72px">
                  <el-input-number v-model="form.config.textNormalization.minLineLength" :min="0" :max="20" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="丢弃行正则" label-width="96px">
              <el-input
                v-model="dropPatternsText"
                type="textarea"
                :rows="3"
                placeholder="每行一条正则，如 ^第\\s*\\d+\\s*页$"
              />
            </el-form-item>
          </el-collapse-item>
        </el-collapse>
        <el-form-item label="分块策略">
          <el-select v-model="form.config.chunkingStrategy" style="width: 200px">
            <el-option label="语义优先（段落）" value="paragraph-first" />
            <el-option label="固定长度" value="fixed-char" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="块大小">
              <el-input-number v-model="form.config.chunkSize" :min="100" :max="8000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="重叠">
              <el-input-number v-model="form.config.chunkOverlap" :min="0" :max="2000" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="最大块">
              <el-input-number v-model="form.config.maxChunkSize" :min="200" :max="16000" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="分块前规范化">
          <el-switch v-model="form.config.normalizeBeforeChunk" />
        </el-form-item>
        <el-form-item label="预览样本文本">
          <el-input
            v-model="previewText"
            type="textarea"
            :rows="5"
            placeholder="粘贴文本，或由 TXT/MD/PDF/Word 解析填充"
            @input="previewDone = false"
          />
        </el-form-item>
        <el-form-item>
          <el-button plain :loading="parseLoading" :disabled="!stagedFiles.length" @click="extractFromFirstFile">
            从已选文件解析
          </el-button>
          <el-button type="primary" plain :loading="previewLoading" @click="runPreview">
            预览分块结果
          </el-button>
          <el-tag v-if="previewDone" type="success" size="small" class="done-tag">已预览</el-tag>
          <el-tag v-else type="warning" size="small" class="done-tag">须预览后才能下一步</el-tag>
        </el-form-item>
        <el-table v-if="previewChunkItems.length" :data="previewChunkItems" size="small" max-height="220" stripe>
          <el-table-column prop="index" label="#" width="48" />
          <el-table-column prop="length" label="字数" width="72" />
          <el-table-column prop="content" label="内容摘录" show-overflow-tooltip />
        </el-table>
        <p v-if="previewSummary" class="hint">{{ previewSummary }}</p>
      </el-form>
    </div>

    <div v-show="step === 3" class="step-body">
      <el-form label-width="120px">
        <el-form-item label="Embedding 模型">
          <el-input v-model="form.config.embeddingProvider" disabled style="width: 100px" />
          <el-input
            v-model="form.config.embeddingModel"
            style="width: 200px; margin-left: 8px"
            placeholder="nomic-embed-text"
          />
          <span class="hint-inline">维度 {{ form.config.embeddingDimension }}</span>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon>
          <template #title>建仓流水线（固定）</template>
          {{ fixedPipeline }}
        </el-alert>
      </el-form>
    </div>

    <template #footer>
      <el-button round @click="visible = false">取消</el-button>
      <el-button v-if="step > 0" round @click="step--">上一步</el-button>
      <el-button v-if="step < 3" type="primary" round @click="nextStep">下一步</el-button>
      <el-button v-else type="primary" round :loading="submitting" @click="submit">
        创建并完成配置
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createVectorLibrary } from '../api/library'
import { previewChunks as fetchChunkPreview } from '../api/chunk'
import { uploadDocumentsBatch, collectDocument, parsePreview } from '../api/ingest'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  tenantId: { type: String, required: true }
})

const emit = defineEmits(['update:modelValue', 'created'])

const fixedPipeline =
  '数据源接入 → 文档解析 → 文本清洗 → 分块 → 向量化 → 写入知识库'

const router = useRouter()
const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const step = ref(0)
const submitting = ref(false)
const previewLoading = ref(false)
const parseLoading = ref(false)
const previewDone = ref(false)
const previewChunkItems = ref([])
const previewSummary = ref('')
const dropPatternsText = ref(
  '^\\d{1,4}$\n^第\\s*\\d+\\s*页$\n^Page\\s+\\d+\\s+of\\s+\\d+$\n^-{3,}$\n^_{3,}$'
)
const previewText = ref(
  '这是用于预览分块效果的示例段落。\n\n第二段内容会按空行切分，过长段落再按字符窗口切分，便于在正式入库前调整规则。'
)
const sampleHint = ref('')
const fileList = ref([])
const stagedFiles = ref([])

const defaultTextNormalization = () => ({
  enabled: true,
  collapseBlankLines: true,
  trimLines: true,
  removeControlChars: true,
  normalizeUnicodeSpaces: true,
  dropNoiseLines: true,
  minLineLength: 2,
  linePatternsToDrop: [
    '^\\d{1,4}$',
    '^第\\s*\\d+\\s*页$',
    '^Page\\s+\\d+\\s+of\\s+\\d+$',
    '^-{3,}$',
    '^_{3,}$'
  ]
})

const defaultConfig = () => ({
  storageType: 'minio',
  storagePathPrefix: '',
  localBasePath: './data/documents',
  metadataDbType: 'postgresql',
  ingestSourceMode: 'upload',
  embeddingProvider: 'ollama',
  embeddingModel: 'nomic-embed-text',
  embeddingDimension: 768,
  chunkingStrategy: 'paragraph-first',
  chunkSize: 600,
  chunkOverlap: 100,
  minChunkSize: 80,
  maxChunkSize: 1200,
  minParagraphLength: 30,
  normalizeBeforeChunk: true,
  textNormalizationEnabled: true,
  textNormalization: defaultTextNormalization()
})

const form = reactive({
  name: '',
  description: '',
  ingestSourceMode: 'upload',
  seedUrl: '',
  config: defaultConfig()
})

watch(
  () => form.ingestSourceMode,
  (mode) => {
    form.config.ingestSourceMode = mode
  }
)

function syncDropPatterns() {
  form.config.textNormalization.linePatternsToDrop = dropPatternsText.value
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

function buildChunkPreviewPayload() {
  syncDropPatterns()
  const norm = { ...form.config.textNormalization, enabled: true }
  return {
    sampleText: previewText.value,
    chunkingStrategy: form.config.chunkingStrategy,
    chunkSize: form.config.chunkSize,
    chunkOverlap: form.config.chunkOverlap,
    minChunkSize: form.config.minChunkSize,
    maxChunkSize: form.config.maxChunkSize,
    minParagraphLength: form.config.minParagraphLength,
    normalizeBeforeChunk: form.config.normalizeBeforeChunk,
    textNormalizationEnabled: form.config.textNormalizationEnabled,
    textNormalization: form.config.textNormalizationEnabled ? norm : null
  }
}

function onFileChange(_f, list) {
  fileList.value = list
  stagedFiles.value = list.map((x) => x.raw).filter(Boolean)
  previewDone.value = false
  loadSampleFromFirstTextFile()
}

function onFileRemove(_f, list) {
  fileList.value = list
  stagedFiles.value = list.map((x) => x.raw).filter(Boolean)
}

function loadSampleFromFirstTextFile() {
  const textFile = stagedFiles.value.find((f) => /\.(txt|md|markdown)$/i.test(f.name))
  if (!textFile) {
    sampleHint.value =
      stagedFiles.value.length > 0
        ? 'PDF/Word 等请点击「Tika 解析」；TXT/MD 将自动载入。'
        : ''
    return
  }
  const reader = new FileReader()
  reader.onload = () => {
    const t = String(reader.result || '').slice(0, 50000)
    if (t.trim()) {
      previewText.value = t
      sampleHint.value = `已从 ${textFile.name} 加载文本（最多 5 万字）`
    }
  }
  reader.readAsText(textFile, 'UTF-8')
}

async function extractFromFirstFile() {
  const file = stagedFiles.value[0]
  if (!file) {
    ElMessage.warning('请先选择文件')
    return
  }
  parseLoading.value = true
  previewDone.value = false
  try {
    const { data } = await parsePreview(file)
    previewText.value = data.text || ''
    const trunc = data.truncated ? '（已截断）' : ''
    sampleHint.value = `Tika 解析 ${data.fileName}（${data.mimeType}），${data.charCount} 字${trunc}`
    ElMessage.success('解析完成，可在第 3 步预览分块')
  } finally {
    parseLoading.value = false
  }
}

function validateStep(s) {
  if (s === 0) {
    if (!form.name?.trim()) {
      ElMessage.warning('请填写知识库名称')
      return false
    }
    if (form.ingestSourceMode === 'upload' && !stagedFiles.value.length) {
      ElMessage.warning('导入本地文档时请至少选择一个文件')
      return false
    }
    if (form.ingestSourceMode === 'crawl' && !form.seedUrl?.trim()) {
      ElMessage.warning('线上采集模式请填写 URL')
      return false
    }
    if (
      form.ingestSourceMode === 'both' &&
      !stagedFiles.value.length &&
      !form.seedUrl?.trim()
    ) {
      ElMessage.warning('请至少选择本地文件或填写采集 URL')
      return false
    }
  }
  if (s === 1 && form.config.storageType === 'local-fs' && !form.config.localBasePath?.trim()) {
    ElMessage.warning('请填写本地存储目录')
    return false
  }
  if (s === 2) {
    if (!previewText.value?.trim()) {
      ElMessage.warning('请填写或解析预览样本文本')
      return false
    }
    if (!previewDone.value) {
      ElMessage.warning('请先点击「预览分块结果」确认规则')
      return false
    }
  }
  return true
}

async function nextStep() {
  if (!validateStep(step.value)) return
  if (step.value === 0 && stagedFiles.value.length && !previewText.value?.trim()) {
    const f = stagedFiles.value[0]
    if (/\.(txt|md|markdown)$/i.test(f.name)) {
      loadSampleFromFirstTextFile()
    } else {
      await extractFromFirstFile()
    }
  }
  step.value++
}

async function runPreview() {
  if (!previewText.value?.trim()) {
    ElMessage.warning('请填写样本文本')
    return
  }
  previewLoading.value = true
  previewChunkItems.value = []
  previewSummary.value = ''
  try {
    const { data } = await fetchChunkPreview(buildChunkPreviewPayload())
    previewChunkItems.value = data.chunks || []
    previewSummary.value = `共 ${data.totalChunks} 块，样本文本 ${data.sampleLength} 字（展示每块前 500 字）`
    previewDone.value = true
  } finally {
    previewLoading.value = false
  }
}

async function submit() {
  if (!validateStep(2) || !validateStep(3)) return
  syncDropPatterns()
  submitting.value = true
  try {
    const { data: lib } = await createVectorLibrary({
      tenantId: props.tenantId,
      name: form.name.trim(),
      description: form.description,
      config: { ...form.config, ingestSourceMode: form.ingestSourceMode }
    })
    const libraryId = lib.libraryId
    emit('created', lib)

    if (stagedFiles.value.length) {
      await uploadDocumentsBatch(libraryId, props.tenantId, stagedFiles.value, true)
      ElMessage.success('知识库已创建，本地文件已开始上传入库')
    } else if (form.seedUrl?.trim()) {
      await collectDocument({
        libraryId,
        tenantId: props.tenantId,
        url: form.seedUrl.trim(),
        autoIndex: true
      })
      ElMessage.success('知识库已创建，URL 采集任务已提交')
    } else {
      ElMessage.success('知识库已创建')
    }

    visible.value = false
    router.push({ name: 'vectorLibraryDetail', params: { libraryId } })
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  step.value = 0
  form.name = ''
  form.description = ''
  form.ingestSourceMode = 'upload'
  form.seedUrl = ''
  Object.assign(form.config, defaultConfig())
  fileList.value = []
  stagedFiles.value = []
  previewChunkItems.value = []
  previewSummary.value = ''
  sampleHint.value = ''
  previewDone.value = false
  dropPatternsText.value =
    '^\\d{1,4}$\n^第\\s*\\d+\\s*页$\n^Page\\s+\\d+\\s+of\\s+\\d+$\n^-{3,}$\n^_{3,}$'
}

function onClosed() {
  resetForm()
}

watch(visible, (v) => {
  if (v) resetForm()
})
</script>

<style scoped>
.wizard-steps {
  margin-bottom: 20px;
}
.step-body {
  min-height: 320px;
  padding: 8px 4px 0;
}
.step-note {
  margin: 0 0 12px;
  font-size: 13px;
  color: #475569;
}
.hint,
.hint-inline {
  font-size: 12px;
  color: #64748b;
}
.hint-inline {
  margin-left: 8px;
}
.upload-icon {
  font-size: 40px;
  color: #38bdf8;
}
.done-tag {
  margin-left: 10px;
  vertical-align: middle;
}
</style>
