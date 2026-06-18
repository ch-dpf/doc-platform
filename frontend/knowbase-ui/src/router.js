import { createRouter, createWebHistory } from 'vue-router';
import HomeView from './views/HomeView.vue';
import LibraryPage from './views/LibraryPage.vue';
import IngestionPage from './views/IngestionPage.vue';
import AgentPage from './views/AgentPage.vue';
import ObservabilityPage from './views/ObservabilityPage.vue';
import QaPage from './views/QaPage.vue';

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: HomeView, meta: { hidePageHeader: true } },
  {
    path: '/libraries',
    name: 'libraries',
    component: LibraryPage,
    meta: {
      title: '知识库管理',
      subtitle: '创建知识库、选择库类型预设，管理异构文档 Profile 与索引版本。'
    }
  },
  {
    path: '/ingestions',
    name: 'ingestions',
    component: IngestionPage,
    meta: {
      title: '入库任务',
      subtitle: '执行 token 驱动的入库 Pipeline，发布可检索索引版本。'
    }
  },
  {
    path: '/agents',
    name: 'agents',
    component: AgentPage,
    meta: {
      title: '知识智能体',
      subtitle: '绑定多知识库，管理版本生命周期，执行检索测试与评测。'
    }
  },
  {
    path: '/observability',
    name: 'observability',
    component: ObservabilityPage,
    meta: {
      title: '观测与评测',
      subtitle: '查看 Pipeline Trace、创建评测运行并追踪问答链路。'
    }
  },
  {
    path: '/qa',
    name: 'qa',
    component: QaPage,
    meta: {
      title: '智能问答',
      subtitle: '基于已发布智能体版本执行多库检索、证据融合与回答生成。'
    }
  }
];

export default createRouter({
  history: createWebHistory(),
  routes
});
