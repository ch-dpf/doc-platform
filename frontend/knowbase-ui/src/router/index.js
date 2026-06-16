import { createRouter, createWebHistory } from 'vue-router'

import DocumentChunksView from '../views/DocumentChunksView.vue'
import DocumentsView from '../views/DocumentsView.vue'
import HomeView from '../views/HomeView.vue'
import IngestView from '../views/IngestView.vue'
import QaView from '../views/QaView.vue'
import VectorLibrariesView from '../views/VectorLibrariesView.vue'
import VectorLibraryDetailView from '../views/VectorLibraryDetailView.vue'
import LibraryDocumentsPanel from '../views/library/LibraryDocumentsPanel.vue'
import LibraryRetrievalTestPanel from '../views/library/LibraryRetrievalTestPanel.vue'
import LibrarySettingsPanel from '../views/library/LibrarySettingsPanel.vue'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/home', name: 'home', component: HomeView, meta: { title: '首页', hidePageHeader: true } },
  { path: '/documents', name: 'documents', component: DocumentsView, meta: { title: '文档库' } },
  {
    path: '/documents/:docId/chunks',
    name: 'documentChunks',
    component: DocumentChunksView,
    meta: { title: '文档分块' }
  },
  { path: '/ingest', name: 'ingest', component: IngestView, meta: { title: '文档采集' } },
  { path: '/vector-libraries', name: 'vectorLibraries', component: VectorLibrariesView, meta: { title: '知识库管理' } },
  {
    path: '/vector-libraries/:libraryId',
    component: VectorLibraryDetailView,
    meta: { title: '知识库详情' },
    children: [
      { path: '', redirect: { name: 'libraryDocuments' } },
      {
        path: 'documents',
        name: 'libraryDocuments',
        component: LibraryDocumentsPanel,
        meta: { title: '文档列表' }
      },
      {
        path: 'retrieval',
        name: 'libraryRetrieval',
        component: LibraryRetrievalTestPanel,
        meta: { title: '检索测试' }
      },
      {
        path: 'settings',
        name: 'librarySettings',
        component: LibrarySettingsPanel,
        meta: { title: '知识库设置' }
      }
    ]
  },
  { path: '/orchestrations', redirect: '/vector-libraries' },
  { path: '/models', redirect: '/vector-libraries' },
  { path: '/qa', name: 'qa', component: QaView, meta: { title: '智能问答' } },
  { path: '/upload', redirect: '/ingest' },
  {
    path: '/query',
    redirect: (to) => ({
      name: 'documentChunks',
      params: { docId: to.query.docId },
      query: { libraryId: to.query.libraryId, from: 'documents' }
    })
  },
  { path: '/rag', redirect: '/qa' },
  { path: '/search', redirect: (to) => {
    const libraryId = to.query.libraryId
    if (libraryId) {
      return { name: 'libraryRetrieval', params: { libraryId } }
    }
    return '/vector-libraries'
  } },
  { path: '/rebuild', redirect: '/documents' },
  { path: '/purge', redirect: '/documents' }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
