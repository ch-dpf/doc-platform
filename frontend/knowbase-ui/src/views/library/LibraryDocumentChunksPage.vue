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
          <iframe
            v-if="previewMode === 'pdf'"
            :key="pdfFrameKey"
            class="preview-frame"
            :src="pdfPreviewUrl"
            title="PDF 预览"
          />
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
          点击分块可定位原文（PDF 跳页）；带页码/bbox 的块会高亮显示定位标签。
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
            <div v-if="extraMetadata(chunk.metadata)" class="chunk-card__metadata muted">
              {{ extraMetadata(chunk.metadata) }}
            </div>
            <div class="chunk-card__actions" @click.stop>
              <el-button link type="primary" @click="openEditDialog(chunk)">编辑</el-button>
              <el-button
                v-if="chunkPageNumber(chunk)"
                link
                type="primary"
                @click="locateChunk(chunk)"
              >
                定位原文
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else-if="!loading" description="该文档暂无分块" />

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
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import { fetchDocumentPreview, getDocument, pageDocumentChunks, updateDocumentChunk } from '../../api';
import {
  formatChunkLocationTags,
  isChunkRetrievalEnabled,
  shortId
} from '../../format';

const route = useRoute();
const router = useRouter();
const { libraryId, showMessage } = useLibraryWorkspace();

const doc = ref(null);
const chunks = ref([]);
const total = ref(0);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 20 });
const activeTab = ref('preview');

const preview = ref(null);
const previewText = ref('');
const previewObjectUrl = ref('');
const docxHtml = ref('');
const previewLoading = ref(false);
const previewError = ref('');
const pdfPage = ref(null);
const selectedChunkId = ref(null);
const docxPreviewRef = ref(null);

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

const previewMode = computed(() => {
  if (!preview.value) {
    return null;
  }
  return classifyPreviewMode(preview.value.contentType, preview.value.filename);
});

const pdfFrameKey = computed(() => `${previewObjectUrl.value || 'pdf'}-${pdfPage.value || 'all'}`);

const pdfPreviewUrl = computed(() => {
  if (!previewObjectUrl.value) {
    return '';
  }
  if (pdfPage.value == null) {
    return previewObjectUrl.value;
  }
  return `${previewObjectUrl.value}#page=${pdfPage.value}`;
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

const LOCATION_KEYS = new Set([
  'pageNumber',
  'bbox',
  'contentFamily',
  'vectorRank',
  'keywordRank',
  'retrievalEnabled',
  'chunkBoundaryType'
]);

function extraMetadata(metadata) {
  if (!metadata || typeof metadata !== 'object') {
    return '';
  }
  return Object.entries(metadata)
    .filter(([key]) => !LOCATION_KEYS.has(key))
    .map(([key, value]) => `${key}: ${value}`)
    .join(' · ');
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
  preview.value = null;
  previewText.value = '';
  docxHtml.value = '';
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

async function highlightDocxSnippet(content) {
  await nextTick();
  const container = docxPreviewRef.value;
  if (!container || !content) {
    return;
  }
  const snippet = String(content).trim().slice(0, 48);
  if (!snippet) {
    return;
  }
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  let node = walker.nextNode();
  while (node) {
    const text = node.textContent || '';
    const index = text.indexOf(snippet.slice(0, Math.min(snippet.length, 24)));
    if (index >= 0) {
      const range = document.createRange();
      range.setStart(node, index);
      range.setEnd(node, Math.min(text.length, index + snippet.length));
      range.startContainer.parentElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    node = walker.nextNode();
  }
}

async function locateChunk(chunk) {
  selectedChunkId.value = chunk.chunkId;
  const page = chunkPageNumber(chunk);
  if (previewMode.value === 'pdf' && page != null) {
    activeTab.value = 'preview';
    pdfPage.value = page;
    showMessage(`已跳转到第 ${page} 页`, 'success');
    return;
  }
  if (previewMode.value === 'docx-html') {
    activeTab.value = 'preview';
    await highlightDocxSnippet(chunk.content);
    showMessage('已在 Word 预览中滚动到匹配片段', 'success');
    return;
  }
  if (page != null) {
    activeTab.value = 'preview';
    showMessage(`该块位于第 ${page} 页，当前格式暂不支持自动跳页`, 'info');
    return;
  }
  showMessage('该块暂无页码/bbox 定位信息', 'info');
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
}

function refreshActiveTab() {
  if (activeTab.value === 'preview') {
    loadPreview();
    return;
  }
  loadChunks();
}

function goBack() {
  router.push({ name: 'library-documents', params: { libraryId: libraryId.value } });
}

watch(documentId, () => {
  doc.value = null;
  pagination.page = 1;
  resetPreviewState();
  activeTab.value = 'preview';
  loadPreview();
  loadChunks();
});

onMounted(() => {
  loadPreview();
  loadChunks();
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

.chunk-card__metadata {
  margin-top: 8px;
  font-size: 12px;
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
</style>
