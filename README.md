# doc-platform

单体文档平台：**文档采集入库**、**向量索引检索** 与 **RAG 问答** 统一在 `doc-platform-service`（端口 **8080**）；前端统一为 `doc-platform-ui`（端口 **5173**）。

> 原 `doc-ingest-service` + `vector-index-service` + Kafka + `doc-platform-contract` 已合并为进程内直接调用，功能与 API 路径保持不变。

默认工作目录：`D:\workspace\doc-platform`

---

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 / 运行时 | **JDK 21** |
| 应用框架 | **Spring Boot 3.2.4**（单体） |
| 持久化 | **MyBatis-Plus 3.5.7**（PostgreSQL 单 schema `public`） |
| API 文档 | **Knife4j 4.4.0** |
| 对象存储 | **MinIO** 或 **本地文件系统**（`storage.type` 可配置） |
| 关系库 | **PostgreSQL 16 + pgvector** |
| 文本解析 | **Apache Tika** |
| Embedding | **Ollama `nomic-embed-text`** |
| RAG 对话 | **Ollama `llama3.2`** |
| 前端 | **Vue 3 + Vite + Element Plus**（`doc-platform-ui`） |

**不再依赖**：Kafka、`doc-platform-contract` 独立模块。

---

## 模块结构

| 路径 | 说明 |
|------|------|
| `doc-platform-service/` | 单体后端（`com.docplatform.ingest.*` + `com.docplatform.vector.*`） |
| `frontend/doc-platform-ui/` | 统一前端控制台 |
| `infra/postgres/` | 数据库初始化与迁移 SQL |
| `scripts/` | 构建、基础设施、启动、E2E |

包内衔接层：`com.docplatform.platform.DocumentIndexCoordinator`（替代原 Kafka 事件总线）。

---

## 业务流程总览

### 文档采集（离线 / 异步，按知识库）

前端侧栏：**知识库管理**、**智能问答**。**文档采集**、文档详情等从知识库详情进入，不在侧栏。

所有采集/检索/RAG 请求需携带 **`libraryId`**（默认库 `00000000-0000-0000-0000-000000000001`）。

```mermaid
flowchart LR
    subgraph prep["建仓准备"]
        L[vector_library]
    end
    subgraph app["doc-platform-service"]
        A[上传 / URL 采集] --> B[对象存储 原文]
        B --> C[doc_metadata]
        C --> D[解析 + 文本清洗]
        D --> E[parsed.txt]
        E --> F[协调器 异步]
        F --> G[分块 + Embedding]
        G --> H[(document_chunk)]
        F --> I[index_status INDEXED]
    end
    prep --> A
```

1. **知识库**（API：`vector-libraries`）：`GET/POST /api/v1/vector-libraries`；`PUT /{libraryId}` 可更新名称、分块/清洗规则、**库级 Embedding 模型与维度**（不改存储与数据源）；入库与检索按库 `config.embeddingModel` 调用 Ollama
2. **入库流水线**（代码固定）：数据源接入 → 解析 → 清洗 → 分块 → 向量化 → 入库，无编排表与步骤开关
3. **采集**：`POST .../upload`、`/upload/batch`、`/upload/async`（大文件）、`/collect`；仅文档 MIME
4. **解析**（异步）：Tika + `ingest.text-normalization`
5. **索引**（异步）：固定执行分块、向量化并写入 `document_chunk`（清洗是否执行由知识库 `textNormalizationEnabled` 控制）
6. **问答/检索**：`POST /api/v1/search`、`/rag/chat` 需 `libraryId`

### 智能问答（在线 / 实时）

前端菜单：**智能问答**（问答 + 检索片段调试）。

| 能力 | API |
|------|-----|
| RAG 问答 | `POST /api/v1/rag/chat`（可选 `chatModel` 覆盖全局对话模型） |
| 语义检索 | `POST /api/v1/search`（查询向量按库 Embedding 配置生成） |
| 补偿重索引 | `POST /api/v1/index/rebuild`（也可在文档库详情触发） |

无命中时 RAG 返回以 **「未找到：」** 开头的固定文案，不调用 LLM，避免编造。

---

## 基础设施与端口

| 组件 | 端口 | 说明 |
|------|------|------|
| PostgreSQL + pgvector | 5432 | 库 `docplatform`，单 schema `public`（见 `infra/postgres/init.sql`） |
| MinIO | 9000 / 9001 | 桶 `documents` |
| Ollama | 11434 | `nomic-embed-text` + `llama3.2` |
| **doc-platform-service** | **8080** | Knife4j：`/doc.html` |
| **doc-platform-ui** | **5173** | 统一控制台 |

`docker-compose.yml` 仅包含 **Postgres、MinIO、Ollama**（已移除 Kafka）。

---

## 快速启动

```powershell
cd D:\workspace\doc-platform
.\scripts\build.ps1
.\scripts\start-infra.ps1    # 或本机安装后 .\scripts\infra-check.ps1
.\scripts\start-services.ps1 # 启动 8080

cd frontend\doc-platform-ui
npm install
npm run dev                  # http://localhost:5173
```

```powershell
java -jar doc-platform-service\target\doc-platform-service-1.0.0-SNAPSHOT.jar
```

---

## HTTP API（路径未变）

### 知识库

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` / `POST` | `/api/v1/vector-libraries` | 列表 / 新增知识库 |
| `GET` / `PUT` | `/api/v1/vector-libraries/{libraryId}` | 详情 / 更新预处理·分块·向量化配置 |
| `GET` | `/api/v1/upload-tasks/{taskId}` | 大文件异步入库任务状态 |

### 文档采集 — `/api/v1/documents`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/parse-preview` | 建仓向导：Tika 解析预览 |
| `GET` | `/upload-constraints?libraryId=` | 上传限制 |
| `POST` | `/upload?libraryId=` | 单文件上传 |
| `POST` | `/upload/async?libraryId=` | 大文件异步上传 |
| `POST` | `/upload/batch?libraryId=` | 批量上传 |
| `POST` | `/collect` | URL 采集（body 含 `libraryId`） |
| `GET` | `/?libraryId=` | 分页列表 |
| `GET` | `/{docId}` | 元数据 |
| `DELETE` | `/{docId}` | 软删除 |
| `DELETE` | `/{docId}/purge` | 物理删除 |

### 向量 / RAG — `/api/v1`

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/search` | 语义检索（body 含 `libraryId`） |
| `POST` | `/rag/chat` | RAG 问答（body 含 `libraryId`） |
| `POST` | `/index/chunk-preview` | 分块规则预览 |
| `POST` | `/index/rebuild` | 补偿重索引（body 含 `libraryId`） |
| `DELETE` | `/index/{docId}` | 清理向量 |

---

## 数据库（单 schema 重建）

全新环境：`docker compose` 首次启动会执行 `infra/postgres/init.sql`。

**一键重置（推荐，会清空全部业务数据）**：

```powershell
.\scripts\reset-db.ps1
```

重建 Postgres 容器（空库自动初始化）：

```powershell
.\scripts\reset-db.ps1 -RecreateContainer
```

手动执行 SQL 见 `infra/postgres/recreate-single-schema.sql` → `drop-public-tables.sql` → `init.sql`。

表：`vector_library`、`upload_task`、`doc_metadata`、`document_chunk`、`document_index_job`、`processed_event`（均在 `public`）。

已有库若仍含 `ingest_orchestration`，可执行 `infra/postgres/migrate-drop-orchestration.sql` 清理废弃表。

### 中文乱码排查（Windows 常见）

原因多为：**SQL 脚本按 GBK 写入库** 或 **JDBC/psql 客户端编码不是 UTF-8**。项目已统一为 UTF-8（`application.yml`、本机 `reset-db.ps1` 管道、`start-services.ps1` 的 `-Dfile.encoding=UTF-8`）。

1. 检查当前库编码与样本数据：

```powershell
.\scripts\check-db-encoding.ps1
# 本机 PostgreSQL：
.\scripts\check-db-encoding.ps1 -UseLocalPsql
```

2. **已乱码的数据无法自动修复**，需用 UTF-8 重新灌库：

```powershell
.\scripts\reset-db.ps1
# Docker 且希望空库重建：
.\scripts\reset-db.ps1 -RecreateContainer -SkipConfirm
```

3. 重新编译并启动后端（务必用 `.\scripts\start-services.ps1`，不要裸 `java -jar` 省略编码参数）。

## 配置要点

- 数据源：`jdbc:postgresql://localhost:5432/docplatform`，`search_path=public`
- `storage.type`：`minio`（默认）或 `local-fs`；`storage.path-prefix` / `storage.local.base-path`
- `ingest.max-file-size`、`ingest.max-batch-files`、`ingest.allowed-mime-types`
- `ingest.text-normalization.*`：解析后文本清洗（配置前缀，非 DB schema）
- `embedding.provider`：向量化实现（一期 `ollama`）
- `chunking.*`：分块策略（默认 `paragraph-first`）
- `rag.*` / `ollama.*`：RAG 与模型配置

---

## 解析规范化与分块

| 环节 | 配置 | 作用 |
|------|------|------|
| Tika 抽取 | — | 多格式 → 纯文本 |
| 文本规范化 | `ingest.text-normalization` | 入库前清洗 |
| 向量分块 | `chunking` | 段落优先 + 长度兜底 |

修改分块规则后需 **rebuild** 或重新上传升版本。

---

## 运维脚本

见 [scripts/README.md](scripts/README.md)、[scripts/github-sync.md](scripts/github-sync.md)。

```powershell
.\scripts\e2e-test.ps1   # 针对 localhost:8080
```

---

## 相关文档

- [frontend/README.md](frontend/README.md)
- Knife4j：http://localhost:8080/doc.html
