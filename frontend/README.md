# 知库前端（knowbase-ui）

统一控制台 **knowbase-ui**，对接单体后端 `knowbase-service`（默认 `http://localhost:8080`）。

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
cd frontend\knowbase-ui
npm install
npm run dev
```

浏览器：

- 本机：http://localhost:5173
- 局域网：`npm run dev` 启动后会打印 `Network: http://192.168.x.x:5173`

开发模式下 API 经 Vite 代理到本机 `8080`。

Knife4j：侧栏链接或 http://\<服务器IP\>:8080/doc.html
