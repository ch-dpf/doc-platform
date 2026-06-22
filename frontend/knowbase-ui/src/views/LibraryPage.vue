<template>
  <div class="page-wrap page-wrap--fluid page-stack">
    <el-alert v-if="message" :title="message" :type="messageType === 'success' ? 'success' : 'error'" show-icon closable @close="message = ''" />

    <div class="stat-grid">
      <article class="stat-card stat-card--primary">
        <span class="stat-card__label">知识库</span>
        <span class="stat-card__value">{{ formatNumber(total) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">库类型预设</span>
        <span class="stat-card__value">{{ formatNumber(libraryTypePresets.length) }}</span>
      </article>
      <article class="stat-card">
        <span class="stat-card__label">标签覆盖数</span>
        <span class="stat-card__value">{{ formatNumber(uniqueTagCount) }}</span>
      </article>
    </div>

    <PageCard title="知识库管理" subtitle="创建知识库、选择库类型预设，管理 Profile 与标签。">
      <template #actions>
        <span v-if="total" class="stat-chip">共 <strong>{{ total }}</strong> 个</span>
        <el-button round @click="refresh">刷新</el-button>
        <el-button type="primary" round :loading="loading" @click="createSample">创建样例</el-button>
      </template>

      <div class="filter-panel">
        <el-form :inline="true" class="filter-form" @submit.prevent="search">
          <el-form-item label="租户 ID">
            <el-input v-model="form.tenantId" style="width: 140px" clearable @change="search" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" round @click="search">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table v-if="libraries.length" :data="libraries" class="library-table data-table" stripe>
        <el-table-column prop="name" label="名称" min-width="160">
          <template #default="{ row }">
            <span class="name-link">{{ row.name }}</span>
            <div class="row-meta">{{ row.description || '无描述' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="libraryTypePresetCode" label="预设" width="160" />
        <el-table-column prop="tenantId" label="租户" width="100" />
        <el-table-column label="标签" min-width="140">
          <template #default="{ row }">
            <el-tag v-for="tag in row.tags || []" :key="tag" size="small" effect="plain" style="margin-right: 4px">{{ tag }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ACTIVE' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="danger" @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else-if="!loading" description="还没有知识库，先创建一个样例库试跑整条链路。" />
      <div v-if="total > 0" class="table-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="refresh"
          @size-change="handlePageSizeChange"
        />
      </div>
    </PageCard>

    <div class="grid cols-2">
      <PageCard title="创建知识库">
        <el-form label-position="top" @submit.prevent="submit">
          <el-form-item label="知识库名称" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="库类型预设" required>
            <el-select v-model="form.libraryTypePresetCode" class="full-width">
              <el-option
                v-for="preset in libraryTypePresets"
                :key="preset.code"
                :label="`${preset.name}（${preset.code}）`"
                :value="preset.code"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="tagsText" placeholder="用逗号分隔" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="3" />
          </el-form-item>
          <p v-if="selectedPreset" class="helper-text">
            {{ selectedPreset.description }}。默认分块 {{ selectedPreset.config?.chunkMaxTokens ?? '--' }} token，
            重叠 {{ selectedPreset.config?.chunkOverlapTokens ?? '--' }} token。
          </p>
          <el-form-item>
            <el-button type="primary" round :loading="loading" native-type="submit">提交知识库</el-button>
          </el-form-item>
        </el-form>
      </PageCard>

      <PageCard title="库类型预设矩阵" subtitle="快速比对不同预设的分块与检索参数。">
        <el-table :data="libraryTypePresets" class="data-table" size="small">
          <el-table-column prop="name" label="预设" min-width="120">
            <template #default="{ row }">
              {{ row.name }}
              <div class="row-meta">{{ row.code }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
          <el-table-column label="chunk_max" width="90">
            <template #default="{ row }">{{ row.config?.chunkMaxTokens ?? '--' }}</template>
          </el-table-column>
          <el-table-column label="overlap" width="80">
            <template #default="{ row }">{{ row.config?.chunkOverlapTokens ?? '--' }}</template>
          </el-table-column>
        </el-table>
      </PageCard>
    </div>

    <LibraryDetailDrawer
      :visible="detailVisible"
      :library="selectedLibrary"
      @close="detailVisible = false"
      @message="showMessage"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessageBox } from 'element-plus';
import PageCard from '../components/PageCard.vue';
import LibraryDetailDrawer from '../components/LibraryDetailDrawer.vue';
import { createLibrary, deleteLibrary, pageLibraries, listLibraryTypePresets } from '../api';
import { formatDateTime, formatNumber } from '../format';

const libraries = ref([]);
const total = ref(0);
const pagination = ref({ page: 1, size: 10 });
const detailVisible = ref(false);
const selectedLibrary = ref(null);
const libraryTypePresets = ref([
  { code: 'technical_docs', name: '技术文档库', description: '接口文档、部署文档、研发规范与配置说明', config: { chunkMaxTokens: 640, chunkOverlapTokens: 96 } },
  { code: 'general_docs', name: '通用文档库', description: '制度、说明、普通文本资料', config: { chunkMaxTokens: 512, chunkOverlapTokens: 64 } }
]);
const loading = ref(false);
const message = ref('');
const messageType = ref('success');
const tagsText = ref('rag,docs');
const form = ref({
  tenantId: 'default',
  name: '研发知识中心',
  description: '用于技术文档、排障知识与研发规范的统一问答',
  libraryTypePresetCode: 'technical_docs'
});

const selectedPreset = computed(() => libraryTypePresets.value.find(item => item.code === form.value.libraryTypePresetCode));
const uniqueTagCount = computed(() => new Set(libraries.value.flatMap(item => item.tags || [])).size);

async function loadPresets() {
  try {
    const presets = await listLibraryTypePresets();
    if (Array.isArray(presets) && presets.length > 0) {
      libraryTypePresets.value = presets;
    }
    if (!libraryTypePresets.value.some(item => item.code === form.value.libraryTypePresetCode)) {
      form.value.libraryTypePresetCode = libraryTypePresets.value[0]?.code || 'general_docs';
    }
  } catch (error) {
    showMessage(`库类型预设加载失败：${error.message}`, 'error');
  }
}

async function refresh() {
  loading.value = true;
  try {
    const data = await pageLibraries({
      tenantId: form.value.tenantId,
      page: pagination.value.page,
      size: pagination.value.size
    });
    libraries.value = data.items ?? [];
    total.value = data.total ?? 0;
    pagination.value.page = data.page ?? pagination.value.page;
    pagination.value.size = data.size ?? pagination.value.size;
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

function handlePageSizeChange() {
  pagination.value.page = 1;
  refresh();
}

function search() {
  pagination.value.page = 1;
  refresh();
}

async function confirmDelete(library) {
  try {
    await ElMessageBox.confirm(
      `确定删除知识库「${library.name}」吗？此操作不可恢复，关联的索引与文档数据将一并删除。`,
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
  loading.value = true;
  try {
    await deleteLibrary(library.libraryId);
    showMessage('知识库已删除', 'success');
    if (detailVisible.value && selectedLibrary.value?.libraryId === library.libraryId) {
      detailVisible.value = false;
      selectedLibrary.value = null;
    }
    if (libraries.value.length === 1 && pagination.value.page > 1) {
      pagination.value.page -= 1;
    }
    await refresh();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function submit() {
  loading.value = true;
  try {
    await createLibrary({
      ...form.value,
      tags: tagsText.value.split(',').map(item => item.trim()).filter(Boolean)
    });
    showMessage('知识库创建成功', 'success');
    await refresh();
  } catch (error) {
    showMessage(error.message, 'error');
  } finally {
    loading.value = false;
  }
}

async function createSample() {
  form.value.name = `样例知识库-${Date.now().toString().slice(-4)}`;
  await submit();
}

function showMessage(text, type) {
  message.value = text || '操作失败';
  messageType.value = type;
}

function openDetail(library) {
  selectedLibrary.value = library;
  detailVisible.value = true;
}

onMounted(async () => {
  await loadPresets();
  await refresh();
});
</script>

<style scoped>
.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
