<template>
  <el-container class="app-layout">
    <el-aside width="232px" class="app-aside">
      <div class="app-brand">
        <div class="app-brand__icon">
          <el-icon><Document /></el-icon>
        </div>
        <div>
          <span class="app-brand__title">文档平台</span>
          <span class="app-brand__sub">doc-platform</span>
        </div>
      </div>

      <el-menu
        class="app-aside-menu"
        :default-active="activeMenu"
        router
        background-color="transparent"
        text-color="#e2e8f0"
        active-text-color="#ffffff"
      >
        <el-menu-item-group title="知识库">
          <el-menu-item index="/vector-libraries">
            <el-icon><Coin /></el-icon>
            <span>知识库管理</span>
          </el-menu-item>
        </el-menu-item-group>
        <el-menu-item-group title="智能问答">
          <el-menu-item index="/qa">
            <el-icon><ChatLineRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>

      <div class="app-aside-footer">
        <el-link class="app-footer-link" :href="knife4jUrl" target="_blank" rel="noopener noreferrer">
          <el-icon><Link /></el-icon>
          Knife4j API
        </el-link>
        <p class="app-footer-hint">{{ backendUrl }}</p>
      </div>
    </el-aside>

    <el-container class="app-main-shell">
      <el-header class="app-header" height="68px">
        <div>
          <h1 class="app-header__title">{{ route.meta.title }}</h1>
        </div>
        <el-tag class="app-header__tag" effect="plain" round>{{ apiHint }}</el-tag>
      </el-header>

      <el-main class="app-main">
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
import { useRoute } from 'vue-router'
import { backendUrl, knife4jUrl } from './config'

const route = useRoute()

const apiHint = computed(() => (import.meta.env.DEV ? 'API 代理 → :8080' : ':8080'))

const activeMenu = computed(() => {
  if (route.path === '/qa' || route.path === '/rag' || route.path === '/search') return '/qa'
  if (
    route.path === '/ingest' ||
    route.path === '/upload' ||
    route.path === '/collect' ||
    route.path.startsWith('/vector-libraries') ||
    route.path === '/documents'
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
