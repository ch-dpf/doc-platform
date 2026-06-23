<template>
  <PageCard title="文档列表" subtitle="管理已入库文档，支持上传、查看详情与删除。">
    <template #actions>
      <el-button type="primary" round @click="uploadDialogVisible = true">上传文档</el-button>
      <el-button round :loading="loading" @click="loadDocuments">刷新</el-button>
    </template>

    <el-table
      v-if="documents.length"
      :data="documents"
      size="small"
      class="data-table doc-table"
      stripe
      @row-click="handleRowClick"
    >
      <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip>
        <template #default="{ row }">
          <el-button link type="primary" class="doc-title-link" @click.stop="openDetail(row)">
            {{ row.title || shortId(row.documentId, 12) }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'INDEXED' ? 'success' : row.status === 'FAILED' ? 'danger' : 'info'">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="块数" width="72">
        <template #default="{ row }">{{ row.chunkCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="最近索引" width="170">
        <template #default="{ row }">{{ row.lastIndexedAt ? formatDateTime(row.lastIndexedAt) : '—' }}</template>
      </el-table-column>
      <el-table-column label="来源" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.sourceUri || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right" class-name="doc-actions">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
          <el-button link type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else-if="!loading" description="暂无文档，点击「上传文档」开始入库。" />
  </PageCard>

  <DocumentIngestDialog
    :visible="uploadDialogVisible"
    :library-id="libraryId"
    :library-name="library?.name || ''"
    @close="uploadDialogVisible = false"
    @completed="handleUploadCompleted"
    @message="(text, type) => showMessage(text, type)"
  />
</template>

<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageCard from '../../components/PageCard.vue';
import DocumentIngestDialog from '../../components/DocumentIngestDialog.vue';
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import {
  deleteDocument,
  listDocuments
} from '../../api';
import { formatDateTime, shortId } from '../../format';

const route = useRoute();
const router = useRouter();
const { library, libraryId, reloadIndexHealth, showMessage } = useLibraryWorkspace();

const documents = ref([]);
const loading = ref(false);
const uploadDialogVisible = ref(false);

async function loadDocuments() {
  loading.value = true;
  try {
    documents.value = await listDocuments(libraryId.value);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

function openDetail(document) {
  router.push({
    name: 'library-document-detail',
    params: { libraryId: libraryId.value, documentId: document.documentId }
  });
}

function handleRowClick(row, _column, event) {
  if (event?.target?.closest('.doc-actions') || event?.target?.closest('.doc-title-link')) {
    return;
  }
  openDetail(row);
}

async function handleDelete(document) {
  try {
    await deleteDocument(libraryId.value, document.documentId);
    showMessage('文档已删除', 'success');
    await loadDocuments();
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function handleUploadCompleted() {
  showMessage('文档入库完成', 'success');
  await Promise.all([loadDocuments(), reloadIndexHealth()]);
}

watch(
  () => route.query.upload,
  (upload) => {
    if (upload === '1') {
      uploadDialogVisible.value = true;
    }
  },
  { immediate: true }
);

onMounted(loadDocuments);
</script>

<style scoped>
:deep(.doc-table .el-table__row) {
  cursor: pointer;
}
</style>
