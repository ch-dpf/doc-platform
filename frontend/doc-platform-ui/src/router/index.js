import { createRouter, createWebHistory } from 'vue-router'

import DocumentsView from '../views/DocumentsView.vue'
import IngestView from '../views/IngestView.vue'
import QaView from '../views/QaView.vue'
import VectorLibrariesView from '../views/VectorLibrariesView.vue'
import VectorLibraryDetailView from '../views/VectorLibraryDetailView.vue'
const routes = [
  { path: '/', redirect: '/vector-libraries' },
  { path: '/documents', name: 'documents', component: DocumentsView, meta: { title: '文档库' } },
  { path: '/ingest', name: 'ingest', component: IngestView, meta: { title: '文档采集' } },
  { path: '/vector-libraries', name: 'vectorLibraries', component: VectorLibrariesView, meta: { title: '知识库管理' } },
  {
    path: '/vector-libraries/:libraryId',
    name: 'vectorLibraryDetail',
    component: VectorLibraryDetailView,
    meta: { title: '知识库详情' }
  },
  { path: '/orchestrations', redirect: '/vector-libraries' },
  { path: '/models', redirect: '/vector-libraries' },
  { path: '/qa', name: 'qa', component: QaView, meta: { title: '智能问答' } },
  { path: '/upload', redirect: '/ingest' },
  { path: '/collect', redirect: { path: '/ingest', query: { tab: 'collect' } } },
  { path: '/query', redirect: (to) => ({ path: '/documents', query: { docId: to.query.docId, poll: '1' } }) },
  { path: '/rag', redirect: '/qa' },
  { path: '/search', redirect: { path: '/qa', query: { tab: 'search' } } },
  { path: '/rebuild', redirect: '/documents' },
  { path: '/purge', redirect: '/documents' }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
