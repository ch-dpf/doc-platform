<template>
  <div class="page-wrap page-wrap--fluid page-stack">
    <el-alert v-if="message" :title="message" :type="messageType === 'success' ? 'success' : 'error'" show-icon closable @close="message = ''" />

    <PageCard title="入库 Pipeline" subtitle="从来源加载到索引发布的数据路径。">
      <el-steps :active="4" align-center class="pipeline-steps">
        <el-step title="LoadSource" description="MinIO / inline / 文件 / 目录" />
        <el-step title="ParseDocument" description="OCR / 表格深度 / Tika 解析" />
        <el-step title="TokenWindow" description="token 切块与重叠控制" />
        <el-step title="PublishIndex" description="写入并发布索引版本" />
      </el-steps>
    </PageCard>

    <div class="grid cols-2">
      <PageCard title="文件上传（MinIO）">
        <el-form label-position="top">
          <el-form-item label="存储桶">
            <el-input v-model="uploadBucket" placeholder="默认 knowbase" />
          </el-form-item>
          <el-form-item label="选择文件">
            <input ref="fileInputRef" type="file" @change="onFileSelected" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round :loading="uploading" :disabled="!selectedFile" @click="submitUpload">
              上传到对象存储
            </el-button>
          </el-form-item>
          <p v-if="uploadResult" class="helper-text">
            已上传：{{ uploadResult.uri }}（{{ formatNumber(uploadResult.size) }} bytes）
          </p>
        </el-form>
      </PageCard>

      <PageCard title="新建入库任务">
        <template #actions>
          <el-button round @click="fillSampleDirectory">填充样例目录</el-button>
        </template>

        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="知识库" required>
            <el-select v-model="form.libraryId" filterable class="full-width" placeholder="选择知识库">
              <el-option
                v-for="library in libraries"
                :key="library.libraryId"
                :label="`${library.name} (${shortId(library.libraryId, 8)})`"
                :value="library.libraryId"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="文档 Profile">
            <el-input v-model="form.documentProfileCode" placeholder="留空时按文件类型自动选择" />
          </el-form-item>
          <el-form-item label="源 URI" required>
            <el-input v-model="sourceUrisText" type="textarea" :rows="4" />
          </el-form-item>
          <div class="grid cols-2 compact-grid">
            <el-form-item label="源类型">
              <el-input v-model="form.sourceType" />
            </el-form-item>
            <el-form-item label="最大文件数">
              <el-input-number v-model="options.maxFiles" :min="1" class="full-width" />
            </el-form-item>
          </div>
          <el-form-item label="扩展名白名单">
            <el-input v-model="extensionsText" placeholder="md,pdf,docx,xlsx" />
          </el-form-item>
          <p class="helper-text">
            当前已解析 {{ sourceCount }} 个来源。上传成功后会自动追加 minio:// URI。
          </p>
          <el-form-item>
            <el-button type="primary" round :loading="loading || polling" native-type="submit">
              {{ loading ? '创建中...' : polling ? '执行中...' : '创建任务' }}
            </el-button>
          </el-form-item>
        </el-form>
      </PageCard>
    </div>

    <div class="grid cols-2">
      <PageCard title="运行状态">
        <template #actions>
          <el-tag v-if="latestRun" :type="statusTagType(latestRun.status)">{{ latestRun.status }}</el-tag>
        </template>

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
          </div>
          <div class="bar"><span :style="{ width: `${progressPercent}%` }" /></div>
          <p class="helper-text">
            {{ latestRun.message || (polling ? '任务正在异步执行，控制台会自动刷新状态。' : '任务已提交。') }}
          </p>
          <p class="row-meta">Run {{ shortId(latestRun.runId, 12) }} · {{ formatDateTime(latestRun.updatedAt) }}</p>
        </template>
        <el-empty v-else description="创建入库任务后，这里会显示状态与进度。" />
      </PageCard>

      <PageCard title="文档级错误">
        <template #actions>
          <el-button v-if="latestRun" size="small" round :loading="loadingErrors" @click="loadErrors">刷新</el-button>
        </template>
        <el-table v-if="ingestionErrors.length" :data="ingestionErrors" size="small" class="data-table">
          <el-table-column prop="sourceUri" label="来源" min-width="160" show-overflow-tooltip />
          <el-table-column prop="errorMessage" label="错误" min-width="180" show-overflow-tooltip />
        </el-table>
        <el-empty v-else description="无失败文档或未加载错误列表。" />
      </PageCard>
    </div>

    <PageCard title="原始响应" subtitle="联调时快速核对接口字段。">
      <pre class="pre-block">{{ resultText }}</pre>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import PageCard from '../components/PageCard.vue';
import {
  createIngestionRun,
  getIngestionRun,
  listIngestionErrors,
  listLibraries,
  uploadFile
} from '../api';
import { requestContext } from '../context';
import { formatDateTime, formatNumber, shortId } from '../format';

const sourceUrisText = ref('inline:text:KnowBase supports structured ingestion.');
const extensionsText = ref('md,pdf,docx,xlsx');
const form = ref({
  libraryId: '',
  documentProfileCode: 'default_markdown',
  sourceType: 'inline'
});
const options = ref({ recursive: true, maxFiles: 12 });
const libraries = ref([]);
const latestRun = ref(null);
const ingestionErrors = ref([]);
const loading = ref(false);
const polling = ref(false);
const loadingErrors = ref(false);
const uploading = ref(false);
const uploadBucket = ref('knowbase');
const selectedFile = ref(null);
const uploadResult = ref(null);
const fileInputRef = ref(null);
const message = ref('');
const messageType = ref('success');

const sourceCount = computed(() => sourceUrisText.value.split('\n').map(item => item.trim()).filter(Boolean).length);
const normalizedExtensions = computed(() => extensionsText.value
  .split(',')
  .map(item => item.trim().replace(/^\./, '').toLowerCase())
  .filter(Boolean));
const progressPercent = computed(() => {
  if (!latestRun.value) return 0;
  const total = Math.max(1, latestRun.value.inputDocuments || 0);
  return Math.min(100, Math.round(((latestRun.value.succeededDocuments || 0) / total) * 100));
});
const resultText = computed(() => (latestRun.value ? JSON.stringify(latestRun.value, null, 2) : '等待提交'));

function statusTagType(status) {
  const value = String(status || '').toUpperCase();
  if (value === 'SUCCEEDED') return 'success';
  if (value === 'FAILED') return 'danger';
  if (value === 'PARTIAL_FAILED') return 'warning';
  return 'info';
}

function onFileSelected(event) {
  selectedFile.value = event.target.files?.[0] || null;
}

async function submitUpload() {
  if (!selectedFile.value) return;
  uploading.value = true;
  try {
    uploadResult.value = await uploadFile(selectedFile.value, uploadBucket.value || undefined);
    const current = sourceUrisText.value.trim();
    sourceUrisText.value = current ? `${current}\n${uploadResult.value.uri}` : uploadResult.value.uri;
    form.value.sourceType = 'minio';
    showMessage('文件上传成功，URI 已追加到源列表', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    uploading.value = false;
  }
}

async function loadLibraries() {
  try {
    libraries.value = await listLibraries({ tenantId: requestContext.tenantId });
    if (!form.value.libraryId && libraries.value.length > 0) {
      form.value.libraryId = libraries.value[0].libraryId;
    }
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function submit() {
  if (!form.value.libraryId) {
    showMessage('请选择知识库', 'error');
    return;
  }
  loading.value = true;
  try {
    const sourceUris = sourceUrisText.value.split('\n').map(item => item.trim()).filter(Boolean);
    const data = await createIngestionRun(form.value.libraryId, {
      ...form.value,
      documentProfileCode: form.value.documentProfileCode || null,
      sourceUris,
      publishIndexOnSuccess: true,
      options: { ...options.value, extensions: normalizedExtensions.value }
    });
    latestRun.value = data;
    ingestionErrors.value = [];
    if (!isTerminal(data.status)) {
      showMessage('入库任务已提交，正在后台执行', 'success');
      await pollIngestionRun(data.runId);
      return;
    }
    await loadErrors();
    showMessage('入库任务创建成功', 'success');
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function loadErrors() {
  if (!latestRun.value?.runId) return;
  loadingErrors.value = true;
  try {
    ingestionErrors.value = await listIngestionErrors(latestRun.value.runId);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loadingErrors.value = false;
  }
}

function showMessage(text, type) {
  message.value = text || '操作失败';
  messageType.value = type;
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
        showMessage(`入库任务结束：${data.status}`, data.status === 'FAILED' ? 'error' : 'success');
        return;
      }
    }
    showMessage('入库任务仍在执行，请稍后刷新查看结果', 'success');
  } finally {
    polling.value = false;
  }
}

function isTerminal(status) {
  return ['SUCCEEDED', 'PARTIAL_FAILED', 'FAILED', 'CANCELLED'].includes(String(status || '').toUpperCase());
}

function sleep(ms) {
  return new Promise(resolve => window.setTimeout(resolve, ms));
}

function fillSampleDirectory() {
  sourceUrisText.value = 'file://D:/document';
  form.value.documentProfileCode = '';
  form.value.sourceType = 'local_directory';
  extensionsText.value = 'md,pdf,docx,xlsx';
  options.value.maxFiles = 12;
  showMessage('已填充后端本地样例目录，提交前请确认后端进程可访问 D:\\document。', 'success');
}

onMounted(loadLibraries);
</script>

<style scoped>
.pipeline-steps {
  margin-top: 8px;
}
</style>
