<template>
  <PageCard title="文档列表" subtitle="管理已入库文档，支持上传、查看详情、分页浏览与批量删除。">
    <template #actions>
      <el-button
        v-if="selectedRows.length"
        type="danger"
        round
        :loading="batchDeleting"
        @click="handleBatchDelete"
      >
        批量删除（{{ selectedRows.length }}）
      </el-button>
      <el-button type="primary" round @click="uploadDialogVisible = true">上传文档</el-button>
      <el-button round :loading="loading" @click="loadDocuments">刷新</el-button>
    </template>

    <el-table
      v-if="documents.length || total > 0"
      ref="tableRef"
      :data="documents"
      size="small"
      class="data-table doc-table"
      stripe
      row-key="documentId"
      @row-click="handleRowClick"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" reserve-selection />
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

    <div v-if="total > 0" class="table-pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        background
        @current-change="loadDocuments"
        @size-change="handlePageSizeChange"
      />
    </div>
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
import { onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import PageCard from '../../components/PageCard.vue';
import DocumentIngestDialog from '../../components/DocumentIngestDialog.vue';
import { useLibraryWorkspace } from '../../composables/libraryWorkspace';
import {
  batchDeleteDocuments,
  deleteDocument,
  pageDocuments
} from '../../api';
import { formatDateTime, shortId } from '../../format';

const route = useRoute();
const router = useRouter();
const { library, libraryId, reloadIndexHealth, showMessage } = useLibraryWorkspace();

const documents = ref([]);
const total = ref(0);
const loading = ref(false);
const batchDeleting = ref(false);
const uploadDialogVisible = ref(false);
const selectedRows = ref([]);
const tableRef = ref(null);
const pagination = reactive({ page: 1, size: 20 });

async function loadDocuments() {
  loading.value = true;
  try {
    const data = await pageDocuments(libraryId.value, {
      page: pagination.page,
      size: pagination.size
    });
    documents.value = data.items ?? [];
    total.value = data.total ?? 0;
    pagination.page = data.page ?? pagination.page;
    pagination.size = data.size ?? pagination.size;
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

function handlePageSizeChange() {
  pagination.page = 1;
  loadDocuments();
}

function handleSelectionChange(rows) {
  selectedRows.value = rows;
}

function openDetail(document) {
  router.push({
    name: 'library-document-detail',
    params: { libraryId: libraryId.value, documentId: document.documentId }
  });
}

function handleRowClick(row, _column, event) {
  if (event?.target?.closest('.doc-actions') || event?.target?.closest('.doc-title-link') || event?.target?.closest('.el-checkbox')) {
    return;
  }
  openDetail(row);
}

async function handleDelete(document) {
  try {
    await ElMessageBox.confirm(
      `确定删除文档「${document.title || shortId(document.documentId, 12)}」吗？此操作不可恢复。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    );
  } catch {
    return;
  }
  try {
    await deleteDocument(libraryId.value, document.documentId);
    showMessage('文档已删除', 'success');
    if (documents.value.length === 1 && pagination.page > 1) {
      pagination.page -= 1;
    }
    tableRef.value?.clearSelection();
    selectedRows.value = [];
    await Promise.all([loadDocuments(), reloadIndexHealth()]);
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function handleBatchDelete() {
  if (!selectedRows.value.length) {
    return;
  }
  const count = selectedRows.value.length;
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${count} 篇文档吗？此操作不可恢复，关联的分块与向量将一并删除。`,
      '批量删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    );
  } catch {
    return;
  }
  batchDeleting.value = true;
  try {
    const documentIds = selectedRows.value.map(row => row.documentId);
    const result = await batchDeleteDocuments(libraryId.value, documentIds);
    showMessage(`已删除 ${result.deletedCount ?? count} 篇文档`, 'success');
    tableRef.value?.clearSelection();
    selectedRows.value = [];
    if (documents.value.length <= count && pagination.page > 1) {
      pagination.page -= 1;
    }
    await Promise.all([loadDocuments(), reloadIndexHealth()]);
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    batchDeleting.value = false;
  }
}

async function handleUploadCompleted() {
  showMessage('文档入库完成', 'success');
  pagination.page = 1;
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

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
