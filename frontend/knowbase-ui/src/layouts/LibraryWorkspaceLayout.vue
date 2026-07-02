<template>
  <div v-loading="loading" class="library-workspace">
    <header class="library-workspace__header">
      <div class="library-workspace__header-top">
        <el-button class="library-workspace__back" link type="primary" @click="router.push('/libraries')">
          <el-icon><ArrowLeft /></el-icon>
          知识库列表
        </el-button>
        <div v-if="library" class="library-workspace__title-block">
          <h1 class="library-workspace__title">{{ library.name }}</h1>
          <p class="library-workspace__meta">
            {{ shortId(library.libraryId, 16) }} · {{ library.libraryTypePresetCode }} · {{ library.status }}
          </p>
        </div>
      </div>
      <el-alert
        v-if="indexHealth?.rebuildRecommended"
        type="warning"
        :closable="false"
        show-icon
        class="library-workspace__health"
        :title="indexHealth.message"
        description="可在「库配置」中发布新 Profile 或执行重索引；日常上传会自动更新检索索引。"
      />
    </header>

    <div class="library-workspace__body">
      <aside class="library-workspace__sidebar">
        <el-menu
          :default-active="activeMenu"
          class="library-workspace__menu"
          @select="handleMenuSelect"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="library-workspace__content">
        <el-alert
          v-if="toast.message"
          :title="toast.message"
          :type="toast.type === 'success' ? 'success' : 'error'"
          show-icon
          closable
          class="library-workspace__toast"
          @close="toast.message = ''"
        />
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, provide, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowLeft, Document, Key, Search, Setting } from '@element-plus/icons-vue';
import { LIBRARY_WORKSPACE_KEY } from '../composables/libraryWorkspace';
import { getIndexHealth, getLibrary } from '../api';
import { usePageTitle } from '../composables/usePageTitle';
import { shortId } from '../format';

const route = useRoute();
const router = useRouter();
const { setPageTitle, clearPageTitle } = usePageTitle();

const library = ref(null);
const indexHealth = ref(null);
const loading = ref(false);
const toast = reactive({ message: '', type: 'success' });

const libraryId = computed(() => route.params.libraryId);

const menuItems = computed(() => [
  { path: `/libraries/${libraryId.value}/documents`, label: '文档列表', icon: Document },
  { path: `/libraries/${libraryId.value}/retrieval-test`, label: '召回与评测', icon: Search },
  { path: `/libraries/${libraryId.value}/settings`, label: '库配置', icon: Setting },
  { path: `/libraries/${libraryId.value}/acl`, label: '权限 ACL', icon: Key }
]);

const activeMenu = computed(() => {
  const path = route.path;
  const match = menuItems.value.find(item => path.startsWith(item.path));
  return match?.path || menuItems.value[0]?.path;
});

function showMessage(text, type = 'success') {
  toast.message = text || '操作失败';
  toast.type = type;
}

function handleMenuSelect(path) {
  if (path !== route.path) {
    router.push(path);
  }
}

async function loadLibrary() {
  if (!libraryId.value) {
    return;
  }
  loading.value = true;
  try {
    library.value = await getLibrary(libraryId.value);
    setPageTitle(library.value.name);
    await loadIndexHealth();
  } catch (error) {
    showMessage(error.message, 'error');
    library.value = null;
  } finally {
    loading.value = false;
  }
}

async function loadIndexHealth() {
  if (!libraryId.value) {
    return;
  }
  try {
    indexHealth.value = await getIndexHealth(libraryId.value);
  } catch {
    indexHealth.value = null;
  }
}

provide(LIBRARY_WORKSPACE_KEY, {
  library,
  libraryId,
  indexHealth,
  reloadLibrary: loadLibrary,
  reloadIndexHealth: loadIndexHealth,
  showMessage
});

watch(libraryId, () => {
  loadLibrary();
});

onMounted(() => {
  loadLibrary();
});

onUnmounted(() => {
  clearPageTitle();
});
</script>

<style scoped>
.library-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: calc(100vh - 56px);
  padding: 0 4px 24px;
}

.library-workspace__header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.library-workspace__header-top {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.library-workspace__back {
  align-self: flex-start;
  padding-left: 0;
}

.library-workspace__title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: var(--dp-text);
}

.library-workspace__meta {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--dp-text-secondary);
}

.library-workspace__body {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.library-workspace__sidebar {
  position: sticky;
  top: 12px;
  background: var(--dp-surface);
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius);
  box-shadow: var(--dp-shadow-sm);
  overflow: hidden;
}

.library-workspace__menu {
  border-right: none;
}

.library-workspace__content {
  min-width: 0;
}

.library-workspace__toast {
  margin-bottom: 12px;
}

@media (max-width: 900px) {
  .library-workspace__body {
    grid-template-columns: 1fr;
  }

  .library-workspace__sidebar {
    position: static;
  }
}
</style>
