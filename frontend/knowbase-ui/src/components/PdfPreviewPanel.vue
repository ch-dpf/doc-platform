<template>
  <div ref="containerRef" class="pdf-preview-panel">
    <canvas ref="canvasRef" class="pdf-preview-panel__canvas" />
    <div
      v-if="overlayReady"
      class="pdf-preview-panel__overlay"
      :style="overlayStyle"
    >
      <div
        v-if="bboxOverlay"
        class="pdf-preview-panel__region"
        :style="bboxOverlay"
      />
      <div
        v-for="word in wordOverlays"
        :key="word.key"
        class="pdf-preview-panel__word"
        :title="word.text"
        :style="word"
      />
    </div>
    <div v-if="loading" class="pdf-preview-panel__loading muted">渲染 PDF…</div>
    <div v-if="error" class="pdf-preview-panel__error">{{ error }}</div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';
import * as pdfjsLib from 'pdfjs-dist/build/pdf.mjs';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.mjs?url';
import { parseBboxOverlay, parseOcrWordOverlays } from '../format';

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker;

const props = defineProps({
  sourceUrl: {
    type: String,
    required: true
  },
  pageNumber: {
    type: Number,
    default: 1
  },
  chunkMetadata: {
    type: Object,
    default: null
  }
});

const containerRef = ref(null);
const canvasRef = ref(null);
const loading = ref(false);
const error = ref('');
const viewportSize = ref({ width: 612, height: 792 });
const overlayReady = ref(false);
let renderTask = null;
let pdfDocument = null;

const bboxOverlay = computed(() => parseBboxOverlay(props.chunkMetadata, viewportSize.value.width, viewportSize.value.height));
const wordOverlays = computed(() => parseOcrWordOverlays(props.chunkMetadata, viewportSize.value.width, viewportSize.value.height));

const overlayStyle = computed(() => ({
  width: `${viewportSize.value.width}px`,
  height: `${viewportSize.value.height}px`
}));

async function renderPage() {
  if (!props.sourceUrl || !canvasRef.value) {
    return;
  }
  loading.value = true;
  error.value = '';
  overlayReady.value = false;
  try {
    if (renderTask) {
      await renderTask.cancel();
      renderTask = null;
    }
    if (pdfDocument) {
      await pdfDocument.destroy();
      pdfDocument = null;
    }
    pdfDocument = await pdfjsLib.getDocument(props.sourceUrl).promise;
    const safePage = Math.min(Math.max(props.pageNumber || 1, 1), pdfDocument.numPages);
    const page = await pdfDocument.getPage(safePage);
    const baseViewport = page.getViewport({ scale: 1 });
    const containerWidth = containerRef.value?.clientWidth || baseViewport.width;
    const scale = containerWidth / baseViewport.width;
    const viewport = page.getViewport({ scale });
    viewportSize.value = { width: viewport.width, height: viewport.height };
    const canvas = canvasRef.value;
    const context = canvas.getContext('2d');
    canvas.width = viewport.width;
    canvas.height = viewport.height;
    renderTask = page.render({ canvasContext: context, viewport });
    await renderTask.promise;
    overlayReady.value = true;
  } catch (renderError) {
    if (renderError?.name !== 'RenderingCancelledException') {
      error.value = renderError?.message || 'PDF 渲染失败';
    }
  } finally {
    loading.value = false;
  }
}

watch(
  () => [props.sourceUrl, props.pageNumber],
  async () => {
    await nextTick();
    await renderPage();
  },
  { immediate: true }
);

onBeforeUnmount(async () => {
  if (renderTask) {
    await renderTask.cancel();
  }
  if (pdfDocument) {
    await pdfDocument.destroy();
  }
});
</script>

<style scoped>
.pdf-preview-panel {
  position: relative;
  width: 100%;
  min-height: 72vh;
  background: #f5f7fa;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: auto;
}

.pdf-preview-panel__canvas {
  display: block;
  width: 100%;
  height: auto;
}

.pdf-preview-panel__overlay {
  position: absolute;
  left: 0;
  top: 0;
  pointer-events: none;
}

.pdf-preview-panel__region {
  position: absolute;
  border: 2px solid #f59e0b;
  background: rgba(245, 158, 11, 0.18);
  box-sizing: border-box;
}

.pdf-preview-panel__word {
  position: absolute;
  border: 1px solid rgba(59, 130, 246, 0.8);
  background: rgba(59, 130, 246, 0.12);
  box-sizing: border-box;
}

.pdf-preview-panel__loading,
.pdf-preview-panel__error {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.pdf-preview-panel__error {
  color: var(--el-color-danger);
}
</style>
