<template>
  <el-container class="app-layout">
    <el-header class="app-topbar" height="56px">
      <div class="app-topbar__brand" @click="router.push('/home')">
        <div class="app-brand__icon">
          <el-icon><Document /></el-icon>
        </div>
        <div>
          <span class="app-brand__title">知库</span>
          <span class="app-brand__sub">knowbase</span>
        </div>
      </div>

      <nav class="app-topnav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="app-topnav__item"
          :class="{ 'is-active': activeNav === item.path }"
        >
          <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="app-topbar__actions">
        <el-link class="app-footer-link" :href="knife4jUrl" target="_blank" rel="noopener noreferrer">
          <el-icon><Link /></el-icon>
          API
        </el-link>
      </div>
    </el-header>

    <el-container class="app-main-shell">
      <el-header v-if="showPageHeader" class="app-header" height="68px">
        <div>
          <h1 class="app-header__title">{{ headerTitle }}</h1>
        </div>
      </el-header>

      <el-main class="app-main" :class="{ 'app-main--flush': !showPageHeader }">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatLineRound, Coin, HomeFilled } from '@element-plus/icons-vue'
import { knife4jUrl } from './config'
import { usePageTitle } from './composables/usePageTitle'

const route = useRoute()
const router = useRouter()
const { pageTitleOverride } = usePageTitle()

const navItems = [
  { path: '/home', label: '首页', icon: HomeFilled },
  { path: '/vector-libraries', label: '知识库', icon: Coin },
  { path: '/qa', label: '智能问答', icon: ChatLineRound }
]

const headerTitle = computed(() => pageTitleOverride.value || route.meta.title || '')
const showPageHeader = computed(() => !route.meta.hidePageHeader)

const activeNav = computed(() => {
  if (route.path === '/home' || route.path === '/') return '/home'
  if (route.path === '/qa' || route.path === '/rag' || route.path === '/search') return '/qa'
  if (
    route.path === '/ingest' ||
    route.path === '/upload' ||
    route.path.startsWith('/vector-libraries') ||
    route.path === '/documents' ||
    route.path.startsWith('/documents/')
  ) {
    return '/vector-libraries'
  }
  return route.path
})
</script>

<style>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
