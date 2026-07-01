# KnowBase 接口规范

## 1. 接口分层

KnowBase 对外提供两类稳定接口：

1. Java Facade：供宿主服务通过 `knowbase-starter` 直接引入调用。
2. REST API：供独立运行模式、管理控制台和外部系统通过 HTTP 调用。

公共请求与响应契约位于 `knowbase-api` 模块，REST 控制器位于 `knowbase-web` 模块。应用用例位于 `knowbase-application` 模块，Facade 与 REST 都调用同一套应用用例，避免两套业务逻辑分叉。

## 2. Java Facade

### 2.1 知识库

接口：`KnowbaseLibraryFacade`

- `createLibrary(CreateLibraryCommand command)`
- `getLibrary(UUID libraryId)`
- `listLibraries(String tenantId)`
- `pageLibraries(String tenantId, int page, int size)`
- `deleteLibrary(UUID libraryId)`

用途：创建知识库、查询知识库详情、按租户查询或分页查询知识库列表、删除未被智能体引用的知识库。

### 2.2 入库运行

接口：`KnowbaseIngestionFacade`

- `createIngestionRun(CreateIngestionRunCommand command)`
- `getIngestionRun(UUID runId)`

用途：创建一次入库运行，并查询入库运行状态。

### 2.3 知识智能体

接口：`KnowbaseAgentFacade`

- `createKnowledgeAgent(CreateKnowledgeAgentCommand command)`
- `getKnowledgeAgent(UUID agentId)`
- `listKnowledgeAgents(String tenantId)`

用途：创建面向问答场景的知识智能体，并查询智能体配置入口。

### 2.4 智能问答

接口：`KnowbaseQuestionFacade`

- `ask(AskQuestionCommand command)`
- `getQueryRun(UUID queryRunId)`

用途：基于知识智能体执行一次问答运行，并查询问答运行结果。

### 2.5 预设管理

接口：`KnowbasePresetFacade`

- `pageLibraryTypePresets(String tenantId, int page, int size)`
- `getLibraryTypePreset(String tenantId, String code)`
- `listLibraryTypePresets()`
- `pageSceneRulePresets(String tenantId, int page, int size)`
- `getSceneRulePreset(String tenantId, String code)`
- `listSceneRulePresets()`

用途：分页查询、查看详情、创建与删除库类型预设和场景规则预设，供宿主服务或前端控制台在建库、创建智能体时选择或管理默认策略模板。

### 2.6 Tokenizer Profile

接口：`KnowbaseTokenizerProfileFacade`

- `createTokenizerProfile(CreateTokenizerProfileCommand command)`
- `getTokenizerProfile(UUID tokenizerProfileId)`
- `listTokenizerProfiles(String provider, boolean includeDisabled)`

用途：声明 Embedding 与 Chat 模型使用的 tokenizer 标识、版本与是否为近似 tokenizer，供入库分块和问答上下文预算统一引用。

## 3. REST API

REST API 统一前缀为 `/api/v1`，响应统一包装为：

```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {},
  "timestamp": "2026-06-17T00:00:00Z"
}
```

### 3.1 创建知识库

`POST /api/v1/libraries`

请求体：

```json
{
  "tenantId": "default",
  "name": "产品知识库",
  "description": "产品手册、FAQ 与排障资料",
  "libraryTypePresetCode": "product_knowledge",
  "tags": ["product", "faq"],
  "profile": {
    "embeddingProvider": "ollama",
    "embeddingModel": "bge-m3",
    "embeddingDimension": 1024,
    "embeddingTokenizerProfileId": null,
    "chunkMaxTokens": 512,
    "chunkOverlapTokens": 80,
    "retrievalTopK": 8,
    "options": {}
  },
  "documentProfiles": [
    {
      "contentFamily": "RICH_TEXT",
      "parserCode": "tika",
      "chunkingStrategy": "structure_token_window",
      "tokenizerProfileId": null,
      "metadataSchema": {},
      "options": {}
    }
  ]
}
```

响应 `data`：`LibraryResult`

### 3.2 查询知识库

`GET /api/v1/libraries/{libraryId}`

响应 `data`：`LibraryResult`

### 3.3 分页查询知识库

`GET /api/v1/libraries?tenantId=default&page=1&size=10`

响应 `data`：`PageResult<LibraryResult>`

说明：`page` 从 1 开始，`size` 默认 10、上限 100。列表结果会按 ACL 过滤，仅返回当前请求上下文有 `READ` 权限的知识库。

### 3.4 删除知识库

`DELETE /api/v1/libraries/{libraryId}`

响应 `data`：`null`

说明：需要对该知识库具备 `ADMIN` 权限。若知识库仍被智能体引用，返回业务错误。

### 3.5 知识库索引与文档目录

控制器：`LibraryCatalogController`，路径前缀 `/api/v1/libraries/{libraryId}`。

#### 查询文档列表

`GET /api/v1/libraries/{libraryId}/documents?page=1&size=20&indexVersionId=`

响应 `data`：`PageResult<KnowledgeDocumentResult>`（`items`、`total`、`page`、`size`；每项含 `status`、`chunkCount`、`lastIndexedAt`）

说明：`page` 从 1 开始，`size` 默认 20、上限 100。`indexVersionId` 可选；**默认省略**时只返回当前 `active` 索引代次下的文档。日常入库不再 bump 代次，文档 `INDEXED` 即可检索。

#### 删除文档

`DELETE /api/v1/libraries/{libraryId}/documents/{documentId}`

说明：同步删除 chunk 与 embedding。

#### 批量删除文档

`POST /api/v1/libraries/{libraryId}/documents/batch-delete`

请求体：

```json
{
  "documentIds": ["00000000-0000-0000-0000-000000000001"]
}
```

响应 `data`：`BatchDeleteDocumentsResult`（`deletedCount`、`deletedDocumentIds`）

说明：跳过不属于当前知识库或已不存在的 ID。

#### 重索引文档

`POST /api/v1/libraries/{libraryId}/documents/{documentId}/reindex`

响应 `data`：`IngestionRunResult`

说明：按文档 `sourceUri` 在当前 active 代次内 upsert 块与向量。

#### 上传文档并入库

`POST /api/v1/libraries/{libraryId}/documents`（`multipart/form-data`）

表单字段：

| 字段 | 必填 | 说明 |
|------|------|------|
| `files` | 是 | 一个或多个文件 |
| `documentProfileCode` | 否 | 文档 Profile 编码 |
| `autoStart` | 否 | 默认 `true`；上传成功后自动创建入库任务 |

响应 `data`：`DocumentUploadResult`（`upload`、`ingestionRun`、`documents`）

说明：文件先写入 ObjectStorage，再按 active 索引代次 upsert 文档与块；与 `ingestion-runs/upload` 等价但路径挂在文档目录下。

#### 索引代次运维

`GET /api/v1/libraries/{libraryId}/index-generations` — 索引代次列表

`POST /api/v1/libraries/{libraryId}/index-generations/rebuild?autoPromote=false`

响应 `data`：`IndexGenerationRebuildResult`（`generation`、`ingestionRun`、`previousActiveId`、`promoted`）

说明：在当前 active 代次文档来源基础上，于新建 `BUILDING` 代次内全量重建；`autoPromote=true` 时成功后自动 promote 并切换 active 指针。

`POST /api/v1/libraries/{libraryId}/index-generations/{indexGenerationId}/promote?force=false`

`GET /api/v1/libraries/{libraryId}/index-health` — L1 Profile 漂移检测，提示 rebuild

`GET /api/v1/libraries/{libraryId}/index-generations/{id}/promote-readiness` — promote 前 blockers/warnings

`GET /api/v1/libraries/{libraryId}/index-generations/promote-eval-gate` — promote 硬门禁：黄金集 Recall@K（默认阈值 85%）+ 相对基线回落 ≤2%；未配置样本则阻断

`POST /api/v1/libraries/{libraryId}/profiles` — 发布 Library Profile 新版本（L1/L2 变更提示）

`GET /api/v1/libraries/{libraryId}/profiles` — Profile 版本历史

`GET /api/v1/libraries/{libraryId}/profiles/{profileId}` — 指定 Library Profile 版本详情

`GET|POST|PUT|DELETE /api/v1/libraries/{libraryId}/document-profiles` — Document Profile CRUD

`GET /api/v1/libraries/{libraryId}/document-profiles/{code}` — 按编码查询单个 Document Profile

`POST /api/v1/libraries/{libraryId}/retrieval-eval-samples/import` — JSON 批量导入黄金集

`POST /api/v1/libraries/{libraryId}/retrieval-eval-samples/bootstrap-sample-documents` — 从 sample-documents 引导样本

`POST /api/v1/libraries/{libraryId}/retrieval-eval-samples/generate-drafts` — 基于入库结果自动生成评测样本草稿

`GET /api/v1/libraries/{libraryId}/retrieval-eval-baseline` — 回归基线

`POST /api/v1/libraries/{libraryId}/retrieval-evaluations/{evalRunId}/baseline` — 手动固定基线

`GET /api/v1/libraries/{libraryId}/profile` — 最新 Library Profile 与 L1 漂移摘要

`POST /api/v1/libraries/{libraryId}/retrieval-tests` — 库级召回测试（无需智能体）；请求体可附带 `expectedDocumentIds` / `expectedSourceUris` / `groundTruthContexts` / `hitRank` 做 Hit@K 判定，响应 `hitCheck`

`GET|POST|PUT|DELETE /api/v1/libraries/{libraryId}/retrieval-eval-samples` — 黄金集 CRUD（表 `kb_retrieval_eval_sample`）

`GET|POST /api/v1/libraries/{libraryId}/retrieval-evaluations` — 批量 Recall@K 评测与历史列表；运行结果含 `mrr`、`contextPrecisionAtK`、`stratifiedRecall`（按 contentFamily）

`GET /api/v1/libraries/{libraryId}/retrieval-evaluations/{evalRunId}` — 单次评测明细（含每题 hit/matchType/trace.explain 检索解释）

`POST /api/v1/libraries/{libraryId}/retrieval-tests` 响应 `trace.explain` 为 Top-K 候选解释；`citations[].metadata` 含 pageNumber/bbox/vectorRank/keywordRank

`GET /api/v1/libraries/{libraryId}/ingestion-runs?limit=50` — 本库入库任务列表

`GET /api/v1/libraries/{libraryId}/ingestion-runs/{runId}/errors` — 本库任务文档级错误

`GET /api/v1/libraries/{libraryId}/ingestion-runs/{runId}/jobs` — 文档级 DocumentIndexJob 列表（阶段/状态/块数）

`POST /api/v1/libraries/{libraryId}/documents/reindex-failed` — 批量重索引 FAILED 文档

`POST /api/v1/libraries/{libraryId}/documents/reindex-by-profile?documentProfileCode=` — 按 Document Profile 批量重索引

`GET /api/v1/libraries/{libraryId}/documents/duplicates` — content_hash 重复文档组

#### 查询文档详情

`GET /api/v1/libraries/{libraryId}/documents/{documentId}`

响应 `data`：`KnowledgeDocumentResult`

#### 预览原始文档

`GET /api/v1/libraries/{libraryId}/documents/{documentId}/preview`

响应：原始二进制流（**非** `ApiResponse` 包装）。响应头含 `Content-Type` 与 `Content-Disposition: inline`。

说明：从文档 `sourceUri` 读取原文（`minio://` / `file://` / `inline://` 等），需库 READ 权限。前端应带鉴权头以 `blob` 拉取后本地渲染（PDF iframe、文本、DOCX mammoth 等）。

#### 下载原始文档

`GET /api/v1/libraries/{libraryId}/documents/{documentId}/download`

响应：同 preview，但 `Content-Disposition: attachment`。

#### 查询文档块列表（分页）

`GET /api/v1/libraries/{libraryId}/documents/{documentId}/chunks?page=1&size=20`

响应 `data`：`PageResult<DocumentChunkResult>`（`items`、`total`、`page`、`size`）

说明：按 `chunkId` 升序分页；单页最多 100 条。返回的块列表与 `chunkCount` **不含** `document_summary` 摘要层（摘要仍参与检索，仅管理端文档详情不展示）。块结果包含 `content`、`tokenCount`、`tokenizerId`、`chunkBoundaryType` 与 `metadata`（含 `pageNumber`、`bbox`、`contentFamily`、`retrievalEnabled` 等）。

#### 更新文档块

`PUT /api/v1/libraries/{libraryId}/documents/{documentId}/chunks/{chunkId}`

请求体：

```json
{
  "content": "修订后的块文本",
  "retrievalEnabled": true
}
```

响应 `data`：`DocumentChunkResult`

说明：至少提供 `content` 或 `retrievalEnabled` 之一。修改 `content` 会重新 token 计数并向量化；`retrievalEnabled=false` 的块不参与检索（vector/keyword/hybrid）。

#### 查询文档入库 Trace

`GET /api/v1/libraries/{libraryId}/documents/{documentId}/pipeline-trace`

响应 `data`：`DocumentPipelineTraceResult`（`runId`、`traceId`、作业 `status`/`stage`、可见 chunk 数）

说明：基于该文档最近一次 `DocumentIndexJob` 与关联 `IngestionRun` 汇总；无入库记录时 404。

### 3.6 创建入库运行

`POST /api/v1/libraries/{libraryId}/ingestion-runs`

请求体：

```json
{
  "libraryId": "00000000-0000-0000-0000-000000000000",
  "sourceUris": ["file://D:/document"],
  "sourceType": "local_directory",
  "documentProfileCode": null,
  "options": {
    "recursive": true,
    "maxFiles": 50,
    "extensions": ["md", "pdf", "docx", "xlsx"]
  }
}
```

响应 `data`：`IngestionRunResult`

说明：

- 路径中的 `libraryId` 优先，服务端会使用路径参数覆盖请求体中的 `libraryId`。
- 默认同步执行时，接口返回终态结果；开启 `knowbase.ingestion.async-enabled=true` 后，接口会先返回已创建任务，客户端应通过 `GET /api/v1/ingestion-runs/{runId}` 轮询状态。
- 默认 `knowbase.ingestion.document-upsert-enabled=true`：入库写 active 索引代次并按 `sourceUri` upsert 文档；设为 `false` 时回退为每次 run 新建 IndexVersion 快照模式。
- `sourceUris` 支持 `inline:text:`、`inline://`、`file://` 单文件和 `file://` 本地目录。
- 当 `sourceUris` 指向目录时，服务端会按 `options.recursive`、`options.maxFiles` 与 `options.extensions` 展开文件列表。
- `documentProfileCode` 为空时，入库 Pipeline 会按文件类型自动选择文档 Profile：Markdown 使用 `default_markdown`，PDF/Word 使用 `default_rich_text`，Excel/CSV 使用 `default_table`，PPT 使用 `default_presentation`，HTML 使用 `default_web_page`，代码/配置使用 `default_code_or_config`。
- 每个 chunk 会保留来源 URI、文件名、MIME、文件扩展名、解析器、内容族、Profile 编码和 tokenizer 统计信息。

### 3.7 查询入库运行

`GET /api/v1/ingestion-runs/{runId}`

响应 `data`：`IngestionRunResult`

### 3.8 查询入库失败文档

`GET /api/v1/ingestion-runs/{runId}/errors`

响应 `data`：`IngestionDocumentErrorResult[]`

说明：返回该次入库运行中解析、切块或向量化失败的源文件及错误码。

### 3.9 上传并创建入库任务

`POST /api/v1/libraries/{libraryId}/ingestion-runs/upload`

请求：`multipart/form-data`

| 字段 | 类型 | 说明 |
|------|------|------|
| `files` | 文件列表 | 必填，支持多文件 |
| `documentProfileCode` | string | 可选 |
| `autoStart` | boolean | 默认 `true`，上传后立即创建入库任务 |
| `maxFiles` | int | 可选 |

响应 `data`：

```json
{
  "upload": { "uploaded": [], "failures": [] },
  "ingestionRun": {},
  "storageType": "local"
}
```

说明：文件先写入 ObjectStorage（默认本地 FS，可配置 MinIO），再按上传 URI 创建入库运行。与 `POST .../documents` 行为等价，路径挂在入库任务命名空间下，适合脚本或外部系统一键上传入库。

**前端控制台实际路径**（`DocumentIngestWizard`）：

| 模式 | 调用链 |
|------|--------|
| 快捷上传并入库 | `POST .../documents`（multipart，含 `autoStart=true`） |
| 分步入库向导 | ① `POST /api/v1/storage/upload-batch` → ② `POST .../ingestion/prepare`（`prepareStage=all`）→ ③ `POST .../ingestion-runs` |

向导**不**调用 `ingestion-runs/upload`；该接口仍供 API 客户端直接使用。

### 3.10 入库分段预览

`POST /api/v1/libraries/{libraryId}/ingestion/preview`

请求体与 `CreateIngestionRunCommand` 类似（`sourceUris`、`documentProfileCode`、`options`），但不写入索引。

响应 `data`：`IngestionPreviewResult`

### 3.11 入库准备（分阶段解析/清洗/切块）

`POST /api/v1/libraries/{libraryId}/ingestion/prepare` — 完整准备（默认 `prepareStage=all`）

`POST /api/v1/libraries/{libraryId}/ingestion/prepare/parse` — 仅解析

`POST /api/v1/libraries/{libraryId}/ingestion/prepare/normalize` — 解析 + 清洗

`POST /api/v1/libraries/{libraryId}/ingestion/prepare/chunk` — 解析 + 清洗 + 切块

`POST /api/v1/libraries/{libraryId}/ingestion/prepare/summarize` — **已停用**（等价于 chunk 阶段，保留仅为兼容旧客户端）

请求体：

```json
{
  "sourceUris": ["file:///path/to/doc.pdf"],
  "documentProfileCode": null,
  "prepareStage": "chunk",
  "options": {}
}
```

响应 `data`：`IngestionPrepareResult`（含每文档的解析块、清洗统计与 chunk 预览）

说明：前端入库向导第二步调用 `POST .../ingestion/prepare`（请求体 `prepareStage=all`），在正式向量化前预览完整流水线效果。分阶段路径（`/parse`、`/normalize`、`/chunk`）供调试或 API 客户端按需使用。

### 3.12 对象存储上传

`POST /api/v1/storage/upload` — 单文件上传，字段名 `file`

`POST /api/v1/storage/upload-batch` — 批量上传，字段名 `files`（单次最多 50 个，单文件不超过 100MB）

响应 `data`：`ObjectUploadResult` 或 `BatchObjectUploadResult`

说明：返回 `uri` 可作为入库 `sourceUris` 使用。分步入库向导第一步调用 `upload-batch`；`ingestion-runs/upload` 则在同一请求内完成上传 + 创建入库任务。

### 3.13 创建知识智能体

`POST /api/v1/agents`

请求体：

```json
{
  "tenantId": "default",
  "name": "内部知识助手",
  "description": "面向内部员工的知识问答入口",
  "scenePresetCode": "internal_knowledge_assistant",
  "libraryIds": ["00000000-0000-0000-0000-000000000000"],
  "routingPolicy": {
    "mode": "selected_libraries"
  },
  "retrievalPolicy": {
    "topKPerLibrary": 8,
    "maxCandidates": 24,
    "maxEvidence": 12,
    "fusion": "rrf",
    "rerank": "mmr",
    "balanceAcrossLibraries": true,
    "deduplicateByChunk": true,
    "deduplicateByContent": true,
    "contentFamilyWeights": {
      "STRUCTURED_TABLE": 1.08,
      "CODE_OR_CONFIG": 1.05,
      "RICH_TEXT": 1.0,
      "PLAIN_TEXT": 1.0
    }
  },
  "answerPolicy": {
    "citationRequired": true,
    "refuseWhenEvidenceLow": true
  },
  "chatTokenizerProfileId": null,
  "systemPrompt": "请基于证据回答，并返回引用。"
}
```

响应 `data`：`KnowledgeAgentResult`

### 3.14 查询知识智能体

`GET /api/v1/agents/{agentId}`

响应 `data`：`KnowledgeAgentResult`

### 3.15 查询知识智能体列表

`GET /api/v1/agents?tenantId=default`

响应 `data`：`KnowledgeAgentResult[]`

### 3.16 智能体版本生命周期

路径前缀：`/api/v1/agents/{agentId}/versions`

- `GET /` — 版本列表，响应 `AgentVersionResult[]`
- `GET /{agentVersionId}` — 版本详情
- `POST /` — 创建版本（绑定知识库、检索/回答策略等）
- `POST /{agentVersionId}/mark-testing` — 标记为测试中
- `POST /{agentVersionId}/publish` — 发布版本（正式问答默认使用最新已发布版本）
- `POST /{agentVersionId}/disable` — 禁用版本

### 3.17 执行智能体检索测试

`POST /api/v1/agents/{agentId}/retrieval-tests`

请求体：

```json
{
  "question": "如何安装 PostgreSQL 和 pgvector？",
  "agentVersionId": null,
  "debugLibraryIds": [],
  "retrievalPolicyOverride": {},
  "answerPolicyOverride": {
    "maxContextTokens": 4096
  }
}
```

响应 `data`：`RetrievalTestResult`

说明：该接口不调用 Chat 模型生成最终答案，只执行多库路由、向量检索、证据构建、引用生成与 Chat tokenizer 上下文拼装。适合在发布智能体或调试多库策略时验证召回质量、证据数量、上下文 token 预算和 tokenizer 轨迹。

检索策略说明：

- `fusion=rrf`：按库内与全局排序进行 Reciprocal Rank Fusion，适合多库召回结果分数尺度不完全一致的场景。
- `rerank=mmr`：使用轻量 MMR 多样性重排，降低相邻重复片段占满上下文的概率。
- `balanceAcrossLibraries=true`：限制单一知识库过度占用候选池，适合多库编排调试。
- `contentFamilyWeights`：按 `DocumentProfile.contentFamily` 对异构文档轻量加权，例如表格、代码、网页、扫描件等。
- 检索测试响应的 `trace` 会返回最终生效的 `fusion`、`rerank`、`maxCandidates`、`maxEvidence` 与候选/证据数量。
- `debugLibraryIds` 只能选择当前智能体版本已绑定的知识库，不能绕过智能体库范围。

### 3.18 发起智能问答

`POST /api/v1/query-runs`

请求体：

```json
{
  "agentId": "00000000-0000-0000-0000-000000000000",
  "agentVersionId": null,
  "sessionId": null,
  "question": "如何处理登录失败？",
  "debugLibraryIds": [],
  "variables": {},
  "stream": false
}
```

响应 `data`：`QueryRunResult`

说明：正式问答默认使用最新已发布智能体版本；显式传入 `agentVersionId` 时，该版本必须属于当前 `agentId`。`debugLibraryIds` 仅用于调试，并且只能选择当前智能体版本已绑定的知识库。

### 3.19 查询问答运行

`GET /api/v1/query-runs/{queryRunId}`

响应 `data`：`QueryRunResult`

### 3.20 多轮会话

路径前缀：`/api/v1/chat`

- `POST /sessions` — 创建会话，请求体含 `tenantId`、`agentId`、`title`
- `GET /sessions?tenantId=&agentId=` — 会话列表
- `GET /sessions/{sessionId}` — 会话详情
- `POST /sessions/{sessionId}/messages` — 发送消息并触发问答
- `GET /sessions/{sessionId}/messages` — 消息列表
- `GET /sessions/{sessionId}/messages/{messageId}/query-run` — 消息关联的 `QueryRunResult`

### 3.21 库类型预设

- `GET /api/v1/presets/library-types?tenantId=&page=1&size=10` — 分页查询，响应 `data`：`PageResult<PresetResult>`
- `GET /api/v1/presets/library-types/{code}?tenantId=` — 详情查询，响应 `data`：`PresetResult`
- `GET /api/v1/presets/ingestion-catalog` — 建仓入库产品目录（解析器、文档 Profile 模板、三层配置说明）
- `GET /api/v1/presets/library-types/{code}/product-guide?tenantId=` — 库类型预设中文产品说明（适合文件、Profile 矩阵、变更影响）
- `POST /api/v1/presets/library-types` — 创建租户自定义预设，响应 `data`：`PresetResult`
- `DELETE /api/v1/presets/library-types/{code}?tenantId=` — 删除租户自定义预设（系统内置不可删）

### 3.22 场景规则预设

- `GET /api/v1/presets/scene-rules?tenantId=&page=1&size=10` — 分页查询，响应 `data`：`PageResult<PresetResult>`
- `GET /api/v1/presets/scene-rules/{code}?tenantId=` — 详情查询，响应 `data`：`PresetResult`
- `POST /api/v1/presets/scene-rules` — 创建租户自定义预设，响应 `data`：`PresetResult`
- `DELETE /api/v1/presets/scene-rules/{code}?tenantId=` — 删除租户自定义预设（系统内置不可删）

### 3.23 创建或更新 Tokenizer Profile

`POST /api/v1/tokenizer-profiles`

请求体：

```json
{
  "provider": "ollama",
  "modelName": "bge-m3",
  "tokenizerId": "ollama:bge-m3",
  "tokenizerVersion": "bge-m3",
  "approximate": true,
  "config": {
    "source": "builtin"
  },
  "enabled": true
}
```

响应 `data`：`TokenizerProfileResult`

### 3.24 查询 Tokenizer Profile

- `GET /api/v1/tokenizer-profiles`
- `GET /api/v1/tokenizer-profiles?provider=ollama`
- `GET /api/v1/tokenizer-profiles/{tokenizerProfileId}`

说明：入库阶段优先使用 `DocumentProfile.tokenizerProfileId`，其次使用 `LibraryProfile.embeddingTokenizerProfileId`，最后按 `embeddingProvider + embeddingModel` 查找启用的 Tokenizer Profile。问答阶段优先使用 `AgentVersion.chatTokenizerProfileId`，否则使用 Chat 模型默认 tokenizer。

### 3.25 ACL 权限

路径前缀：`/api/v1/acls`

- `POST /` — 授予 ACL

```json
{
  "tenantId": "default",
  "resourceType": "LIBRARY",
  "resourceId": "00000000-0000-0000-0000-000000000000",
  "principalType": "USER",
  "principalId": "alice",
  "permission": "READ"
}
```

- `GET /?tenantId=&resourceType=LIBRARY&resourceId=` — 查询资源 ACL 列表
- `DELETE /{aclId}` — 撤销 ACL

说明：`resourceType` 支持 `LIBRARY`、`AGENT` 等；`permission` 为 `READ`、`WRITE`、`ADMIN`。REST 请求通过 `X-Knowbase-Tenant-Id`、`X-Knowbase-User-Id`、`X-Knowbase-Roles` 头注入请求上下文，前端控制台在 `context.js` 中统一设置。无 ACL 记录时默认允许访问（开发模式）。

### 3.26 观测与评测

路径前缀：`/api/v1/observability`

- `GET /traces/{traceId}` — 按 traceId 查询 Pipeline Span 列表
- `GET /pipelines/{pipeline}/runs/{runId}` — 按 pipeline（`ingestion` / `query`）与 runId 查询 Span
- `POST /eval-runs` — 创建评测运行（绑定智能体与样例问答对）
- `GET /eval-runs/{evalRunId}` — 评测运行详情
- `GET /eval-runs?tenantId=&agentId=` — 评测运行列表

响应 Span 含 `stage`、`status`、`durationMs`、`pipeline`；评测结果含 `metrics` 与逐样例 `EvalSampleResult`。

## 4. 运行模式

### 4.1 独立运行

独立应用模块为 `knowbase-app`，默认开启 REST API：

```yaml
knowbase:
  enabled: true
  web:
    exposed: true
```

### 4.2 宿主服务引入

宿主服务引入 `knowbase-starter` 后，默认装配 Facade 与应用用例，但不暴露 REST Controller：

```yaml
knowbase:
  enabled: true
  web:
    exposed: false
```

如宿主希望直接暴露 KnowBase REST API，可显式设置：

```yaml
knowbase:
  web:
    exposed: true
```

## 5. 当前实现范围

当前实现已具备一期完整纵向闭环，并在管理与运维侧扩展了以下能力：

**知识库与目录**

- 创建/分页查询/详情/删除知识库；按库类型预设生成默认 Profile 与 Document Profile。
- 索引代次运维（`index-generations` 列表/rebuild/promote、健康检查、promote 门禁）；文档分页列表/详情/预览/下载/批量删除；chunk 分页与编辑；文档级 pipeline trace（`LibraryCatalogController`）。
- Library Profile 版本管理与 Document Profile CRUD（库配置页）；库级 ACL 授予/撤销/列表（`AclController`）。

**入库 Pipeline**

- URI 驱动入库（inline、file 单文件、file 目录）；同步/异步执行可配置。
- ObjectStorage 上传（单文件/批量）；文档目录 `POST .../documents` 或 `ingestion-runs/upload` 一键上传入库。
- 分阶段 preview / prepare（parse → normalize → chunk）；入库失败文档错误列表与 DocumentIndexJob 追踪。
- Tokenizer Profile 驱动分块；PostgreSQL + pgvector 持久化；document upsert 写入 active 索引代次。

**智能体与问答**

- 知识智能体 CRUD；版本生命周期（创建 → 测试中 → 发布 → 禁用）。
- 多库路由、RRF 融合、MMR 重排、contentFamily 权重；检索测试与正式问答。
- 多轮 Chat 会话 API（后端已实现）；前端问答页当前以单次 QueryRun 为主。

**预设、评测与观测**

- 库类型/场景规则预设分页 CRUD（租户自定义 + 系统内置）；ingestion 产品目录与 product-guide。
- Tokenizer Profile REST/Facade 管理。
- 库级黄金集 CRUD、Recall@K 评测、promote 评测门禁；Pipeline Trace 查询；Observability 评测运行。

**前端控制台**（`frontend/knowbase-ui`）

- 首页；知识库列表与建仓向导。
- 库工作区：文档列表、召回与评测、库配置（Profile/Document Profile/索引健康）、ACL。
- 入库向导（分步：upload-batch → prepare → ingestion-runs；快捷：`POST .../documents`）。
- 智能体、观测评测、预设管理、智能问答。

**运行形态**

- REST API、Java Facade、Swagger/Knife4j、Vue 管理控制台、宿主 starter 与独立应用均已可用。

仍在后续演进范围内的能力见 [DESIGN_EVOLUTION_OUTLINE.md](./DESIGN_EVOLUTION_OUTLINE.md) 与 [PHASE2_INGESTION_PLAN.md](./PHASE2_INGESTION_PLAN.md)，主要包括：知识库元数据更新 API、独立 evidence 查询、OCR/表格深度解析、生产级异步队列与细粒度权限工作流、Chat 多轮会话前端接入等。
