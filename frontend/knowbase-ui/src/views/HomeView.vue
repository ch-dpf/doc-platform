<template>
  <div class="page-wrap home-page">
    <section class="home-hero">
      <h2 class="home-hero__title">欢迎使用知库</h2>
      <p class="home-hero__desc">从创建知识库、执行入库到编排智能体与智能问答，构建完整 RAG 能力链路</p>
    </section>

    <div class="home-entry-grid">
      <button type="button" class="home-entry-card home-entry-card--library" @click="router.push('/libraries')">
        <div class="home-entry-card__icon">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="home-entry-card__body">
          <h3 class="home-entry-card__title">知识库</h3>
          <p class="home-entry-card__desc">创建知识库、选择库类型预设，管理异构文档 Profile 与索引版本</p>
        </div>
        <el-icon class="home-entry-card__arrow"><ArrowRight /></el-icon>
      </button>

      <button type="button" class="home-entry-card home-entry-card--ingest" @click="router.push('/ingestions')">
        <div class="home-entry-card__icon">
          <el-icon><Upload /></el-icon>
        </div>
        <div class="home-entry-card__body">
          <h3 class="home-entry-card__title">入库任务</h3>
          <p class="home-entry-card__desc">提交文档入库，按 token 驱动 Pipeline 解析、切块并发布索引</p>
        </div>
        <el-icon class="home-entry-card__arrow"><ArrowRight /></el-icon>
      </button>

      <button type="button" class="home-entry-card home-entry-card--agent" @click="router.push('/agents')">
        <div class="home-entry-card__icon">
          <el-icon><DataBoard /></el-icon>
        </div>
        <div class="home-entry-card__body">
          <h3 class="home-entry-card__title">知识智能体</h3>
          <p class="home-entry-card__desc">绑定多知识库，选择场景规则预设，发布可复用问答编排</p>
        </div>
        <el-icon class="home-entry-card__arrow"><ArrowRight /></el-icon>
      </button>

      <button type="button" class="home-entry-card home-entry-card--qa" @click="router.push('/qa')">
        <div class="home-entry-card__icon">
          <el-icon><ChatLineRound /></el-icon>
        </div>
        <div class="home-entry-card__body">
          <h3 class="home-entry-card__title">智能问答</h3>
          <p class="home-entry-card__desc">基于已发布智能体执行多库检索、证据融合与回答生成</p>
        </div>
        <el-icon class="home-entry-card__arrow"><ArrowRight /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { ArrowRight, ChatLineRound, Coin, DataBoard, Upload } from '@element-plus/icons-vue';

const router = useRouter();
</script>

<style scoped>
.home-page {
  max-width: 1080px;
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

.home-entry-card--ingest:hover {
  border-color: rgba(99, 102, 241, 0.45);
}

.home-entry-card--agent:hover {
  border-color: rgba(245, 158, 11, 0.45);
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

.home-entry-card--ingest .home-entry-card__icon {
  background: linear-gradient(135deg, #6366f1 0%, #818cf8 100%);
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
}

.home-entry-card--agent .home-entry-card__icon {
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
  box-shadow: 0 4px 14px rgba(245, 158, 11, 0.35);
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
