import { createRouter, createWebHistory } from 'vue-router'
import SearchView from '../views/SearchView.vue'
import RagView from '../views/RagView.vue'
import RebuildView from '../views/RebuildView.vue'
import PurgeView from '../views/PurgeView.vue'

const routes = [
  { path: '/', redirect: '/rag' },
  { path: '/rag', name: 'rag', component: RagView, meta: { title: 'RAG 问答' } },
  { path: '/search', name: 'search', component: SearchView, meta: { title: '语义检索' } },
  { path: '/rebuild', name: 'rebuild', component: RebuildView, meta: { title: '补偿重索引' } },
  { path: '/purge', name: 'purge', component: PurgeView, meta: { title: '清理向量' } }
]

export default createRouter({
  history: createWebHistory(),
  routes
})
