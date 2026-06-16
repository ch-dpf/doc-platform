<template>
  <div class="page-wrap page-wrap--fluid library-detail-page library-page-fill">
    <PageCard :title="libraryTitle">
      <template #actions>
        <el-button round @click="goBack">返回列表</el-button>
      </template>

      <el-skeleton v-if="libraryLoading && !library" :rows="4" animated />

      <template v-else-if="library">
        <div class="library-detail-layout">
          <nav class="library-detail-nav">
            <el-menu
              :default-active="activeMenu"
              router
              class="library-detail-menu"
            >
              <el-menu-item :index="documentsPath">
                <el-icon><Document /></el-icon>
                <span>文档列表</span>
              </el-menu-item>
              <el-menu-item :index="retrievalPath">
                <el-icon><Search /></el-icon>
                <span>检索测试</span>
              </el-menu-item>
              <el-menu-item :index="settingsPath">
                <el-icon><Setting /></el-icon>
                <span>设置</span>
              </el-menu-item>
            </el-menu>
          </nav>
          <section class="library-detail-content">
            <router-view />
          </section>
        </div>
      </template>

      <el-result v-else icon="warning" title="知识库不存在或无法加载">
        <template #extra>
          <el-button type="primary" round @click="goBack">返回列表</el-button>
        </template>
      </el-result>
    </PageCard>
  </div>
</template>

<script setup>
import { computed, onMounted, provide, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Search, Setting } from '@element-plus/icons-vue'
import { getVectorLibrary } from '../api/library'
import { useLibraryContext } from '../composables/useLibraryContext'
import PageCard from '../components/PageCard.vue'

const route = useRoute()
const router = useRouter()
const { libraryId, persist } = useLibraryContext()

const libraryIdParam = computed(() => String(route.params.libraryId || ''))
const libraryLoading = ref(false)
const library = ref(null)

const libraryTitle = computed(() => library.value?.name || '知识库详情')

const basePath = computed(() => `/vector-libraries/${libraryIdParam.value}`)
const documentsPath = computed(() => `${basePath.value}/documents`)
const retrievalPath = computed(() => `${basePath.value}/retrieval`)
const settingsPath = computed(() => `${basePath.value}/settings`)

const activeMenu = computed(() => {
  const path = route.path
  if (path.endsWith('/retrieval')) return retrievalPath.value
  if (path.endsWith('/settings')) return settingsPath.value
  return documentsPath.value
})

provide('libraryDetail', library)
provide('libraryDetailLoading', libraryLoading)

async function loadLibrary() {
  if (!libraryIdParam.value) return
  libraryLoading.value = true
  try {
    const { data } = await getVectorLibrary(libraryIdParam.value)
    library.value = data
    libraryId.value = data.libraryId
    persist()
  } catch {
    library.value = null
  } finally {
    libraryLoading.value = false
  }
}

provide('refreshLibraryDetail', loadLibrary)

function goBack() {
  router.push('/vector-libraries')
}

watch(libraryIdParam, () => {
  if (libraryIdParam.value) loadLibrary()
})

onMounted(() => {
  loadLibrary()
})
</script>

<style scoped>
.library-detail-page {
  width: 100%;
  min-width: 0;
}
.library-detail-layout {
  display: flex;
  gap: 0;
  min-height: 520px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}
.library-detail-nav {
  flex: 0 0 168px;
  border-right: 1px solid #e2e8f0;
  background: #f8fafc;
}
.library-detail-menu {
  border-right: none;
  background: transparent;
}
.library-detail-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
}
.library-detail-content {
  flex: 1;
  min-width: 0;
  padding: 16px 20px;
  overflow: auto;
}
</style>
