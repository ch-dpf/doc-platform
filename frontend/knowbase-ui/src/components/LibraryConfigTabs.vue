<template>
  <el-tabs
    v-model="activeTab"
    tab-position="left"
    class="library-config-tabs"
  >
    <el-tab-pane label="基本信息" name="basic">
      <div class="tab-pane-body">
        <section class="cfg-section">
          <header class="cfg-section__head">
            <span class="cfg-section__title">基本信息</span>
          </header>
          <LibraryBasicFields :form="form" layout="inline" />
        </section>
      </div>
    </el-tab-pane>

    <el-tab-pane label="解析配置" name="parsing">
      <LibraryParsingConfigTab :form="form" />
    </el-tab-pane>

    <el-tab-pane label="分块配置" name="pipeline">
      <div class="tab-pane-body">
        <section class="cfg-section cfg-section--chunk">
          <header class="cfg-section__head">
            <span class="cfg-section__title">分块</span>
          </header>
          <p class="cfg-section__intro">
            入库前将文档切分为可检索片段。策略按文件类型自动路由；合并规则由系统统一配置。
          </p>
          <div v-if="form.config.primaryChunkProfileId" class="chunk-profile-meta">
            <span class="chunk-profile-meta__label">主分块档</span>
            <code class="chunk-profile-meta__id">{{ form.config.primaryChunkProfileId }}</code>
            <span class="chunk-profile-meta__hint">默认问答仅检索此档；采集可产生其他分块档但问答默认不跨档</span>
          </div>

          <el-collapse v-model="chunkCollapseExpanded" class="strategy-collapse">
            <el-collapse-item name="types">
              <template #title>
                <div class="strategy-collapse__head">
                  <span class="chunk-part__label">分块策略</span>
                  <span class="strategy-collapse__summary">{{ strategySummaryText }}</span>
                </div>
              </template>
              <p class="chunk-part__desc">展示各类型文档入库时将采用的策略；库级分隔符优先生效。</p>
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
                  <span class="strategy-collapse__summary">{{ chunkParamsSummaryText }}</span>
                </div>
              </template>
              <p class="chunk-part__desc">
                目标块大小与重叠作用于全库；超长段落再按重叠窗口定长切分。
              </p>
              <div class="chunk-metric-row">
                <div class="chunk-metric">
                  <div class="chunk-metric__head">
                    <span class="chunk-metric__name">分块大小</span>
                    <span class="chunk-metric__value">{{ form.config.chunkSize }} 字</span>
                  </div>
                  <el-slider
                    v-model="form.config.chunkSize"
                    :min="chunkSizeRange.min"
                    :max="chunkSizeRange.max"
                    :step="chunkSizeRange.step"
                    show-input
                    :show-input-controls="false"
                  />
                  <p class="chunk-metric__hint">通用文档建议 500–800；FAQ 可降至 200–400</p>
                </div>
                <div class="chunk-metric">
                  <div class="chunk-metric__head">
                    <span class="chunk-metric__name">分块重叠</span>
                    <span class="chunk-metric__value">
                      {{ form.config.chunkOverlap }} 字
                      <span class="chunk-metric__ratio">≈ {{ overlapPercent }}%</span>
                    </span>
                  </div>
                  <el-slider
                    v-model="form.config.chunkOverlap"
                    :min="chunkOverlapRange.min"
                    :max="chunkOverlapRange.max"
                    :step="chunkOverlapRange.step"
                    show-input
                    :show-input-controls="false"
                  />
                  <p class="chunk-metric__hint">建议约为块大小的 10%–20%；结构化表格可设为 0</p>
                </div>
              </div>

              <div class="chunk-advanced">
                <div class="chunk-toggle-row">
                  <div class="chunk-toggle-row__text">
                    <span class="chunk-toggle-row__title">父子块</span>
                    <span class="chunk-toggle-row__desc">
                      长文档按标题切父段，子块用于向量检索（heading-level 时生效）
                    </span>
                  </div>
                  <el-switch v-model="form.config.hierarchicalChunkingEnabled" />
                </div>
                <div class="chunk-field-row">
                  <label class="chunk-field-row__label">自定义分隔符</label>
                  <el-input
                    v-model="form.config.chunkDelimiter"
                    placeholder="留空按 MIME 策略；\\n 表示换行，--- 表示横线分隔"
                    clearable
                    class="full-width"
                  />
                  <p class="chunk-field-row__hint">设置后优先按分隔符切段，再应用上方块大小与重叠</p>
                </div>
              </div>
            </el-collapse-item>

            <el-collapse-item name="profiles">
              <template #title>
                <div class="strategy-collapse__head">
                  <span class="chunk-part__label">分块档管理</span>
                  <span class="strategy-collapse__summary">
                    {{ chunkProfiles.length ? `${chunkProfiles.length} 个活跃档` : '暂无活跃档' }}
                  </span>
                </div>
              </template>
              <p class="chunk-part__desc">
                每次采集/解析按有效分块配置生成 <code>cp_*</code> 指纹。默认问答仅检索主档。
              </p>
              <el-alert
                v-if="migrationCandidates?.candidateCount > 0"
                class="migration-alert"
                type="warning"
                :closable="false"
                show-icon
              >
                <template #title>
                  尚有 {{ migrationCandidates.candidateCount }} 篇文档不在主档
                  <code class="migration-alert__primary">{{ migrationCandidates.primaryChunkProfileId }}</code>
                </template>
                <div class="migration-alert__body">
                  <span>默认问答将无法检索这些文档，建议迁移到当前主档。</span>
                  <el-button
                    size="small"
                    round
                    type="warning"
                    :loading="migrating"
                    @click.stop="openMigrationWizard"
                  >
                    一键迁移到主档
                  </el-button>
                </div>
              </el-alert>
              <div class="chunk-governance-row">
                <div class="chunk-toggle-row">
                  <div class="chunk-toggle-row__text">
                    <span class="chunk-toggle-row__title">允许采集覆盖分块</span>
                    <span class="chunk-toggle-row__desc">关闭后仅允许库默认分块档入库</span>
                  </div>
                  <el-switch v-model="form.config.allowCustomChunkProfiles" />
                </div>
                <el-form-item label="最大活跃档" label-width="96px" class="chunk-governance-row__limit">
                  <el-input-number
                    v-model="form.config.maxActiveChunkProfiles"
                    :min="1"
                    :max="20"
                    controls-position="right"
                  />
                </el-form-item>
              </div>
              <div class="chunk-profiles-toolbar">
                <el-button
                  size="small"
                  round
                  :loading="profilesLoading"
                  :disabled="!libraryId"
                  @click.stop="loadChunkProfiles"
                >
                  刷新
                </el-button>
                <el-button
                  size="small"
                  round
                  type="warning"
                  plain
                  :loading="backfillLoading"
                  :disabled="!libraryId"
                  @click.stop="runBackfill"
                >
                  回填历史档 ID
                </el-button>
              </div>
              <div v-if="activeBatchJob" class="batch-job-panel">
                <div class="batch-job-panel__head">
                  <span class="batch-job-panel__title">
                    {{ batchJobTypeLabel(activeBatchJob.jobType) }}
                    · {{ batchJobStatusLabel(activeBatchJob.status) }}
                  </span>
                  <code v-if="activeBatchJob.chunkProfileId" class="batch-job-panel__profile">
                    {{ activeBatchJob.chunkProfileId }}
                  </code>
                </div>
                <el-progress
                  :percentage="activeBatchJob.progressPercent"
                  :status="batchJobProgressStatus(activeBatchJob.status)"
                />
                <p class="batch-job-panel__meta">
                  {{ activeBatchJob.completedCount }} / {{ activeBatchJob.totalCount }} 完成
                  <span v-if="activeBatchJob.failedCount">，{{ activeBatchJob.failedCount }} 失败</span>
                </p>
              </div>
              <el-table
                v-loading="profilesLoading"
                :data="chunkProfiles"
                size="small"
                stripe
                empty-text="暂无活跃分块档（入库后将自动出现）"
                class="chunk-profiles-table"
              >
                <el-table-column label="分块档" min-width="140">
                  <template #default="{ row }">
                    <code class="chunk-profile-meta__id">{{ row.chunkProfileId }}</code>
                  </template>
                </el-table-column>
                <el-table-column prop="docCount" label="文档" width="72" align="center" />
                <el-table-column prop="chunkCount" label="分块" width="72" align="center" />
                <el-table-column label="状态" width="88" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.primary" size="small" type="success" effect="plain">主档</el-tag>
                    <el-tag v-else size="small" type="info" effect="plain">非主档</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="220" align="center">
                  <template #default="{ row }">
                    <el-button
                      v-if="!row.primary"
                      link
                      type="primary"
                      :loading="settingPrimaryId === row.chunkProfileId"
                      @click.stop="onSetPrimary(row)"
                    >
                      设为主档
                    </el-button>
                    <el-button
                      link
                      type="warning"
                      :loading="reindexingProfileId === row.chunkProfileId"
                      :disabled="!row.docCount"
                      @click.stop="onReindexProfile(row)"
                    >
                      重索引
                    </el-button>
                    <el-button
                      v-if="!row.primary"
                      link
                      type="danger"
                      :loading="archivingProfileId === row.chunkProfileId"
                      :disabled="!row.docCount"
                      @click.stop="onArchiveProfile(row)"
                    >
                      归档
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>

            <el-collapse-item name="batchJobs">
              <template #title>
                <div class="strategy-collapse__head">
                  <span class="chunk-part__label">批量任务历史</span>
                  <span class="strategy-collapse__summary">
                    {{ batchJobHistory.length ? `最近 ${batchJobHistory.length} 条` : '暂无记录' }}
                  </span>
                </div>
              </template>
              <div class="chunk-profiles-toolbar">
                <el-button
                  size="small"
                  round
                  :loading="batchJobHistoryLoading"
                  :disabled="!libraryId"
                  @click.stop="loadBatchJobHistory"
                >
                  刷新
                </el-button>
              </div>
              <el-table
                v-loading="batchJobHistoryLoading"
                :data="batchJobHistory"
                size="small"
                stripe
                empty-text="暂无批量任务记录"
                class="chunk-profiles-table"
              >
                <el-table-column label="时间" width="148">
                  <template #default="{ row }">
                    {{ formatListTime(row.createdAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="类型" width="88">
                  <template #default="{ row }">
                    {{ batchJobTypeLabel(row.jobType) }}
                  </template>
                </el-table-column>
                <el-table-column label="分块档" min-width="120">
                  <template #default="{ row }">
                    <code v-if="row.chunkProfileId" class="chunk-profile-meta__id">
                      {{ row.chunkProfileId }}
                    </code>
                    <span v-else class="chunk-profiles-table__muted">全库</span>
                  </template>
                </el-table-column>
                <el-table-column label="状态" width="88" align="center">
                  <template #default="{ row }">
                    <el-tag
                      size="small"
                      :type="batchJobTagType(row.status)"
                      effect="plain"
                    >
                      {{ batchJobStatusLabel(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="进度" width="120" align="center">
                  <template #default="{ row }">
                    {{ row.completedCount }}/{{ row.totalCount }}
                    <span v-if="row.failedCount" class="batch-job-history__fail">
                      ({{ row.failedCount }} 败)
                    </span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="156" align="center">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.failedCount > 0"
                      link
                      type="info"
                      @click.stop="openJobDetail(row)"
                    >
                      详情
                    </el-button>
                    <el-button
                      v-if="row.retryable"
                      link
                      type="warning"
                      :loading="retryingJobId === row.jobId"
                      @click.stop="onRetryBatchJob(row)"
                    >
                      重试
                    </el-button>
                    <el-button
                      v-else-if="!isBatchJobTerminal(row.status)"
                      link
                      type="primary"
                      @click.stop="trackBatchJob(row.jobId, loadBatchJobHistory)"
                    >
                      跟踪
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-collapse-item>
          </el-collapse>
        </section>
      </div>
    </el-tab-pane>

    <el-tab-pane label="索引配置" name="index">
      <div class="tab-pane-body">
        <el-alert
          v-if="showReindexHint"
          class="index-hint-alert"
          type="warning"
          :closable="false"
          show-icon
          title="变更后建议重索引"
        >
        </el-alert>

      
        <section class="cfg-section">
          <header class="cfg-section__head">
            <span class="cfg-section__title">向量化</span>
          </header>
          <el-form-item label="Embedding">
            <el-select
              v-model="form.config.embeddingModel"
              filterable
              allow-create
              class="select-md full-width"
              @change="onEmbeddingModelChange"
            >
              <el-option
                v-for="opt in embeddingModelOptions"
                :key="opt.value"
                :label="`${opt.label}（${opt.dimension} 维）`"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="向量维度">
            <el-input-number
              v-model="form.config.embeddingDimension"
              :min="1"
              :max="4096"
              :disabled="embeddingDimensionLocked"
              controls-position="right"
            />
          </el-form-item>
        </section>
      </div>
    </el-tab-pane>

    <el-tab-pane label="检索配置" name="retrieval">
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
            <el-select
              v-model="form.config.retrieval.rerankModel"
              filterable
              allow-create
              clearable
              placeholder="使用库 Embedding 模型"
              class="select-md full-width"
            >
              <el-option
                v-for="opt in rerankModelOptions"
                :key="opt.value || '__default__'"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <p class="field-hint">默认与库 Embedding 一致；可指定其他向量模型作重排</p>
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
          </el-form-item>
          <el-form-item label="默认 Top K">
            <el-slider
              v-model="form.config.retrieval.defaultTopK"
              :min="1"
              :max="30"
              :step="1"
              show-input
              class="cfg-slider"
            />
            <p class="field-hint">智能问答会话可临时覆盖此默认值</p>
          </el-form-item>
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
  </el-tabs>

  <el-dialog
    v-model="archivePreviewVisible"
    title="归档分块档"
    width="520px"
    append-to-body
    @closed="onArchivePreviewClosed"
  >
    <p v-if="archivePreviewRow" class="archive-preview-desc">
      将软删除分块档 <code>{{ archivePreviewRow.chunkProfileId }}</code> 下的文档并清理向量，不可从界面撤销。
    </p>
    <el-skeleton v-if="archivePreviewLoading" :rows="4" animated />
    <template v-else-if="archivePreview">
      <p class="archive-preview-summary">
        共 <strong>{{ archivePreview.totalCount }}</strong> 篇文档将被归档
      </p>
      <ul v-if="archivePreview.previewItems?.length" class="archive-preview-list">
        <li v-for="item in archivePreview.previewItems" :key="item.docId">
          {{ item.fileName }}
        </li>
      </ul>
      <p
        v-if="archivePreview.totalCount > archivePreview.previewItems?.length"
        class="archive-preview-more"
      >
        另有 {{ archivePreview.totalCount - archivePreview.previewItems.length }} 篇未列出
      </p>
    </template>
    <template #footer>
      <el-button round @click="archivePreviewVisible = false">取消</el-button>
      <el-button
        type="danger"
        round
        :loading="archivingProfileId === archivePreviewRow?.chunkProfileId"
        :disabled="!archivePreview?.totalCount"
        @click="confirmArchive"
      >
        确认归档
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="migrationWizardVisible"
    title="迁移到主档"
    width="520px"
    append-to-body
    @closed="onMigrationWizardClosed"
  >
    <p v-if="migrationCandidates" class="migration-wizard-desc">
      将把非主档文档按当前库分块配置重索引，迁移到主档
      <code>{{ migrationCandidates.primaryChunkProfileId }}</code>。
    </p>
    <el-table
      v-if="migrationCandidates?.profileBreakdown?.length"
      :data="migrationCandidates.profileBreakdown"
      size="small"
      stripe
      class="migration-wizard-table"
    >
      <el-table-column label="当前分块档" min-width="160">
        <template #default="{ row }">
          <code>{{ row.chunkProfileId }}</code>
        </template>
      </el-table-column>
      <el-table-column prop="docCount" label="文档数" width="80" align="center" />
    </el-table>
    <p v-if="migrationCandidates" class="migration-wizard-summary">
      共 <strong>{{ migrationCandidates.candidateCount }}</strong> 篇将参与迁移（异步）
    </p>
    <template #footer>
      <el-button round @click="migrationWizardVisible = false">取消</el-button>
      <el-button
        type="warning"
        round
        :loading="migrating"
        :disabled="!migrationCandidates?.candidateCount"
        @click="confirmMigration"
      >
        开始迁移
      </el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="jobDetailVisible"
    title="批量任务详情"
    width="560px"
    append-to-body
    @closed="onJobDetailClosed"
  >
    <el-skeleton v-if="jobDetailLoading" :rows="5" animated />
    <template v-else-if="jobDetail">
      <el-descriptions :column="2" border size="small" class="job-detail-desc">
        <el-descriptions-item label="类型">
          {{ batchJobTypeLabel(jobDetail.jobType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ batchJobStatusLabel(jobDetail.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="进度">
          {{ jobDetail.completedCount }}/{{ jobDetail.totalCount }}
          <span v-if="jobDetail.failedCount" class="batch-job-history__fail">
            （{{ jobDetail.failedCount }} 失败）
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="分块档">
          <code v-if="jobDetail.chunkProfileId">{{ jobDetail.chunkProfileId }}</code>
          <span v-else>全库/迁移</span>
        </el-descriptions-item>
      </el-descriptions>
      <p v-if="jobDetail.lastError" class="job-detail-error">{{ jobDetail.lastError }}</p>
      <h4 v-if="jobFailedItems.length" class="job-detail-subtitle">失败文档</h4>
      <el-table
        v-if="jobFailedItems.length"
        :data="jobFailedItems"
        size="small"
        stripe
        max-height="240"
      >
        <el-table-column label="文件名" min-width="200">
          <template #default="{ row }">
            {{ row.fileName || '—' }}
            <el-tag v-if="row.deleted" size="small" type="info" effect="plain">已删除</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="文档 ID" min-width="220">
          <template #default="{ row }">
            <code class="chunk-profile-meta__id">{{ row.docId }}</code>
          </template>
        </el-table-column>
      </el-table>
    </template>
    <template #footer>
      <el-button round @click="jobDetailVisible = false">关闭</el-button>
      <el-button
        v-if="jobDetail?.retryable"
        type="warning"
        round
        :loading="retryingJobId === jobDetail?.jobId"
        @click="onRetryBatchJob(jobDetail)"
      >
        重试失败项
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  archiveChunkProfile,
  backfillChunkProfiles,
  getArchiveCandidates,
  getMigrationCandidates,
  listChunkProfiles,
  migrateToPrimary,
  setPrimaryChunkProfile
} from '../api/library'
import {
  getBatchJob,
  getBatchJobFailedItems,
  listBatchJobs,
  rebuildLibrary,
  retryBatchJob
} from '../api/vector'
import { useBatchJobPoll, isBatchJobTerminal } from '../composables/useBatchJobPoll'
import { useLibraryContext } from '../composables/useLibraryContext'
import LibraryParsingConfigTab from './LibraryParsingConfigTab.vue'
import LibraryBasicFields from './LibraryBasicFields.vue'
import { formatListTime } from '../utils/documentDisplay'
import {
  batchJobProgressStatus,
  batchJobStatusLabel,
  batchJobTagType,
  batchJobTypeLabel
} from '../utils/batchJobDisplay'
import {
  EMBEDDING_MODEL_OPTIONS,
  RERANK_MODEL_OPTIONS,
  dimensionForEmbeddingModel,
  isKnownEmbeddingModel
} from '../utils/embeddingModels'
import {
  LIBRARY_CHUNK_OVERLAP_RANGE,
  LIBRARY_CHUNK_SIZE_RANGE
} from '../utils/libraryDefaults'

const STRATEGY_TAG_TYPES = {
  'paragraph-first': 'info',
  'heading-level': 'primary',
  'fixed-char': 'warning',
  semantic: 'success'
}

const props = defineProps({
  form: { type: Object, required: true },
  libraryId: { type: String, default: '' },
  chunkStrategyRows: { type: Array, default: () => [] },
  strategyEmptyText: { type: String, default: '加载策略摘要…' },
  /** 库内已有向量分块数；大于 0 时展示重索引提示 */
  indexedChunkCount: { type: Number, default: 0 }
})

const { tenantId } = useLibraryContext()

const chunkProfiles = ref([])
const profilesLoading = ref(false)
const backfillLoading = ref(false)
const settingPrimaryId = ref('')
const reindexingProfileId = ref('')
const archivingProfileId = ref('')
const batchJobHistory = ref([])
const batchJobHistoryLoading = ref(false)
const retryingJobId = ref('')
const archivePreviewVisible = ref(false)
const archivePreviewRow = ref(null)
const archivePreview = ref(null)
const archivePreviewLoading = ref(false)
const migrationCandidates = ref(null)
const migrationCandidatesLoading = ref(false)
const migrationWizardVisible = ref(false)
const migrating = ref(false)
const jobDetailVisible = ref(false)
const jobDetailLoading = ref(false)
const jobDetail = ref(null)
const jobFailedItems = ref([])
const { activeJob: activeBatchJob, start: startBatchJobPoll, stop: stopBatchJobPoll } = useBatchJobPoll()

const activeTab = defineModel('activeTab', { type: String, default: 'basic' })

const chunkSizeRange = LIBRARY_CHUNK_SIZE_RANGE
const chunkOverlapRange = LIBRARY_CHUNK_OVERLAP_RANGE
const embeddingModelOptions = EMBEDDING_MODEL_OPTIONS
const rerankModelOptions = RERANK_MODEL_OPTIONS
/** 默认收缩；展开项 name 为 types / params */
const chunkCollapseExpanded = ref([])

async function loadChunkProfiles() {
  if (!props.libraryId) return
  profilesLoading.value = true
  try {
    const { data } = await listChunkProfiles(props.libraryId)
    chunkProfiles.value = data || []
  } catch {
    chunkProfiles.value = []
  } finally {
    profilesLoading.value = false
  }
}

async function loadMigrationCandidates() {
  if (!props.libraryId || !tenantId.value?.trim()) {
    migrationCandidates.value = null
    return
  }
  migrationCandidatesLoading.value = true
  try {
    const { data } = await getMigrationCandidates(props.libraryId, {
      tenantId: tenantId.value.trim()
    })
    migrationCandidates.value = data
  } catch {
    migrationCandidates.value = null
  } finally {
    migrationCandidatesLoading.value = false
  }
}

function openMigrationWizard() {
  if (!migrationCandidates.value?.candidateCount) {
    ElMessage.info('没有需要迁移的文档')
    return
  }
  migrationWizardVisible.value = true
}

function onMigrationWizardClosed() {
  // keep migrationCandidates for alert
}

async function confirmMigration() {
  if (!props.libraryId || !tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID')
    return
  }
  migrating.value = true
  try {
    const { data } = await migrateToPrimary(props.libraryId, {
      tenantId: tenantId.value.trim()
    })
    migrationWizardVisible.value = false
    if (data.candidateCount > 0) {
      ElMessage.success(data.message || `已提交 ${data.candidateCount} 个文档`)
      trackBatchJob(data.jobId, async () => {
        await loadChunkProfiles()
        await loadMigrationCandidates()
        await loadBatchJobHistory()
      })
      await loadBatchJobHistory()
    } else {
      ElMessage.warning(data.message || '无需迁移')
      await loadMigrationCandidates()
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '迁移失败')
  } finally {
    migrating.value = false
  }
}

async function openJobDetail(row) {
  if (!row?.jobId) return
  jobDetailVisible.value = true
  jobDetailLoading.value = true
  jobDetail.value = null
  jobFailedItems.value = []
  try {
    const [jobRes, itemsRes] = await Promise.all([
      getBatchJob(row.jobId),
      row.failedCount > 0 ? getBatchJobFailedItems(row.jobId) : Promise.resolve({ data: { items: [] } })
    ])
    jobDetail.value = jobRes.data
    jobFailedItems.value = itemsRes.data?.items || []
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载任务详情失败')
    jobDetailVisible.value = false
  } finally {
    jobDetailLoading.value = false
  }
}

function onJobDetailClosed() {
  jobDetail.value = null
  jobFailedItems.value = []
}

async function onSetPrimary(row) {
  if (!props.libraryId || !row?.chunkProfileId) return
  const otherDocs = chunkProfiles.value
    .filter((p) => p.chunkProfileId !== row.chunkProfileId)
    .reduce((sum, p) => sum + (p.docCount || 0), 0)
  try {
    await ElMessageBox.confirm(
      `设为主档后，默认问答将仅检索 ${row.chunkProfileId}（${row.docCount} 篇）。另有 ${otherDocs} 篇在非主档，默认将不再被检索（可开「全部分块档」或重索引迁移）。是否继续？`,
      '设为主分块档',
      { type: 'warning', confirmButtonText: '设为主档', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  settingPrimaryId.value = row.chunkProfileId
  try {
    await setPrimaryChunkProfile(props.libraryId, row.chunkProfileId)
    props.form.config.primaryChunkProfileId = row.chunkProfileId
    await loadChunkProfiles()
    ElMessage.success('已设为主分块档，默认问答将检索此档')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '设置失败')
  } finally {
    settingPrimaryId.value = ''
  }
}

function trackBatchJob(jobId, onDone) {
  if (!jobId) return
  startBatchJobPoll(jobId, {
    onDone: async (job) => {
      if (job.status === 'COMPLETED') {
        if (job.jobType === 'MIGRATE') {
          ElMessage.success('迁移任务已完成，空档孤儿分块已自动清理')
        } else {
          ElMessage.success('批量任务已完成')
        }
      } else if (job.status === 'PARTIAL') {
        ElMessage.warning(`批量任务部分失败（${job.failedCount} 项）`)
      } else if (job.status === 'FAILED') {
        ElMessage.error(job.lastError || '批量任务失败')
      }
      await loadChunkProfiles()
      await loadBatchJobHistory()
      onDone?.(job)
    }
  })
}

async function loadBatchJobHistory() {
  if (!props.libraryId) return
  batchJobHistoryLoading.value = true
  try {
    const params = { libraryId: props.libraryId, limit: 15 }
    if (tenantId.value?.trim()) params.tenantId = tenantId.value.trim()
    const { data } = await listBatchJobs(params)
    batchJobHistory.value = data || []
  } catch {
    batchJobHistory.value = []
  } finally {
    batchJobHistoryLoading.value = false
  }
}

async function onRetryBatchJob(row) {
  if (!row?.jobId) return
  retryingJobId.value = row.jobId
  try {
    const { data } = await retryBatchJob(row.jobId)
    ElMessage.success(data.message || '已提交重试任务')
    jobDetailVisible.value = false
    trackBatchJob(data.jobId, loadBatchJobHistory)
    await loadBatchJobHistory()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '重试失败')
  } finally {
    retryingJobId.value = ''
  }
}

async function loadActiveBatchJob() {
  if (!props.libraryId) return
  stopBatchJobPoll()
  activeBatchJob.value = null
  try {
    const params = { libraryId: props.libraryId, limit: 5 }
    if (tenantId.value?.trim()) params.tenantId = tenantId.value.trim()
    const { data } = await listBatchJobs(params)
    const running = (data || []).find((j) => !isBatchJobTerminal(j.status))
    if (running?.jobId) {
      trackBatchJob(running.jobId)
    }
  } catch {
    // ignore
  }
}

async function onReindexProfile(row) {
  if (!props.libraryId || !row?.chunkProfileId || !tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID 后再重索引')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将按当前库分块配置，重索引分块档 ${row.chunkProfileId} 下的 ${row.docCount} 篇文档（异步）。文档将迁移到新的主档指纹。是否继续？`,
      '按档重索引',
      { type: 'warning', confirmButtonText: '提交', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  reindexingProfileId.value = row.chunkProfileId
  try {
    const { data } = await rebuildLibrary({
      libraryId: props.libraryId,
      tenantId: tenantId.value.trim(),
      chunkProfileId: row.chunkProfileId
    })
    if (data.candidateCount > 0) {
      ElMessage.success(data.message || `已提交 ${data.candidateCount} 个文档`)
      trackBatchJob(data.jobId)
    } else {
      ElMessage.warning(data.message || '没有可重索引的文档')
      await loadChunkProfiles()
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '提交失败')
  } finally {
    reindexingProfileId.value = ''
  }
}

async function onArchiveProfile(row) {
  if (!props.libraryId || !row?.chunkProfileId || !tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID 后再归档')
    return
  }
  archivePreviewRow.value = row
  archivePreviewVisible.value = true
  archivePreviewLoading.value = true
  archivePreview.value = null
  try {
    const { data } = await getArchiveCandidates(props.libraryId, {
      tenantId: tenantId.value.trim(),
      chunkProfileId: row.chunkProfileId
    })
    archivePreview.value = data
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加载归档预览失败')
    archivePreviewVisible.value = false
  } finally {
    archivePreviewLoading.value = false
  }
}

function onArchivePreviewClosed() {
  archivePreviewRow.value = null
  archivePreview.value = null
}

async function confirmArchive() {
  const row = archivePreviewRow.value
  if (!props.libraryId || !row?.chunkProfileId || !tenantId.value?.trim()) return
  archivingProfileId.value = row.chunkProfileId
  try {
    const { data } = await archiveChunkProfile(props.libraryId, {
      tenantId: tenantId.value.trim(),
      chunkProfileId: row.chunkProfileId
    })
    archivePreviewVisible.value = false
    if (data.candidateCount > 0) {
      ElMessage.success(data.message || `已提交 ${data.candidateCount} 个文档`)
      trackBatchJob(data.jobId)
      await loadBatchJobHistory()
    } else {
      ElMessage.warning(data.message || '没有可归档的文档')
      await loadChunkProfiles()
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '归档失败')
  } finally {
    archivingProfileId.value = ''
  }
}

async function runBackfill() {
  if (!props.libraryId) return
  try {
    await ElMessageBox.confirm(
      '将为缺少分块档 ID 的历史文档与分块 metadata 补写指纹。是否继续？',
      '回填分块档',
      { type: 'warning', confirmButtonText: '开始回填', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  backfillLoading.value = true
  try {
    const { data } = await backfillChunkProfiles(props.libraryId)
    ElMessage.success(`已回填 ${data.backfilledDocs} 篇文档、${data.updatedChunks} 个分块`)
    await loadChunkProfiles()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '回填失败')
  } finally {
    backfillLoading.value = false
  }
}

watch(
  () => props.libraryId,
  (id) => {
    if (id) {
      loadChunkProfiles()
      loadMigrationCandidates()
      loadActiveBatchJob()
      loadBatchJobHistory()
    }
  },
  { immediate: true }
)

watch(activeTab, (tab) => {
  if (tab === 'pipeline' && props.libraryId) {
    loadChunkProfiles()
    loadMigrationCandidates()
    loadActiveBatchJob()
    loadBatchJobHistory()
  }
})

const strategySummaryText = computed(() => {
  const rows = props.chunkStrategyRows
  if (!rows.length) return props.strategyEmptyText
  const brief = rows.map((r) => `${r.fileTypeLabel}·${r.chunkingStrategyLabel}`).join(' / ')
  return rows.length > 2 ? `${rows.length} 种类型：${brief}` : brief
})

const overlapPercent = computed(() => {
  const size = props.form.config.chunkSize || 1
  return Math.round((props.form.config.chunkOverlap / size) * 100)
})

const chunkParamsSummaryText = computed(() => {
  const { chunkSize, chunkOverlap, hierarchicalChunkingEnabled, chunkDelimiter } = props.form.config
  const parts = [
    `${chunkSize} 字`,
    `重叠 ${chunkOverlap} 字（≈${overlapPercent.value}%）`,
    hierarchicalChunkingEnabled ? '父子块开' : '父子块关'
  ]
  const delim = chunkDelimiter && String(chunkDelimiter).trim()
  if (delim) parts.push('自定义分隔符')
  return parts.join(' · ')
})

const embeddingDimensionLocked = computed(() =>
  isKnownEmbeddingModel(props.form.config.embeddingModel)
)

const showReindexHint = computed(() => props.indexedChunkCount > 0)

function strategyTagType(strategy) {
  return STRATEGY_TAG_TYPES[strategy] || 'info'
}

function onEmbeddingModelChange(model) {
  if (isKnownEmbeddingModel(model)) {
    props.form.config.embeddingDimension = dimensionForEmbeddingModel(model)
  }
}

function resetUi() {
  chunkCollapseExpanded.value = []
  activeTab.value = 'basic'
}

defineExpose({ resetUi, loadMigrationCandidates, openMigrationWizard })
</script>

<style scoped>
.library-config-tabs {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.library-config-tabs :deep(.el-tabs) {
  height: 100%;
  display: flex;
}

.library-config-tabs :deep(.el-tabs--left) {
  flex-direction: row;
}

.library-config-tabs :deep(.el-tabs--left > .el-tabs__content) {
  flex: 1;
  min-width: 0;
  min-height: 0;
  height: 100%;
}

.library-config-tabs :deep(.el-tabs__header) {
  margin-right: 0;
  margin-bottom: 0;
}

.library-config-tabs :deep(.el-tabs__nav-wrap) {
  padding-right: 4px;
}

.library-config-tabs :deep(.el-tabs__item) {
  height: 44px;
  font-size: 13px;
  color: #64748b;
  justify-content: flex-start;
  padding: 0 14px;
}

.library-config-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.library-config-tabs :deep(.el-tabs__active-bar) {
  width: 3px;
}

.library-config-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-left: 8px;
}

.library-config-tabs :deep(.el-tab-pane) {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;
  box-sizing: border-box;
}

.tab-pane-body {
  flex: 1;
  min-height: 0;
  padding-top: 2px;
  overflow-x: hidden;
  overflow-y: auto;
  padding-right: 4px;
  -webkit-overflow-scrolling: touch;
  align-content: flex-start;
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

.cfg-section--chunk .cfg-section__intro,
.cfg-section--index .cfg-section__intro {
  margin: 0 0 14px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.55;
}

.index-hint-alert {
  margin-bottom: 12px;
}

.index-hint-alert :deep(.el-alert__description) {
  font-size: 12px;
  line-height: 1.55;
  color: #92400e;
}

.index-pipeline-steps {
  margin: 0 0 12px;
  padding-left: 18px;
  font-size: 12px;
  color: #475569;
  line-height: 1.65;
}

.index-pipeline-steps li + li {
  margin-top: 4px;
}

.index-pipeline-note {
  margin: 0;
  padding: 10px 12px;
  font-size: 12px;
  color: #64748b;
  line-height: 1.55;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.index-empty-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.45;
}

:deep(.el-form-item) {
  margin-bottom: 12px;
}

:deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

:deep(.el-form-item__label) {
  color: #64748b;
  font-size: 13px;
}

.full-width {
  width: 100%;
}

.select-md {
  width: 100%;
  max-width: 100%;
}

.cfg-slider {
  max-width: 100%;
  padding-right: 8px;
}

.switch-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 0 12px;
}

.switch-row :deep(.el-form-item) {
  margin-bottom: 12px;
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
  font-size: inherit;
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.chunk-part__label {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 0;
}

.chunk-part__desc {
  margin: 0 0 10px;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.45;
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
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.strategy-card__badge {
  display: inline-block;
  margin-top: 6px;
  font-size: 11px;
  color: #0ea5e9;
}

.chunk-metric-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chunk-metric__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 2px;
}

.chunk-metric__name {
  font-size: 13px;
  color: #475569;
}

.chunk-metric__value {
  font-size: 12px;
  font-weight: 600;
  color: #0f172a;
}

.chunk-metric__ratio {
  margin-left: 4px;
  font-weight: 500;
  color: #64748b;
}

.chunk-metric :deep(.el-slider__input) {
  width: 88px;
}

.chunk-metric__hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}

.chunk-advanced {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chunk-toggle-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  background: #fafbfc;
  border: 1px solid #eef2f7;
  border-radius: 8px;
}

.chunk-toggle-row__text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.chunk-toggle-row__title {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.chunk-toggle-row__desc {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}

.chunk-field-row__label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

.chunk-field-row__hint {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.4;
}

.field-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: #94a3b8;
  line-height: 1.45;
}

.chunk-profile-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 12px;
  color: #64748b;
}

.chunk-profile-meta__label {
  font-weight: 500;
  color: #475569;
}

.chunk-profile-meta__id {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  color: #0f172a;
  background: #fff;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid #e2e8f0;
}

.chunk-profile-meta__hint {
  flex: 1 1 100%;
  font-size: 11px;
  color: #94a3b8;
}

.chunk-governance-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
}

.chunk-governance-row__limit {
  margin: 0;
  flex: 1 1 200px;
}

.chunk-profiles-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.batch-job-panel {
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.batch-job-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.batch-job-panel__title {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.batch-job-panel__profile {
  font-size: 11px;
  color: #64748b;
}

.batch-job-panel__meta {
  margin: 6px 0 0;
  font-size: 11px;
  color: #64748b;
}

.chunk-profiles-table {
  width: 100%;
}

.chunk-profiles-table__current {
  font-size: 12px;
  color: #94a3b8;
}

.chunk-profiles-table__muted {
  font-size: 12px;
  color: #94a3b8;
}

.batch-job-history__fail {
  color: #dc2626;
  font-size: 11px;
}

.archive-preview-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.archive-preview-summary {
  margin: 0 0 10px;
  font-size: 13px;
  color: #334155;
}

.archive-preview-list {
  margin: 0;
  padding: 10px 12px 10px 28px;
  max-height: 220px;
  overflow-y: auto;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 12px;
  color: #475569;
  line-height: 1.6;
}

.archive-preview-more {
  margin: 8px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.migration-alert {
  margin-bottom: 10px;
}

.migration-alert__primary {
  margin-left: 4px;
  font-size: 11px;
}

.migration-alert__body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
}

.migration-wizard-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.migration-wizard-table {
  margin-bottom: 10px;
}

.migration-wizard-summary {
  margin: 0;
  font-size: 13px;
  color: #334155;
}

.job-detail-desc {
  margin-bottom: 10px;
}

.job-detail-error {
  margin: 0 0 10px;
  padding: 8px 10px;
  font-size: 12px;
  color: #b91c1c;
  background: #fef2f2;
  border-radius: 6px;
  line-height: 1.45;
}

.job-detail-subtitle {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
</style>
