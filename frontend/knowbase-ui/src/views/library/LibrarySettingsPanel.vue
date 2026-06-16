<template>
  <div class="library-panel library-settings-panel">
    <LibraryOnboardingStrip
      v-if="showOnboarding"
      :config-saved="configSaved"
      :has-documents="hasDocuments"
      @dismiss="dismissOnboarding"
      @verify-retrieval="goRetrievalTest"
    />

    <el-form
      v-loading="loading"
      class="cfg-form library-settings-form"
      label-width="100px"
      label-position="right"
    >
      <LibraryConfigTabs
        ref="tabsRef"
        v-model:active-tab="activeTab"
        :form="form"
        :library-id="libraryIdParam"
        :chunk-strategy-rows="chunkStrategyRows"
        :indexed-chunk-count="loadedChunkCount"
        strategy-empty-text="加载策略摘要…"
      />
    </el-form>

    <div class="library-settings-panel__footer">
      <el-button type="primary" round :loading="saving" @click="submit">
        {{ showOnboarding && !configSaved ? '保存配置' : '保存库配置' }}
      </el-button>
      <el-button v-if="showOnboarding && configSaved && !hasDocuments" round @click="goIngest">
        上传文档
      </el-button>
      <el-button v-if="showOnboarding && hasDocuments" round @click="goRetrievalTest">
        验证检索
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LibraryConfigTabs from '../../components/LibraryConfigTabs.vue'
import LibraryOnboardingStrip from '../../components/LibraryOnboardingStrip.vue'
import { useLibraryContext } from '../../composables/useLibraryContext'
import { useLibrarySettingsEditor } from '../../composables/useLibrarySettingsEditor'
import {
  dismissLibraryOnboarding,
  isOnboardingConfigSaved,
  isOnboardingDismissed,
  isOnboardingInProgress,
  markOnboardingConfigSaved
} from '../../utils/libraryOnboarding'

const route = useRoute()
const router = useRouter()
const { libraryId, tenantId, persist } = useLibraryContext()
const refreshLibrary = inject('refreshLibraryDetail', null)
const library = inject('libraryDetail', ref(null))

const libraryIdParam = computed(() => String(route.params.libraryId || ''))
const onboardingDismissed = ref(false)
const onboardingConfigSavedLocal = ref(false)

const {
  form,
  activeTab,
  loading,
  saving,
  loadedChunkCount,
  chunkStrategyRows,
  tabsRef,
  load,
  submit: saveSettings
} = useLibrarySettingsEditor(() => libraryIdParam.value, { tenantId })

const hasDocuments = computed(() => (library.value?.documentCount ?? 0) > 0)
const configSaved = computed(
  () => isOnboardingConfigSaved(libraryIdParam.value) || onboardingConfigSavedLocal.value
)

const showOnboarding = computed(() => {
  if (!isOnboardingInProgress(route, libraryIdParam.value)) return false
  if (onboardingDismissed.value || isOnboardingDismissed(libraryIdParam.value)) return false
  return true
})

function clearOnboardingQuery() {
  if (!route.query.onboarding) return
  const nextQuery = { ...route.query }
  delete nextQuery.onboarding
  delete nextQuery.setup
  router.replace({ query: nextQuery })
}

function dismissOnboarding() {
  dismissLibraryOnboarding(libraryIdParam.value)
  onboardingDismissed.value = true
  clearOnboardingQuery()
}

function goIngest() {
  if (!configSaved.value) return
  libraryId.value = libraryIdParam.value
  persist()
  clearOnboardingQuery()
  router.push({ path: '/ingest', query: { libraryId: libraryIdParam.value } })
}

function goRetrievalTest() {
  if (!hasDocuments.value) return
  libraryId.value = libraryIdParam.value
  persist()
  clearOnboardingQuery()
  router.push({
    name: 'libraryRetrieval',
    params: { libraryId: libraryIdParam.value }
  })
}

async function submit() {
  await saveSettings()
  if (!showOnboarding.value) return
  markOnboardingConfigSaved(libraryIdParam.value)
  onboardingConfigSavedLocal.value = true
  await refreshLibrary?.()
}

watch(
  () => route.query.tab,
  (tab) => {
    if (typeof tab === 'string' && tab) activeTab.value = tab
  },
  { immediate: true }
)

watch(libraryIdParam, () => {
  onboardingDismissed.value = isOnboardingDismissed(libraryIdParam.value)
  onboardingConfigSavedLocal.value = isOnboardingConfigSaved(libraryIdParam.value)
  if (libraryIdParam.value) load(activeTab.value)
})

watch(hasDocuments, (v) => {
  if (v && configSaved.value && !isOnboardingInProgress(route, libraryIdParam.value)) {
    clearOnboardingQuery()
  }
})

onMounted(() => {
  onboardingDismissed.value = isOnboardingDismissed(libraryIdParam.value)
  onboardingConfigSavedLocal.value = isOnboardingConfigSaved(libraryIdParam.value)
  if (libraryIdParam.value) load(activeTab.value)
})
</script>

<style scoped>
.library-settings-panel {
  max-width: 960px;
}
.library-settings-form {
  min-height: 320px;
}
.library-settings-panel__footer {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
