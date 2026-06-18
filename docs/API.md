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

用途：创建知识库、查询知识库详情、按租户查询知识库列表。

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

### 2.5 预设查询

接口：`KnowbasePresetFacade`

- `listLibraryTypePresets()`
- `listSceneRulePresets()`

用途：查询库类型预设与场景规则预设，供宿主服务或前端控制台在建库、创建智能体时选择默认策略模板。

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

### 3.3 查询知识库列表

`GET /api/v1/libraries?tenantId=default`

响应 `data`：`LibraryResult[]`

### 3.4 创建入库运行

`POST /api/v1/libraries/{libraryId}/ingestion-runs`

请求体：

```json
{
  "libraryId": "00000000-0000-0000-0000-000000000000",
  "sourceUris": ["file://D:/document"],
  "sourceType": "local_directory",
  "documentProfileCode": null,
  "publishIndexOnSuccess": true,
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
- `sourceUris` 支持 `inline:text:`、`inline://`、`file://` 单文件和 `file://` 本地目录。
- 当 `sourceUris` 指向目录时，服务端会按 `options.recursive`、`options.maxFiles` 与 `options.extensions` 展开文件列表。
- `documentProfileCode` 为空时，入库 Pipeline 会按文件类型自动选择文档 Profile：Markdown 使用 `default_markdown`，PDF/Word 使用 `default_rich_text`，Excel/CSV 使用 `default_table`，PPT 使用 `default_presentation`，HTML 使用 `default_web_page`，代码/配置使用 `default_code_or_config`。
- 每个 chunk 会保留来源 URI、文件名、MIME、文件扩展名、解析器、内容族、Profile 编码和 tokenizer 统计信息。

### 3.5 查询入库运行

`GET /api/v1/ingestion-runs/{runId}`

响应 `data`：`IngestionRunResult`

### 3.6 创建知识智能体

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

### 3.7 查询知识智能体

`GET /api/v1/agents/{agentId}`

响应 `data`：`KnowledgeAgentResult`

### 3.8 查询知识智能体列表

`GET /api/v1/agents?tenantId=default`

响应 `data`：`KnowledgeAgentResult[]`

### 3.9 执行智能体检索测试

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

### 3.10 发起智能问答

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

### 3.11 查询问答运行

`GET /api/v1/query-runs/{queryRunId}`

响应 `data`：`QueryRunResult`

### 3.12 查询库类型预设

`GET /api/v1/presets/library-types`

响应 `data`：`PresetResult[]`

### 3.13 查询场景规则预设

`GET /api/v1/presets/scene-rules`

响应 `data`：`PresetResult[]`

### 3.14 创建或更新 Tokenizer Profile

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

### 3.15 查询 Tokenizer Profile

- `GET /api/v1/tokenizer-profiles`
- `GET /api/v1/tokenizer-profiles?provider=ollama`
- `GET /api/v1/tokenizer-profiles/{tokenizerProfileId}`

说明：入库阶段优先使用 `DocumentProfile.tokenizerProfileId`，其次使用 `LibraryProfile.embeddingTokenizerProfileId`，最后按 `embeddingProvider + embeddingModel` 查找启用的 Tokenizer Profile。问答阶段优先使用 `AgentVersion.chatTokenizerProfileId`，否则使用 Chat 模型默认 tokenizer。

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

当前实现已具备一期最小纵向闭环：创建知识库、按库类型预设生成默认 Profile、执行 token 驱动入库、写入 PostgreSQL/pgvector、发布索引版本、创建知识智能体、按场景预设执行多库问答、返回答案/证据/引用，并支持 REST API、Java Facade、Swagger/Knife4j、前端控制台、宿主 starter 与独立应用运行。Tokenizer Profile 已支持 REST/Facade 管理，并被入库分块和问答上下文预算引用。

仍在后续演进范围内的能力包括：预设持久化管理、文件上传与 MinIO、OCR 深度解析、异步任务队列、细粒度权限、多轮会话、混合检索、重排评测和生产级观测。
