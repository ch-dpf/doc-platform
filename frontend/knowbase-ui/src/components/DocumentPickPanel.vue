<template>
  <div class="document-pick-panel">
    <header class="pick-head">
      <span class="pick-head__title">选择材料</span>
      <div v-if="fileList.length" class="pick-head__stats pick-stats">
        <span class="pick-stat pick-stat--primary">{{ validFiles.length }} 有效</span>
        <span v-if="invalidFiles.length" class="pick-stat pick-stat--muted">{{ invalidFiles.length }} 忽略</span>
        <span v-if="oversizedFiles.length" class="pick-stat pick-stat--warn">{{ oversizedFiles.length }} 超限</span>
        <span v-if="validFiles.length" class="pick-stat">{{ formatFileSize(totalValidSize) }}</span>
      </div>
      <el-button v-if="fileList.length" link type="danger" size="small" @click="clearAllFiles">
        清空
      </el-button>
    </header>

    <div class="pick-body">
      <div class="pick-panel" :class="{ 'pick-panel--has-files': fileList.length }">
        <el-tabs v-model="pickMode" class="pick-tabs">
          <el-tab-pane label="文件" name="file" />
          <el-tab-pane label="文件夹" name="folder" />
        </el-tabs>

        <div v-show="pickMode === 'file'" class="pick-dropzone-wrap">
          <el-upload
            class="pick-upload"
            drag
            multiple
            :show-file-list="false"
            :auto-upload="false"
            :limit="maxFiles"
            :file-list="fileList"
            :disabled="reachesMaxFileCount"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onExceed"
          >
            <div class="pick-dropzone">
              <div class="pick-dropzone__icon pick-dropzone__icon--file">
                <el-icon><UploadFilled /></el-icon>
              </div>
              <p class="pick-dropzone__title">拖拽文件到此处，或点击选择</p>
              <p class="pick-dropzone__desc">支持多选，单次最多 {{ maxFiles }} 个</p>
            </div>
          </el-upload>
        </div>

        <div
          v-show="pickMode === 'folder'"
          class="pick-dropzone-wrap pick-dropzone-wrap--folder"
          :class="{ 'pick-dropzone-wrap--disabled': reachesMaxFileCount }"
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
          <div class="pick-dropzone">
            <div class="pick-dropzone__icon pick-dropzone__icon--folder">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <p class="pick-dropzone__title">点击选择文件夹</p>
            <p class="pick-dropzone__desc">将导入文件夹内所有符合类型的文件</p>
          </div>
        </div>

        <div class="pick-meta">
          <span>{{ supportedTypesLabelText }}</span>
          <span> · ≤ {{ maxFileSizeDisplay }}</span>
          <span> · {{ maxFiles }} 个/次</span>
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

      <div v-if="validFiles.length" class="pick-file-list">
        <div
          v-for="row in validFiles"
          :key="row.uid"
          class="pick-file-card"
        >
          <div class="pick-file-card__icon">
            <el-icon><Document /></el-icon>
          </div>
          <div class="pick-file-card__meta">
            <span class="pick-file-card__name" :title="row.name">{{ displayFileName(row.name) }}</span>
            <span class="pick-file-card__size">{{ formatFileSize(row.raw?.size) }}</span>
          </div>
          <el-button
            link
            type="info"
            :icon="Close"
            class="pick-file-card__remove"
            title="移除此文件"
            @click.stop="removeFile(row.uid)"
          />
        </div>
      </div>

      <div v-else-if="invalidFiles.length || oversizedFiles.length" class="pick-list-empty pick-list-empty--warn">
        所选文件均不符合要求（类型不支持或超过大小限制）
      </div>

      <div v-if="invalidFiles.length || oversizedFiles.length" class="pick-skipped">
        <span class="pick-skipped__label">
          已忽略 {{ invalidFiles.length + oversizedFiles.length }} 个
        </span>
        <div class="pick-skipped__tags">
          <el-tag
            v-for="f in [...invalidFiles, ...oversizedFiles].slice(0, 4)"
            :key="f.uid"
            size="small"
            type="info"
            effect="plain"
          >
            {{ displayFileName(f.name) }}
          </el-tag>
          <span v-if="invalidFiles.length + oversizedFiles.length > 4" class="muted">
            +{{ invalidFiles.length + oversizedFiles.length - 4 }}
          </span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { ElMessageBox } from 'element-plus';
import { Close, Document, FolderOpened, UploadFilled } from '@element-plus/icons-vue';
import { formatFileSize } from '../utils/formatFileSize';
import {
  SYSTEM_SUPPORTED_FILE_TYPES,
  filterFolderFiles,
  matchesSupportedType,
  supportedTypesLabel
} from '../utils/supportedFileTypes';

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  maxFiles: {
    type: Number,
    default: 50
  },
  maxFileSize: {
    type: Number,
    default: 100 * 1024 * 1024
  },
  supportedTypes: {
    type: Array,
    default: () => SYSTEM_SUPPORTED_FILE_TYPES
  }
});

const emit = defineEmits(['update:modelValue']);

const pickMode = ref('file');
const folderInputRef = ref(null);
const folderPickHint = ref('');
const pickNotice = ref(null);
const fileList = ref([]);

const maxFileSizeDisplay = computed(() => `${Math.round(props.maxFileSize / 1024 / 1024)} MB`);
const supportedTypesLabelText = computed(() => supportedTypesLabel(props.supportedTypes));
const reachesMaxFileCount = computed(() => validFiles.value.length >= props.maxFiles);

const validFiles = computed(() =>
  fileList.value.filter((f) => {
    const name = f.name || f.raw?.name || '';
    return matchesSupportedType(name, props.supportedTypes) && !isOversized(f);
  })
);

const invalidFiles = computed(() =>
  fileList.value.filter((f) => {
    const name = f.name || f.raw?.name || '';
    return !matchesSupportedType(name, props.supportedTypes);
  })
);

const oversizedFiles = computed(() =>
  fileList.value.filter((f) => matchesSupportedType(f.name || f.raw?.name || '', props.supportedTypes) && isOversized(f))
);

const totalValidSize = computed(() =>
  validFiles.value.reduce((sum, f) => sum + (f.raw?.size || 0), 0)
);

watch(
  validFiles,
  (files) => {
    emit(
      'update:modelValue',
      files.map((f) => f.raw).filter(Boolean)
    );
  },
  { deep: true }
);

function isOversized(row) {
  const size = row.raw?.size ?? 0;
  return size > props.maxFileSize;
}

function displayFileName(name) {
  if (!name) return '—';
  const i = name.lastIndexOf('/');
  return i >= 0 ? name.slice(i + 1) : name;
}

function toUploadRow(raw) {
  const name = raw.webkitRelativePath || raw.name;
  return {
    name,
    raw,
    uid: `${name}-${raw.size}-${raw.lastModified}`
  };
}

function mergeFileList(rawFiles, replace = false) {
  const incoming = rawFiles.map(toUploadRow);
  const existingUids = new Set(fileList.value.map((f) => f.uid));
  const merged = replace
    ? incoming
    : [...fileList.value, ...incoming.filter((f) => !existingUids.has(f.uid))];

  if (merged.length > props.maxFiles) {
    fileList.value = merged.slice(0, props.maxFiles);
    pickNotice.value = {
      type: 'warning',
      title: '超出单次文件数量上限',
      description: `单次最多 ${props.maxFiles} 个文件，已保留前 ${props.maxFiles} 个。`
    };
    return;
  }

  fileList.value = merged;
}

function pickFolder() {
  if (reachesMaxFileCount.value) return;
  folderInputRef.value?.click();
}

function onFolderSelected(event) {
  const input = event.target;
  const files = Array.from(input.files || []);
  input.value = '';
  if (!files.length) return;

  const { accepted, skipped } = filterFolderFiles(files, props.supportedTypes);
  if (!accepted.length) {
    pickNotice.value = {
      type: 'warning',
      title: '未找到可接入的文件',
      description: `文件夹内没有符合支持的文件类型（${supportedTypesLabelText.value}），请检查目录。`
    };
    return;
  }

  const beforeCount = fileList.value.length;
  mergeFileList(accepted);
  const addedCount = fileList.value.length - beforeCount;
  folderPickHint.value = `已导入 ${addedCount || accepted.length} 个${skipped.length ? `，忽略 ${skipped.length} 个` : ''}`;

  const parts = [];
  if (skipped.length) {
    parts.push(`已忽略 ${skipped.length} 个不符合类型的文件`);
  }
  if (accepted.length > addedCount && addedCount >= 0) {
    parts.push(`符合类型的文件共 ${accepted.length} 个，部分因重复或上限未全部加入`);
  }
  if (parts.length) {
    pickNotice.value = {
      type: 'info',
      title: '文件夹导入说明',
      description: `${parts.join('；')}。`
    };
  } else {
    pickNotice.value = {
      type: 'success',
      title: `已导入 ${addedCount || accepted.length} 个文件`,
      description: '可在下方列表查看并移除不需要的文件。'
    };
  }
}

function onFileChange(_uploadFile, uploadFiles) {
  fileList.value = uploadFiles;
  folderPickHint.value = '';
  if (!pickNotice.value?.type || pickNotice.value.type !== 'warning') {
    pickNotice.value = null;
  }
}

function onFileRemove(_file, uploadFiles) {
  fileList.value = uploadFiles;
  folderPickHint.value = '';
  if (!fileList.value.length) {
    pickNotice.value = null;
  }
}

function onExceed() {
  pickNotice.value = {
    type: 'warning',
    title: '超出单次文件数量上限',
    description: `单次最多选择 ${props.maxFiles} 个文件。请分批上传，或使用文件夹页签导入。`
  };
}

function removeFile(uid) {
  fileList.value = fileList.value.filter((f) => f.uid !== uid);
  if (!fileList.value.length) {
    folderPickHint.value = '';
    pickNotice.value = null;
  }
}

async function clearAllFiles() {
  if (!fileList.value.length) return;
  try {
    await ElMessageBox.confirm('将清空已选文件，此操作不可撤销。', '清空已选文件', {
      confirmButtonText: '清空',
      cancelButtonText: '取消',
      type: 'warning'
    });
  } catch {
    return;
  }
  fileList.value = [];
  folderPickHint.value = '';
  pickNotice.value = null;
}

function reset() {
  pickMode.value = 'file';
  fileList.value = [];
  folderPickHint.value = '';
  pickNotice.value = null;
  if (folderInputRef.value) {
    folderInputRef.value.value = '';
  }
}

defineExpose({ reset, clearAllFiles });
</script>

<style scoped>
.document-pick-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.pick-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pick-head__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--dp-text);
}

.pick-head__stats {
  margin-left: auto;
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

.pick-stat--warn {
  color: #b45309;
  background: #fef3c7;
}

.pick-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.pick-panel {
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

.pick-tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 12px;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
}

.pick-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.pick-tabs :deep(.el-tabs__item) {
  height: 36px;
  font-size: 13px;
  color: #64748b;
}

.pick-tabs :deep(.el-tabs__item.is-active) {
  color: var(--dp-primary);
  font-weight: 600;
}

.pick-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--dp-primary);
}

.pick-tabs :deep(.el-tabs__content) {
  display: none;
}

.pick-dropzone-wrap {
  padding: 4px 12px 0;
}

.pick-dropzone-wrap--folder {
  cursor: pointer;
  transition: background 0.15s ease;
}

.pick-dropzone-wrap--folder:hover,
.pick-dropzone-wrap--folder:focus-visible {
  background: #f8fafc;
  outline: none;
}

.pick-dropzone-wrap--folder:hover .pick-dropzone,
.pick-dropzone-wrap--folder:focus-visible .pick-dropzone {
  border-color: #bae6fd;
  background: #f0f9ff;
}

.pick-dropzone-wrap--disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.pick-upload {
  width: 100%;
}

.pick-upload :deep(.el-upload) {
  width: 100%;
}

.pick-upload :deep(.el-upload-dragger) {
  width: 100%;
  min-height: 140px;
  padding: 0;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #fafbfc;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.pick-upload :deep(.el-upload-dragger:hover) {
  border-color: #bae6fd;
  background: #f0f9ff;
}

.pick-upload :deep(.el-upload-dragger.is-dragover) {
  border-color: var(--dp-primary);
  background: #e0f2fe;
}

.pick-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 140px;
  padding: 20px 16px;
  text-align: center;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #fafbfc;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.pick-dropzone__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  font-size: 22px;
}

.pick-dropzone__icon--file {
  background: linear-gradient(145deg, #e0f2fe 0%, #f0f9ff 100%);
  color: var(--dp-primary);
}

.pick-dropzone__icon--folder {
  background: linear-gradient(145deg, #ecfdf5 0%, #f0fdf4 100%);
  color: #059669;
}

.pick-dropzone__title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #334155;
}

.pick-dropzone__desc {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}

.pick-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  padding: 8px 12px;
  font-size: 11px;
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

.pick-notice {
  margin: 0 12px 12px;
}

.pick-notice :deep(.el-alert__title) {
  font-size: 12px;
}

.pick-notice :deep(.el-alert__description) {
  font-size: 11px;
  line-height: 1.45;
}

.pick-file-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 260px;
  overflow-y: auto;
}

.pick-file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fff;
  transition: background 0.12s ease;
}

.pick-file-card:hover {
  background: #f8fafc;
}

.pick-file-card__icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #f1f5f9;
  color: #64748b;
  font-size: 14px;
}

.pick-file-card__meta {
  flex: 1;
  min-width: 0;
}

.pick-file-card__name {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pick-file-card__size {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  font-variant-numeric: tabular-nums;
  color: #94a3b8;
}

.pick-file-card__remove {
  flex-shrink: 0;
  opacity: 0.5;
  transition: opacity 0.12s ease;
}

.pick-file-card:hover .pick-file-card__remove {
  opacity: 1;
}

.pick-list-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
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

.pick-skipped {
  padding: 6px 10px;
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

.muted {
  color: #94a3b8;
  font-size: 11px;
}
</style>
