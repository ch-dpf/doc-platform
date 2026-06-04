# doc-platform 前端控制台

两个独立的 Vue 3 小项目，分别对接 **doc-ingest-service (8081)** 与 **vector-index-service (8082)**。

| 项目 | 端口 | 后端 | 覆盖接口 |
|------|------|------|----------|
| [doc-ingest-ui](./doc-ingest-ui) | 5173 | :8081 | 文档管理、收集上传、状态查询、删除 |
| [doc-vector-ui](./doc-vector-ui) | 5174 | :8082 | RAG 问答、语义检索、补偿重索引、清理向量 |

## 启动

先启动对应 Java 服务，再分别安装依赖并运行：

```powershell
# 文档接入
cd D:\workspace\doc-platform\frontend\doc-ingest-ui
npm install
npm run dev

# 向量检索（新终端）
cd D:\workspace\doc-platform\frontend\doc-vector-ui
npm install
npm run dev
```

浏览器访问：

- 文档接入：http://localhost:5173
- 向量检索：http://localhost:5174

**doc-ingest-ui 页面**：文档管理 `/documents`、文档收集上传 `/ingest`、查询状态 `/query`

**doc-vector-ui 页面**：RAG 问答 `/rag`、语义检索 `/search`、补偿重索引 `/rebuild`、清理向量 `/purge`

开发模式：`/api` 走 Vite 代理；侧栏 **Knife4j** 直接打开后端地址（`http://localhost:8081/doc.html` 或 `:8082`）。

可在 `.env` 中修改后端地址：

```env
VITE_BACKEND_URL=http://localhost:8081
```

## 构建

```powershell
npm run build
npm run preview
```
