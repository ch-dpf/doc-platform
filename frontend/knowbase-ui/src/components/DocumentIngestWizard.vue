<template>
  <div class="ingestion-wizard" :class="{ 'ingestion-wizard--embedded': embedded }">
    <el-alert v-if="message" :title="message" :type="messageType === 'success' ? 'success' : 'error'" show-icon closable @close="message = ''" />

    <el-steps :active="currentStep" align-center finish-status="success" class="wizard-steps">
      <el-step title="上传文档" description="选择文件并上传" />
      <el-step title="解析与分段" description="Layout/OCR 解析与流水线预览" />
      <el-step title="入库完成" description="向量化与索引写入" />
    </el-steps>

    <!-- Step 1: Upload -->
    <PageCard v-show="currentStep === 0" :title="embedded ? undefined : '第一步：上传文档'" :class="{ 'wizard-section--embedded': embedded }">
      <h4 v-if="embedded" class="wizard-section__title">上传文档</h4>
      <el-form label-position="top" class="wizard-form">
        <DocumentPickPanel
          ref="pickPanelRef"
          v-model="selectedFiles"
          :max-files="MAX_FILES"
          :max-file-size="MAX_FILE_SIZE"
        />

        <div class="wizard-actions">
          <el-button type="primary" round :loading="uploading" :disabled="!canProceedUpload" @click="goToSegmentationStep">
            下一步：分段设置
          </el-button>
          <el-button
            type="success"
            round
            class="quick-ingest-button"
            :loading="quickIngesting"
            :disabled="!canProceedUpload"
            :style="quickIngestButtonStyle"
            @click="quickUploadAndIngest"
          >
            快速入库（跳过预览）
          </el-button>
        </div>
      </el-form>
    </PageCard>

    <!-- Step 2: Parse + Segmentation -->
    <PageCard v-show="currentStep === 1" title="第二步：解析预览与本次覆盖">
      <template #actions>
        <el-tag v-if="prepareResult" :type="prepareOk ? 'success' : 'warning'">
          {{ prepareResult.succeeded }}/{{ prepareResult.sourceCount }} 文档就绪
        </el-tag>
      </template>

      <el-form label-position="top" class="wizard-form wizard-form--wide">
        <div class="parse-strategy-panel">
          <div class="parse-strategy-main">
            <span class="parse-strategy-main__label">解析策略</span>
            <div class="parse-strategy-main__title">
              <strong>{{ parseStrategyTitle }}</strong>
              <el-tag v-if="parseMode === 'standard'" size="small" type="success" effect="plain">默认</el-tag>
              <el-tag v-else size="small" type="warning" effect="plain">修复模式</el-tag>
            </div>
            <p>{{ parseStrategyDescription }}</p>
          </div>
          <div class="parse-repair-actions">
            <el-button
              v-for="action in parseRepairActions"
              :key="action.value"
              size="small"
              plain
              :type="parseMode === action.value ? 'primary' : action.type"
              :title="action.hint"
              @click="setParseMode(action.value)"
            >
              {{ action.label }}
            </el-button>
          </div>
          <el-form-item v-if="parseMode === 'ocr'" label="OCR 语言" class="ocr-language-item">
            <el-input v-model="ocrLanguage" placeholder="chi_sim+eng（留空自动）" />
          </el-form-item>
        </div>

        <div class="ingest-config-panel" :class="{ 'ingest-config-panel--override': segmentationMode === 'advanced' }">
          <div class="ingest-config-main">
            <span class="ingest-config-main__label">分段配置</span>
            <div class="ingest-config-main__title">
              <strong>{{ segmentationMode === 'advanced' ? '本次上传覆盖' : '使用库默认配置' }}</strong>
              <el-tag v-if="segmentationMode === 'smart'" size="small" type="success" effect="plain">推荐</el-tag>
              <el-tag v-else size="small" type="warning" effect="plain">仅本次</el-tag>
            </div>
            <p>
              {{ segmentationMode === 'advanced'
                ? '只影响当前这批文档的预览与入库；长期规则请回到库配置中调整 Library Profile / Document Profile。'
                : '按库配置中的 Library Profile / Document Profile 执行解析、清洗与分块；上传页用于预览和临时修复。' }}
            </p>
          </div>
          <div class="ingest-config-actions">
            <el-button
              v-if="segmentationMode === 'smart'"
              size="small"
              plain
              type="warning"
              @click="segmentationMode = 'advanced'"
            >
              本次上传覆盖
            </el-button>
            <el-button v-else size="small" plain @click="segmentationMode = 'smart'">取消覆盖</el-button>
          </div>
        </div>

        <div v-if="segmentationMode === 'smart'" class="segment-panel segment-panel--smart">
          <p>库默认配置会按文档类型自动选择分块策略，并保留语义/结构边界、token 预算和递归字符切分兜底。</p>
          <div class="capability-strip">
            <span><b>表格</b> 多表区 / 多级表头 / 行角色 / 解析置信度</span>
            <span><b>PDF</b> 单元格级 table_row + bbox</span>
            <span><b>OCR</b> 块级 confidence / 低置信过滤</span>
          </div>
        </div>

        <div v-else class="segment-panel segment-panel--advanced">
          <div class="override-panel-head">
            <strong>本次上传覆盖</strong>
            <span>适合预览发现切块过长、标题上下文不足、或需要指定文档 Profile 的一次性修复。</span>
          </div>
          <div class="grid cols-2 compact-grid">
            <el-form-item label="Chunk 上限 (tokens)">
              <el-input-number v-model="advanced.chunkMaxTokens" :min="128" :max="8192" class="full-width" />
            </el-form-item>
            <el-form-item label="Chunk 上限 (chars)">
              <el-input-number v-model="advanced.chunkMaxChars" :min="128" :max="8192" class="full-width" />
            </el-form-item>
            <el-form-item label="重叠 (tokens)">
              <el-input-number v-model="advanced.chunkOverlapTokens" :min="0" :max="1024" class="full-width" />
            </el-form-item>
            <el-form-item label="重叠 (chars)">
              <el-input-number v-model="advanced.chunkOverlapChars" :min="0" :max="1024" class="full-width" />
            </el-form-item>
          </div>
          <el-form-item label="尺寸单位">
            <el-select v-model="advanced.chunkSizeUnit" class="full-width">
              <el-option label="Token（默认，生产推荐）" value="token" />
              <el-option label="字符（兜底/调试）" value="char" />
            </el-select>
          </el-form-item>
          <el-form-item label="文档 Profile（可选）">
            <el-select v-model="advanced.documentProfileCode" clearable filterable class="full-width" placeholder="留空则按文件类型自动选择">
              <el-option
                v-for="item in documentProfileOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="切块策略">
            <el-select v-model="advanced.chunkingStrategy" class="full-width">
              <el-option v-for="item in chunkingStrategies" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="保留结构边界">
            <el-switch v-model="advanced.preserveStructureBoundary" />
          </el-form-item>
          <el-form-item label="分段模式">
            <el-select v-model="advanced.chunkMode" class="full-width">
              <el-option label="扁平分段（主流 RAG）" value="flat" />
              <el-option label="父子分段（平台模式）" value="parent_child" />
            </el-select>
          </el-form-item>
          <el-form-item label="切分策略">
            <el-select v-model="advanced.splitMode" class="full-width">
              <el-option label="递归字符切分" value="recursive" />
              <el-option label="仅结构切分" value="structure_only" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题上下文">
            <el-switch v-model="advanced.prependHeadingContext" />
          </el-form-item>
          <el-form-item label="自定义分隔符（| 分隔，支持 \\n）">
            <el-input v-model="advanced.customSeparators" placeholder="\\n\\n|\\n|。|. | " />
          </el-form-item>
        </div>

        <el-divider />

        <div class="wizard-actions">
          <el-button round @click="currentStep = 0">上一步</el-button>
          <el-button round :loading="previewing" @click="runPipelinePreview">运行流水线预览</el-button>
          <el-button
            type="primary"
            round
            :loading="loading || polling"
            :disabled="!prepareOk"
            @click="confirmIngestion"
          >
            确认并开始入库
          </el-button>
        </div>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="publish-hint"
          title="文档索引成功即标记 INDEXED 并写入当前 active 代次，无需额外发布索引版本。"
        />
        <p v-if="!prepareOk" class="helper-text">请先运行流水线预览，确认解析/清洗/分段结果无误后再入库。</p>
      </el-form>

      <div v-if="uploadedUris.length" class="uploaded-summary">
        <span>已上传 {{ uploadedUris.length }} 个文件</span>
      </div>

      <div v-if="prepareResult" class="preview-panel">
        <el-alert
          v-if="qualityAlertVisible"
          :type="qualityAlertType"
          :closable="false"
          show-icon
          class="parse-confidence-banner"
          :title="qualityAlertTitle"
        >
          <template #default>
            <span>{{ qualityAlertDescription }}</span>
          </template>
        </el-alert>

        <div v-if="batchQualityInsight" class="quality-dashboard" :class="qualityPanelClass">
          <div class="quality-score">
            <strong>{{ batchQualityInsight.score ?? '--' }}</strong>
            <span>质量分</span>
          </div>
          <div class="quality-main">
            <div class="quality-main__head">
              <h4>{{ batchQualityInsight.summary || '入库质量分析' }}</h4>
              <el-tag :type="qualityTagType(batchQualityInsight.level)" effect="plain">
                {{ qualityLevelText(batchQualityInsight.level) }}
              </el-tag>
            </div>
            <p>{{ qualityBatchDescription }}</p>
            <div v-if="qualityQuickActions.length" class="quality-actions">
              <el-button
                v-for="action in qualityQuickActions"
                :key="action.id"
                size="small"
                plain
                :type="action.type"
                :title="action.hint || action.label"
                @click="applyQualityAction(action)"
              >
                {{ action.label }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="ingestion-insight-grid">
          <div class="insight-card">
            <span class="insight-card__label">命中文档 Profile</span>
            <strong>{{ profileSummary }}</strong>
          </div>
          <div class="insight-card">
            <span class="insight-card__label">内容类型</span>
            <strong>{{ contentFamilySummary }}</strong>
          </div>
          <div class="insight-card">
            <span class="insight-card__label">解析能力</span>
            <strong>{{ parserCapabilitySummary }}</strong>
          </div>
          <div class="insight-card" :class="{ 'insight-card--warning': hasLowParseConfidence }">
            <span class="insight-card__label">解析质量</span>
            <strong>{{ parseQualitySummary }}</strong>
            <span v-if="hasLowParseConfidence" class="insight-card__hint">建议复核分段预览</span>
          </div>
          <div class="insight-card" :class="{ 'insight-card--warning': qualityIssues.length }">
            <span class="insight-card__label">质量问题</span>
            <strong>{{ qualityIssues.length ? `${qualityIssues.length} 项` : '无明显风险' }}</strong>
            <span v-if="qualityIssues.length" class="insight-card__hint">查看下方建议动作</span>
          </div>
        </div>

        <div v-if="qualityIssues.length" class="quality-issue-list">
          <div v-for="issue in qualityIssues" :key="`${issue.stage}-${issue.title}-${issue.description}`" class="quality-issue">
            <div class="quality-issue__head">
              <el-tag size="small" :type="issueTagType(issue.severity)" effect="dark">{{ issueSeverityText(issue.severity) }}</el-tag>
              <span>{{ issueStageLabel(issue.stage) }}</span>
              <strong>{{ issue.title }}</strong>
            </div>
            <p>{{ issue.description }}</p>
            <small>{{ issue.action }}</small>
            <div v-if="issueQuickActions(issue).length" class="quality-issue__actions">
              <el-button
                v-for="action in issueQuickActions(issue)"
                :key="action.id"
                size="small"
                plain
                :type="action.type"
                :title="action.hint || action.label"
                @click="applyQualityAction(action)"
              >
                {{ action.label }}
              </el-button>
            </div>
          </div>
        </div>

        <el-tabs v-model="prepareTab" class="pipeline-tabs">
          <el-tab-pane label="解析" name="parse">
            <div class="preview-summary">
              <span>文档 {{ prepareResult.succeeded }}/{{ prepareResult.sourceCount }}</span>
            </div>
            <el-collapse accordion>
              <el-collapse-item
                v-for="document in prepareResult.documents"
                :key="'parse-' + document.sourceUri"
                :title="prepareDocumentTitle(document, 'parse')"
              >
                <p v-if="document.error" class="error-text">{{ document.error }}</p>
                <template v-else-if="document.parse">
                  <div v-if="document.qualityInsight" class="document-quality-strip" :class="documentQualityClass(document)">
                    <span>{{ document.qualityInsight.score }} 分</span>
                    <strong>{{ document.qualityInsight.summary }}</strong>
                    <em v-if="document.qualityInsight.issues?.length">{{ document.qualityInsight.issues.length }} 项建议</em>
                  </div>
                  <p class="helper-text">
                    {{ document.parse.parserCode }} · {{ document.parse.blockCount }} 结构块 ·
                    {{ document.parse.structureAware ? '结构感知' : '纯文本' }}
                  </p>
                  <div class="metadata-tags">
                    <el-tag v-for="item in documentMetadataTags(document)" :key="item.key" size="small" effect="plain">
                      {{ item.label }}: {{ item.value }}
                    </el-tag>
                  </div>
                  <el-table :data="document.parse.blocks" size="small" class="data-table">
                    <el-table-column prop="ordinal" label="#" width="50" />
                    <el-table-column prop="blockType" label="类型" width="90" />
                    <el-table-column label="行角色" width="90">
                      <template #default="{ row }">{{ row.metadata?.rowRole || '—' }}</template>
                    </el-table-column>
                    <el-table-column label="版面" width="90">
                      <template #default="{ row }">{{ row.metadata?.layoutRole || '—' }}</template>
                    </el-table-column>
                    <el-table-column label="可索引" width="88">
                      <template #default="{ row }">
                        <el-tag
                          v-if="row.metadata?.indexableHint != null"
                          size="small"
                          :type="indexableHintTagType(row.metadata?.indexableHint)"
                        >
                          {{ formatIndexableHint(row.metadata?.indexableHint) }}
                        </el-tag>
                        <span v-else>—</span>
                      </template>
                    </el-table-column>
                    <el-table-column label="页码" width="70">
                      <template #default="{ row }">{{ row.metadata?.pageNumber ?? '—' }}</template>
                    </el-table-column>
                    <el-table-column label="结构元数据" min-width="210">
                      <template #default="{ row }">
                        <div class="metadata-tags metadata-tags--compact">
                          <el-tag v-for="item in blockMetadataTags(row)" :key="item.key" size="small" effect="plain">
                            {{ item.label }} {{ item.value }}
                          </el-tag>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="contentPreview" label="内容预览" min-width="240" show-overflow-tooltip />
                  </el-table>
                </template>
              </el-collapse-item>
            </el-collapse>
          </el-tab-pane>

          <el-tab-pane label="清洗" name="normalize">
            <el-collapse accordion>
              <el-collapse-item
                v-for="document in prepareResult.documents"
                :key="'norm-' + document.sourceUri"
                :title="prepareDocumentTitle(document, 'normalize')"
              >
                <p v-if="document.error" class="error-text">{{ document.error }}</p>
                <template v-else-if="document.normalize">
                  <div class="preview-summary">
                    <span>字符 {{ document.normalize.rawCharCount }} → {{ document.normalize.normalizedCharCount }}</span>
                    <span>结构块 {{ document.normalize.rawBlockCount }} → {{ document.normalize.normalizedBlockCount }}</span>
                  </div>
                  <p class="helper-text">规则：{{ (document.normalize.appliedRules || []).join('、') || '无' }}</p>
                  <pre class="stage-preview">{{ document.normalize.textPreview }}</pre>
                </template>
              </el-collapse-item>
            </el-collapse>
          </el-tab-pane>

          <el-tab-pane label="分段" name="chunk">
            <div class="preview-summary">
              <span>总分块 {{ totalChunkCount }}</span>
              <span>可索引 {{ totalIndexableChunks }}</span>
              <span v-if="prepareResult.failed">失败 {{ prepareResult.failed }}</span>
            </div>
            <el-collapse accordion>
              <el-collapse-item
                v-for="document in prepareResult.documents"
                :key="'chunk-' + document.sourceUri"
                :title="prepareDocumentTitle(document, 'chunk')"
              >
                <p v-if="document.error" class="error-text">{{ document.error }}</p>
                <template v-else-if="document.chunk">
                  <p class="helper-text">
                    {{ document.documentProfileCode }} · {{ document.chunk.indexableChunkCount }}/{{ document.chunk.chunkCount }} 可索引
                  </p>
                  <el-table
                    :data="document.chunk.chunks"
                    size="small"
                    class="data-table"
                    :row-class-name="chunkPreviewRowClass"
                  >
                    <el-table-column prop="ordinal" label="#" width="50" />
                    <el-table-column prop="boundaryType" label="边界" width="100" />
                    <el-table-column prop="tokenCount" label="Tokens" width="90" />
                    <el-table-column label="可索引" width="80">
                      <template #default="{ row }">
                        <el-tag size="small" :type="row.indexable ? 'success' : 'info'">{{ row.indexable ? '是' : '否' }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="分段元数据" min-width="220">
                      <template #default="{ row }">
                        <div class="metadata-tags metadata-tags--compact">
                          <el-tag v-for="item in chunkMetadataTags(row)" :key="item.key" size="small" effect="plain">
                            {{ item.label }} {{ item.value }}
                          </el-tag>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="content" label="内容预览" min-width="240" show-overflow-tooltip />
                  </el-table>
                </template>
              </el-collapse-item>
            </el-collapse>
          </el-tab-pane>
        </el-tabs>
      </div>
    </PageCard>

    <!-- Step 3: Result -->
    <PageCard v-show="currentStep === 2" title="第三步：入库执行">
      <template v-if="latestRun">
        <div class="summary-metrics" style="margin-bottom: 14px">
          <div class="summary-metric summary-metric--primary">
            <span class="summary-metric__label">输入文档</span>
            <span class="summary-metric__value">{{ formatNumber(latestRun.inputDocuments) }}</span>
          </div>
          <div class="summary-metric">
            <span class="summary-metric__label">成功</span>
            <span class="summary-metric__value">{{ formatNumber(latestRun.succeededDocuments) }}</span>
          </div>
          <div class="summary-metric">
            <span class="summary-metric__label">失败</span>
            <span class="summary-metric__value">{{ formatNumber(latestRun.failedDocuments) }}</span>
          </div>
          <div class="summary-metric">
            <span class="summary-metric__label">分块</span>
            <span class="summary-metric__value">{{ formatNumber(latestRun.chunkCount) }}</span>
          </div>
          <div class="summary-metric">
            <span class="summary-metric__label">索引版本</span>
            <span class="summary-metric__value">{{ latestRun.indexVersionId ? shortId(latestRun.indexVersionId, 8) : '—' }}</span>
          </div>
        </div>
        <div class="bar"><span :style="{ width: `${progressPercent}%` }" /></div>
        <p class="helper-text">
          <el-tag :type="statusTagType(latestRun.status)" style="margin-right: 8px">{{ latestRun.status }}</el-tag>
          {{ latestRun.message || (polling ? '正在向量化并写入索引…' : '任务已提交') }}
        </p>
        <p class="row-meta">Run {{ shortId(latestRun.runId, 12) }} · {{ formatDateTime(latestRun.updatedAt) }}</p>
        <p v-if="latestRun.traceId" class="row-meta">
          Trace {{ shortId(latestRun.traceId, 12) }}
          <el-button link type="primary" @click="openObservabilityTrace(latestRun.traceId)">在观测页查看</el-button>
        </p>
        <p class="row-meta">入库策略：文档 INDEXED 后即可检索（active 代次 upsert）</p>

        <el-table v-if="ingestionErrors.length" :data="ingestionErrors" size="small" class="data-table" style="margin-top: 16px">
          <el-table-column prop="sourceUri" label="来源" min-width="160" show-overflow-tooltip />
          <el-table-column prop="errorCode" label="错误码" width="120" />
          <el-table-column prop="errorMessage" label="错误" min-width="180" show-overflow-tooltip />
        </el-table>
      </template>
      <el-empty v-else description="等待入库任务启动…" />

      <div class="wizard-actions" style="margin-top: 20px">
        <el-button v-if="isTerminal(latestRun?.status)" type="primary" round @click="resetWizard">上传新文档</el-button>
      </div>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import PageCard from './PageCard.vue';
import DocumentPickPanel from './DocumentPickPanel.vue';
import {
  createIngestionRun,
  getIngestionCatalog,
  getIngestionRun,
  listIngestionErrors,
  prepareIngestion,
  uploadDocuments,
  uploadFiles
} from '../api';
import {
  BLOCK_METADATA_TAG_KEYS,
  CHUNK_METADATA_TAG_KEYS,
  DOCUMENT_METADATA_DISPLAY_KEYS,
  buildMetadataTags,
  collectLowConfidenceReasons,
  formatDateTime,
  formatIndexableHint,
  formatMetadataValue,
  formatNumber,
  hasAnyLowParseConfidence,
  shortId,
  summarizeParseQuality
} from '../format';

const props = defineProps({
  libraryId: { type: String, required: true },
  embedded: { type: Boolean, default: false }
});

const emit = defineEmits(['completed', 'message']);

const router = useRouter();

const MAX_FILES = 50;
const MAX_FILE_SIZE = 100 * 1024 * 1024;

const chunkingStrategies = [
  { value: 'structure_token_window', label: '结构/标题优先' },
  { value: 'paragraph_token_window', label: '段落' },
  { value: 'qa_token_window', label: '问答对' },
  { value: 'table_row_token_window', label: '表格行' },
  { value: 'code_token_window', label: '代码块' },
  { value: 'heading_code_token_window', label: '标题 + 代码' },
  { value: 'slide_token_window', label: '幻灯片/页面' },
  { value: 'dom_token_window', label: 'HTML 块' }
];

const documentProfileOptions = [
  { value: 'default_markdown', label: 'Markdown / 富文本 · markdown-structure' },
  { value: 'default_text', label: '纯文本 · text-structure' },
  { value: 'default_pdf', label: 'PDF Layout · page_token_window' },
  { value: 'default_pdf_structure', label: 'PDF 文本结构 · pdf-structure' },
  { value: 'default_table', label: '表格 / Excel / CSV · table-deep' },
  { value: 'default_scanned_document', label: '扫描 PDF · OCR Layout' },
  { value: 'default_image', label: '图片 OCR · OCR Layout' },
  { value: 'default_web_page', label: 'HTML / Web 页面 · html-structure' },
  { value: 'default_faq', label: 'FAQ 问答对 · qa' },
  { value: 'default_code_or_config', label: '代码 / 配置 · code_token_window' },
  { value: 'default_presentation', label: 'PPT / 演示文稿 · slide_token_window' },
  { value: 'default_rich_text', label: '通用富文本 · Tika fallback' }
];

const metadataDisplayKeys = DOCUMENT_METADATA_DISPLAY_KEYS;

const currentStep = ref(0);
const selectedFiles = ref([]);
const uploadedUris = ref([]);
const pickPanelRef = ref(null);

const parseMode = ref('standard');
const ocrLanguage = ref('');
const prepareTab = ref('parse');
const prepareResult = ref(null);
const ingestionCatalog = ref(null);

const segmentationMode = ref('smart');
const advanced = ref({
  chunkMaxTokens: 512,
  chunkOverlapTokens: 64,
  chunkMaxChars: 2048,
  chunkOverlapChars: 256,
  chunkSizeUnit: 'token',
  documentProfileCode: '',
  chunkingStrategy: 'structure_token_window',
  preserveStructureBoundary: true,
  chunkMode: 'flat',
  splitMode: 'recursive',
  prependHeadingContext: true,
  customSeparators: '\\n\\n|\\n|。|！|？|；|. | '
});

const previewValidated = ref(false);
const latestRun = ref(null);
const ingestionErrors = ref([]);

const uploading = ref(false);
const quickIngesting = ref(false);
const previewing = ref(false);
const loading = ref(false);
const polling = ref(false);
const message = ref('');
const messageType = ref('success');

const canProceedUpload = computed(() => Boolean(props.libraryId) && selectedFiles.value.length > 0);
const prepareOk = computed(() =>
  previewValidated.value
  && prepareResult.value
  && prepareResult.value.failed === 0
  && prepareResult.value.succeeded > 0
  && totalIndexableChunks.value > 0
);
const totalChunkCount = computed(() =>
  (prepareResult.value?.documents || []).reduce((sum, doc) => sum + (doc.chunk?.chunkCount || 0), 0)
);
const totalIndexableChunks = computed(() =>
  (prepareResult.value?.documents || []).reduce((sum, doc) => sum + (doc.chunk?.indexableChunkCount || 0), 0)
);
const profileSummary = computed(() => uniquePreviewValues('documentProfileCode').join(' / ') || '自动路由');
const contentFamilySummary = computed(() => uniquePreviewValues('contentFamily').join(' / ') || '—');
const parserCapabilitySummary = computed(() => {
  const parsers = new Set();
  for (const doc of prepareResult.value?.documents || []) {
    if (doc.parse?.parserCode) parsers.add(doc.parse.parserCode);
    if (doc.parse?.metadata?.parser) parsers.add(doc.parse.metadata.parser);
    if (doc.parse?.metadata?.parserEngine) parsers.add(doc.parse.metadata.parserEngine);
  }
  return [...parsers].join(' / ') || '—';
});
const parserHealthByCode = computed(() => {
  const mapped = {};
  for (const parser of ingestionCatalog.value?.parsers || []) {
    if (parser.code) {
      mapped[parser.code] = parser.health || null;
    }
  }
  return mapped;
});
const parseQualitySummary = computed(() =>
  summarizeParseQuality(prepareResult.value?.documents)
);
const hasLowParseConfidence = computed(() =>
  hasAnyLowParseConfidence(prepareResult.value?.documents)
);
const lowConfidenceReasons = computed(() =>
  collectLowConfidenceReasons(prepareResult.value?.documents)
);
const batchQualityInsight = computed(() => prepareResult.value?.qualityInsight || null);
const qualityIssues = computed(() => batchQualityInsight.value?.issues || []);
const qualityQuickActions = computed(() => {
  const actions = [];
  for (const issue of qualityIssues.value) {
    actions.push(...issueQuickActions(issue));
  }
  if (!actions.some(action => action.id === 'rerun-preview')) {
    actions.push({ id: 'rerun-preview', label: '重新预览', type: 'primary', kind: 'rerun-preview' });
  }
  return dedupeActions(actions).slice(0, 5);
});
const qualityPanelClass = computed(() => `quality-dashboard--${batchQualityInsight.value?.level || 'good'}`);
const qualityAlertVisible = computed(() =>
  Boolean(batchQualityInsight.value && batchQualityInsight.value.level !== 'good') || hasLowParseConfidence.value
);
const qualityAlertType = computed(() => batchQualityInsight.value?.level === 'risk' ? 'error' : 'warning');
const qualityAlertTitle = computed(() => {
  if (batchQualityInsight.value?.level === 'risk') return '入库质量存在高风险，建议处理后再确认入库。';
  if (batchQualityInsight.value?.level === 'review') return '入库质量建议复核。';
  return '解析置信度偏低，建议检查「分段」预览后再确认入库。';
});
const qualityAlertDescription = computed(() => {
  const backendReasons = qualityIssues.value.map(issue => issue.title).filter(Boolean);
  if (backendReasons.length) return backendReasons.slice(0, 3).join('；');
  if (lowConfidenceReasons.value.length) return lowConfidenceReasons.value.join('；');
  return batchQualityInsight.value?.summary || '请检查解析、清洗和分段预览。';
});
const qualityBatchDescription = computed(() => {
  const facts = batchQualityInsight.value?.facts || {};
  const parts = [];
  if (facts.succeeded != null || facts.failed != null) {
    parts.push(`成功 ${facts.succeeded ?? 0} 个，失败 ${facts.failed ?? 0} 个`);
  }
  if (facts.reviewDocuments != null || facts.riskDocuments != null) {
    parts.push(`需复核 ${Number(facts.reviewDocuments || 0) + Number(facts.riskDocuments || 0)} 个`);
  }
  return parts.length ? parts.join(' · ') : '基于解析、清洗、分段和引用线索自动生成。';
});
const parseStrategyTitle = computed(() => {
  if (parseMode.value === 'layout') return '版面优先';
  if (parseMode.value === 'ocr') return 'OCR 识别优先';
  return '自动推荐';
});
const parseStrategyDescription = computed(() => {
  if (parseMode.value === 'layout') return '用于修复复杂 PDF 的阅读顺序、表格区域和页内定位问题。';
  if (parseMode.value === 'ocr') return '用于修复扫描 PDF、图片或文本层为空的文档，速度会更慢。';
  return '默认按文件类型和文档 Profile 自动选择解析器；多数文档无需手动调整。';
});
const parseRepairActions = computed(() => [
  { value: 'standard', label: '自动推荐', type: 'success', hint: '恢复后端自动路由' },
  { value: 'layout', label: '版面优先', type: 'warning', hint: 'PDF 顺序乱、表格弱、缺少 bbox 时使用' },
  { value: 'ocr', label: 'OCR 识别', type: 'warning', hint: '扫描件、图片或解析为空时使用' }
]);
const progressPercent = computed(() => {
  if (!latestRun.value) return 0;
  const total = Math.max(1, latestRun.value.inputDocuments || 0);
  return Math.min(100, Math.round(((latestRun.value.succeededDocuments || 0) / total) * 100));
});
const quickIngestButtonStyle = computed(() => !canProceedUpload.value ? {
  color: '#047857',
  backgroundColor: '#d1fae5',
  borderColor: '#10b981',
  boxShadow: '0 1px 2px rgba(16, 185, 129, 0.12)'
} : {});

watch(segmentationMode, resetPreview);
watch(parseMode, resetPreview);
watch(ocrLanguage, resetPreview);
watch(advanced, resetPreview, { deep: true });

function resetPreview() {
  prepareResult.value = null;
  previewValidated.value = false;
}

function buildSegmentationOptions() {
  const base = {
    parseMode: parseMode.value,
    segmentationMode: segmentationMode.value,
    chunkMode: 'flat',
    splitMode: 'recursive',
    chunkSizeUnit: 'token',
    chunkMaxChars: 2048,
    chunkOverlapChars: 256,
    prependHeadingContext: true,
    maxPreviewChunks: 100,
    maxPreviewChars: 600,
    maxPreviewBlocks: 40
  };
  if (parseMode.value === 'ocr' && ocrLanguage.value.trim()) {
    base.ocrLanguage = ocrLanguage.value.trim();
  }
  if (segmentationMode.value === 'advanced') {
    return {
      ...base,
      chunkMaxTokens: advanced.value.chunkMaxTokens,
      chunkOverlapTokens: advanced.value.chunkOverlapTokens,
      chunkMaxChars: advanced.value.chunkMaxChars,
      chunkOverlapChars: advanced.value.chunkOverlapChars,
      chunkSizeUnit: advanced.value.chunkSizeUnit,
      chunkingStrategy: advanced.value.chunkingStrategy,
      preserveStructureBoundary: advanced.value.preserveStructureBoundary,
      chunkMode: advanced.value.chunkMode,
      splitMode: advanced.value.splitMode,
      prependHeadingContext: advanced.value.prependHeadingContext,
      customSeparators: advanced.value.customSeparators
    };
  }
  return base;
}

function buildDocumentProfileCode() {
  if (segmentationMode.value === 'advanced' && advanced.value.documentProfileCode.trim()) {
    return advanced.value.documentProfileCode.trim();
  }
  return null;
}

function statusTagType(status) {
  const value = String(status || '').toUpperCase();
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'PARTIAL_FAILED') return 'warning';
  return 'info';
}

function prepareDocumentTitle(document, stage) {
  if (document.error) {
    return `${document.sourceUri} · 失败`;
  }
  if (stage === 'parse' && document.parse) {
    return `${document.title || document.sourceUri} · ${document.parse.blockCount} 块`;
  }
  if (stage === 'normalize' && document.normalize) {
    return `${document.title || document.sourceUri} · 清洗后 ${document.normalize.normalizedCharCount} 字符`;
  }
  if (stage === 'chunk' && document.chunk) {
    return `${document.title || document.sourceUri} · ${document.chunk.chunkCount} 分段`;
  }
  return document.title || document.sourceUri;
}

function openObservabilityTrace(traceId) {
  if (!traceId) {
    return;
  }
  router.push({ path: '/observability', query: { traceId: String(traceId) } });
}

async function goToSegmentationStep() {
  if (!canProceedUpload.value) return;
  uploading.value = true;
  try {
    const data = await uploadFiles(selectedFiles.value);
    uploadedUris.value = (data.uploaded || []).map(item => item.uri);
    if (!uploadedUris.value.length) {
      const failureText = (data.failures || []).map(item => `${item.filename}: ${item.message}`).join('；');
      throw new Error(failureText || '没有文件上传成功');
    }
    if (data.failures?.length) {
      showMessage(`${uploadedUris.value.length} 个成功，${data.failures.length} 个失败`, 'warning');
    }
    prepareResult.value = null;
    previewValidated.value = false;
    currentStep.value = 1;
    loadIngestionCatalog();
    showMessage('文件已上传，请选择解析模式并运行流水线预览', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    uploading.value = false;
  }
}

async function loadIngestionCatalog() {
  if (ingestionCatalog.value) return;
  try {
    ingestionCatalog.value = await getIngestionCatalog();
  } catch {
    ingestionCatalog.value = null;
  }
}

async function quickUploadAndIngest() {
  if (!canProceedUpload.value) return;
  quickIngesting.value = true;
  currentStep.value = 2;
  try {
    const data = await uploadDocuments(props.libraryId, selectedFiles.value, {
      documentProfileCode: buildDocumentProfileCode(),
      autoStart: true
    });
    uploadedUris.value = (data.upload?.uploaded || []).map(item => item.uri);
    latestRun.value = data.ingestionRun;
    ingestionErrors.value = [];
    const storageText = data.storageType ? `（${data.storageType}）` : '';
    if (!latestRun.value) {
      showMessage(`文件已上传到对象存储${storageText}，但未创建入库任务`, 'success');
      return;
    }
    showMessage(`已上传到对象存储${storageText}并创建入库任务`, 'success');
    if (!isTerminal(latestRun.value.status)) {
      await pollIngestionRun(latestRun.value.runId);
    } else {
      await loadErrors();
      emitCompletedIfOk(latestRun.value);
    }
  } catch (error) {
    showMessage(error.message, 'error');
    currentStep.value = 0;
  } finally {
    quickIngesting.value = false;
  }
}

async function runPipelinePreview() {
  if (!props.libraryId || !uploadedUris.value.length) return;
  previewing.value = true;
  try {
    await loadIngestionCatalog();
    prepareResult.value = await prepareIngestion(props.libraryId, {
      libraryId: props.libraryId,
      sourceUris: uploadedUris.value,
      documentProfileCode: buildDocumentProfileCode(),
      prepareStage: 'all',
      options: buildSegmentationOptions()
    }, 'all');
    previewValidated.value = true;
    if (prepareResult.value.failed > 0) {
      showMessage(`预览完成，但有 ${prepareResult.value.failed} 个文档失败`, 'error');
      return;
    }
    showMessage(`流水线预览完成：${totalIndexableChunks.value} 个可索引分块`, 'success');
  } catch (error) {
    previewValidated.value = false;
    showMessage(error.message, 'error');
  } finally {
    previewing.value = false;
  }
}

async function confirmIngestion() {
  if (!prepareOk.value) return;
  loading.value = true;
  currentStep.value = 2;
  try {
    const data = await createIngestionRun(props.libraryId, {
      libraryId: props.libraryId,
      sourceUris: uploadedUris.value,
      sourceType: 'minio',
      documentProfileCode: buildDocumentProfileCode(),
      options: buildSegmentationOptions()
    });
    latestRun.value = data;
    ingestionErrors.value = [];
    if (!isTerminal(data.status)) {
      showMessage('入库任务已启动，正在向量化…', 'success');
      await pollIngestionRun(data.runId);
      return;
    }
    await loadErrors();
    showMessage(`入库完成：${data.status}。评测样本将自动生成（每批最多 20 条，前 5 条默认启用），请到「召回与评测」审核。`, data.status === 'FAILED' ? 'error' : 'success');
    emitCompletedIfOk(data);
  } catch (error) {
    showMessage(error.message, 'error');
    currentStep.value = 1;
  } finally {
    loading.value = false;
  }
}

async function loadErrors() {
  if (!latestRun.value?.runId) return;
  try {
    ingestionErrors.value = await listIngestionErrors(latestRun.value.runId);
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function pollIngestionRun(runId) {
  polling.value = true;
  try {
    for (let attempt = 0; attempt < 90; attempt++) {
      await sleep(1500);
      const data = await getIngestionRun(runId);
      latestRun.value = data;
      if (isTerminal(data.status)) {
        await loadErrors();
        showMessage(`入库结束：${data.status}。评测样本将自动生成（每批最多 20 条，前 5 条默认启用），请到「召回与评测」审核。`, data.status === 'FAILED' ? 'error' : 'success');
        emitCompletedIfOk(data);
        return;
      }
    }
    showMessage('任务仍在执行，请稍后查看', 'success');
  } finally {
    polling.value = false;
  }
}

function resetWizard() {
  currentStep.value = 0;
  selectedFiles.value = [];
  uploadedUris.value = [];
  pickPanelRef.value?.reset();
  prepareResult.value = null;
  previewValidated.value = false;
  latestRun.value = null;
  ingestionErrors.value = [];
  parseMode.value = 'standard';
  ocrLanguage.value = '';
  prepareTab.value = 'parse';
  segmentationMode.value = 'smart';
}

function uniquePreviewValues(key) {
  const values = new Set();
  for (const doc of prepareResult.value?.documents || []) {
    if (doc?.[key]) values.add(doc[key]);
  }
  return [...values];
}

function documentMetadataTags(document) {
  const tags = [];
  if (document.documentProfileCode) tags.push({ key: 'profile', label: 'Profile', value: document.documentProfileCode });
  if (document.contentFamily) tags.push({ key: 'family', label: '类型', value: document.contentFamily });
  const metadata = document.parse?.metadata || {};
  for (const [key, label] of metadataDisplayKeys) {
    if (metadata[key] !== undefined && metadata[key] !== null && metadata[key] !== '') {
      tags.push({ key, label, value: formatMetadataValue(metadata[key]) });
    }
  }
  return tags.slice(0, 12);
}

function blockMetadataTags(row) {
  return buildMetadataTags(row?.metadata || {}, BLOCK_METADATA_TAG_KEYS);
}

function chunkMetadataTags(row) {
  return buildMetadataTags(row?.metadata || {}, CHUNK_METADATA_TAG_KEYS);
}

function indexableHintTagType(value) {
  return value === true || value === 'true' ? 'success' : 'info';
}

function chunkPreviewRowClass({ row }) {
  return row?.indexable ? '' : 'chunk-preview-row--muted';
}

function setParseMode(value) {
  parseMode.value = value;
  prepareTab.value = 'parse';
}

function issueQuickActions(issue) {
  if (!issue) return [];
  const stage = String(issue.stage || '');
  const title = String(issue.title || '');
  const description = String(issue.description || '');
  const actionText = String(issue.action || '');
  const text = `${title} ${description} ${actionText}`;
  const actions = [];

  if (stage === 'parse' || text.includes('解析') || text.includes('OCR') || text.includes('Layout')) {
    actions.push({ id: 'view-parse', label: '查看解析', type: 'info', kind: 'tab', tab: 'parse' });
    if (parseMode.value !== 'layout') {
      actions.push({ id: 'set-layout', label: '改用 Layout', type: 'warning', kind: 'set-parse-mode', value: 'layout' });
    }
    if (parseMode.value !== 'ocr') {
      actions.push({ id: 'set-ocr', label: '改用 OCR', type: 'warning', kind: 'set-parse-mode', value: 'ocr' });
    }
  }

  if (stage === 'normalize' || text.includes('清洗')) {
    actions.push({ id: 'view-normalize', label: '查看清洗', type: 'info', kind: 'tab', tab: 'normalize' });
  }

  if (stage === 'chunk' || stage === 'citation' || text.includes('分块') || text.includes('分段') || text.includes('引用')) {
    actions.push({ id: 'view-chunk', label: '查看分段', type: 'info', kind: 'tab', tab: 'chunk' });
    actions.push({ id: 'open-advanced', label: '打开高级分段', type: 'warning', kind: 'open-advanced' });
  }

  if (text.includes('偏碎') || text.includes('行组合') || text.includes('提高 chunk')) {
    actions.push({ id: 'increase-chunk-budget', label: '增大 Chunk', type: 'warning', kind: 'adjust-chunk-budget', direction: 'increase' });
  }

  if (text.includes('过大') || text.includes('降低 chunk')) {
    actions.push({ id: 'decrease-chunk-budget', label: '减小 Chunk', type: 'warning', kind: 'adjust-chunk-budget', direction: 'decrease' });
  }

  if (text.includes('父子')) {
    actions.push({ id: 'set-parent-child', label: '启用父子分段', type: 'warning', kind: 'set-parent-child' });
  }

  if (text.includes('table-deep') || text.includes('表格') || text.includes('tableRegion')) {
    actions.push({ id: 'set-table-profile', label: '使用表格 Profile', type: 'warning', kind: 'set-document-profile', value: 'default_table' });
  }

  return enrichQualityActions(dedupeActions(actions));
}

function dedupeActions(actions) {
  const seen = new Set();
  const result = [];
  for (const action of actions || []) {
    if (!action?.id || seen.has(action.id)) continue;
    seen.add(action.id);
    result.push(action);
  }
  return result;
}

function enrichQualityActions(actions) {
  return (actions || []).map(action => {
    const parserCode = actionParserCode(action);
    if (!parserCode) return action;
    const health = parserHealthByCode.value[parserCode];
    if (!health) return action;
    const status = health.status || 'UNKNOWN';
    const suffix = parserHealthSuffix(status);
    return {
      ...action,
      parserCode,
      health,
      label: suffix ? `${action.label}${suffix}` : action.label,
      type: parserHealthActionType(status, action.type),
      hint: health.message || action.label
    };
  });
}

function actionParserCode(action) {
  if (action.kind === 'set-parse-mode' && action.value === 'layout') {
    return 'pdf-layout';
  }
  if (action.kind === 'set-parse-mode' && action.value === 'ocr') {
    return 'ocr-layout';
  }
  if (action.kind === 'set-document-profile' && action.value === 'default_table') {
    return 'table-deep';
  }
  return null;
}

function parserHealthSuffix(status) {
  if (status === 'DEGRADED') return '（降级）';
  if (status === 'UNCONFIGURED') return '（未配置）';
  if (status === 'UNKNOWN') return '（待验证）';
  return '';
}

function parserHealthActionType(status, fallback) {
  if (status === 'UNCONFIGURED') return 'danger';
  if (status === 'DEGRADED' || status === 'UNKNOWN') return 'warning';
  return fallback;
}

function applyQualityAction(action) {
  if (!action) return;
  if (action.kind === 'tab') {
    prepareTab.value = action.tab;
    return;
  }
  if (action.kind === 'rerun-preview') {
    runPipelinePreview();
    return;
  }
  if (action.kind === 'set-parse-mode') {
    setParseMode(action.value);
    showMessage(actionHealthMessage(
      action,
      `已切换为${action.value === 'ocr' ? 'OCR + 版面' : 'Layout 版面'}解析，请重新运行预览`
    ), action.health?.status === 'UNCONFIGURED' ? 'error' : 'success');
    return;
  }
  if (action.kind === 'open-advanced') {
    segmentationMode.value = 'advanced';
    prepareTab.value = 'chunk';
    showMessage('已打开高级分段，请调整后重新运行预览', 'success');
    return;
  }
  if (action.kind === 'adjust-chunk-budget') {
    segmentationMode.value = 'advanced';
    if (action.direction === 'increase') {
      advanced.value.chunkMaxTokens = Math.min(8192, Math.max(advanced.value.chunkMaxTokens + 256, Math.round(advanced.value.chunkMaxTokens * 1.5)));
      advanced.value.chunkOverlapTokens = Math.min(1024, Math.max(advanced.value.chunkOverlapTokens, 96));
    } else {
      advanced.value.chunkMaxTokens = Math.max(128, Math.floor(advanced.value.chunkMaxTokens * 0.7));
      advanced.value.chunkOverlapTokens = Math.min(advanced.value.chunkOverlapTokens, Math.floor(advanced.value.chunkMaxTokens * 0.2));
    }
    prepareTab.value = 'chunk';
    showMessage('已调整 Chunk 参数，请重新运行预览', 'success');
    return;
  }
  if (action.kind === 'set-parent-child') {
    segmentationMode.value = 'advanced';
    advanced.value.chunkMode = 'parent_child';
    advanced.value.prependHeadingContext = true;
    prepareTab.value = 'chunk';
    showMessage('已启用父子分段，请重新运行预览', 'success');
    return;
  }
  if (action.kind === 'set-document-profile') {
    segmentationMode.value = 'advanced';
    advanced.value.documentProfileCode = action.value;
    advanced.value.chunkingStrategy = action.value === 'default_table' ? 'table_row_token_window' : advanced.value.chunkingStrategy;
    prepareTab.value = 'parse';
    showMessage(actionHealthMessage(action, '已切换文档 Profile，请重新运行预览'), 'success');
  }
}

function actionHealthMessage(action, baseMessage) {
  if (!action?.health || action.health.status === 'READY') {
    return baseMessage;
  }
  return `${baseMessage}；解析依赖${healthLabel(action.health.status)}：${action.health.message || '请检查解析器配置'}`;
}

function qualityTagType(level) {
  if (level === 'good') return 'success';
  if (level === 'risk') return 'danger';
  return 'warning';
}

function qualityLevelText(level) {
  if (level === 'good') return '可入库';
  if (level === 'risk') return '高风险';
  return '建议复核';
}

function healthLabel(status) {
  if (status === 'READY') return '可用';
  if (status === 'DEGRADED') return '降级';
  if (status === 'UNCONFIGURED') return '未配置';
  return '待验证';
}

function issueTagType(severity) {
  if (severity === 'high') return 'danger';
  if (severity === 'medium') return 'warning';
  return 'info';
}

function issueSeverityText(severity) {
  if (severity === 'high') return '高';
  if (severity === 'medium') return '中';
  return '提示';
}

function issueStageLabel(stage) {
  const labels = {
    parse: '解析',
    normalize: '清洗',
    chunk: '分段',
    citation: '引用',
    prepare: '准备'
  };
  return labels[stage] || stage || '质量';
}

function documentQualityClass(document) {
  return `document-quality-strip--${document?.qualityInsight?.level || 'good'}`;
}

function isTerminal(status) {
  return ['SUCCEEDED', 'PARTIAL_FAILED', 'FAILED', 'CANCELLED'].includes(String(status || '').toUpperCase());
}

function emitCompletedIfOk(run) {
  const status = String(run?.status || '').toUpperCase();
  if (status === 'SUCCEEDED' || status === 'PARTIAL_FAILED') {
    emit('completed', run);
  }
}

function showMessage(text, type) {
  message.value = text || '操作失败';
  messageType.value = type;
  emit('message', text, type);
}

function sleep(ms) {
  return new Promise(resolve => window.setTimeout(resolve, ms));
}

watch(
  () => props.libraryId,
  () => resetWizard()
);
</script>

<style scoped>
.ingestion-wizard--embedded {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wizard-section__title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
}

.wizard-section--embedded {
  box-shadow: none;
  border: none;
  padding: 0;
}
.wizard-steps {
  margin-top: 8px;
}

.wizard-form {
  max-width: 720px;
}

.wizard-form--wide {
  max-width: 960px;
}

.parse-strategy-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fafc;
}

.parse-strategy-main {
  min-width: 0;
}

.parse-strategy-main__label {
  display: block;
  margin-bottom: 5px;
  color: #64748b;
  font-size: 12px;
}

.parse-strategy-main__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.parse-strategy-main__title strong {
  color: #0f172a;
  font-size: 15px;
}

.parse-strategy-main p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.parse-repair-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  max-width: 360px;
}

.parse-repair-actions :deep(.el-button) {
  margin-left: 0;
}

.ingest-config-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: start;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
}

.ingest-config-panel--override {
  border-color: #fde68a;
  background: #fffbeb;
}

.ingest-config-main {
  min-width: 0;
}

.ingest-config-main__label {
  display: block;
  margin-bottom: 5px;
  color: #64748b;
  font-size: 12px;
}

.ingest-config-main__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.ingest-config-main__title strong {
  color: #0f172a;
  font-size: 15px;
}

.ingest-config-main p {
  margin: 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.ingest-config-actions {
  display: flex;
  justify-content: flex-end;
}

.ingest-config-actions :deep(.el-button) {
  margin-left: 0;
}

.ocr-language-item {
  margin-top: 10px;
  margin-bottom: 0;
}

.capability-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.capability-strip span {
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(14, 165, 233, 0.16);
  color: #334155;
  font-size: 12px;
}

.publish-switch {
  align-self: center;
  margin-left: 4px;
}

:deep(.quick-ingest-button.el-button.is-disabled),
:deep(.quick-ingest-button.el-button.is-disabled:hover),
:deep(.quick-ingest-button.el-button.is-disabled:focus) {
  color: #047857 !important;
  background: #d1fae5 !important;
  border: 1px solid #10b981 !important;
  box-shadow: 0 1px 2px rgba(16, 185, 129, 0.12);
  opacity: 0.72;
}

.pipeline-tabs {
  margin-top: 8px;
}

.stage-preview {
  margin: 0;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 240px;
  overflow: auto;
}

.wizard-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.segment-panel {
  padding: 14px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.6;
}

.segment-panel--smart {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.segment-panel--advanced {
  background: #f4f4f5;
  border: 1px solid #e9e9eb;
}

.override-panel-head {
  display: grid;
  gap: 4px;
  margin-bottom: 12px;
}

.override-panel-head strong {
  color: #0f172a;
  font-size: 14px;
}

.override-panel-head span {
  color: #64748b;
  font-size: 13px;
}

.segment-hints {
  margin: 8px 0 0;
  padding-left: 18px;
  color: var(--text-secondary, #666);
}

.uploaded-summary {
  margin-top: 16px;
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.preview-panel {
  margin-top: 20px;
}

.quality-dashboard {
  display: grid;
  grid-template-columns: 104px 1fr;
  gap: 14px;
  align-items: stretch;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fafc;
  margin-bottom: 14px;
}

.quality-dashboard--good {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.quality-dashboard--review {
  border-color: #fde68a;
  background: #fffbeb;
}

.quality-dashboard--risk {
  border-color: #fecaca;
  background: #fef2f2;
}

.quality-score {
  display: grid;
  place-items: center;
  align-content: center;
  min-height: 88px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.quality-score strong {
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
}

.quality-score span {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.quality-main {
  min-width: 0;
}

.quality-main__head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.quality-main__head h4 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.quality-main p {
  margin: 0 0 10px;
  color: #475569;
  font-size: 13px;
}

.quality-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.quality-actions :deep(.el-button) {
  margin-left: 0;
}

.ingestion-insight-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.parse-confidence-banner {
  margin-bottom: 14px;
}

.insight-card--warning {
  border-color: rgba(245, 158, 11, 0.35);
  background:
    linear-gradient(135deg, rgba(245, 158, 11, 0.12), rgba(251, 191, 36, 0.06)),
    #fff;
}

.insight-card__hint {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #b45309;
  font-weight: 500;
}

.insight-card {
  padding: 14px 16px;
  border: 1px solid rgba(14, 165, 233, 0.14);
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(14, 165, 233, 0.08), rgba(16, 185, 129, 0.04)),
    #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.insight-card__label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #64748b;
}

.insight-card strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.35;
}

.preview-summary {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  font-size: 13px;
  color: var(--text-secondary, #666);
}

.quality-issue-list {
  display: grid;
  gap: 8px;
  margin-bottom: 16px;
}

.quality-issue {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.quality-issue__head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.quality-issue__head strong {
  color: #0f172a;
  font-size: 13px;
}

.quality-issue p {
  margin: 0 0 4px;
  color: #475569;
  font-size: 13px;
}

.quality-issue small {
  color: #0f766e;
}

.quality-issue__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.quality-issue__actions :deep(.el-button) {
  margin-left: 0;
}

.document-quality-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 10px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
}

.document-quality-strip--good {
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.document-quality-strip--review {
  border-color: #fde68a;
  background: #fffbeb;
}

.document-quality-strip--risk {
  border-color: #fecaca;
  background: #fef2f2;
}

.document-quality-strip span {
  font-weight: 700;
}

.document-quality-strip strong {
  font-size: 12px;
}

.document-quality-strip em {
  color: #64748b;
  font-style: normal;
}

.metadata-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0 12px;
}

.metadata-tags--compact {
  margin: 0;
}

.metadata-tags :deep(.el-tag) {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  vertical-align: middle;
}

:deep(.chunk-preview-row--muted) {
  --el-table-tr-bg-color: #f8fafc;
  color: #64748b;
}

.error-text {
  color: var(--el-color-danger);
}

@media (max-width: 1200px) {
  .ingestion-insight-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .parse-strategy-panel,
  .ingest-config-panel {
    grid-template-columns: 1fr;
  }

  .parse-repair-actions,
  .ingest-config-actions {
    justify-content: flex-start;
    max-width: none;
  }

  .ingestion-insight-grid {
    grid-template-columns: 1fr;
  }
}
</style>
