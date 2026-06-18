<template>
  <el-drawer :model-value="visible" :title="library?.name || '知识库详情'" size="720px" @close="emit('close')">
    <div v-if="library" class="drawer-stack">
      <div class="meta-bar">
        {{ shortId(library.libraryId, 16) }} · {{ library.libraryTypePresetCode }} · {{ library.status }}
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="索引版本" name="index">
          <div class="drawer-actions">
            <el-button size="small" round :loading="loadingIndex" @click="loadIndexVersions">刷新</el-button>
          </div>
          <el-table v-if="indexVersions.length" :data="indexVersions" size="small" class="data-table">
            <el-table-column label="版本" width="70">
              <template #default="{ row }">v{{ row.version }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 'PUBLISHED' ? 'success' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="文档/块" width="100">
              <template #default="{ row }">{{ row.documentCount }} / {{ row.chunkCount }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" @click="selectIndexVersion(row)">看文档</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无索引版本" />
        </el-tab-pane>

        <el-tab-pane label="文档" name="documents">
          <div class="drawer-actions">
            <el-select v-model="selectedIndexVersionId" clearable placeholder="全部索引版本" size="small" style="width: 220px">
              <el-option
                v-for="item in indexVersions"
                :key="item.indexVersionId"
                :label="`v${item.version} · ${item.status}`"
                :value="item.indexVersionId"
              />
            </el-select>
            <el-button size="small" round :loading="loadingDocs" @click="loadDocuments">刷新</el-button>
          </div>
          <el-table v-if="documents.length" :data="documents" size="small" class="data-table">
            <el-table-column prop="title" label="标题" min-width="140" show-overflow-tooltip />
            <el-table-column label="来源" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">{{ row.sourceUri || '--' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" @click="loadChunks(row)">块</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无文档" />

          <div v-if="chunks.length" class="chunk-panel">
            <div class="section-head"><h4>文档块（{{ chunks.length }}）</h4></div>
            <div v-for="chunk in chunks.slice(0, 8)" :key="chunk.chunkId" class="evidence-item">
              <div class="row-title">{{ shortId(chunk.chunkId, 10) }} · {{ chunk.tokenCount }} tokens</div>
              <p class="muted evidence-copy">{{ chunk.content }}</p>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="ACL" name="acl">
          <el-form label-position="top" size="small" @submit.prevent="submitAcl">
            <div class="grid cols-2 compact-grid">
              <el-form-item label="主体类型">
                <el-select v-model="aclForm.principalType" class="full-width">
                  <el-option label="USER" value="USER" />
                  <el-option label="ROLE" value="ROLE" />
                </el-select>
              </el-form-item>
              <el-form-item label="主体 ID">
                <el-input v-model="aclForm.principalId" placeholder="alice / admin" />
              </el-form-item>
            </div>
            <el-form-item label="权限">
              <el-select v-model="aclForm.permission" class="full-width">
                <el-option label="READ" value="READ" />
                <el-option label="WRITE" value="WRITE" />
                <el-option label="ADMIN" value="ADMIN" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" round :loading="granting" native-type="submit">授予 ACL</el-button>
              <el-button size="small" round :loading="loadingAcl" @click="loadAcls">刷新</el-button>
            </el-form-item>
          </el-form>
          <el-table v-if="acls.length" :data="acls" size="small" class="data-table">
            <el-table-column prop="principalType" label="主体" width="80" />
            <el-table-column prop="principalId" label="ID" min-width="100" />
            <el-table-column prop="permission" label="权限" width="90" />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="danger" @click="removeAcl(row.aclId)">撤销</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无 ACL 记录" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, watch } from 'vue';
import {
  grantAcl,
  listAcls,
  listDocumentChunks,
  listDocuments,
  listIndexVersions,
  revokeAcl
} from '../api';
import { requestContext } from '../context';
import { shortId } from '../format';

const props = defineProps({
  visible: { type: Boolean, default: false },
  library: { type: Object, default: null }
});

const emit = defineEmits(['close', 'message']);

const activeTab = ref('index');
const indexVersions = ref([]);
const documents = ref([]);
const chunks = ref([]);
const acls = ref([]);
const selectedIndexVersionId = ref('');
const loadingIndex = ref(false);
const loadingDocs = ref(false);
const loadingAcl = ref(false);
const granting = ref(false);
const aclForm = ref({
  principalType: 'USER',
  principalId: '',
  permission: 'READ'
});

watch(
  () => [props.visible, props.library?.libraryId],
  async ([visible, libraryId]) => {
    if (!visible || !libraryId) {
      return;
    }
    activeTab.value = 'index';
    selectedIndexVersionId.value = '';
    chunks.value = [];
    await Promise.all([loadIndexVersions(), loadAcls()]);
  }
);

watch(selectedIndexVersionId, () => {
  if (props.visible) {
    loadDocuments();
  }
});

async function loadIndexVersions() {
  if (!props.library) return;
  loadingIndex.value = true;
  try {
    indexVersions.value = await listIndexVersions(props.library.libraryId);
  } catch (error) {
    emit('message', error.message, 'error');
  } finally {
    loadingIndex.value = false;
  }
}

async function loadDocuments() {
  if (!props.library) return;
  loadingDocs.value = true;
  try {
    documents.value = await listDocuments(props.library.libraryId, {
      indexVersionId: selectedIndexVersionId.value || undefined
    });
    chunks.value = [];
  } catch (error) {
    emit('message', error.message, 'error');
  } finally {
    loadingDocs.value = false;
  }
}

async function loadChunks(document) {
  if (!props.library) return;
  try {
    chunks.value = await listDocumentChunks(props.library.libraryId, document.documentId);
  } catch (error) {
    emit('message', error.message, 'error');
  }
}

function selectIndexVersion(row) {
  selectedIndexVersionId.value = row.indexVersionId;
  activeTab.value = 'documents';
  loadDocuments();
}

async function loadAcls() {
  if (!props.library) return;
  loadingAcl.value = true;
  try {
    acls.value = await listAcls({
      tenantId: requestContext.tenantId,
      resourceType: 'LIBRARY',
      resourceId: props.library.libraryId
    });
  } catch (error) {
    emit('message', error.message, 'error');
  } finally {
    loadingAcl.value = false;
  }
}

async function submitAcl() {
  if (!props.library || !aclForm.value.principalId) {
    emit('message', '请填写主体 ID', 'error');
    return;
  }
  granting.value = true;
  try {
    await grantAcl({
      tenantId: requestContext.tenantId,
      resourceType: 'LIBRARY',
      resourceId: props.library.libraryId,
      principalType: aclForm.value.principalType,
      principalId: aclForm.value.principalId,
      permission: aclForm.value.permission
    });
    emit('message', 'ACL 已授予', 'success');
    aclForm.value.principalId = '';
    await loadAcls();
  } catch (error) {
    emit('message', error.message, 'error');
  } finally {
    granting.value = false;
  }
}

async function removeAcl(aclId) {
  try {
    await revokeAcl(aclId);
    emit('message', 'ACL 已撤销', 'success');
    await loadAcls();
  } catch (error) {
    emit('message', error.message, 'error');
  }
}
</script>

<style scoped>
.drawer-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.drawer-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
  align-items: center;
}

.chunk-panel {
  margin-top: 16px;
}

.section-head h4 {
  margin: 0 0 8px;
  font-size: 14px;
}
</style>
