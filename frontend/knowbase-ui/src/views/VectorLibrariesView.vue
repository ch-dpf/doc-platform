<template>

  <div class="page-wrap page-wrap--fluid library-list-page library-page-fill">

    <PageCard title="知识库管理">

      <template #actions>

        <span v-if="total > 0" class="stat-chip">共 <strong>{{ total }}</strong> 个</span>
        <span v-if="pendingMigrationLibraries > 0" class="stat-chip stat-chip--warn">
          <strong>{{ pendingMigrationLibraries }}</strong> 个待迁移
        </span>

        <el-button type="primary" round @click="wizardVisible = true">创建知识库</el-button>

      </template>



      <div class="library-page-content">

      <div class="filter-panel">

        <el-form :inline="true" class="filter-form" @submit.prevent="load(1)">

          <el-form-item label="租户 ID">

            <el-input v-model="tenantId" style="width: 120px" clearable @change="onTenantChange" />

          </el-form-item>

          <el-form-item label="关键字">

            <el-input

              v-model="keyword"

              placeholder="名称 / 描述"

              clearable

              style="width: 180px"

              @clear="load(1)"

            />

          </el-form-item>

          <el-form-item label="标签">

            <el-select

              v-model="tagFilter"

              filterable

              clearable

              allow-create

              default-first-option

              placeholder="按标签筛选"

              style="width: 160px"

              @change="load(1)"

              @clear="load(1)"

            >

              <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />

            </el-select>

          </el-form-item>

          <el-form-item>

            <el-button type="primary" round :loading="loading" @click="load(1)">查询</el-button>

          </el-form-item>

          <el-form-item class="filter-form__view">

            <el-radio-group v-model="viewMode" size="small" @change="onViewModeChange">

              <el-radio-button value="list">

                <el-icon><List /></el-icon>

                <span class="view-label">列表</span>

              </el-radio-button>

              <el-radio-button value="card">

                <el-icon><Grid /></el-icon>

                <span class="view-label">卡片</span>

              </el-radio-button>

            </el-radio-group>

          </el-form-item>

        </el-form>

        <div v-if="tagOptions.length" class="tag-index">

          <span class="tag-index__label">标签：</span>

          <el-check-tag

            v-for="t in tagOptions"

            :key="t"

            :checked="tagFilter === t"

            class="tag-index__item"

            @change="(checked) => onTagIndexToggle(t, checked)"

          >

            {{ t }}

          </el-check-tag>

        </div>

      </div>



      <div class="library-list-body">

        <el-table

          v-if="viewMode === 'list'"

          v-loading="loading"

          :data="items"

          height="100%"

          stripe

          class="data-table library-table library-table--fixed"

          highlight-current-row

          empty-text="暂无知识库，点击右上角创建"

          table-layout="auto"

          @row-click="drillDown"

        >

        <el-table-column prop="name" label="名称" min-width="180">

          <template #default="{ row }">

            <span class="name-link">{{ row.name }}</span>
            <el-tag
              v-if="row.pendingMigrationCount > 0"
              size="small"
              type="warning"
              effect="plain"
              class="migration-badge"
              @click.stop="openEdit(row, 'pipeline')"
            >
              待迁移 {{ row.pendingMigrationCount }}
            </el-tag>

          </template>

        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="120" show-overflow-tooltip />

        <el-table-column label="标签" min-width="160">

          <template #default="{ row }">

            <div class="tag-cell" @click.stop>

              <template v-if="row.tags?.length">

                <el-tag

                  v-for="t in row.tags"

                  :key="t"

                  size="small"

                  effect="plain"

                  class="tag-chip tag-chip--clickable"

                  @click="filterByTag(t)"

                >

                  {{ t }}

                </el-tag>

              </template>

              <span v-else class="tag-cell__empty">—</span>

            </div>

          </template>

        </el-table-column>

        <el-table-column label="Embedding大模型" min-width="140" show-overflow-tooltip>

          <template #default="{ row }">

            <span class="embedding-cell" :title="row.embeddingModel || ''">

              {{ formatEmbeddingModel(row.embeddingModel) }}

            </span>

          </template>

        </el-table-column>

        <el-table-column prop="documentCount" label="文档" min-width="64" align="center" />

        <el-table-column prop="chunkCount" label="分块" min-width="64" align="center" />

        <el-table-column label="更新时间" min-width="140" align="center">

          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>

        </el-table-column>

        <el-table-column label="操作" min-width="120" fixed="right" align="center">

          <template #default="{ row }">

            <el-button link type="primary" @click.stop="openEdit(row)">库配置</el-button>

            <el-button

              link

              type="danger"

              :disabled="isDefaultLibrary(row.libraryId)"

              @click.stop="confirmDelete(row)"

            >

              删除

            </el-button>

          </template>

        </el-table-column>

        </el-table>



        <div

          v-else

          v-loading="loading"

          class="library-card-grid"

        >

        <article

          v-for="row in items"

          :key="row.libraryId"

          class="library-card"

          tabindex="0"

          @click="drillDown(row)"

          @keydown.enter="drillDown(row)"

        >

          <header class="library-card__head">

            <h3 class="library-card__title">{{ row.name }}</h3>
            <el-tag
              v-if="row.pendingMigrationCount > 0"
              size="small"
              type="warning"
              effect="plain"
              class="migration-badge"
              @click.stop="openEdit(row, 'pipeline')"
            >
              待迁移 {{ row.pendingMigrationCount }}
            </el-tag>

          </header>

          <div v-if="row.tags?.length" class="library-card__tags" @click.stop>

            <el-tag

              v-for="t in row.tags"

              :key="t"

              size="small"

              effect="plain"

              class="tag-chip tag-chip--clickable"

              @click="filterByTag(t)"

            >

              {{ t }}

            </el-tag>

          </div>

          <p v-if="row.description" class="library-card__desc">{{ row.description }}</p>

          <p class="library-card__summary">

            <span class="library-card__embedding">{{ formatEmbeddingModel(row.embeddingModel) }}</span>

            <span class="library-card__dot">·</span>

            <span>文档 {{ row.documentCount ?? 0 }}</span>

            <span class="library-card__dot">·</span>

            <span>分块 {{ row.chunkCount ?? 0 }}</span>

          </p>

          <p class="library-card__meta">

            {{ formatTime(row.updatedAt) }}

          </p>

          <footer class="library-card__foot" @click.stop>

            <el-button link type="primary" size="small" @click="openEdit(row)">库配置</el-button>

            <el-button

              link

              type="danger"

              size="small"

              :disabled="isDefaultLibrary(row.libraryId)"

              @click="confirmDelete(row)"

            >

              删除

            </el-button>

          </footer>

        </article>



        <el-empty

          v-if="!loading && items.length === 0"

          class="library-card-empty"

          description="暂无知识库，点击右上角创建"

          :image-size="80"

        />

        </div>

      </div>



      <div class="library-list-footer">

        <el-pagination

          v-if="total > 0"

          class="pager-row"

          background

          layout="total, sizes, prev, pager, next"

          :total="total"

          :current-page="page"

          :page-size="pageSize"

          :page-sizes="[10, 20, 50]"

          @current-change="onPageChange"

          @size-change="onPageSizeChange"

        />

      </div>

      </div>

    </PageCard>



    <CreateLibraryWizard

      v-model="wizardVisible"

      :tenant-id="tenantId"

      @created="onCreated"

    />

    <EditLibrarySettingsDrawer

      v-model="editVisible"

      :library-id="editLibraryId"

      :initial-tab="editInitialTab"

      @saved="onSettingsSaved"

    />

  </div>

</template>



<script setup>

import { computed, onMounted, ref } from 'vue'

import { useRouter } from 'vue-router'

import { Grid, List } from '@element-plus/icons-vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import {

  deleteVectorLibrary,

  listVectorLibraries,

  listVectorLibraryTags

} from '../api/library'

import { useLibraryContext } from '../composables/useLibraryContext'

import PageCard from '../components/PageCard.vue'

import CreateLibraryWizard from '../components/CreateLibraryWizard.vue'

import EditLibrarySettingsDrawer from '../components/EditLibrarySettingsDrawer.vue'
import { labelForEmbeddingModel } from '../utils/embeddingModels'

const DEFAULT_LIBRARY_ID = '00000000-0000-0000-0000-000000000001'

const VIEW_MODE_KEY = 'libraryListViewMode'



const router = useRouter()

const { libraryId, tenantId, persist } = useLibraryContext()



const loading = ref(false)

const items = ref([])

const total = ref(0)

const page = ref(1)

const pageSize = ref(20)

const keyword = ref('')

const tagFilter = ref('')

const tagOptions = ref([])

const wizardVisible = ref(false)

const editVisible = ref(false)

const editLibraryId = ref('')

const editInitialTab = ref('basic')

const pendingMigrationLibraries = computed(
  () => items.value.filter((row) => (row.pendingMigrationCount || 0) > 0).length
)

const viewMode = ref(localStorage.getItem(VIEW_MODE_KEY) === 'card' ? 'card' : 'list')



function onViewModeChange(mode) {

  localStorage.setItem(VIEW_MODE_KEY, mode)

}



function formatEmbeddingModel(model) {
  return labelForEmbeddingModel(model)
}

function formatTime(value) {

  if (!value) return '—'

  const d = new Date(value)

  if (Number.isNaN(d.getTime())) return '—'

  return d.toLocaleString('zh-CN', {

    year: 'numeric',

    month: '2-digit',

    day: '2-digit',

    hour: '2-digit',

    minute: '2-digit'

  })

}



function isDefaultLibrary(id) {

  return id === DEFAULT_LIBRARY_ID

}



async function loadTagOptions() {

  if (!tenantId.value?.trim()) {

    tagOptions.value = []

    return

  }

  try {

    const { data } = await listVectorLibraryTags(tenantId.value.trim())

    tagOptions.value = Array.isArray(data) ? data : []

  } catch {

    tagOptions.value = []

  }

}



async function load(p = page.value) {

  if (!tenantId.value?.trim()) {

    ElMessage.warning('请填写租户 ID')

    return

  }

  page.value = p

  persist()

  loading.value = true

  try {

    const { data } = await listVectorLibraries({

      tenantId: tenantId.value.trim(),

      keyword: keyword.value?.trim() || undefined,

      tag: tagFilter.value?.trim() || undefined,

      page: page.value,

      size: pageSize.value

    })

    items.value = data.items || []

    total.value = data.total ?? 0

    if (typeof data.page === 'number' && data.page >= 1) {

      page.value = data.page

    }

    const maxPage = Math.max(1, Math.ceil(total.value / pageSize.value))

    if (total.value > 0 && items.value.length === 0 && page.value > maxPage) {

      await load(maxPage)

    }

  } finally {

    loading.value = false

  }

}



function onTenantChange() {

  persist()

  tagFilter.value = ''

  loadTagOptions()

  load(1)

}



function onPageChange(p) {

  load(p)

}



function onPageSizeChange(s) {

  pageSize.value = s

  load(1)

}



function filterByTag(tag) {

  tagFilter.value = tag

  load(1)

}



function onTagIndexToggle(tag, checked) {

  tagFilter.value = checked ? tag : ''

  load(1)

}



function drillDown(row) {

  libraryId.value = row.libraryId

  persist()

  router.push({ name: 'vectorLibraryDetail', params: { libraryId: row.libraryId } })

}



function onCreated(lib) {

  libraryId.value = lib.libraryId

  loadTagOptions()

  load(1)

  drillDown(lib)

}



function openEdit(row, tab = 'basic') {

  editInitialTab.value = tab

  editLibraryId.value = row.libraryId

  editVisible.value = true

}



async function onSettingsSaved() {

  await loadTagOptions()

  await load(page.value)

}



async function confirmDelete(row) {

  if (isDefaultLibrary(row.libraryId)) {

    ElMessage.warning('系统默认知识库不可删除')

    return

  }

  const docHint = row.documentCount > 0 ? `（含 ${row.documentCount} 个文档、${row.chunkCount} 个分块）` : ''

  try {

    await ElMessageBox.confirm(

      `确定删除知识库「${row.name}」？将永久删除库内数据与向量索引${docHint}，不可恢复。`,

      '删除知识库',

      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }

    )

  } catch {

    return

  }

  try {

    const { data } = await deleteVectorLibrary(row.libraryId, tenantId.value.trim())

    ElMessage.success(data.message || '知识库已删除')

    if (libraryId.value === row.libraryId) {

      libraryId.value = ''

      persist()

    }

    await loadTagOptions()

    if (items.value.length === 1 && page.value > 1) {

      await load(page.value - 1)

    } else {

      await load(page.value)

    }

  } catch (e) {

    ElMessage.error(e?.response?.data?.message || e?.message || '删除失败')

  }

}



onMounted(async () => {

  await loadTagOptions()

  await load(1)

})

</script>



<style scoped>

.library-list-page {

  width: 100%;

  min-width: 0;

}

.filter-form {

  display: flex;

  flex-wrap: wrap;

  align-items: flex-start;

  width: 100%;

}

.filter-form__view {

  margin-left: auto;

}

.filter-form__view :deep(.el-radio-button__inner) {

  display: inline-flex;

  align-items: center;

  gap: 4px;

  padding: 7px 12px;

}

.view-label {

  font-size: 13px;

}

.tag-index {

  display: flex;

  flex-wrap: wrap;

  align-items: center;

  gap: 6px;

  margin-top: 4px;

  padding-bottom: 8px;

}

.tag-index__label {

  font-size: 12px;

  color: #64748b;

  flex-shrink: 0;

}

.tag-index__item {

  cursor: pointer;

}

.tag-cell {

  display: flex;

  flex-wrap: wrap;

  gap: 4px;

  align-items: center;

}

.tag-cell__empty {

  color: #94a3b8;

}

.tag-chip--clickable {

  cursor: pointer;

}

.library-list-footer {

  display: flex;

  justify-content: flex-end;

  align-items: center;

  flex-wrap: wrap;

  gap: 8px;

  width: 100%;

  min-width: 0;

  min-height: 40px;

}

.pager-row {

  margin: 0;

}

.stat-chip {

  margin-right: 12px;

  font-size: 13px;

  color: #64748b;

}

.stat-chip strong {

  color: #0f172a;

}

.stat-chip--warn {
  color: #b45309;
}

.stat-chip--warn strong {
  color: #b45309;
}

.migration-badge {
  margin-left: 8px;
  vertical-align: middle;
  cursor: pointer;
}

.library-card-grid {

  display: grid;

  grid-template-columns: repeat(auto-fill, minmax(min(100%, 240px), 1fr));

  gap: 12px;

  width: 100%;

  height: 100%;

  overflow-x: hidden;

  overflow-y: auto;

  align-content: start;

  box-sizing: border-box;

}

.library-table--fixed {

  width: 100%;

}

.library-table--fixed :deep(.el-table__body-wrapper) {

  overflow-y: auto;

}

.library-table--fixed :deep(.el-scrollbar__bar.is-horizontal) {

  height: 6px;

}

.library-table--fixed :deep(.el-scrollbar__bar.is-vertical) {

  width: 6px;

}

.library-card {

  display: flex;

  flex-direction: column;

  gap: 4px;

  padding: 12px 14px;

  border: 1px solid var(--dp-border, #e2e8f0);

  border-radius: 8px;

  background: #fff;

  cursor: pointer;

  transition: border-color 0.15s ease, box-shadow 0.15s ease;

}

.library-card:hover,

.library-card:focus-visible {

  border-color: rgba(14, 165, 233, 0.35);

  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);

  outline: none;

}

.library-card__head {

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 8px;

}

.library-card__title {

  margin: 0;

  font-size: 14px;

  font-weight: 600;

  color: #0f172a;

  line-height: 1.3;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;

}

.library-card__tags {

  display: flex;

  flex-wrap: wrap;

  gap: 4px;

}

.library-card__desc {

  margin: 0;

  font-size: 12px;

  line-height: 1.4;

  color: #64748b;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;

}

.embedding-cell {
  font-size: 13px;
  color: #475569;
}

.library-card__summary {

  margin: 2px 0 0;

  font-size: 12px;

  color: #334155;

  line-height: 1.4;

}

.library-card__embedding {
  color: #475569;
}

.library-card__meta {

  margin: 0;

  font-size: 11px;

  color: #94a3b8;

  line-height: 1.4;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;

}

.library-card__dot {

  margin: 0 4px;

  color: #cbd5e1;

}

.library-card__foot {

  display: flex;

  justify-content: flex-end;

  gap: 2px;

  margin-top: 4px;

  padding-top: 6px;

  border-top: 1px solid #f1f5f9;

}

.library-card-empty {

  grid-column: 1 / -1;

  display: flex;

  align-items: center;

  justify-content: center;

  height: 100%;

}

</style>


