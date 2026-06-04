<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <el-icon><DataAnalysis /></el-icon>
        <span>向量检索</span>
      </div>
      <el-menu
        class="aside-menu"
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="#ecfdf5"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/rag">
          <el-icon><ChatLineRound /></el-icon>
          <span>RAG 问答</span>
        </el-menu-item>
        <el-menu-item index="/search">
          <el-icon><Search /></el-icon>
          <span>语义检索</span>
        </el-menu-item>
        <el-menu-item index="/rebuild">
          <el-icon><Refresh /></el-icon>
          <span>补偿重索引</span>
        </el-menu-item>
        <el-menu-item index="/purge">
          <el-icon><Delete /></el-icon>
          <span>清理向量</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <el-link class="footer-link" :href="knife4jUrl" target="_blank" rel="noopener noreferrer">
          Knife4j API
        </el-link>
        <p class="hint">{{ backendUrl }}</p>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <h1>{{ route.meta.title }}</h1>
        <el-tag type="success">vector-index-service</el-tag>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { backendUrl, knife4jUrl } from './config'

const route = useRoute()
</script>

<style>
html, body, #app {
  margin: 0;
  height: 100%;
  font-family: 'Segoe UI', system-ui, sans-serif;
}
.layout {
  height: 100vh;
}
.aside {
  background: #134e4a;
  color: #ecfdf5;
  display: flex;
  flex-direction: column;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 16px;
  font-size: 18px;
  font-weight: 600;
  color: #f0fdfa;
}
.brand .el-icon {
  color: #5eead4;
  font-size: 22px;
}
.aside-menu {
  border-right: none;
  flex: 1;
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.1);
  --el-menu-active-color: #ffffff;
}
.aside-menu .el-menu-item {
  margin: 4px 10px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  font-size: 14px;
  font-weight: 500;
  color: #d1fae5;
}
.aside-menu .el-menu-item .el-icon {
  color: #99f6e4;
}
.aside-menu .el-menu-item:hover {
  color: #ffffff;
  background-color: rgba(255, 255, 255, 0.1);
}
.aside-menu .el-menu-item:hover .el-icon {
  color: #ccfbf1;
}
.aside-menu .el-menu-item.is-active {
  color: #ffffff;
  background-color: #0f766e;
}
.aside-menu .el-menu-item.is-active .el-icon {
  color: #5eead4;
}
.aside-footer {
  padding: 16px;
  border-top: 1px solid #0f766e;
}
.footer-link {
  color: #99f6e4 !important;
  font-weight: 500;
}
.footer-link:hover {
  color: #ccfbf1 !important;
}
.hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #a7f3d0;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
}
.header h1 {
  margin: 0;
  font-size: 20px;
}
.el-main {
  background: #f8fafc;
}
</style>
