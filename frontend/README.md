# 知库前端（knowbase-ui）

统一控制台 **knowbase-ui**，对接知库 API 服务 **knowbase-app**（默认 `http://localhost:8010`）。

## 与 kanhai 宿主分离（方案 B）

| 服务 | 端口 | 说明 |
|------|------|------|
| kanhai 宿主 | 8080 | 业务系统；`knowbase.web.expose-controllers: false` |
| knowbase-app | 8010 | 知库 REST API；供 knowbase-ui 使用 |
| knowbase-ui | 5173 | 本前端；Vite 代理 `/api` → **8010** |

本地 `.env` 已配置 `VITE_DEV_PROXY_TARGET=http://127.0.0.1:8010`，**不要**把前端代理指到 kanhai 的 8080。

## 页面

| 菜单组 | 路由 | 功能 |
|--------|------|------|
| 知识库 | `/vector-libraries` | 新增知识库、列表、行点击进入详情 |
| 文档采集 | `/documents` | 按知识库列表、详情、进度轮询、补偿重索引 |
| 文档采集 | `/ingest` | 从知识库详情进入后上传/采集（含流水线预览、大文件异步） |
| 智能问答 | `/qa` | 在指定知识库内 RAG 问答与检索片段调试 |

全局上下文：`libraryId`、`tenantId` 保存在 `localStorage`，各页共享。

旧路由重定向：`/rag` → `/qa`，`/search` → `/qa?tab=search`，`/query` → `/documents?docId=…&poll=1`。

## 启动

```powershell
# 1. 知库 API（8010）
.\scripts\start-services.ps1

# 2. 前端（5173）
cd frontend\knowbase-ui
npm install
npm run dev
```

浏览器：

- 本机：http://localhost:5173
- 局域网：`npm run dev` 启动后会打印 `Network: http://192.168.x.x:5173`

开发模式下 API 经 Vite 代理到本机 **8010**（`knowbase-app`）。

Knife4j：侧栏链接或 http://\<服务器IP\>:5173/doc.html（开发代理） / http://\<服务器IP\>:8010/doc.html
