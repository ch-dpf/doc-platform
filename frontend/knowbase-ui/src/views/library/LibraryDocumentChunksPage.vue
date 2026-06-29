<template>
  <PageCard title="文档详情" :subtitle="documentSubtitle">
    <template #actions>
      <el-button link type="primary" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回文档列表
      </el-button>
      <el-button round :loading="loading" @click="refreshActiveTab">刷新</el-button>
    </template>

    <el-descriptions v-if="doc" :column="2" border size="small" class="doc-meta">
      <el-descriptions-item label="标题">{{ doc.title || '—' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag size="small" :type="doc.status === 'INDEXED' ? 'success' : doc.status === 'FAILED' ? 'danger' : 'info'">
          {{ doc.status }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="块总数">{{ total }}</el-descriptions-item>
      <el-descriptions-item label="来源" :span="1">{{ doc.sourceUri || '—' }}</el-descriptions-item>
    </el-descriptions>

    <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="原文" name="preview">
        <div v-if="previewLoading" class="preview-state muted">加载原文预览…</div>
        <el-alert v-else-if="previewError" type="error" :title="previewError" show-icon :closable="false" />
        <template v-else-if="preview">
          <div v-if="pdfPage" class="preview-toolbar muted">
            当前定位：第 {{ pdfPage }} 页
            <el-button link type="primary" @click="pdfPage = null">清除</el-button>
          </div>
          <div v-if="previewMode === 'pdf'" class="pdf-preview-layout">
            <PdfPreviewPanel
              :source-url="previewObjectUrl"
              :page-number="pdfPage || selectedChunkPage || 1"
              :chunk-metadata="selectedChunkMetadata"
            />
          </div>
          <pre v-else-if="previewMode === 'text'" class="preview-text">{{ previewText }}</pre>
          <div
            v-else-if="previewMode === 'docx-html'"
            ref="docxPreviewRef"
            class="preview-docx docx-preview"
            v-html="docxHtml"
          />
          <img
            v-else-if="previewMode === 'image'"
            class="preview-image"
            :src="previewObjectUrl"
            :alt="preview.filename"
          />
          <div v-else-if="previewMode === 'excel'" class="preview-excel-wrap">
            <el-tabs
              v-if="excelSheets.length > 1"
              v-model="activeExcelSheet"
              class="excel-sheet-tabs"
            >
              <el-tab-pane
                v-for="(sheet, index) in excelSheets"
                :key="sheet.name"
                :label="sheet.name"
                :name="String(index)"
              />
            </el-tabs>
            <div ref="excelPreviewRef" class="preview-excel" v-html="currentExcelHtml" />
          </div>
          <div v-else class="preview-fallback">
            <el-alert
              type="info"
              show-icon
              :closable="false"
              title="该格式暂不支持在线预览"
              :description="`类型：${preview.contentType}`"
            />
            <el-button type="primary" round @click="downloadPreview">下载原文</el-button>
          </div>
          <div v-if="previewMode !== 'unsupported'" class="preview-actions">
            <el-button round @click="downloadPreview">下载原文</el-button>
          </div>
        </template>
        <el-empty v-else description="暂无原文预览" />
      </el-tab-pane>

      <el-tab-pane label="分块" name="chunks">
        <p v-if="chunks.length" class="chunks-hint muted">
          点击分块可定位原文：PDF 跳页并高亮 bbox；Excel 跳转 Sheet 并高亮单元格；Word 滚动并高亮匹配片段。
        </p>
        <div v-if="chunks.length" class="chunk-list">
          <div
            v-for="(chunk, index) in chunks"
            :key="chunk.chunkId"
            class="chunk-card"
            :class="{
              'chunk-card--active': selectedChunkId === chunk.chunkId,
              'chunk-card--disabled': !isChunkRetrievalEnabled(chunk.metadata)
            }"
            @click="locateChunk(chunk)"
          >
            <div class="chunk-card__head">
              <span class="chunk-card__index">#{{ rowIndex(index) }}</span>
              <span class="chunk-card__id">{{ shortId(chunk.chunkId, 12) }}</span>
              <el-tag size="small" :type="chunkRoleTagType(chunk.metadata, chunk.chunkBoundaryType)">
                {{ formatChunkRoleLabel(chunk.metadata, chunk.chunkBoundaryType) }}
              </el-tag>
              <span class="chunk-card__meta">{{ chunk.tokenCount }} tokens · {{ chunk.chunkBoundaryType || '—' }}</span>
              <el-tag
                v-if="!isChunkRetrievalEnabled(chunk.metadata)"
                size="small"
                type="info"
              >
                不参与检索
              </el-tag>
              <el-tag
                v-for="tag in formatChunkLocationTags(chunk.metadata)"
                :key="`${chunk.chunkId}-${tag.key}`"
                size="small"
                type="warning"
                effect="plain"
              >
                {{ tag.label }}
              </el-tag>
            </div>
            <p class="chunk-card__content">{{ chunk.content }}</p>
            <div class="chunk-card__actions" @click.stop>
              <el-button link type="primary" @click="openEditDialog(chunk)">编辑</el-button>
              <el-button
                v-if="canLocateChunk(chunk.metadata)"
                link
                type="primary"
                @click="locateChunk(chunk)"
              >
                定位原文
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="该文档暂无检索分块" />

        <div v-if="total > 0" class="table-pagination">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            background
            @current-change="loadChunks"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="入库 Trace" name="trace">
        <div v-if="pipelineTrace" class="trace-panel" v-loading="traceLoading">
          <p class="trace-panel__hint muted">
            展示该文档最近一次入库的主线阶段：加载 → 解析 → 清洗 → 分块 → 向量化 → 写索引。
          </p>
          <div class="trace-panel__header">
            <div class="trace-panel__meta">
              <div class="trace-panel__meta-item">
                <span class="trace-panel__meta-label">入库任务</span>
                <code class="trace-panel__meta-value">{{ shortId(pipelineTrace.runId, 12) }}</code>
              </div>
              <div v-if="pipelineTrace.traceId" class="trace-panel__meta-item">
                <span class="trace-panel__meta-label">Trace</span>
                <code class="trace-panel__meta-value">{{ shortId(pipelineTrace.traceId, 12) }}</code>
              </div>
              <div class="trace-panel__meta-item">
                <span class="trace-panel__meta-label">作业状态</span>
                <el-tag size="small" :type="statusTagType(pipelineTrace.jobStatus)">
                  {{ pipelineTrace.jobStage || '—' }} / {{ pipelineTrace.jobStatus || '—' }}
                </el-tag>
              </div>
              <div class="trace-panel__meta-item">
                <span class="trace-panel__meta-label">索引分块</span>
                <span class="trace-panel__meta-value">{{ pipelineTrace.chunkCount }}</span>
              </div>
            </div>
            <el-button size="small" round :loading="traceLoading" @click="loadPipelineTrace(true)">
              刷新
            </el-button>
          </div>
          <PipelineTraceTimeline
            variant="stepper"
            :stages="documentPipelineStages"
            :spans="documentTraceSpans"
            show-timestamps
            empty-text="暂无该文档的主线阶段 Span，请确认入库任务已完成"
          />
        </div>
        <el-empty v-else-if="!traceLoading" description="该文档尚无入库 Trace 记录" />
        <div v-else class="preview-state muted">加载 Trace…</div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="editDialogVisible" title="编辑分块" width="640px" destroy-on-close>
      <el-form label-width="96px">
        <el-form-item label="块内容">
          <el-input v-model="editForm.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="参与检索">
          <el-switch v-model="editForm.retrievalEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveChunkEdit">保存</el-button>
      </template>
    </el-dialog>
  </PageCard>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft } from '@element-plus/icons-vue';
import mammoth from 'mammoth';
import PageCard from '../../components/PageCard.vue';
import PdfPreviewPanel from '../../components/PdfPreviewPanel.vue';
import PipelineTraceTimeline from '../../components/PipelineTraceTimeline.vue';
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import { fetchDocumentPreview, getDocument, getDocumentPipelineTrace, listPipelineTrace, pageDocumentChunks, updateDocumentChunk } from '../../api';
import {
  formatChunkLocationTags,
  formatChunkRoleLabel,
  chunkRoleTagType,
  isChunkRetrievalEnabled,
  shortId,
  DOCUMENT_PIPELINE_STAGES,
  filterDocumentPipelineSpans
} from '../../format';
import {
  canLocateChunk,
  clearDocxHighlights,
  clearExcelHighlights,
  highlightDocxSnippet,
  highlightExcelCells,
  primaryCellRef
} from '../../citationLocate';

const documentPipelineStages = DOCUMENT_PIPELINE_STAGES;

const route = useRoute();
const router = useRouter();
const { libraryId, showMessage } = useLibraryWorkspace();

const doc = ref(null);
const chunks = ref([]);
const total = ref(0);
const loading = ref(false);
const traceLoading = ref(false);
const pipelineTrace = ref(null);
const traceSpans = ref([]);
const pagination = reactive({ page: 1, size: 20 });
const activeTab = ref('preview');

const preview = ref(null);
const previewText = ref('');
const previewObjectUrl = ref('');
const docxHtml = ref('');
const excelSheets = ref([]);
const activeExcelSheet = ref('0');
const previewLoading = ref(false);
const previewError = ref('');
const pdfPage = ref(null);
const selectedChunkId = ref(null);
const docxPreviewRef = ref(null);
const excelPreviewRef = ref(null);

const editDialogVisible = ref(false);
const editSaving = ref(false);
const editForm = reactive({
  chunkId: '',
  content: '',
  retrievalEnabled: true
});

const documentId = computed(() => route.params.documentId);

const documentSubtitle = computed(() => {
  if (!doc.value) {
    return '加载文档详情…';
  }
  return `${doc.value.title || shortId(documentId.value, 12)} · 共 ${total.value} 块`;
});

const documentTraceSpans = computed(() =>
  filterDocumentPipelineSpans(traceSpans.value, {
    documentId: documentId.value,
    sourceUri: doc.value?.sourceUri
  })
);

function statusTagType(status) {
  const value = String(status || '').toUpperCase();
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  return 'info';
}

const previewMode = computed(() => {
  if (!preview.value) {
    return null;
  }
  return classifyPreviewMode(preview.value.contentType, preview.value.filename);
});

const selectedChunkMetadata = computed(() => {
  if (!selectedChunkId.value) {
    return null;
  }
  return chunks.value.find((item) => item.chunkId === selectedChunkId.value)?.metadata ?? null;
});

const selectedChunkPage = computed(() => {
  if (!selectedChunkId.value) {
    return null;
  }
  const chunk = chunks.value.find((item) => item.chunkId === selectedChunkId.value);
  const page = chunk?.metadata?.pageNumber;
  return page == null ? null : Number(page);
});

const currentExcelHtml = computed(() => {
  const index = Number(activeExcelSheet.value);
  return excelSheets.value[index]?.html || '';
});

function classifyPreviewMode(contentType, filename) {
  const mime = String(contentType || '').toLowerCase();
  const lower = String(filename || '').toLowerCase();
  if (mime.includes('pdf') || lower.endsWith('.pdf')) {
    return 'pdf';
  }
  if (
    mime.startsWith('text/')
    || mime.includes('markdown')
    || lower.endsWith('.md')
    || lower.endsWith('.txt')
    || lower.endsWith('.csv')
    || lower.endsWith('.log')
  ) {
    return 'text';
  }
  if (mime.startsWith('image/')) {
    return 'image';
  }
  if (
    lower.endsWith('.xlsx')
    || lower.endsWith('.xls')
    || ((mime.includes('spreadsheet') || mime.includes('excel') || mime.includes('ms-excel'))
      && !mime.includes('csv'))
  ) {
    return 'excel';
  }
  if (
    mime.includes('wordprocessingml')
    || mime.includes('word')
    || lower.endsWith('.docx')
  ) {
    return 'docx-html';
  }
  if (lower.endsWith('.doc')) {
    return 'unsupported';
  }
  return 'unsupported';
}

function rowIndex(index) {
  return (pagination.page - 1) * pagination.size + index + 1;
}

function chunkPageNumber(chunk) {
  const page = chunk?.metadata?.pageNumber;
  return page == null ? null : Number(page);
}

function revokePreviewUrl() {
  if (previewObjectUrl.value) {
    URL.revokeObjectURL(previewObjectUrl.value);
    previewObjectUrl.value = '';
  }
}

function resetPreviewState() {
  revokePreviewUrl();
  clearDocxHighlights(docxPreviewRef.value);
  clearExcelHighlights(excelPreviewRef.value);
  preview.value = null;
  previewText.value = '';
  docxHtml.value = '';
  excelSheets.value = [];
  activeExcelSheet.value = '0';
  previewError.value = '';
  pdfPage.value = null;
  selectedChunkId.value = null;
}

async function loadDocument() {
  doc.value = await getDocument(libraryId.value, documentId.value);
}

async function renderDocxPreview(blob) {
  const arrayBuffer = await blob.arrayBuffer();
  const result = await mammoth.convertToHtml({ arrayBuffer });
  docxHtml.value = result.value;
}

async function renderExcelPreview(blob) {
  const XLSX = await import('xlsx');
  const arrayBuffer = await blob.arrayBuffer();
  const workbook = XLSX.read(arrayBuffer, { type: 'array' });
  excelSheets.value = workbook.SheetNames.map((name) => ({
    name,
    html: XLSX.utils.sheet_to_html(workbook.Sheets[name])
  }));
  activeExcelSheet.value = '0';
}

async function loadPreview() {
  previewLoading.value = true;
  previewError.value = '';
  resetPreviewState();
  try {
    if (!doc.value) {
      await loadDocument();
    }
    const result = await fetchDocumentPreview(libraryId.value, documentId.value);
    preview.value = result;
    const mode = classifyPreviewMode(result.contentType, result.filename);
    if (mode === 'text') {
      previewText.value = await result.blob.text();
    } else if (mode === 'pdf' || mode === 'image') {
      previewObjectUrl.value = URL.createObjectURL(result.blob);
    } else if (mode === 'docx-html') {
      await renderDocxPreview(result.blob);
    } else if (mode === 'excel') {
      await renderExcelPreview(result.blob);
    }
  } catch (error) {
    previewError.value = error.message;
  } finally {
    previewLoading.value = false;
  }
}

function downloadPreview() {
  if (!preview.value) {
    return;
  }
  const url = URL.createObjectURL(preview.value.blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = preview.value.filename || 'document';
  anchor.click();
  URL.revokeObjectURL(url);
}

async function locateChunk(chunk) {
  selectedChunkId.value = chunk.chunkId;
  const page = chunkPageNumber(chunk);
  const sheetName = chunk?.metadata?.sheetName;
  if (previewMode.value === 'excel' && sheetName && excelSheets.value.length) {
    activeTab.value = 'preview';
    const index = excelSheets.value.findIndex((sheet) => sheet.name === String(sheetName));
    if (index >= 0) {
      activeExcelSheet.value = String(index);
      await nextTick();
      const highlighted = highlightExcelCells(excelPreviewRef.value, chunk.metadata);
      const cellLabel = primaryCellRef(chunk.metadata);
      if (highlighted.length) {
        showMessage(
          cellLabel
            ? `已定位 ${sheetName} · ${cellLabel}（高亮 ${highlighted.length} 个单元格）`
            : `已定位 ${sheetName}（高亮 ${highlighted.length} 个单元格）`,
          'success'
        );
      } else {
        showMessage(`已定位 Sheet：${sheetName}，未匹配到可高亮单元格`, 'info');
      }
      return;
    }
    showMessage(`未找到 Sheet：${sheetName}`, 'info');
    return;
  }
  if (previewMode.value === 'pdf' && page != null) {
    activeTab.value = 'preview';
    pdfPage.value = page;
    showMessage(`已跳转到第 ${page} 页并高亮引用区域`, 'success');
    return;
  }
  if (previewMode.value === 'docx-html') {
    activeTab.value = 'preview';
    await nextTick();
    const highlighted = highlightDocxSnippet(docxPreviewRef.value, chunk.content);
    const sectionPath = chunk.metadata?.wordSectionPath;
    if (highlighted) {
      showMessage(
        sectionPath
          ? `已在 Word 预览中高亮片段（${Array.isArray(sectionPath) ? sectionPath.join(' > ') : sectionPath}）`
          : '已在 Word 预览中高亮匹配片段',
        'success'
      );
    } else {
      showMessage('已在 Word 预览中滚动到匹配片段', 'success');
    }
    return;
  }
  if (previewMode.value === 'excel') {
    activeTab.value = 'preview';
    showMessage('Excel 预览缺少 Sheet 元数据，无法定位单元格', 'info');
    return;
  }
  if (page != null) {
    activeTab.value = 'preview';
    showMessage(`该块位于第 ${page} 页，当前格式暂不支持自动跳页`, 'info');
    return;
  }
  showMessage('该块暂无页码/bbox/单元格定位信息', 'info');
}

function openEditDialog(chunk) {
  editForm.chunkId = chunk.chunkId;
  editForm.content = chunk.content;
  editForm.retrievalEnabled = isChunkRetrievalEnabled(chunk.metadata);
  editDialogVisible.value = true;
}

async function saveChunkEdit() {
  editSaving.value = true;
  try {
    const updated = await updateDocumentChunk(
      libraryId.value,
      documentId.value,
      editForm.chunkId,
      {
        content: editForm.content,
        retrievalEnabled: editForm.retrievalEnabled
      }
    );
    const index = chunks.value.findIndex((item) => item.chunkId === updated.chunkId);
    if (index >= 0) {
      chunks.value[index] = updated;
    }
    editDialogVisible.value = false;
    showMessage('分块已更新', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    editSaving.value = false;
  }
}

async function loadPipelineTrace(force = false) {
  if (pipelineTrace.value && !force) {
    return;
  }
  traceLoading.value = true;
  try {
    pipelineTrace.value = await getDocumentPipelineTrace(libraryId.value, documentId.value);
    traceSpans.value = [];
    if (pipelineTrace.value?.traceId) {
      traceSpans.value = await listPipelineTrace(pipelineTrace.value.traceId);
    }
  } catch (error) {
    pipelineTrace.value = null;
    traceSpans.value = [];
    if (force) {
      showMessage(error.message, 'error');
    }
  } finally {
    traceLoading.value = false;
  }
}

async function loadChunks() {
  loading.value = true;
  try {
    if (!doc.value) {
      await loadDocument();
    }
    const data = await pageDocumentChunks(libraryId.value, documentId.value, {
      page: pagination.page,
      size: pagination.size
    });
    chunks.value = data.items ?? [];
    total.value = data.total ?? 0;
    pagination.page = data.page ?? pagination.page;
    pagination.size = data.size ?? pagination.size;
  } catch (error) {
    showMessage(error.message, 'error');
    chunks.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function handlePageSizeChange() {
  pagination.page = 1;
  loadChunks();
}

function handleTabChange(tabName) {
  if (tabName === 'preview' && !preview.value && !previewLoading.value && !previewError.value) {
    loadPreview();
  }
  if (tabName === 'chunks' && !chunks.value.length && !loading.value) {
    loadChunks();
  }
  if (tabName === 'trace' && !pipelineTrace.value && !traceLoading.value) {
    loadPipelineTrace();
  }
}

function refreshActiveTab() {
  if (activeTab.value === 'preview') {
    loadPreview();
    return;
  }
  if (activeTab.value === 'trace') {
    loadPipelineTrace(true);
    return;
  }
  loadChunks();
}

function goBack() {
  router.push({ name: 'library-documents', params: { libraryId: libraryId.value } });
}

watch(documentId, () => {
  doc.value = null;
  chunks.value = [];
  pipelineTrace.value = null;
  traceSpans.value = [];
  pagination.page = 1;
  resetPreviewState();
  activeTab.value = 'preview';
  loadPreview();
  loadChunks();
});

async function applyRouteLocate() {
  const page = route.query.page ? Number(route.query.page) : null;
  const sheet = route.query.sheet ? String(route.query.sheet) : null;
  const chunkId = route.query.chunkId ? String(route.query.chunkId) : null;
  if (page != null && Number.isFinite(page)) {
    activeTab.value = 'preview';
    pdfPage.value = page;
  }
  if (sheet && excelSheets.value.length) {
    activeTab.value = 'preview';
    const index = excelSheets.value.findIndex((item) => item.name === sheet);
    if (index >= 0) {
      activeExcelSheet.value = String(index);
    }
  }
  if (chunkId && chunks.value.length) {
    const chunk = chunks.value.find((item) => item.chunkId === chunkId);
    if (chunk) {
      activeTab.value = 'chunks';
      await locateChunk(chunk);
    }
  }
}

watch(activeExcelSheet, async () => {
  if (!selectedChunkId.value || previewMode.value !== 'excel') {
    return;
  }
  const chunk = chunks.value.find((item) => item.chunkId === selectedChunkId.value);
  if (!chunk) {
    return;
  }
  await nextTick();
  highlightExcelCells(excelPreviewRef.value, chunk.metadata);
});

onMounted(async () => {
  await loadPreview();
  await loadChunks();
  await applyRouteLocate();
});

onBeforeUnmount(revokePreviewUrl);
</script>

<style scoped>
.doc-meta {
  margin-bottom: 20px;
}

.detail-tabs {
  margin-top: 4px;
}

.preview-state {
  padding: 24px 0;
  text-align: center;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
}

.preview-frame {
  width: 100%;
  min-height: 72vh;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: #fff;
}

.pdf-preview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 12px;
  align-items: start;
}

.pdf-bbox-schematic {
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  padding: 12px;
  background: var(--dp-surface, #fafafa);
}

.pdf-bbox-schematic__title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.pdf-bbox-schematic__page {
  position: relative;
  width: 100%;
  aspect-ratio: 612 / 792;
  background: #fff;
  border: 1px dashed var(--dp-border);
}

.pdf-bbox-schematic__region {
  position: absolute;
  border: 2px solid #409eff;
  background: rgba(64, 158, 255, 0.12);
  box-sizing: border-box;
}

.pdf-bbox-schematic__word {
  position: absolute;
  border: 1px solid rgba(230, 162, 60, 0.9);
  background: rgba(230, 162, 60, 0.18);
  box-sizing: border-box;
}

.pdf-bbox-schematic__meta {
  margin: 8px 0 0;
  font-size: 12px;
}

@media (max-width: 960px) {
  .pdf-preview-layout {
    grid-template-columns: 1fr;
  }
}

.preview-text {
  margin: 0;
  padding: 16px;
  max-height: 72vh;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.55;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: var(--dp-surface);
}

.preview-docx {
  max-height: 72vh;
  overflow: auto;
  padding: 16px 20px;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: #fff;
  font-size: 14px;
  line-height: 1.6;
}

.preview-image {
  max-width: 100%;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
}

.preview-excel-wrap {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.excel-sheet-tabs {
  margin-bottom: 0;
}

.preview-excel {
  max-height: 72vh;
  overflow: auto;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: #fff;
}

.preview-excel :deep(table) {
  border-collapse: collapse;
  width: max-content;
  min-width: 100%;
  font-size: 13px;
}

.preview-excel :deep(td),
.preview-excel :deep(th) {
  border: 1px solid #e2e8f0;
  padding: 4px 10px;
  white-space: nowrap;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.preview-excel :deep(tr:first-child td),
.preview-excel :deep(th) {
  background: #f1f5f9;
  font-weight: 600;
}

.preview-excel :deep(tr:nth-child(even)) {
  background: #f8fafc;
}

.preview-excel :deep(.citation-cell-highlight) {
  background: #fff3cd !important;
  box-shadow: inset 0 0 0 2px #e6a23c;
  outline: 2px solid rgba(230, 162, 60, 0.35);
}

.preview-docx :deep(mark.docx-citation-highlight) {
  background: #fff3cd;
  padding: 0 2px;
  border-radius: 2px;
  box-shadow: inset 0 -2px 0 #e6a23c;
}

.preview-fallback,
.preview-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 560px;
  margin-top: 12px;
}

.chunks-hint {
  margin: 0 0 12px;
  font-size: 13px;
}

.chunk-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chunk-card {
  padding: 12px 14px;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: var(--dp-surface);
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.chunk-card:hover {
  border-color: var(--el-color-primary-light-5);
}

.chunk-card--active {
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 1px var(--el-color-primary-light-7);
}

.chunk-card--disabled {
  opacity: 0.78;
}

.chunk-card__head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
}

.chunk-card__index {
  font-weight: 600;
  color: var(--dp-text);
}

.chunk-card__id {
  font-family: var(--dp-font-mono, monospace);
  color: var(--dp-text-secondary);
}

.chunk-card__meta {
  color: var(--dp-text-secondary);
  font-size: 12px;
}

.chunk-card__content {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 13px;
  line-height: 1.55;
  color: var(--dp-text);
}

.chunk-card__actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.trace-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trace-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  background: #f8fafc;
}

.trace-panel__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 24px;
}

.trace-panel__meta-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.trace-panel__meta-label {
  color: var(--dp-text-secondary);
}

.trace-panel__meta-value {
  font-family: var(--dp-font-mono, ui-monospace, monospace);
  font-size: 12px;
  color: var(--dp-text);
}

.trace-panel__hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
}
</style>
