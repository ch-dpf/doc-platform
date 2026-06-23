import { createRouter, createWebHistory } from 'vue-router';
import HomeView from './views/HomeView.vue';
import LibraryPage from './views/LibraryPage.vue';
import LibraryWorkspaceLayout from './layouts/LibraryWorkspaceLayout.vue';
import LibraryDocumentsPage from './views/library/LibraryDocumentsPage.vue';
import LibraryDocumentChunksPage from './views/library/LibraryDocumentChunksPage.vue';
import LibraryRetrievalTestPage from './views/library/LibraryRetrievalTestPage.vue';
import LibrarySettingsPage from './views/library/LibrarySettingsPage.vue';
import LibraryAclPage from './views/library/LibraryAclPage.vue';
import AgentPage from './views/AgentPage.vue';
import ObservabilityPage from './views/ObservabilityPage.vue';
import PresetPage from './views/PresetPage.vue';
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
      subtitle: '创建知识库、选择库类型预设；点击进入文档列表与召回评测。'
    }
  },
  {
    path: '/libraries/:libraryId',
    component: LibraryWorkspaceLayout,
    meta: { hidePageHeader: true },
    children: [
      { path: '', redirect: { name: 'library-documents' } },
      {
        path: 'documents',
        name: 'library-documents',
        component: LibraryDocumentsPage,
        meta: { title: '文档列表' }
      },
      {
        path: 'documents/:documentId',
        name: 'library-document-detail',
        component: LibraryDocumentChunksPage,
        meta: { title: '文档详情' }
      },
      {
        path: 'documents/:documentId/chunks',
        redirect: (to) => ({
          name: 'library-document-detail',
          params: { libraryId: to.params.libraryId, documentId: to.params.documentId }
        })
      },
      {
        path: 'retrieval-test',
        name: 'library-retrieval-test',
        component: LibraryRetrievalTestPage,
        meta: { title: '召回与评测' }
      },
      {
        path: 'settings',
        name: 'library-settings',
        component: LibrarySettingsPage,
        meta: { title: '库配置' }
      },
      {
        path: 'acl',
        name: 'library-acl',
        component: LibraryAclPage,
        meta: { title: '权限 ACL' }
      }
    ]
  },
  {
    path: '/agents',
    name: 'agents',
    component: AgentPage,
    meta: {
      title: '知识智能体',
      subtitle: '绑定多知识库，管理版本生命周期，执行召回预览与评测。'
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
    path: '/presets',
    name: 'presets',
    component: PresetPage,
    meta: {
      title: '预设管理',
      subtitle: '查看、创建与删除库类型预设和场景规则预设，浏览完整配置参数。'
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
