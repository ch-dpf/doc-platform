import { createRouter, createWebHistory } from 'vue-router'
import IngestView from '../views/IngestView.vue'
import DocumentsView from '../views/DocumentsView.vue'
import QueryView from '../views/QueryView.vue'

const routes = [
  { path: '/', redirect: '/documents' },
  { path: '/documents', name: 'documents', component: DocumentsView, meta: { title: '文档管理' } },
  { path: '/ingest', name: 'ingest', component: IngestView, meta: { title: '文档收集上传' } },
  { path: '/upload', redirect: '/ingest' },
  { path: '/collect', redirect: { path: '/ingest', query: { tab: 'collect' } } },
  { path: '/query', name: 'query', component: QueryView, meta: { title: '查询状态' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
