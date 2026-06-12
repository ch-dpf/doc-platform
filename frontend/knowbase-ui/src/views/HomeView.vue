<template>
  <div class="page-wrap home-page">
    <section class="home-hero">
      <h2 class="home-hero__title">欢迎使用知库</h2>
      <p class="home-hero__desc">从创建知识库或开启智能问答开始，构建你的企业知识能力</p>
    </section>

    <div class="home-entry-grid">
      <button type="button" class="home-entry-card home-entry-card--library" @click="openCreateLibrary">
        <div class="home-entry-card__icon">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="home-entry-card__body">
          <h3 class="home-entry-card__title">创建知识库</h3>
          <p class="home-entry-card__desc">上传文档、配置解析与分块策略，构建可检索的知识资产</p>
        </div>
        <el-icon class="home-entry-card__arrow"><ArrowRight /></el-icon>
      </button>

      <button type="button" class="home-entry-card home-entry-card--qa" @click="goCreateQa">
        <div class="home-entry-card__icon">
          <el-icon><ChatLineRound /></el-icon>
        </div>
        <div class="home-entry-card__body">
          <h3 class="home-entry-card__title">创建智能问答</h3>
          <p class="home-entry-card__desc">基于知识库进行 RAG 对话，快速验证检索与回答效果</p>
        </div>
        <el-icon class="home-entry-card__arrow"><ArrowRight /></el-icon>
      </button>
    </div>

    <CreateLibraryWizard
      v-model="wizardVisible"
      :tenant-id="tenantId"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, ChatLineRound, Coin } from '@element-plus/icons-vue'
import CreateLibraryWizard from '../components/CreateLibraryWizard.vue'
import { useLibraryContext } from '../composables/useLibraryContext'

const router = useRouter()
const { tenantId } = useLibraryContext()
const wizardVisible = ref(false)

function openCreateLibrary() {
  wizardVisible.value = true
}

function goCreateQa() {
  router.push({ name: 'qa', query: { new: '1' } })
}
</script>

<style scoped>
.home-page {
  max-width: 960px;
  padding-top: 12px;
}

.home-hero {
  margin-bottom: 36px;
}

.home-hero__title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--dp-text);
  letter-spacing: -0.03em;
}

.home-hero__desc {
  margin: 10px 0 0;
  font-size: 15px;
  color: var(--dp-text-secondary);
  line-height: 1.6;
}

.home-entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
}

.home-entry-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  width: 100%;
  padding: 24px;
  text-align: left;
  border: 1px solid var(--dp-border);
  border-radius: var(--dp-radius-lg);
  background: var(--dp-surface);
  box-shadow: var(--dp-shadow);
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.home-entry-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--dp-shadow-hover);
}

.home-entry-card--library:hover {
  border-color: rgba(14, 165, 233, 0.45);
}

.home-entry-card--qa:hover {
  border-color: rgba(16, 185, 129, 0.45);
}

.home-entry-card__icon {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 26px;
  color: #fff;
}

.home-entry-card--library .home-entry-card__icon {
  background: linear-gradient(135deg, var(--dp-primary) 0%, #38bdf8 100%);
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.35);
}

.home-entry-card--qa .home-entry-card__icon {
  background: linear-gradient(135deg, var(--dp-accent) 0%, #34d399 100%);
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.35);
}

.home-entry-card__body {
  flex: 1;
  min-width: 0;
}

.home-entry-card__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--dp-text);
}

.home-entry-card__desc {
  margin: 8px 0 0;
  font-size: 13px;
  line-height: 1.55;
  color: var(--dp-text-secondary);
}

.home-entry-card__arrow {
  flex-shrink: 0;
  margin-top: 4px;
  font-size: 18px;
  color: #cbd5e1;
  transition: color 0.2s ease, transform 0.2s ease;
}

.home-entry-card:hover .home-entry-card__arrow {
  color: var(--dp-primary);
  transform: translateX(3px);
}
</style>
