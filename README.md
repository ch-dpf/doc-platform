# KnowBase RAG 平台

KnowBase 是面向内部知识管理场景的 RAG 平台，提供知识库建设、文档入库、向量检索、多知识库编排与智能问答能力。

项目采用 Java 21、Spring Boot 3.2、MyBatis-Plus、PostgreSQL、pgvector、Flyway、Ollama、Vue 3、Vite、Element Plus、Axios 与 Vue Router 构建，支持独立运行和宿主服务引入两种形态。

## 核心流程

1. 建仓入库：创建知识库，按库类型预设生成默认 Profile；文档持续入库（tokenizer 驱动切块、Embedding、pgvector 写入 **active 索引代次**），日常路径下文档 `INDEXED` 即可检索。
2. 智能体编排：创建知识智能体，绑定一个或多个知识库，叠加场景规则预设、路由策略、检索策略与回答策略。
3. 智能问答：基于智能体执行多库路由、检索、证据融合、上下文拼装、回答生成、引用返回与运行轨迹记录。

## 模块结构

- `knowbase-api`：公共 Command、Result、Facade 与 SPI 契约。
- `knowbase-domain`：领域模型、状态与仓储接口。
- `knowbase-application`：应用服务、Facade 实现与问答 Pipeline。
- `knowbase-ingestion`：文档加载、解析、token 分块与入库 Pipeline。
- `knowbase-retrieval`：检索、证据构建与上下文拼装。
- `knowbase-agent`：知识智能体、多库路由与场景编排。
- `knowbase-preset`：库类型预设、文档 Profile 默认值与场景规则预设。
- `knowbase-model`：Embedding 与 Chat 模型适配。
- `knowbase-tokenizer`：模型 tokenizer 注册、token 计数与 token 窗口切分。
- `knowbase-persistence`：PostgreSQL、pgvector、MyBatis 与 Flyway 持久化。
- `knowbase-web`：REST API、Swagger/Knife4j 与异常处理。
- `knowbase-autoconfigure`：Spring Boot 自动配置。
- `knowbase-starter`：宿主服务引入入口。
- `knowbase-app`：独立运行应用。
- `frontend/knowbase-ui`：Vue 管理控制台。
- `infra`、`scripts`：本地基础设施与验证脚本。

当前 ingestion core 接口演进：

- `DocumentParser`：解析器 SPI，按 `sourceUri` 与 `mimeType` 选择 Markdown、HTML、PDF、Word、Excel、OCR、ZIP 等解析器，表格默认走 `table-deep`。
- `DocumentNormalizer`：清洗阶段接口，默认 `DocumentTextNormalizer` 执行文本归一化和结构块清洗。
- `DocumentMetadataEnricher`：元数据增强接口，默认实现补充块统计、首标题、Profile 与 chunk/token 配置上下文。
- `DocumentChunker`：切分阶段接口，默认 `TokenBasedDocumentChunker` 统一为语义边界优先、token 预算约束、字符切分兜底。
- `DocumentPreparationPipeline`：入库准备编排，将加载、解析、结构增强、清洗、元数据增强和切分串联为可分阶段验证的流程。

## 独立运行

启动基础设施：

```powershell
docker compose up -d postgres minio ollama
```

构建并启动后端：

```powershell
mvn -q "-Dmaven.repo.local=.m2/repository" -DskipTests package
.\scripts\start-app.ps1 -Port 8080 -SkipPackage
```

启动前端控制台：

```powershell
.\scripts\start-ui.ps1
```

访问入口：

- 管理控制台：`http://localhost:5173`
- REST API：`http://localhost:8080/api/v1`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- Knife4j：`http://localhost:8080/doc.html`

管理控制台主路由：

| 路径 | 功能 |
|------|------|
| `/home` | 首页入口 |
| `/libraries` | 知识库分页列表、建库（库类型预设与 product-guide） |
| `/libraries/:libraryId/documents` | 文档列表、上传入库向导（快捷上传或分步：upload-batch → prepare → ingestion-runs） |
| `/libraries/:libraryId/documents/:documentId` | 文档详情：原文预览、chunk 分页编辑、pipeline trace |
| `/libraries/:libraryId/retrieval-test` | 库级召回测试、黄金集 CRUD、Recall@K 评测 |
| `/libraries/:libraryId/settings` | Library / Document Profile、索引健康、重复文档、批量重索引 |
| `/libraries/:libraryId/acl` | 库级 ACL |
| `/agents` | 智能体创建、版本发布/禁用、检索测试 |
| `/qa` | 基于已发布智能体版本的单次问答（`POST /query-runs`） |
| `/observability` | Pipeline Trace 与 Observability 评测运行 |
| `/presets` | 库类型 / 场景规则预设管理 |

REST 一键上传入库：`POST .../documents` 或 `POST .../ingestion-runs/upload`；索引代次运维（rebuild / promote）见 [API.md](docs/API.md) §3.5，当前控制台未提供对应 UI。

也可以在后端打包后使用 root compose 启动应用服务：

```powershell
mvn -q "-Dmaven.repo.local=.m2/repository" -DskipTests package
docker compose up -d
```

## 宿主服务引入

宿主服务添加依赖：

```xml
<dependency>
    <groupId>com.knowbase</groupId>
    <artifactId>knowbase-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

默认模式为 Facade-only：自动装配应用服务与 Facade，不暴露 REST Controller，不强制引入 Web/JDBC/Flyway 运行栈。

```yaml
knowbase:
  enabled: true
  web:
    exposed: false
  persistence:
    enabled: false
```

宿主需要持久化时，显式引入 `knowbase-persistence`、数据库驱动与 datasource 配置，并开启：

```yaml
knowbase:
  persistence:
    enabled: true
    schema: knowbase          # 可选；嵌入宿主时建议独立 PostgreSQL schema
  flyway:
    enabled: true
    table: knowbase_flyway_schema_history
```

宿主需将 `spring.flyway.locations` 限定为自身业务脚本目录（如 `classpath:db/flyway`），避免与 KnowBase 的 `classpath:db/migration` 混用同一 Flyway 历史表。KnowBase 在宿主模式下默认使用独立 history 表；若库中已存在 `kb_*` 表但无 history 记录，会自动 baseline 到最新脚本版本。

宿主需声明自身 Mapper 扫描包（与 KnowBase 的 `@MapperScan` 并存），例如：

```yaml
mybatis-plus:
  mapper-packages: org.example.app.mapper
```

或通过 `@MapperScan("org.example.app.mapper")` 注册宿主 Mapper。可选：实现 `KnowbaseTenantResolver` Bean 覆盖默认租户解析（否则使用 `knowbase.tenant.default-tenant-id`）。

宿主需要直接暴露 KnowBase REST API 时，显式引入 `knowbase-web` 并开启：

```yaml
knowbase:
  web:
    exposed: true
```

入库任务默认同步执行，便于本地验证和脚本调试。处理大批量异构文件时可开启后台执行：

```yaml
knowbase:
  ingestion:
    async-enabled: true
    async-pool-size: 2
```

开启后，创建入库任务接口会先返回任务 ID，任务状态可通过 `GET /api/v1/ingestion-runs/{runId}` 查询；前端控制台与验证脚本会自动轮询直到进入终态。

入库与准备链路会在后端标准输出打印**中文结构化日志**（如 `入库任务开始`、`文档加载开始`、`向量化完成`）。按 `runId=` 或 `sourceUri=` 过滤即可跟踪单次任务；完整消息列表见 [docs/INGESTION_INTERFACES.md](docs/INGESTION_INTERFACES.md) §结构化日志。

可注入的 Facade：

- `KnowbaseLibraryFacade`
- `KnowbaseIngestionFacade`
- `KnowbaseAgentFacade`
- `KnowbaseQuestionFacade`
- `KnowbasePresetFacade`

## 集成验证

后端服务启动后，可执行真实 API 纵向验证：

```powershell
.\scripts\verify-postgres-rag.ps1 -BaseUrl http://localhost:8080
```

验证内容包括：

- 创建知识库。
- 执行 inline 文档入库。
- 通过 Flyway/pgvector 持久化 chunk 与向量。
- 创建知识智能体。
- 发起问答并返回证据、引用与回答。

如需使用本机样例目录验证异构文档入库，可执行：

```powershell
.\scripts\verify-sample-documents.ps1 -BaseUrl http://localhost:8080 -DocumentRoot D:\document -MaxFiles 12
```

该脚本会提交 `file://D:/document` 目录来源，后端按扩展名自动展开 Markdown、PDF、Word、Excel 等文件，并在同一个知识库内按文档 Profile 自动路由解析。默认只入库前 12 个样例文件，避免首次验证时批量处理过大；可通过 `-MaxFiles` 与 `-Extensions` 调整范围。

当前入库解析遵循主流 RAG 项目的分层方式：来源加载与目录扫描、文件类型识别、Profile/Parser 自动路由、结构优先分段、模型 tokenizer token 窗口切块、Embedding、向量写入 active 索引代次分阶段执行。默认支持 Markdown/TXT、PDF、Word、Excel/CSV、PPT、HTML、代码配置文件，并为后续 OCR 图片解析与专用表格切分保留扩展点。

Tokenizer Profile 已作为一等配置对象暴露：

- `GET /api/v1/tokenizer-profiles`：查询当前可用 tokenizer profile。
- `POST /api/v1/tokenizer-profiles`：创建或更新模型 tokenizer 声明。
- 入库阶段优先使用文档 Profile 或知识库 Profile 绑定的 tokenizer profile。
- 问答阶段可在创建知识智能体时传入 `chatTokenizerProfileId`，用于 Chat 上下文 token 预算和返回的 token 轨迹。

智能体支持检索测试接口：

```powershell
POST /api/v1/agents/{agentId}/retrieval-tests
```

该接口不生成最终答案，只执行多库路由、向量检索、证据构建、引用生成与上下文 token 拼装，用于调试召回质量、上下文预算和 tokenizer 轨迹。

多库检索已支持可配置后处理策略：`fusion=rrf` 用于跨库排序融合，`rerank=mmr` 用于证据多样性重排，`balanceAcrossLibraries` 用于避免单库占满候选池，`contentFamilyWeights` 用于对表格、代码、富文本、网页等异构文档做轻量权重调节。内存模式与 PostgreSQL/pgvector 模式共用同一套后处理器，宿主服务也可以通过覆盖 `RetrievalPostProcessor` Bean 接入自定义重排策略。

**已实现的管理与运维能力**（详见 [API.md](docs/API.md)）：

- 知识库 CRUD；文档分页 / 详情 / 预览 / 下载 / 批量删除；chunk 分页与编辑；文档 pipeline trace。
- 索引代次运维 API（`index-generations` 列表 / rebuild / promote、健康检查、promote 门禁）；Library Profile 与 Document Profile 版本管理。
- ObjectStorage 上传；`POST .../documents` 或 `ingestion-runs/upload` 一键入库；分阶段 `preview` / `prepare`。
- 库级召回测试与黄金集评测（Recall@K、基线、generate-drafts）；智能体版本生命周期；库级 ACL；Observability Trace / 评测运行。
- 多轮 Chat 会话 REST API（`/api/v1/chat/sessions`）— 后端已实现，控制台问答页当前使用单次 `POST /query-runs`。

架构演进背景与迁移记录见 [DESIGN_EVOLUTION_OUTLINE.md](docs/DESIGN_EVOLUTION_OUTLINE.md)（Wave 3 文档一等入库与 `index-generations` 命名已完成；后续能力见该文档 Wave 4+）。

## 文档

- [总体设计规划](docs/DESIGN.md)
- [接口规范](docs/API.md)
- [Ingestion 接口说明](docs/INGESTION_INTERFACES.md)
- [二期复杂文档解析与分段方案](docs/PHASE2_INGESTION_PLAN.md)
- [架构演进修订提纲](docs/DESIGN_EVOLUTION_OUTLINE.md)

## 核心接口自检

当前 ingestion 接口与清洗/元数据边界可直接运行模块测试：

```bash
mvn -pl knowbase-ingestion -Dtest=IngestionInterfaceBoundaryTest,DocumentTextNormalizerTest test
```
