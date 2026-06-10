<template>
  <div class="page-wrap page-wrap--fluid ingest-page library-page-fill">
    <PageCard title="文档采集">
      <template #actions>
        <span class="stat-chip">{{ currentLibraryName }}</span>
        <el-tag v-if="configVersion" size="small" type="info" effect="plain">v{{ configVersion }}</el-tag>
        <el-button round size="small" @click="goBackToLibrary">返回知识库</el-button>
      </template>

      <div class="library-page-content ingest-layout">
        <el-tabs v-model="mainTab" class="ingest-main-tabs" @tab-change="onMainTabChange">
          <el-tab-pane name="workflow">
            <template #label>
              <span class="ingest-tab-label">
                选材料与预览
                <el-badge v-if="validFiles.length" :value="validFiles.length" type="primary" class="ingest-tab-badge" />
              </span>
            </template>

            <div class="ingest-tab-body ingest-tab-body--workflow">
              <div class="ingest-workspace">
                <div class="workflow-split">
                  <div class="ingest-card ingest-card--pick">
                    <header class="pick-head">
                      <span class="ingest-card__title">选择材料</span>
                      <div v-if="fileList.length" class="pick-head__stats pick-stats">
                        <span class="pick-stat pick-stat--primary">{{ validFiles.length }} 有效</span>
                        <span v-if="invalidFiles.length" class="pick-stat pick-stat--muted">{{ invalidFiles.length }} 忽略</span>
                        <span v-if="validFiles.length" class="pick-stat">{{ formatFileSize(totalValidSize) }}</span>
                      </div>
                      <el-button v-if="fileList.length" link type="danger" size="small" @click="clearAllFiles">
                        清空
                      </el-button>
                    </header>

                    <div class="pick-body">
                      <div class="pick-panel" :class="{ 'pick-panel--has-files': fileList.length }">
                        <div class="pick-entries pick-entries--row">
                          <div class="pick-entry-wrap">
                            <el-upload
                              class="pick-upload pick-upload--entry"
                              drag
                              multiple
                              :show-file-list="false"
                              :auto-upload="false"
                              :limit="uploadLimit"
                              :file-list="fileList"
                              :on-change="onFileChange"
                              :on-remove="onFileRemove"
                              :on-exceed="onExceed"
                            >
                              <div class="pick-entry__inner">
                                <div class="pick-entry__icon pick-entry__icon--file">
                                  <el-icon><UploadFilled /></el-icon>
                                </div>
                                <p class="pick-entry__title">选择文件</p>
                                <p class="pick-entry__desc">拖拽或点击，支持多选</p>
                              </div>
                            </el-upload>
                          </div>

                          <div
                            class="pick-entry-wrap pick-entry-wrap--folder"
                            role="button"
                            tabindex="0"
                            @click="pickFolder"
                            @keydown.enter="pickFolder"
                          >
                            <input
                              ref="folderInputRef"
                              type="file"
                              class="folder-input-hidden"
                              webkitdirectory
                              directory
                              multiple
                              @change="onFolderSelected"
                            />
                            <div class="pick-entry__inner">
                              <div class="pick-entry__icon pick-entry__icon--folder">
                                <el-icon><Folder /></el-icon>
                              </div>
                              <p class="pick-entry__title">选择文件夹</p>
                              <p class="pick-entry__desc">批量导入目录内文件</p>
                            </div>
                          </div>
                        </div>

                        <div class="pick-meta">
                          <span>{{ supportedTypesLabel }}</span>
                          <span v-if="constraints"> · ≤ {{ constraints.maxFileSizeDisplay }}</span>
                          <span> · {{ uploadLimit }} 个/次</span>
                          <span v-if="folderPickHint" class="pick-meta__accent">{{ folderPickHint }}</span>
                        </div>

                        <el-alert
                          v-if="pickNotice"
                          :type="pickNotice.type"
                          :title="pickNotice.title"
                          :description="pickNotice.description"
                          show-icon
                          closable
                          class="pick-notice"
                          @close="pickNotice = null"
                        />
                      </div>

                      <div class="pick-list-section">
                        <div class="pick-list-bar">
                          <span class="pick-list-bar__title">已选文件</span>
                          <span v-if="validFiles.length" class="pick-list-bar__count">{{ validFiles.length }} 个</span>
                          <span v-if="previewedFileCount" class="pick-list-bar__hint">
                            已预览 {{ previewedFileCount }}/{{ validFiles.length }}
                          </span>
                        </div>

                        <ul v-if="validFiles.length" class="pick-list__body">
                          <li
                            v-for="row in validFiles"
                            :key="row.uid"
                            class="pick-item"
                            :class="{
                              'pick-item--active': row.uid === selectedPreviewUid,
                              'pick-item--previewed': !!previewCache[row.uid]
                            }"
                            role="button"
                            tabindex="0"
                            @click="selectFileForPreview(row.uid)"
                            @keydown.enter="selectFileForPreview(row.uid)"
                          >
                            <div class="pick-item__icon">
                              <el-icon><Document /></el-icon>
                            </div>
                            <div class="pick-item__info">
                              <span class="pick-item__name" :title="row.name">{{ displayFileName(row.name) }}</span>
                              <span v-if="filePathHint(row.name)" class="pick-item__path">{{ row.name }}</span>
                            </div>
                            <span class="pick-item__size">{{ formatFileSize(row.raw?.size) }}</span>
                            <el-tag
                              v-if="previewCache[row.uid]"
                              size="small"
                              type="success"
                              effect="plain"
                              round
                              class="pick-item__tag"
                            >
                              已预览
                            </el-tag>
                            <el-tag
                              v-else-if="row.duplicate"
                              size="small"
                              type="warning"
                              effect="plain"
                              round
                              class="pick-item__tag"
                            >
                              重复
                            </el-tag>
                            <el-tag v-else size="small" type="info" effect="plain" round class="pick-item__tag">
                              待预览
                            </el-tag>
                            <el-button
                              link
                              type="info"
                              :icon="Close"
                              class="pick-item__remove"
                              title="移除此文件"
                              @click.stop="removeFile(row.uid)"
                            />
                          </li>
                        </ul>

                        <div v-else-if="invalidFiles.length" class="pick-list-empty pick-list-empty--warn">
                          所选文件均不符合库类型
                        </div>
                        <div v-else class="pick-list-empty">
                          在上方选择文件或文件夹
                        </div>

                        <div v-if="invalidFiles.length" class="pick-skipped">
                          <span class="pick-skipped__label">已忽略 {{ invalidFiles.length }} 个</span>
                          <div class="pick-skipped__tags">
                            <el-tag
                              v-for="f in invalidFiles.slice(0, 4)"
                              :key="f.uid"
                              size="small"
                              type="info"
                              effect="plain"
                            >
                              {{ displayFileName(f.name) }}
                            </el-tag>
                            <span v-if="invalidFiles.length > 4" class="muted">+{{ invalidFiles.length - 4 }}</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div class="ingest-card ingest-card--preview">
              <header class="ingest-card__head ingest-card__head--fixed ingest-card__head--compact">
                <div class="ingest-card__head-left">
                  <span class="ingest-card__title">解析预览</span>
                </div>
                <div class="preview-actions">
                  <span
                    v-if="selectedPreviewFile"
                    class="preview-actions__target"
                    :title="selectedPreviewFile.name"
                  >
                    {{ displayFileName(selectedPreviewFile.name) }}
                  </span>
                  <el-tag
                    v-if="previewedFileCount"
                    type="success"
                    size="small"
                    effect="light"
                    round
                  >
                    {{ previewedFileCount }}/{{ validFiles.length }} 已预览
                  </el-tag>
                  <el-button
                    size="small"
                    round
                    :loading="previewLoading"
                    :disabled="!selectedPreviewFile"
                    @click="runIngestPreview"
                  >
                    {{ previewLoading ? '解析中…' : currentFilePreviewed ? '重新预览' : '生成预览' }}
                  </el-button>
                </div>
              </header>

              <div v-if="!previewLoading && !previewStats" class="preview-placeholder">
                <template v-if="selectedPreviewFile">
                  已选中 <strong>{{ displayFileName(selectedPreviewFile.name) }}</strong>，点击「生成预览」查看解析与分块效果
                </template>
                <template v-else>在左侧选择文件或文件夹，再点选列表中的文件进行预览</template>
              </div>

              <div v-else class="preview-body">
                <div v-if="previewLoading" class="preview-loading">
                  <p v-if="selectedPreviewFile" class="preview-loading__label">
                    正在解析 {{ displayFileName(selectedPreviewFile.name) }}…
                  </p>
                  <el-skeleton :rows="5" animated />
                </div>

                <template v-else-if="previewStats">
                  <div class="preview-overview">
                    <span class="preview-overview__item">
                      <strong>{{ previewStats.charCount }}</strong> 字
                    </span>
                    <span class="preview-overview__sep">·</span>
                    <span class="preview-overview__item">
                      <strong>{{ previewStats.chunkCount }}</strong> 块
                    </span>
                    <span class="preview-overview__sep">·</span>
                    <span class="preview-overview__item">~{{ previewStats.estimatedTokens }} tok</span>
                    <span
                      class="preview-overview__file"
                      :title="previewStats.fileName"
                    >
                      {{ previewStats.fileName }}
                    </span>
                    <el-tag
                      v-if="previewStats.truncated"
                      size="small"
                      type="info"
                      effect="plain"
                      round
                      class="preview-overview__tag"
                    >
                      原文已截断
                    </el-tag>
                    <el-tag
                      v-if="previewStats.filteredOutCount > 0"
                      size="small"
                      type="success"
                      effect="plain"
                      round
                      class="preview-overview__tag"
                      :title="`分块 ${previewStats.rawChunkCount} → 入库 ${previewStats.chunkCount}`"
                    >
                      已过滤 {{ previewStats.filteredOutCount }} 个表头块
                    </el-tag>
                  </div>
                  <p class="preview-overview-hint">{{ INGEST_CHUNK_PREVIEW_HINT }}</p>

                  <el-tabs v-model="previewTab" class="preview-tabs">
                    <el-tab-pane label="原文摘录" name="text">
                      <div class="preview-pane preview-pane--text">
                        <pre v-if="previewTextExcerpt" class="text-excerpt">{{ previewTextExcerpt }}</pre>
                        <div v-else class="preview-pane__empty">暂无解析文本</div>
                      </div>
                    </el-tab-pane>
                    <el-tab-pane :label="`分块（${previewChunks.length}）`" name="chunks">
                      <div class="preview-pane preview-pane--table">
                        <el-table
                          v-if="previewChunks.length"
                          :data="previewChunks"
                          size="small"
                          stripe
                          class="data-table preview-chunk-table"
                        >
                          <el-table-column prop="index" label="#" width="44" align="center" />
                          <el-table-column prop="length" label="字数" width="64" align="right" />
                          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
                        </el-table>
                        <div v-else class="preview-pane__empty">暂无分块结果</div>
                      </div>
                    </el-tab-pane>
                  </el-tabs>
                </template>
              </div>
                  </div>
                </div>

                <div v-if="uploading || batchResult || singleResult" class="ingest-card ingest-card--result">
              <header class="ingest-card__head">
                <span class="ingest-card__title">入库结果</span>
              </header>

              <div v-if="uploading" class="upload-progress">
                <el-progress :percentage="uploadPercent" :stroke-width="6" :status="uploadPercent >= 100 ? 'success' : ''" />
                <span class="hint">上传并触发流水线…</span>
              </div>

              <div v-if="batchResult" class="result-block">
                <div
                  class="result-banner"
                  :class="batchResult.failed === 0 ? 'result-banner--success' : 'result-banner--warn'"
                >
                  <span class="result-banner__title">
                    {{ batchResult.failed === 0 ? '全部入库成功' : '部分入库失败' }}
                  </span>
                  <span class="result-banner__sub">
                    共 {{ batchResult.total }} · 成功 {{ batchResult.succeeded }} · 失败 {{ batchResult.failed }}
                  </span>
                </div>
                <el-alert
                  v-if="batchCapacityAlert"
                  class="result-capacity-alert"
                  type="warning"
                  :closable="false"
                  show-icon
                  :title="batchCapacityAlert"
                />
                <el-table :data="batchResult.items" stripe size="small" class="data-table" max-height="200">
                  <el-table-column prop="fileName" label="文件" min-width="140" show-overflow-tooltip />
                  <el-table-column label="结果" width="72" align="center">
                    <template #default="{ row }">
                      <el-tag :type="row.success ? 'success' : 'danger'" size="small" effect="light" round>
                        {{ row.success ? '成功' : '失败' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="状态" width="88" align="center">
                    <template #default="{ row }">
                      <span v-if="row.success" class="status-text">{{ docStatusLabel(row.document) }}</span>
                      <span v-else class="muted">—</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="说明" min-width="200" show-overflow-tooltip>
                    <template #default="{ row }">
                      {{ formatBatchUploadMessage(row) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="" width="64" align="center" fixed="right">
                    <template #default="{ row }">
                      <el-button
                        v-if="row.success && row.document?.docId"
                        link
                        type="primary"
                        size="small"
                        @click="openDoc(row.document.docId)"
                      >
                        详情
                      </el-button>
                      <el-button v-else-if="!row.success" link type="primary" size="small" @click="retryFailed">
                        重试
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div v-else-if="singleResult" class="result-block">
                <div class="result-banner result-banner--success">
                  <span class="result-banner__title">入库成功</span>
                  <span class="result-banner__sub">
                    <code class="id-code">{{ singleResult.docId }}</code>
                    · {{ docStatusLabel(singleResult) }}
                  </span>
                </div>
                <div class="result-actions">
                  <el-button type="primary" size="small" round @click="openDoc(singleResult.docId)">文档详情</el-button>
                  <el-button size="small" round @click="goDocuments">文档库</el-button>
                </div>
              </div>
            </div>
              </div>

              <footer class="ingest-action-bar">
                <div class="ingest-action-bar__hint">
                  <template v-if="!validFiles.length">请先选择材料</template>
                  <template v-else-if="!previewConfirmed">请至少预览 1 份文件后再入库（可在列表中切换文件逐份预览）</template>
                  <template v-else-if="filesToUpload.length">
                    将入库 <strong>{{ filesToUpload.length }}</strong> 个文件
                    <span v-if="duplicateFiles.length" class="hint-inline"> · 重复策略见「规则与配置」</span>
                  </template>
                  <template v-else>跳过重复后无可入库文件</template>
                </div>
                <div class="ingest-action-bar__btns">
                  <el-button round size="small" @click="switchMainTab('settings')">规则与配置</el-button>
                  <el-button v-if="batchResult || singleResult" round size="small" @click="resetIngest">继续采集</el-button>
                  <el-button v-if="batchResult || singleResult" round size="small" @click="goDocuments">文档库</el-button>
                  <el-button
                    type="primary"
                    round
                    :loading="loading"
                    :disabled="!canSubmit"
                    @click="submitUpload"
                  >
                    {{ submitLabel }}
                  </el-button>
                </div>
              </footer>
            </div>
          </el-tab-pane>

          <el-tab-pane label="规则与配置" name="settings">
            <div class="ingest-tab-body ingest-tab-body--settings">
              <div class="settings-panel">
                <section class="ingest-card settings-section">
                  <header class="settings-section__head">
                    <span class="ingest-card__title">库默认规则</span>
                    <el-tag size="small" type="info" effect="plain">只读</el-tag>
                    <el-tag v-if="lockPipeline" size="small" type="warning" effect="plain">处理已锁定</el-tag>
                    <el-button link type="primary" size="small" class="ingest-card__action" @click="goEditRules">
                      打开完整配置
                    </el-button>
                  </header>
                  <el-alert
                    v-if="lockPipeline"
                    class="settings-lock-alert"
                    type="warning"
                    :closable="false"
                    show-icon
                    title="库内已有文档"
                    description="完整配置中「处理」页（解析、清洗、分块、向量化）已锁定；本次采集不可覆盖分块大小。"
                  />
                  <div v-if="rulesSummary" class="settings-rules-grid">
                    <div class="settings-kv">
                      <span class="settings-kv__label">文档采集</span>
                      <span class="settings-kv__value">{{ fixedIngestAccessLabel }}</span>
                    </div>
                    <div class="settings-kv">
                      <span class="settings-kv__label">支持类型</span>
                      <span class="settings-kv__value">{{ supportedTypesLabel }}</span>
                    </div>
                    <div class="settings-kv">
                      <span class="settings-kv__label">文本清洗</span>
                      <span class="settings-kv__value">{{ rulesSummary.textNormalizationEnabled ? '开启' : '关闭' }}</span>
                    </div>
                    <div class="settings-kv">
                      <span class="settings-kv__label">去重段落</span>
                      <span class="settings-kv__value">{{ libraryConfig?.cleaning?.removeDuplicateParagraphs !== false ? '开启' : '关闭' }}</span>
                    </div>
                    <div class="settings-kv">
                      <span class="settings-kv__label">分块策略</span>
                      <span class="settings-kv__value">{{ rulesSummary.chunkingStrategy }}</span>
                    </div>
                    <div class="settings-kv">
                      <span class="settings-kv__label">块大小 / 重叠</span>
                      <span class="settings-kv__value">{{ rulesSummary.chunkSize }} / {{ rulesSummary.chunkOverlap }}</span>
                    </div>
                    <div class="settings-kv">
                      <span class="settings-kv__label">Embedding</span>
                      <span class="settings-kv__value">{{ rulesSummary.embeddingModel }}（{{ rulesSummary.embeddingDimension }} 维）</span>
                    </div>
                    <div v-if="constraints" class="settings-kv">
                      <span class="settings-kv__label">上传限制</span>
                      <span class="settings-kv__value">≤ {{ constraints.maxFileSizeDisplay }} · {{ uploadLimit }} 个/次</span>
                    </div>
                  </div>
                  <el-skeleton v-else :rows="4" animated />
                </section>

                <section class="ingest-card settings-section">
                  <header class="settings-section__head">
                    <span class="ingest-card__title">本次覆盖</span>
                    <span class="settings-section__hint">仅影响当前采集任务</span>
                  </header>
                  <el-form label-width="96px" label-position="left" size="small" class="settings-form">
                    <el-form-item label="租户 ID" required>
                      <el-input v-model="tenantId" placeholder="demo" style="max-width: 240px" @change="onTenantChange" />
                    </el-form-item>
                    <el-form-item label="自动索引">
                      <el-switch
                        v-model="autoIndex"
                        :disabled="reviewModeEnabled || ingestMode === 'review'"
                        inline-prompt
                        active-text="开"
                        inactive-text="关"
                      />
                      <span v-if="reviewModeEnabled" class="hint-inline">库已开启人工审核，解析后不自动索引</span>
                    </el-form-item>
                    <el-form-item label="文档元数据">
                      <el-input
                        v-model="documentMetadataText"
                        type="textarea"
                        :rows="3"
                        placeholder='可选 JSON，如 {"department":"研发","docType":"周报"}'
                        style="max-width: 360px"
                      />
                    </el-form-item>
                    <el-form-item v-if="lockPipeline" label="分块参数">
                      <span class="settings-kv__value">与库配置一致</span>
                      <p class="settings-form__tip">库内已有文档，分块参数须与库配置一致</p>
                    </el-form-item>
                  </el-form>
                </section>

                <section class="ingest-card settings-section">
                  <header class="settings-section__head">
                    <span class="ingest-card__title">入库策略</span>
                  </header>
                  <el-form label-width="96px" label-position="left" size="small" class="settings-form">
                    <el-form-item label="入库模式">
                      <el-radio-group v-model="ingestMode" size="small">
                        <el-radio-button label="immediate">立即入库</el-radio-button>
                        <el-radio-button label="review" :disabled="!reviewModeEnabled">提交审核</el-radio-button>
                      </el-radio-group>
                      <p v-if="reviewModeEnabled" class="settings-form__tip">库已开启人工审核</p>
                    </el-form-item>
                    <el-form-item label="重复处理">
                      <template v-if="duplicatePolicyLocked">
                        <span class="settings-kv__value">{{ duplicatePolicyLabel }}</span>
                        <p class="settings-form__tip">跟随知识库版本策略，不可在本页修改</p>
                      </template>
                      <el-radio-group v-else v-model="duplicatePolicy" size="small">
                        <el-radio label="overwrite">覆盖（升版本重处理）</el-radio>
                        <el-radio label="skip">跳过重复项</el-radio>
                        <el-radio label="keep-history">新建版本</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-form>
                  <div v-if="duplicateFiles.length" class="notice notice--warn">
                    <div class="notice__title">当前已选 {{ duplicateFiles.length }} 个重复文件名</div>
                    <div class="notice__tags">
                      <el-tag v-for="f in duplicateFiles" :key="f" size="small" type="warning" effect="plain">
                        {{ f }}
                      </el-tag>
                    </div>
                  </div>
                </section>
              </div>

              <footer class="ingest-action-bar ingest-action-bar--secondary">
                <span class="hint">配置完成后返回「选材料与预览」继续采集</span>
                <el-button type="primary" round size="small" @click="switchMainTab('workflow')">返回采集</el-button>
              </footer>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </PageCard>

    <EditLibrarySettingsDrawer
      v-model="editDrawerVisible"
      :library-id="libraryId"
      @saved="onLibrarySettingsSaved"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Document, Folder, UploadFilled } from '@element-plus/icons-vue'
import {
  getUploadConstraints,
  uploadDocument,
  uploadDocumentsBatch,
  uploadDocumentAsync,
  parsePreview,
  listDocuments
} from '../api/ingest'
import { previewChunks as fetchChunkPreview } from '../api/chunk'
import { getVectorLibrary, getUploadTask } from '../api/library'
import { useLibraryContext } from '../composables/useLibraryContext'
import EditLibrarySettingsDrawer from '../components/EditLibrarySettingsDrawer.vue'
import { buildRulesSummary, FIXED_INGEST_ACCESS_LABEL, hasIngestedContent } from '../utils/libraryConfig'
import { FILE_TYPE_OPTIONS } from '../utils/libraryDefaults'
import { INGEST_CHUNK_PREVIEW_HINT, libraryChunkParams } from '../utils/chunkPreviewSample'
import { filterFolderFiles, matchesSupportedType } from '../utils/supportedFileTypes'
import {
  batchFailureSummary,
  enrichPlainIngestMessage,
  formatBatchUploadMessage,
  formatIngestError,
  isCapacityLimitError
} from '../utils/ingestErrors'
import PageCard from '../components/PageCard.vue'

const route = useRoute()
const router = useRouter()
const { libraryId, tenantId, persist } = useLibraryContext()

const currentLibraryName = ref('—')
const folderInputRef = ref(null)
const folderPickHint = ref('')
const pickNotice = ref(null)
const selectedPreviewUid = ref(null)
const previewCache = ref({})
const configVersion = ref(null)
const libraryConfig = ref(null)
const rulesSummary = ref(null)
const constraints = ref(null)
const existingDocs = ref([])
const editDrawerVisible = ref(false)

const autoIndex = ref(true)
const fileList = ref([])
const loading = ref(false)
const uploading = ref(false)
const uploadPercent = ref(0)
const singleResult = ref(null)
const batchResult = ref(null)

const previewLoading = ref(false)
const previewChunks = ref([])
const previewStats = ref(null)
const previewTextFull = ref('')
const previewTab = ref('text')

const duplicatePolicy = ref('overwrite')
const ingestMode = ref('immediate')
const mainTab = ref('workflow')
const documentMetadataText = ref('')

const VERSION_STRATEGY_TO_POLICY = {
  overwrite: 'overwrite',
  incremental: 'skip',
  'keep-history': 'keep-history'
}

const DUPLICATE_POLICY_LABELS = {
  overwrite: '覆盖（升版本重处理）',
  skip: '跳过重复项',
  'keep-history': '新建版本'
}

const uploadLimit = computed(() => constraints.value?.maxBatchFiles ?? 20)

const batchCapacityAlert = computed(() => {
  const items = batchResult.value?.items
  if (!items?.length) return ''
  const failed = items.filter((item) => !item.success)
  if (!failed.length) return ''
  if (!failed.every((item) => isCapacityLimitError(item.errorCode))) return ''
  if (failed.some((item) => item.errorCode === 'LIBRARY_DOCUMENT_LIMIT_EXCEEDED')) {
    return '文档数量已达库上限：请提高容量配置或删除部分文档后再试。'
  }
  if (failed.some((item) => item.errorCode === 'LIBRARY_SIZE_LIMIT_EXCEEDED')) {
    return '知识库总大小已达上限：请提高总大小配置或清理大文件 / 历史版本后再试。'
  }
  return '向量条目已达库上限：请提高向量条目配置，或调大分块大小、删除部分已索引文档后再试。'
})

const fixedIngestAccessLabel = FIXED_INGEST_ACCESS_LABEL

const supportedTypesLabel = computed(() => {
  const types = libraryConfig.value?.ingestAccess?.supportedFileTypes || []
  if (!types.length) return '—'
  return types.map((t) => FILE_TYPE_OPTIONS.find((o) => o.value === t)?.label || t).join('、')
})

const supportedTypes = computed(() => libraryConfig.value?.ingestAccess?.supportedFileTypes || [])

const validFiles = computed(() => {
  const types = supportedTypes.value
  return fileList.value
    .filter((f) => matchesSupportedType(f.name, types))
    .map((f) => ({
      ...f,
      duplicate: isDuplicateName(f.name)
    }))
})

const invalidFiles = computed(() => {
  const types = supportedTypes.value
  return fileList.value.filter((f) => !matchesSupportedType(f.name, types))
})

const duplicateFiles = computed(() =>
  validFiles.value.filter((f) => f.duplicate).map((f) => f.name)
)

const filesToUpload = computed(() => {
  let files = validFiles.value.map((f) => f.raw).filter(Boolean)
  if (duplicatePolicy.value === 'skip') {
    files = validFiles.value.filter((f) => !f.duplicate).map((f) => f.raw)
  }
  return files
})

const totalValidSize = computed(() =>
  validFiles.value.reduce((sum, f) => sum + (f.raw?.size || 0), 0)
)

const selectedPreviewFile = computed(
  () => validFiles.value.find((f) => f.uid === selectedPreviewUid.value) || null
)

const previewedFileCount = computed(
  () => validFiles.value.filter((f) => previewCache.value[f.uid]).length
)

const previewConfirmed = computed(() => previewedFileCount.value > 0)

const currentFilePreviewed = computed(
  () => !!(selectedPreviewUid.value && previewCache.value[selectedPreviewUid.value])
)

const reviewModeEnabled = computed(
  () =>
    constraints.value?.manualReviewRequired === true
    || libraryConfig.value?.governance?.ingestReviewMode === 'manual-review'
)

const lockPipeline = computed(() =>
  hasIngestedContent({
    documentCount: rulesSummary.value?.documentCount ?? 0,
    chunkCount: rulesSummary.value?.chunkCount ?? 0
  })
)

const duplicatePolicyLocked = computed(() => !!constraints.value?.versionUpdateStrategy)

const duplicatePolicyLabel = computed(
  () => DUPLICATE_POLICY_LABELS[duplicatePolicy.value] || duplicatePolicy.value
)

const effectiveAutoIndex = computed(() => {
  if (reviewModeEnabled.value || ingestMode.value === 'review') return false
  return autoIndex.value
})

const previewTextExcerpt = computed(() => {
  const t = previewTextFull.value || ''
  return t.length > 2000 ? `${t.slice(0, 2000)}…` : t
})

const canSubmit = computed(
  () => filesToUpload.value.length > 0 && previewConfirmed.value && !uploading.value
)

watch(validFiles, () => syncSelectedPreviewFile(), { immediate: true })

watch(previewTab, (tab) => {
  const uid = selectedPreviewUid.value
  if (!uid || !previewCache.value[uid]) return
  previewCache.value[uid].tab = tab
})

const submitLabel = computed(() => {
  const n = filesToUpload.value.length
  if (ingestMode.value === 'review') return `提交审核（${n}）`
  if (n > 1) return `确认入库（${n}）`
  return '确认入库'
})

function isDuplicateName(name) {
  const base = name.includes('/') ? name.split('/').pop() : name
  return existingDocs.value.some((d) => d.fileName === base || d.fileName === name)
}

function formatFileSize(bytes) {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 ** 2).toFixed(1)} MB`
}

function displayFileName(name) {
  if (!name) return '—'
  const i = name.lastIndexOf('/')
  return i >= 0 ? name.slice(i + 1) : name
}

function filePathHint(name) {
  return name && name.includes('/') ? name : ''
}

function removeFile(uid) {
  fileList.value = fileList.value.filter((f) => f.uid !== uid)
  if (previewCache.value[uid]) {
    const next = { ...previewCache.value }
    delete next[uid]
    previewCache.value = next
  }
  if (!fileList.value.length) {
    folderPickHint.value = ''
    pickNotice.value = null
  }
  syncSelectedPreviewFile()
}

async function clearAllFiles() {
  if (!fileList.value.length) return
  try {
    await ElMessageBox.confirm('将清空已选文件及全部预览结果，此操作不可撤销。', '清空已选文件', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  fileList.value = []
  folderPickHint.value = ''
  pickNotice.value = null
  resetPreviewState()
}

function syncSelectedPreviewFile() {
  if (!validFiles.value.length) {
    selectedPreviewUid.value = null
    restorePreviewForSelected()
    return
  }
  if (!validFiles.value.some((f) => f.uid === selectedPreviewUid.value)) {
    selectedPreviewUid.value = validFiles.value[0].uid
  }
  restorePreviewForSelected()
}

function selectFileForPreview(uid) {
  if (selectedPreviewUid.value === uid) return
  selectedPreviewUid.value = uid
  restorePreviewForSelected()
}

function restorePreviewForSelected() {
  const cached = selectedPreviewUid.value ? previewCache.value[selectedPreviewUid.value] : null
  if (cached) {
    previewChunks.value = cached.chunks || []
    previewStats.value = cached.stats || null
    previewTextFull.value = cached.text || ''
    previewTab.value = cached.tab || 'text'
    return
  }
  previewChunks.value = []
  previewStats.value = null
  previewTextFull.value = ''
}

function estimateTokens(charCount) {
  return Math.max(1, Math.ceil(charCount / 2))
}

function docStatusLabel(doc) {
  if (!doc) return '—'
  if (ingestMode.value === 'review') return '待审核'
  if (doc.indexStatus === 'INDEXED') return '已生效'
  if (doc.parseStatus === 'FAILED' || doc.indexStatus === 'FAILED') return '失败'
  return '处理中'
}

function goBackToLibrary() {
  if (libraryId.value) {
    router.push({ name: 'vectorLibraryDetail', params: { libraryId: libraryId.value } })
  } else {
    router.push('/vector-libraries')
  }
}

function goEditRules() {
  if (!libraryId.value) return
  editDrawerVisible.value = true
}

async function onLibrarySettingsSaved(saved) {
  if (saved?.config) {
    libraryConfig.value = saved.config
    configVersion.value = saved.config?.configVersion ?? configVersion.value
    rulesSummary.value = buildRulesSummary(saved)
  }
  await Promise.all([loadCurrentLibrary(), loadConstraints(), loadExistingDocs()])
}

function goDocuments() {
  goBackToLibrary()
}

function navigateToLibraryDocuments() {
  persist()
  goBackToLibrary()
}

function openDoc(docId) {
  if (!docId || !libraryId.value) return
  persist()
  router.push({
    name: 'vectorLibraryDetail',
    params: { libraryId: libraryId.value },
    query: { docId }
  })
}

async function loadCurrentLibrary() {
  if (!libraryId.value) return
  try {
    const { data } = await getVectorLibrary(libraryId.value)
    currentLibraryName.value = data.name
    libraryConfig.value = data.config || {}
    configVersion.value = data.config?.configVersion ?? 1
    rulesSummary.value = buildRulesSummary(data)
  } catch {
    currentLibraryName.value = libraryId.value
    rulesSummary.value = null
  }
}

async function loadConstraints() {
  if (!libraryId.value) return
  const { data } = await getUploadConstraints(libraryId.value)
  constraints.value = data
  if (data.versionUpdateStrategy) {
    duplicatePolicy.value = VERSION_STRATEGY_TO_POLICY[data.versionUpdateStrategy] || 'overwrite'
  }
  if (data.manualReviewRequired) {
    ingestMode.value = 'review'
    autoIndex.value = false
  }
}

function resolveDocumentMetadataParam() {
  const raw = documentMetadataText.value?.trim()
  if (!raw) return undefined
  try {
    JSON.parse(raw)
    return raw
  } catch {
    throw new Error('文档元数据须为合法 JSON 对象')
  }
}

async function loadExistingDocs() {
  if (!libraryId.value || !tenantId.value?.trim()) return
  try {
    const { data } = await listDocuments({
      libraryId: libraryId.value,
      tenantId: tenantId.value.trim(),
      page: 1,
      size: 500
    })
    existingDocs.value = data.items || []
  } catch {
    existingDocs.value = []
  }
}

function onTenantChange() {
  persist()
  loadExistingDocs()
}

function onMainTabChange(tab) {
  const query = { ...route.query }
  if (tab === 'settings') query.tab = 'settings'
  else delete query.tab
  router.replace({ query })
}

function switchMainTab(tab) {
  mainTab.value = tab
  onMainTabChange(tab)
}

async function initPage() {
  const qLib = route.query.libraryId
  if (qLib && typeof qLib === 'string') libraryId.value = qLib
  const qTab = route.query.tab
  if (qTab === 'settings' || qTab === 'config') mainTab.value = 'settings'
  else mainTab.value = 'workflow'
  persist()
  if (!libraryId.value) {
    ElMessage.warning('请先从知识库详情进入文档采集')
    router.replace('/vector-libraries')
    return
  }
  await Promise.all([loadCurrentLibrary(), loadConstraints(), loadExistingDocs()])
}

onMounted(initPage)

watch(ingestMode, (mode) => {
  if (mode === 'review') autoIndex.value = false
})

watch(reviewModeEnabled, (enabled) => {
  if (enabled) {
    ingestMode.value = 'review'
    autoIndex.value = false
  }
})

function resetPreviewState() {
  previewCache.value = {}
  selectedPreviewUid.value = null
  previewChunks.value = []
  previewStats.value = null
  previewTextFull.value = ''
  previewTab.value = 'text'
}

function pickFolder() {
  folderInputRef.value?.click()
}

function applyFileList(rawFiles) {
  fileList.value = rawFiles.map((raw) => ({
    name: raw.webkitRelativePath || raw.name,
    raw,
    uid: `${raw.webkitRelativePath || raw.name}-${raw.size}-${raw.lastModified}`
  }))
  resetPreviewState()
  syncSelectedPreviewFile()
}

function onFolderSelected(event) {
  const input = event.target
  const files = Array.from(input.files || [])
  input.value = ''
  if (!files.length) return

  const { accepted, skipped } = filterFolderFiles(files, supportedTypes.value)
  if (!accepted.length) {
    pickNotice.value = {
      type: 'warning',
      title: '未找到可接入的文件',
      description: `文件夹内没有符合当前库类型（${supportedTypesLabel.value}）的文件，请检查目录或调整知识库支持的文件类型。`
    }
    return
  }

  let list = accepted
  let truncated = false
  if (list.length > uploadLimit.value) {
    truncated = true
    list = list.slice(0, uploadLimit.value)
  }

  applyFileList(list)
  folderPickHint.value = `已选 ${list.length} 个${skipped.length ? `，忽略 ${skipped.length} 个` : ''}`

  const parts = []
  if (skipped.length) {
    parts.push(`已忽略 ${skipped.length} 个不符合类型的文件`)
  }
  if (truncated) {
    parts.push(`符合类型的文件共 ${accepted.length} 个，已按单次上限仅接入前 ${uploadLimit.value} 个`)
  }
  if (parts.length) {
    pickNotice.value = {
      type: truncated ? 'warning' : 'info',
      title: '文件夹导入说明',
      description: parts.join('；') + '。可在下方列表点选任意文件查看预览。'
    }
  } else {
    pickNotice.value = {
      type: 'success',
      title: `已导入 ${list.length} 个文件`,
      description: '点击列表中的文件可切换预览对象，建议抽样或逐份确认分块效果后再入库。'
    }
  }
}

function onFileChange(_uploadFile, uploadFiles) {
  fileList.value = uploadFiles
  folderPickHint.value = ''
  pickNotice.value = null
  resetPreviewState()
  syncSelectedPreviewFile()
}

function onFileRemove(_file, uploadFiles) {
  fileList.value = uploadFiles
  folderPickHint.value = ''
  pickNotice.value = null
  resetPreviewState()
  syncSelectedPreviewFile()
}

function onExceed() {
  pickNotice.value = {
    type: 'warning',
    title: '超出单次文件数量上限',
    description: `单次最多选择 ${uploadLimit.value} 个文件。请分批上传，或使用文件夹导入（将自动截取前 ${uploadLimit.value} 个符合类型的文件）。`
  }
}

function onUploadProgress(event) {
  if (!event.total) return
  uploadPercent.value = Math.min(99, Math.round((event.loaded * 100) / event.total))
}

function buildChunkPreviewBody(sampleText) {
  const cfg = libraryConfig.value || {}
  const norm = cfg.textNormalization
  const sizing = libraryChunkParams(cfg)
  return {
    sampleText,
    libraryId: libraryId.value || null,
    chunkingStrategy: cfg.chunkingStrategy || 'paragraph-first',
    chunkSize: sizing.chunkSize,
    chunkOverlap: sizing.chunkOverlap,
    minChunkSize: sizing.minChunkSize,
    maxChunkSize: sizing.maxChunkSize,
    minParagraphLength: sizing.minParagraphLength,
    normalizeBeforeChunk: cfg.normalizeBeforeChunk !== false,
    textNormalizationEnabled: cfg.textNormalizationEnabled !== false,
    textNormalization: cfg.textNormalizationEnabled !== false ? norm : null,
    cleaning: cfg.cleaning || null
  }
}

async function runIngestPreview() {
  const row = selectedPreviewFile.value
  const file = row?.raw
  if (!file) {
    ElMessage.warning('请先在列表中点选要预览的文件')
    return
  }
  previewLoading.value = true
  try {
    const { data: parsed } = await parsePreview(file, libraryId.value)
    const { data: chunked } = await fetchChunkPreview(buildChunkPreviewBody(parsed.text || ''))
    const chunks = chunked.chunks || []
    const stats = {
      fileName: parsed.fileName || displayFileName(row.name),
      charCount: parsed.charCount ?? 0,
      chunkCount: chunked.totalChunks ?? chunks.length,
      rawChunkCount: chunked.rawTotalChunks ?? chunks.length,
      filteredOutCount: chunked.filteredOutCount ?? 0,
      estimatedTokens: estimateTokens(parsed.charCount ?? 0),
      truncated: parsed.truncated
    }
    const text = parsed.text || ''
    previewCache.value = {
      ...previewCache.value,
      [row.uid]: {
        chunks,
        stats,
        text,
        tab: previewTab.value
      }
    }
    previewChunks.value = chunks
    previewTextFull.value = text
    previewStats.value = stats
    ElMessage.success(`「${displayFileName(row.name)}」预览完成`)
  } catch (e) {
    if (!e?.response) {
      ElMessage.error(formatIngestError(e, '预览失败'))
    }
  } finally {
    previewLoading.value = false
  }
}

async function submitUpload() {
  if (!libraryId.value || !tenantId.value?.trim()) {
    ElMessage.warning('请填写租户 ID')
    return
  }
  const files = filesToUpload.value
  if (!files.length) {
    ElMessage.warning(duplicatePolicy.value === 'skip' ? '跳过重复后无文件可入库' : '请选择文件')
    return
  }
  if (!previewConfirmed.value) {
    ElMessage.warning('请至少在列表中预览 1 份文件后再入库')
    return
  }

  persist()
  loading.value = true
  uploading.value = true
  uploadPercent.value = 0
  singleResult.value = null
  batchResult.value = null

  let documentMetadata
  try {
    documentMetadata = resolveDocumentMetadataParam()
  } catch (e) {
    ElMessage.warning(e.message || '文档元数据格式不正确')
    loading.value = false
    uploading.value = false
    return
  }

  const indexFlag = effectiveAutoIndex.value

  try {
    const asyncThreshold = 5 * 1024 * 1024
    if (files.length === 1 && files[0].size >= asyncThreshold) {
      const { data: task } = await uploadDocumentAsync(
        libraryId.value,
        tenantId.value.trim(),
        files[0],
        indexFlag,
        onUploadProgress,
        documentMetadata
      )
      ElMessage.success(`大文件已提交异步任务 ${task.taskId}`)
      uploadPercent.value = 100
      pollTask(task.taskId)
      return
    }
    if (files.length === 1) {
      const { data } = await uploadDocument(
        libraryId.value,
        tenantId.value.trim(),
        files[0],
        indexFlag,
        onUploadProgress,
        documentMetadata
      )
      if (data?.docId) localStorage.setItem('lastDocId', data.docId)
      ElMessage.success(reviewModeEnabled.value || ingestMode.value === 'review' ? '已提交，待审核' : '入库成功')
      uploadPercent.value = 100
      await loadExistingDocs()
      navigateToLibraryDocuments()
      return
    }
    const { data } = await uploadDocumentsBatch(
      libraryId.value,
      tenantId.value.trim(),
      files,
      indexFlag,
      onUploadProgress,
      documentMetadata
    )
    uploadPercent.value = 100
    await loadExistingDocs()
    const asyncTasks = (data.items || [])
      .filter((i) => i.success && i.asyncTaskId)
      .map((i) => i.asyncTaskId)
    if (asyncTasks.length) {
      ElMessage.info(`${asyncTasks.length} 个大文件已提交异步任务，完成后可在知识库详情查看`)
      pollBatchAsyncTasks(asyncTasks)
    }
    if (data.failed === 0) {
      const firstOk = data.items?.find((i) => i.success && i.document?.docId)
      if (firstOk?.document?.docId) localStorage.setItem('lastDocId', firstOk.document.docId)
      ElMessage.success(`全部 ${data.succeeded} 个文件入库成功`)
      navigateToLibraryDocuments()
      return
    }
    if (data.succeeded > 0) {
      const firstOk = data.items?.find((i) => i.success && i.document?.docId)
      if (firstOk?.document?.docId) localStorage.setItem('lastDocId', firstOk.document.docId)
      ElMessage.warning(
        batchFailureSummary(data.items)
          ? `成功 ${data.succeeded} 个，失败 ${data.failed} 个：${batchFailureSummary(data.items)}`
          : `成功 ${data.succeeded} 个，失败 ${data.failed} 个`
      )
      navigateToLibraryDocuments()
      return
    }
    batchResult.value = data
    ElMessage.error(batchFailureSummary(data.items) || '全部文件入库失败')
  } finally {
    loading.value = false
    uploading.value = false
  }
}

async function pollTask(taskId) {
  const timer = setInterval(async () => {
    try {
      const { data } = await getUploadTask(taskId)
      if (data.status === 'COMPLETED') {
        clearInterval(timer)
        if (data.docId) localStorage.setItem('lastDocId', data.docId)
        ElMessage.success('异步入库完成')
        navigateToLibraryDocuments()
      } else if (data.status === 'FAILED') {
        clearInterval(timer)
        ElMessage.error(enrichPlainIngestMessage(data.errorMessage) || '异步入库失败，请稍后重试')
      }
    } catch {
      clearInterval(timer)
    }
  }, 3000)
}

function pollBatchAsyncTasks(taskIds) {
  const pending = new Set(taskIds.filter(Boolean))
  if (!pending.size) return
  const timer = setInterval(async () => {
    for (const taskId of [...pending]) {
      try {
        const { data } = await getUploadTask(taskId)
        if (data.status === 'COMPLETED' || data.status === 'FAILED') {
          pending.delete(taskId)
        }
      } catch {
        pending.delete(taskId)
      }
    }
    if (!pending.size) {
      clearInterval(timer)
      ElMessage.success('批量异步入库任务已全部结束')
      loadExistingDocs()
    }
  }, 4000)
}

function resetIngest() {
  fileList.value = []
  folderPickHint.value = ''
  pickNotice.value = null
  singleResult.value = null
  batchResult.value = null
  resetPreviewState()
}

function retryFailed() {
  if (!batchResult.value?.items) return
  const failedNames = new Set(
    batchResult.value.items.filter((i) => !i.success).map((i) => i.fileName)
  )
  fileList.value = fileList.value.filter((f) => failedNames.has(f.name))
  batchResult.value = null
  singleResult.value = null
  resetPreviewState()
  syncSelectedPreviewFile()
  ElMessage.info('已保留失败文件，请重新预览并入库')
}
</script>

<style scoped>
.ingest-page {
  width: 100%;
  min-width: 0;
}

.stat-chip {
  margin-right: 8px;
  font-size: 13px;
  color: var(--dp-text-secondary);
  font-weight: 600;
}

.ingest-layout {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ingest-main-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ingest-main-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
  margin-bottom: 8px;
}

.ingest-main-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  height: 36px;
}

.ingest-main-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.ingest-main-tabs :deep(.el-tab-pane) {
  height: 100%;
  overflow: hidden;
}

.ingest-tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.ingest-tab-badge :deep(.el-badge__content) {
  font-size: 10px;
}

.ingest-tab-body {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.ingest-tab-body--settings {
  overflow: hidden;
}

.ingest-workspace {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workflow-split {
  flex: 1;
  min-height: 0;
  display: grid;
  grid-template-columns: minmax(360px, 48%) 1fr;
  gap: 10px;
  overflow: hidden;
}

.ingest-card--pick {
  min-height: 0;
  height: 100%;
  max-height: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-bottom: 8px;
}

.ingest-card--preview {
  min-height: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ingest-card__head--fixed {
  flex-shrink: 0;
}

.ingest-card--result {
  flex-shrink: 0;
  max-height: 160px;
  overflow-y: auto;
  background: #fafbfc;
}

.ingest-card {
  padding: 12px 14px;
  background: var(--dp-surface);
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  box-shadow: var(--dp-shadow-sm);
}

.ingest-card--compact {
  padding: 8px 10px;
}

.ingest-card--compact .ingest-card__head {
  margin-bottom: 6px;
}

.ingest-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.ingest-card__head-left {
  flex: 1;
  min-width: 0;
}

.ingest-card__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text);
}

.ingest-card__subtitle {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  color: var(--dp-text-secondary);
}

.ingest-card__meta {
  margin-left: auto;
  font-size: 12px;
  color: var(--dp-text-secondary);
}

.ingest-card__meta strong {
  color: var(--dp-text);
}

.ingest-card__action {
  margin-left: auto;
}

.override-form :deep(.el-form-item) {
  margin-bottom: 8px;
}

.override-form :deep(.el-form-item__label) {
  font-size: 12px;
  padding-bottom: 2px;
  line-height: 1.3;
}

.override-inline {
  display: flex;
  gap: 10px;
}

.override-inline__item {
  flex: 1;
  min-width: 0;
  margin-bottom: 0 !important;
}

.override-chunk-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notice {
  padding: 6px 8px;
  border-radius: 6px;
  margin-bottom: 6px;
  font-size: 11px;
}

.notice--warn {
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.notice--error {
  background: #fef2f2;
  border: 1px solid #fecaca;
}

.notice__title {
  font-weight: 600;
  color: #334155;
  margin-bottom: 6px;
}

.notice__controls {
  margin-bottom: 6px;
}

.notice__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.ingest-mode {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 4px;
}

.ingest-mode__label {
  font-size: 12px;
  color: var(--dp-text-secondary);
  flex-shrink: 0;
}

.pick-head {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.pick-head__stats {
  margin-left: auto;
}

.pick-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: hidden;
}

.pick-list-section {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding-top: 8px;
  border-top: 1px solid #f1f5f9;
}

.pick-list-bar__count {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
}

.pick-entries--row {
  grid-template-columns: 1fr 1fr;
}

.pick-entries--row .pick-entry-wrap--folder {
  border-left: 1px solid #f1f5f9;
  border-top: none;
}

.settings-panel {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 12px;
  align-content: start;
  padding-right: 4px;
}

.settings-section__head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.settings-lock-alert {
  margin-bottom: 12px;
}

.settings-lock-alert :deep(.el-alert__description) {
  line-height: 1.5;
}

.settings-section__hint {
  margin-left: auto;
  font-size: 11px;
  color: #94a3b8;
}

.settings-rules-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
}

.settings-kv {
  min-width: 0;
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

.settings-form :deep(.el-form-item) {
  margin-bottom: 14px;
}

.settings-form :deep(.el-form-item__label) {
  color: #64748b;
}

.settings-form__tip {
  margin: 4px 0 0;
  font-size: 11px;
  color: #94a3b8;
}

.ingest-action-bar--secondary {
  justify-content: space-between;
}

.pick-list-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.pick-list-bar__title {
  font-size: 12px;
  font-weight: 600;
  color: #475569;
}

.pick-list-bar .pick-stats {
  flex: 1;
  min-width: 0;
}

.pick-list-hint {
  flex-shrink: 0;
  margin-bottom: 4px;
  font-size: 11px;
  color: #0369a1;
}

.pick-list-empty {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
}

.pick-list-empty--warn {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.pick-head__main {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pick-stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.pick-stat {
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  color: #475569;
  background: #f1f5f9;
  border-radius: 999px;
}

.pick-stat--primary {
  color: #0369a1;
  background: #e0f2fe;
}

.pick-stat--muted {
  color: #64748b;
  background: #f8fafc;
}

.pick-panel {
  flex-shrink: 0;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
  transition: border-color 0.2s ease;
}

.pick-panel--has-files {
  border-style: solid;
  border-color: #e2e8f0;
}

.pick-entries {
  display: grid;
  grid-template-columns: 1fr 1fr;
  min-height: 96px;
}

.pick-entry-wrap {
  position: relative;
  min-height: 96px;
  background: #fafbfc;
  transition: background 0.15s ease;
}

.pick-entry-wrap--folder {
  cursor: pointer;
  border-left: 1px solid #f1f5f9;
}

.pick-entry-wrap--folder:hover,
.pick-entry-wrap--folder:focus-visible {
  background: #f0f9ff;
  outline: none;
}

.pick-entry-wrap--primary {
  background: linear-gradient(180deg, #f0f9ff 0%, #fafbfc 100%);
}

.pick-entry-wrap--primary.pick-entry-wrap--folder:hover,
.pick-entry-wrap--primary.pick-entry-wrap--folder:focus-visible {
  background: linear-gradient(180deg, #e0f2fe 0%, #f0f9ff 100%);
}

.pick-upload--entry {
  width: 100%;
  height: 100%;
}

.pick-upload--entry :deep(.el-upload) {
  width: 100%;
  height: 100%;
}

.pick-upload--entry :deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
}

.pick-upload--entry :deep(.el-upload-dragger:hover) {
  background: #f0f9ff;
}

.pick-entry-wrap--primary .pick-upload--entry :deep(.el-upload-dragger:hover) {
  background: linear-gradient(180deg, #e0f2fe 0%, #f0f9ff 100%);
}

.pick-entry__inner {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 100%;
  min-height: 88px;
  padding: 10px 12px;
  text-align: center;
}

.pick-entry__badge {
  position: absolute;
  top: 8px;
  right: 8px;
  height: 20px;
  padding: 0 8px;
  font-size: 10px;
}

.pick-entry__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 18px;
}

.pick-entry__icon--file {
  background: linear-gradient(145deg, #e0f2fe 0%, #f0f9ff 100%);
  color: var(--dp-primary);
}

.pick-entry__icon--folder {
  background: linear-gradient(145deg, #ecfdf5 0%, #f0fdf4 100%);
  color: #059669;
}

.pick-entry__title {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.pick-entry__desc {
  margin: 0;
  font-size: 11px;
  line-height: 1.4;
  color: #94a3b8;
}

.pick-meta {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0;
  padding: 5px 10px;
  font-size: 10px;
  line-height: 1.4;
  color: #94a3b8;
  background: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

.pick-meta__accent {
  color: #0369a1;
  font-weight: 500;
}

.folder-input-hidden {
  display: none;
}

.pick-list__body {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-y: auto;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
}

.pick-notice {
  margin-top: 8px;
}

.pick-notice :deep(.el-alert__title) {
  font-size: 12px;
}

.pick-notice :deep(.el-alert__description) {
  font-size: 11px;
  line-height: 1.45;
}

.pick-list-bar__hint {
  margin-left: auto;
  font-size: 11px;
  color: #64748b;
}

.pick-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 8px;
  border-bottom: 1px solid #f8fafc;
  border-left: 2px solid transparent;
  cursor: pointer;
  transition: background 0.12s ease, border-color 0.12s ease;
}

.pick-item:last-child {
  border-bottom: none;
}

.pick-item:hover {
  background: #f8fafc;
}

.pick-item--active {
  background: #eff6ff;
  border-left-color: #3b82f6;
}

.pick-item--active .pick-item__name {
  color: #1d4ed8;
}

.pick-item--previewed.pick-item--active {
  background: #ecfdf5;
  border-left-color: #10b981;
}

.pick-item__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 5px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
}

.pick-item__info {
  flex: 1;
  min-width: 0;
}

.pick-item__name {
  display: block;
  font-size: 12px;
  font-weight: 500;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pick-item__path {
  display: block;
  margin-top: 1px;
  font-size: 10px;
  color: #94a3b8;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pick-item__size {
  flex-shrink: 0;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
}

.pick-item__tag {
  flex-shrink: 0;
}

.pick-item__remove {
  flex-shrink: 0;
  padding: 4px;
  margin: -4px 0;
  opacity: 0.5;
  transition: opacity 0.12s ease;
}

.pick-item:hover .pick-item__remove {
  opacity: 1;
}

.pick-empty {
  margin-top: 10px;
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
}

.pick-empty--warn {
  color: #b45309;
  background: #fffbeb;
  border-color: #fde68a;
}

.pick-skipped {
  flex-shrink: 0;
  margin-top: 4px;
  padding: 4px 8px;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 6px;
}

.pick-skipped__label {
  display: block;
  margin-bottom: 4px;
  font-size: 10px;
  color: #64748b;
}

.pick-skipped__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  flex-shrink: 0;
  min-width: 0;
}

.preview-actions__target {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: #64748b;
}

.preview-loading__label {
  margin: 0 0 8px;
  font-size: 12px;
  color: #64748b;
}

.preview-placeholder {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #94a3b8;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
  border-radius: 8px;
}

.preview-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-loading {
  flex: 1;
  min-height: 0;
  padding: 4px 0;
  overflow: hidden;
}

.ingest-card__head--compact {
  margin-bottom: 6px;
}

.preview-overview {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px 8px;
  padding: 4px 8px;
  margin-bottom: 4px;
  font-size: 11px;
  line-height: 1.4;
  color: #64748b;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 6px;
}

.preview-overview__item strong {
  color: #0f172a;
  font-weight: 600;
}

.preview-overview__sep {
  color: #cbd5e1;
  user-select: none;
}

.preview-overview__file {
  flex: 1;
  min-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
  font-size: 11px;
  color: #94a3b8;
}

.preview-overview__tag {
  flex-shrink: 0;
  height: 20px;
  padding: 0 6px;
  font-size: 10px;
}

.preview-overview-hint {
  flex-shrink: 0;
  margin: 0 0 6px;
  padding: 0 2px;
  font-size: 11px;
  line-height: 1.4;
  color: #94a3b8;
}

.preview-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-tabs :deep(.el-tabs__header) {
  flex-shrink: 0;
  margin-bottom: 0;
}

.preview-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0;
}

.preview-tabs :deep(.el-tabs__item) {
  font-size: 12px;
  height: 28px;
  line-height: 28px;
  padding: 0 12px;
}

.preview-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-tabs :deep(.el-tab-pane) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.preview-pane {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-pane--text {
  overflow-x: hidden;
  overflow-y: auto;
  padding-top: 4px;
}

.preview-pane--table {
  overflow-x: hidden;
  overflow-y: auto;
  padding-top: 4px;
}

.preview-chunk-table {
  width: 100%;
}

.preview-pane__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 12px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.text-excerpt {
  margin: 0;
  flex: 1;
  min-height: 0;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.6;
  color: #475569;
  background: #f8fafc;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  white-space: pre-wrap;
  word-break: break-word;
  overflow-y: auto;
}

.upload-progress {
  margin-bottom: 10px;
}

.upload-progress .hint {
  display: block;
  margin-top: 4px;
  font-size: 11px;
}

.result-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-capacity-alert {
  margin: 0;
}

.result-banner {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.result-banner--success {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
}

.result-banner--warn {
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.result-banner__title {
  font-weight: 600;
  color: #0f172a;
}

.result-banner__sub {
  font-size: 12px;
  color: #64748b;
}

.result-actions {
  display: flex;
  gap: 8px;
}

.status-text {
  font-size: 12px;
  color: #475569;
}

.id-code {
  font-size: 11px;
  background: #fff;
  padding: 1px 6px;
  border-radius: 4px;
  font-family: ui-monospace, monospace;
}

.ingest-action-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  margin-top: 2px;
  background: linear-gradient(180deg, #fafbfc 0%, #fff 100%);
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
}

.ingest-action-bar__hint {
  font-size: 12px;
  color: var(--dp-text-secondary);
}

.ingest-action-bar__hint strong {
  color: var(--dp-text);
}

.ingest-action-bar__btns {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.hint {
  font-size: 11px;
  color: #64748b;
  margin: 0;
  line-height: 1.4;
}

.muted {
  color: #94a3b8;
  font-size: 11px;
}

@media (max-width: 960px) {
  .workflow-split {
    grid-template-columns: 1fr;
    overflow: visible;
  }

  .ingest-card--pick,
  .ingest-card--preview {
    height: auto;
    min-height: 200px;
  }

  .pick-entries--row {
    grid-template-columns: 1fr 1fr;
  }

  .pick-list__body {
    max-height: 180px;
  }

  .settings-rules-grid {
    grid-template-columns: 1fr;
  }

  .settings-panel {
    grid-template-columns: 1fr;
  }

  .ingest-card--preview {
    min-height: 280px;
  }

  .ingest-card--result {
    max-height: none;
  }

  .override-inline {
    flex-direction: column;
  }

  .ingest-action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .ingest-action-bar__btns {
    justify-content: flex-end;
  }
}
</style>
