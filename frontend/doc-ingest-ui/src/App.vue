<template>
  <el-container class="layout">
    <el-aside width="232px" class="aside">
      <div class="brand">
        <div class="brand-icon">
          <el-icon><Document /></el-icon>
        </div>
        <div class="brand-text">
          <span class="brand-title">文档接入</span>
          <span class="brand-sub">doc-ingest-service</span>
        </div>
      </div>
      <el-menu
        class="aside-menu"
        :default-active="route.path"
        router
        background-color="transparent"
        text-color="#e2e8f0"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/documents">
          <el-icon><Folder /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/ingest">
          <el-icon><Upload /></el-icon>
          <span>文档收集上传</span>
        </el-menu-item>
        <el-menu-item index="/query">
          <el-icon><Search /></el-icon>
          <span>查询状态</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <el-link class="footer-link" :href="knife4jUrl" target="_blank" rel="noopener noreferrer">
          <el-icon><Link /></el-icon>
          Knife4j API
        </el-link>
        <p class="hint">{{ backendUrl }}</p>
      </div>
    </el-aside>
    <el-container class="main-shell">
      <el-header class="header">
        <div class="header-left">
          <h1>{{ route.meta.title }}</h1>
          <p class="header-desc">{{ pageDesc }}</p>
        </div>
        <el-tag class="service-tag" effect="plain" round>8081</el-tag>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { backendUrl, knife4jUrl } from './config'

const route = useRoute()

const pageDesc = computed(() => {
  const map = {
    '/documents': '浏览、筛选与管理已接入文档',
    '/ingest': '本地文件上传或 URL 远程采集',
    '/query': '按文档 ID 查看解析与索引进度'
  }
  return map[route.path] || ''
})
</script>

<style>
html,
body,
#app {
  margin: 0;
  height: 100%;
  font-family: 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

.layout {
  height: 100vh;
}

.aside {
  background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%);
  color: #e2e8f0;
  display: flex;
  flex-direction: column;
  box-shadow: 4px 0 24px rgba(15, 23, 42, 0.12);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 22px 18px;
}

.brand-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #0ea5e9 0%, #2563eb 100%);
  color: #fff;
  font-size: 20px;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title {
  font-size: 17px;
  font-weight: 600;
  color: #f8fafc;
}

.brand-sub {
  font-size: 11px;
  color: #94a3b8;
}

.aside-menu {
  border-right: none;
  flex: 1;
  padding: 0 8px;
  --el-menu-hover-bg-color: rgba(148, 163, 184, 0.16);
  --el-menu-active-color: #ffffff;
}

.aside-menu .el-menu-item {
  margin: 4px 4px;
  border-radius: 10px;
  height: 46px;
  line-height: 46px;
  font-size: 14px;
  font-weight: 500;
  color: #e2e8f0;
}

.aside-menu .el-menu-item .el-icon {
  color: #94a3b8;
}

.aside-menu .el-menu-item:hover {
  color: #f8fafc;
  background-color: rgba(148, 163, 184, 0.16);
}

.aside-menu .el-menu-item:hover .el-icon {
  color: #e2e8f0;
}

.aside-menu .el-menu-item.is-active {
  color: #ffffff;
  background: linear-gradient(90deg, rgba(56, 189, 248, 0.25) 0%, rgba(51, 65, 85, 0.9) 100%);
}

.aside-menu .el-menu-item.is-active .el-icon {
  color: #38bdf8;
}

.aside-footer {
  padding: 18px;
  border-top: 1px solid rgba(71, 85, 105, 0.6);
}

.footer-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #7dd3fc !important;
  font-weight: 500;
  font-size: 13px;
}

.footer-link:hover {
  color: #bae6fd !important;
}

.hint {
  margin: 10px 0 0;
  font-size: 11px;
  color: #94a3b8;
  word-break: break-all;
}

.main-shell {
  background: #eef2f7;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 72px;
  padding: 0 28px;
  border-bottom: none;
  background: #ffffff;
  box-shadow: 0 1px 0 #e2e8f0;
}

.header-left h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #0f172a;
  letter-spacing: -0.02em;
}

.header-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #64748b;
}

.service-tag {
  border-color: #e2e8f0 !important;
  color: #475569 !important;
  background: #f8fafc !important;
  font-family: ui-monospace, monospace;
}

.main-content {
  padding: 24px 28px 32px;
  background: linear-gradient(160deg, #eef2f7 0%, #f8fafc 50%, #f1f5f9 100%);
}
</style>
