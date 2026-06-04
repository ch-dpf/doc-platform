<template>

  <div class="page-wrap">

    <PageCard title="知识库管理">

      <template #actions>

        <el-button type="primary" round @click="wizardVisible = true">新建知识库</el-button>

      </template>



      <el-form :inline="true" class="filter-panel">

        <el-form-item label="租户 ID">

          <el-input v-model="tenantId" style="width: 120px" @change="persist" />

        </el-form-item>

        <el-form-item>

          <el-button type="primary" round @click="load">刷新</el-button>

        </el-form-item>

      </el-form>



      <el-table
        v-loading="loading"
        :data="items"
        stripe
        class="data-table library-table"
        highlight-current-row
        @row-click="drillDown"
      >

        <el-table-column prop="name" label="名称" min-width="140">

          <template #default="{ row }">

            <span class="name-link">{{ row.name }}</span>

          </template>

        </el-table-column>

        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />

        <el-table-column label="数据源" width="100">

          <template #default="{ row }">

            {{ sourceLabel(row.config?.ingestSourceMode) }}

          </template>

        </el-table-column>

        <el-table-column label="存储" width="100">

          <template #default="{ row }">{{ row.config?.storageType || '—' }}</template>

        </el-table-column>

        <el-table-column prop="documentCount" label="文档数" width="80" align="center" />

        <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />

        <el-table-column label="操作" width="100" fixed="right">

          <template #default="{ row }">

            <el-button link type="primary" @click.stop="openEdit(row)">编辑规则</el-button>

          </template>

        </el-table-column>

      </el-table>

    </PageCard>



    <CreateLibraryWizard

      v-model="wizardVisible"

      :tenant-id="tenantId"

      @created="onCreated"

    />

    <EditLibrarySettingsDrawer

      v-model="editVisible"

      :library-id="editLibraryId"

      @saved="load"

    />

  </div>

</template>



<script setup>

import { onMounted, ref } from 'vue'

import { useRouter } from 'vue-router'

import { listVectorLibraries } from '../api/library'

import { useLibraryContext } from '../composables/useLibraryContext'

import PageCard from '../components/PageCard.vue'

import CreateLibraryWizard from '../components/CreateLibraryWizard.vue'

import EditLibrarySettingsDrawer from '../components/EditLibrarySettingsDrawer.vue'



const router = useRouter()

const { libraryId, tenantId, persist } = useLibraryContext()



const loading = ref(false)

const items = ref([])

const wizardVisible = ref(false)

const editVisible = ref(false)

const editLibraryId = ref('')



function sourceLabel(mode) {

  if (mode === 'crawl') return '线上采集'

  if (mode === 'both') return '本地+线上'

  return '本地文件'

}



async function load() {

  persist()

  loading.value = true

  try {

    const { data } = await listVectorLibraries(tenantId.value)

    items.value = data

  } finally {

    loading.value = false

  }

}



function drillDown(row) {

  libraryId.value = row.libraryId

  persist()

  router.push({ name: 'vectorLibraryDetail', params: { libraryId: row.libraryId } })

}



function onCreated(lib) {

  libraryId.value = lib.libraryId

  load()

  drillDown(lib)

}



function openEdit(row) {

  editLibraryId.value = row.libraryId

  editVisible.value = true

}



onMounted(load)

</script>



