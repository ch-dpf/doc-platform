# KnowBase RAG 平台总体设计规划

## 1. 项目定位

KnowBase 是面向内部知识管理场景的 RAG 平台，用于完成知识库建设、文档入库、向量索引、多知识库检索编排与智能问答。

平台围绕三段式核心产品流程设计：

1. 建仓入库。
2. 知识智能体编排创建。
3. 智能问答执行。

平台支持两种运行方式：

1. 独立运行：KnowBase 作为独立 Spring Boot 应用运行，提供 REST API 与管理前端。
2. 宿主引入：业务系统通过 Spring Boot Starter 引入 KnowBase，调用 Facade API，或按需暴露 KnowBase 控制器。

## 2. 核心技术栈

后端技术栈：

- Java 21
- Spring Boot 3.2.4
- MyBatis-Plus 3.5.5
- PostgreSQL 16
- pgvector
- Flyway
- MinIO 或 FS
- Apache Tika
- Tess4J / Tesseract OCR
- Ollama Embedding 与 Chat Model
- Knife4j / OpenAPI

前端技术栈：

- Vue 3
- Vite
- Element Plus
- Axios
- Vue Router

本地基础设施：

- Docker Compose
- PostgreSQL + pgvector
- MinIO
- Ollama

## 3. 产品范围

### 3.1 建仓入库

建仓入库流程覆盖从创建知识库到发布可检索索引版本的完整生命周期。

核心能力：

- 创建知识库。
- 配置知识库 Profile。
- 上传文件或从数据源导入文档。
- 解析文档为统一文本。
- 提取文档元数据。
- 清洗与规范化文本。
- 文档切块。
- 生成 Embedding。
- 写入文本块与向量索引。
- 发布索引版本。
- 追踪入库任务、统计结果与失败原因。

入库流程被建模为一次完整的 `IngestionRun`。一次 `IngestionRun` 拥有明确的输入、阶段状态、输出产物、统计信息和失败原因。

### 3.2 多知识库编排问答

多知识库问答由“知识智能体编排创建”和“智能问答执行”两部分组成。

知识智能体负责沉淀可复用的问答编排配置，避免每次提问都临时选择知识库、检索策略、提示词和回答规则。

智能问答执行负责在指定智能体上下文内完成一次从用户问题到最终答案的运行。

### 3.2.1 知识智能体编排创建

知识智能体是面向问答场景的一等业务对象。它不是复杂自治 Agent，而是一个可配置、可审计、可复用的 RAG 编排入口。

核心能力：

- 创建知识智能体。
- 绑定一个或多个知识库。
- 配置库选择策略。
- 配置检索策略。
- 配置场景规则预设。
- 配置系统提示词与回答风格。
- 配置引用、拒答、证据阈值与安全规则。
- 保存智能体版本。
- 对智能体执行检索测试。

知识智能体的典型字段：

- `agent_id`
- `tenant_id`
- `name`
- `description`
- `scene_preset`
- `library_scope`
- `routing_policy`
- `retrieval_policy`
- `answer_policy`
- `system_prompt`
- `status`
- `version`

### 3.2.2 智能问答执行

智能问答流程覆盖一次从用户问题到最终答案的完整执行过程。

核心能力：

- 接收用户问题。
- 选择知识智能体。
- 根据智能体配置确定知识库范围。
- 支持在调试模式下临时选择多个知识库。
- 规划检索策略。
- 并行检索多个知识库。
- 融合与重排检索结果。
- 构造结构化证据包。
- 基于证据生成答案。
- 返回引用来源。
- 保存问答执行轨迹，支持调试与审计。

问答流程被建模为一次 `QueryRun`。`QueryRun` 记录路由决策、检索计划、证据包、答案、引用与执行轨迹。

### 3.3 库类型预设

平台需要支持库类型预设。库类型预设用于降低建库成本，并为不同文档形态提供合理默认值。

库类型预设不是强制业务规则，而是可覆盖的配置模板。

推荐内置预设：

- `general_docs`：通用文档库，适合制度、说明、普通文本资料。
- `product_knowledge`：产品知识库，适合产品手册、FAQ、故障排查。
- `technical_docs`：技术文档库，适合接口文档、部署文档、研发规范。
- `policy_compliance`：制度合规库，适合规章制度、审计材料、合规条款。
- `contract_legal`：合同法务库，适合合同、协议、条款文本。
- `table_report`：表格报表库，适合 Excel、周报、月报、统计表。
- `research_archive`：研究资料库，适合论文、报告、调研材料。

库类型预设应影响：

- 支持的文件类型。
- 默认解析器。
- OCR 策略。
- 元数据 schema。
- 切块策略。
- Tokenizer 策略。
- 父子块策略。
- Embedding 配置。
- 默认检索策略。
- 引用展示方式。

### 3.4 场景规则预设

平台需要支持场景规则预设。场景规则预设作用于知识智能体，用于定义问答过程中的行为约束。

场景规则预设不应写成代码里的固定分支，而应以配置模板方式进入智能体。

推荐内置预设：

- `internal_knowledge_assistant`：内部知识助手，强调准确、简洁、可追溯。
- `customer_service_bot`：客服问答，强调口径统一、拒答安全、话术友好。
- `research_analyst`：研究分析，强调跨文档归纳、差异比较、证据覆盖。
- `compliance_qa`：合规问答，强调引用完整、不可推测、严格拒答。
- `report_writer`：报告生成，强调结构化输出、来源分组、摘要提炼。
- `technical_support`：技术支持，强调步骤化排障、版本与环境信息。

场景规则预设应影响：

- 系统提示词。
- 证据最低要求。
- 检索 topK 与重排策略。
- 是否允许跨库汇总。
- 是否允许生成建议。
- 拒答模板。
- 答案格式。
- 引用粒度。
- 风险提示规则。

### 3.5 同库异构文档入库

同一个知识库必须允许接入异构文档，例如 PDF、Word、Markdown、HTML、Excel、扫描件、图片 OCR 文档和结构化表格。

处理原则：

1. 知识库定义统一的业务边界。
2. 文档类型决定处理 Profile。
3. 索引版本统一发布。
4. 检索阶段按文档类型与元数据做融合。

同库异构文档通过 `DocumentProfile` 处理：

- 入库时识别 `content_family`。
- 根据 `content_family` 选择解析策略。
- 根据文档结构选择切块策略。
- 每个文档记录实际使用的 `document_profile_id`。
- 每个文本块记录来源文档类型、章节、表格、页码、行列范围等元数据。

推荐 `content_family`：

- `plain_text`
- `rich_text`
- `structured_table`
- `presentation`
- `scanned_document`
- `image_text`
- `web_page`
- `code_or_config`

同库异构文档的约束：

- 同一索引版本内建议使用统一 Embedding 模型与维度。
- 不同文档可使用不同解析、清洗和切块策略。
- 不同文档可使用不同结构化切块策略，但同一索引版本内应使用同一套 tokenizer 计算规则。
- 表格类文档应保留行列语义、表头与单元格上下文。
- 扫描件应保存 OCR 置信度与页码。
- 代码或配置类文档应保留路径、符号、块类型等元数据。
- 检索融合时需要保留文档类型权重，避免短 FAQ、长 PDF、表格行记录互相挤压。

### 3.6 Tokenizer 驱动的分块设计

文档分块必须依赖模型 tokenizer 进行 token 级切分，而不是只按字符数切分。

设计目标：

- 分块大小与 Embedding 模型真实 token 语义一致。
- 问答上下文拼装与 Chat 模型 token 预算一致。
- 不同模型切换时能明确触发索引版本变化。
- 支持中文、英文、代码、表格等不同文本形态。

核心原则：

1. Embedding 阶段使用 Embedding 模型对应的 tokenizer。
2. 问答上下文阶段使用 Chat 模型对应的 tokenizer。
3. 入库文本块保存 token 统计信息。
4. 模型或 tokenizer 变化必须创建新的索引版本。
5. 结构切分优先，token 限制兜底。

推荐分块流程：

1. `StructureSegmenter`：按标题、段落、页码、表格、代码块、列表等结构生成候选片段。
2. `TokenizerCounter`：使用 Embedding 模型 tokenizer 计算候选片段 token 数。
3. `TokenWindowChunker`：按 `max_tokens` 与 `overlap_tokens` 组装文本块。
4. `BoundaryAdjuster`：优先在自然边界处断开，例如段落、句子、表格行、代码块。
5. `ChunkMetadataWriter`：写入 token 数、tokenizer、模型、结构来源和边界信息。

推荐配置：

- `embedding_model`
- `embedding_tokenizer`
- `chunk_max_tokens`
- `chunk_overlap_tokens`
- `chunk_min_tokens`
- `preserve_structure_boundary`
- `fallback_split_mode`
- `tokenizer_version`

推荐文本块字段：

- `token_count`
- `tokenizer_id`
- `tokenizer_version`
- `embedding_model`
- `chunk_boundary_type`
- `parent_chunk_id`
- `source_structure`

Tokenizer 来源：

- Ollama 模型优先通过模型适配器声明 tokenizer。
- 对 OpenAI-compatible 模型，通过模型适配器声明 tokenizer。
- 对无法精确获得 tokenizer 的模型，必须提供显式近似 tokenizer，并在配置中标记 `approximate=true`。
- 生产环境不允许使用未声明 tokenizer 的 Embedding 模型发布索引版本。

异构文档处理：

- 普通文档：标题与段落优先，再按 token 窗口切分。
- Markdown/HTML：标题层级与 DOM 块优先，再按 token 窗口切分。
- 表格：按表头、行组、语义行块切分，每个块受 token 上限约束。
- 扫描/OCR：页级与段落级优先，保留 OCR 置信度。
- 代码/配置：文件路径、符号、函数、配置段优先，再按 token 窗口切分。

问答上下文拼装：

- 检索结果进入 `ContextPacker`。
- `ContextPacker` 使用 Chat 模型 tokenizer 计算最终上下文 token。
- 超出预算时按证据分数、来源权重、去重结果和引用完整性裁剪。
- 裁剪后仍必须保留引用与证据片段一致性。

## 4. 架构原则

1. Pipeline 优先：入库与问答都必须是显式 Pipeline，拥有清晰的输入、输出、状态与执行轨迹。
2. 索引版本发布：问答只读取已发布的索引版本，避免读取未完成的入库结果。
3. 公共契约稳定：宿主侧 Facade API 不暴露内部 DTO、持久化实体或 Pipeline 对象。
4. 适配器可替换：存储、Embedding、Chat、解析器、向量库都通过适配器边界接入。
5. 多库优先：单库问答是多知识库编排问答的特殊情况。
6. 智能体优先：正式问答通过知识智能体运行，临时多库问答仅作为调试能力。
7. 预设可覆盖：库类型预设与场景规则预设提供默认值，但不锁死用户配置。
8. 异构文档统一治理：同库文档可异构处理，但统一发布索引版本。
9. 部署形态中立：独立运行与宿主引入复用同一套应用服务。
10. 可观测：入库任务、智能体版本与问答任务都是一等审计对象。

## 5. 目标模块结构

推荐 Maven 模块：

- `knowbase-api`：公共 command、result、Facade 接口与 SPI。
- `knowbase-domain`：领域模型与领域服务。
- `knowbase-application`：建仓入库与问答编排用例。
- `knowbase-ingestion`：Loader、Parser、Cleaner、Chunker、入库 Pipeline。
- `knowbase-retrieval`：向量检索、混合检索、融合、重排、证据构造。
- `knowbase-agent`：知识智能体、库路由、场景规则、回答策略。
- `knowbase-preset`：库类型预设、文档 Profile 预设、场景规则预设。
- `knowbase-model`：Embedding 与 Chat Model 抽象。
- `knowbase-tokenizer`：模型 tokenizer 适配、token 计数、token 窗口切分。
- `knowbase-persistence`：MyBatis Mapper 与 Repository 实现。
- `knowbase-storage`：对象存储抽象与实现。
- `knowbase-web`：REST Controller、OpenAPI、错误处理。
- `knowbase-autoconfigure`：宿主引入场景下的 Spring Boot 自动配置。
- `knowbase-starter`：宿主服务引入的 starter 依赖入口。
- `knowbase-app`：独立运行的 Spring Boot 应用。
- `frontend/knowbase-ui`：Vue 管理控制台。
- `infra`：Docker Compose 与基础设施初始化。
- `scripts`：开发、启动与验证脚本。

## 6. 后端设计

### 6.1 公共 API 层

公共 API 层为宿主系统提供稳定契约：

- `KnowbaseLibraryFacade`
- `KnowbaseIngestionFacade`
- `KnowbaseAgentFacade`
- `KnowbaseQuestionFacade`

Facade 的 command 与 result 放在 `knowbase-api` 中，不能依赖持久化实体、REST DTO 或内部 Pipeline 对象。

### 6.2 应用层

应用服务负责编排完整用例：

- `CreateLibraryUseCase`
- `RunIngestionUseCase`
- `PublishIndexVersionUseCase`
- `CreateKnowledgeAgentUseCase`
- `UpdateKnowledgeAgentUseCase`
- `AskQuestionUseCase`
- `RouteLibrariesUseCase`

REST Controller 和 Facade 实现都调用应用服务，避免出现两套业务逻辑。

### 6.3 领域层

核心领域对象：

- `KnowledgeLibrary`
- `LibraryProfile`
- `LibraryTypePreset`
- `DocumentProfile`
- `IngestionRun`
- `DocumentAsset`
- `ParsedDocument`
- `DocumentChunk`
- `IndexVersion`
- `KnowledgeAgent`
- `AgentVersion`
- `SceneRulePreset`
- `TokenizerProfile`
- `QueryRun`
- `RetrievalPlan`
- `EvidencePack`
- `Citation`

领域对象不依赖 Spring MVC、MyBatis、MinIO、Ollama 或前端 DTO。

### 6.4 入库 Pipeline

入库 Pipeline 在用例层面表现为一次完整运行，内部可按批次处理文件。最终状态由 `IngestionRun` 表达。

Pipeline 阶段：

1. `LoadSource`
2. `ParseDocument`
3. `NormalizeText`
4. `ExtractMetadata`
5. `ChunkDocument`
6. `EmbedChunks`
7. `WriteIndex`
8. `PublishIndexVersion`

失败处理：

- 任一关键阶段失败，标记本次 `IngestionRun` 失败。
- 可保留部分中间产物用于排查。
- 重新执行入库时创建新的 `IngestionRun`。

### 6.5 问答编排 Pipeline

问答 Pipeline 创建一次 `QueryRun`。

Pipeline 阶段：

1. `LoadAgentConfig`
2. `AnalyzeQuestion`
3. `SelectLibraries`
4. `PlanRetrieval`
5. `RetrieveFromLibraries`
6. `FuseResults`
7. `RerankEvidence`
8. `BuildEvidencePack`
9. `GenerateAnswer`
10. `PersistTrace`

同一套 Pipeline 支持：

- 用户手动选择知识库。
- 系统自动路由知识库。
- 手动选择与自动路由混合模式。
- 单知识库兼容模式。

## 7. 数据模型

推荐 PostgreSQL 表：

- `kb_library`
- `kb_library_profile`
- `kb_library_type_preset`
- `kb_document_profile`
- `kb_index_version`
- `kb_ingestion_run`
- `kb_document`
- `kb_document_artifact`
- `kb_chunk`
- `kb_embedding`
- `kb_tokenizer_profile`
- `kb_agent`
- `kb_agent_version`
- `kb_scene_rule_preset`
- `kb_query_run`
- `kb_query_evidence`
- `kb_chat_session`
- `kb_chat_message`

### 7.1 知识库

`kb_library` 存储稳定的知识库元信息：

- `library_id`
- `tenant_id`
- `name`
- `description`
- `tags`
- `status`
- `created_at`
- `updated_at`

### 7.2 知识库 Profile

`kb_library_profile` 存储入库与检索配置快照：

- 解析配置。
- OCR 配置。
- 清洗配置。
- 切块配置。
- Embedding 模型。
- Embedding 维度。
- 检索默认参数。

Profile 必须版本化。索引版本引用创建该版本时使用的 Profile。

### 7.3 文档 Profile

`kb_document_profile` 存储同库异构文档的处理策略。

推荐字段：

- `document_profile_id`
- `library_id`
- `content_family`
- `parser_config`
- `cleaning_config`
- `chunking_config`
- `tokenizer_profile_id`
- `metadata_schema`
- `enabled`

同一知识库可以有多个文档 Profile。入库时根据文件类型、MIME、内容结构和用户指定规则选择实际使用的 Profile。

### 7.4 Tokenizer Profile

`kb_tokenizer_profile` 存储模型 tokenizer 配置。

推荐字段：

- `tokenizer_profile_id`
- `provider`
- `model_name`
- `tokenizer_id`
- `tokenizer_version`
- `approximate`
- `config_json`
- `enabled`

`LibraryProfile` 必须引用 Embedding 阶段使用的 tokenizer profile。`AgentVersion` 必须引用 Chat 阶段使用的 tokenizer profile。

### 7.5 知识智能体

`kb_agent` 存储知识智能体的稳定身份。

推荐字段：

- `agent_id`
- `tenant_id`
- `name`
- `description`
- `status`
- `created_at`
- `updated_at`

`kb_agent_version` 存储可执行的智能体编排配置。

推荐字段：

- `agent_version_id`
- `agent_id`
- `version`
- `scene_preset_id`
- `library_scope`
- `routing_policy`
- `retrieval_policy`
- `answer_policy`
- `system_prompt`
- `published`
- `created_at`

问答运行时绑定一个明确的 `agent_version_id`。

### 7.6 预设

`kb_library_type_preset` 存储库类型预设。

`kb_scene_rule_preset` 存储场景规则预设。

预设表应区分系统内置预设与租户自定义预设。

推荐字段：

- `preset_id`
- `tenant_id`
- `preset_type`
- `name`
- `description`
- `config_json`
- `built_in`
- `enabled`

### 7.7 索引版本

`kb_index_version` 表示一个已发布、可检索的知识库状态：

- `index_version_id`
- `library_id`
- `profile_id`
- `status`
- `document_count`
- `chunk_count`
- `published_at`

问答流程只读取已发布索引版本。

### 7.8 Embedding

`kb_embedding` 将向量与文本块分开存储，便于支持不同模型与不同维度。

推荐字段：

- `embedding_id`
- `chunk_id`
- `embedding_model`
- `embedding_dimension`
- `embedding vector`

如果 pgvector 的维度约束影响多模型支持，可以按 Embedding 维度或 Profile 拆分物理向量表。

## 8. 独立运行模式

独立运行模式使用 `knowbase-app`。

运行组件：

- Spring Boot API 服务。
- PostgreSQL + pgvector。
- MinIO 或本地文件系统。
- Ollama。
- Vue 管理前端。

独立服务建议暴露 API：

- `/api/v1/libraries`
- `/api/v1/libraries/{libraryId}/ingestion-runs`
- `/api/v1/agents`
- `/api/v1/query-runs`
- `/api/v1/chat`
- `/api/v1/presets`
- `/api/v1/admin/*`

## 9. 宿主引入模式

宿主服务通过 `knowbase-starter` 引入。

宿主接入方式：

1. Facade-only 模式：宿主调用 Java Facade API，并自行封装业务 REST 接口。
2. 嵌入控制器模式：宿主服务直接暴露 KnowBase REST Controller。
3. 分离模式：宿主调用 Facade API，KnowBase 独立服务负责控制台与管理能力。

配置原则：

- 优先使用独立 KnowBase datasource。
- 仅在显式配置时允许共享宿主 datasource。
- 宿主模式下默认不暴露 Controller。
- 宿主模式下默认不暴露 OpenAPI。
- 租户上下文通过 `KnowbaseTenantResolver` 提供。

## 10. 前端设计

前端是 Vue 3 管理控制台，包含四个主区域：

1. 知识库。
2. 入库任务。
3. 知识智能体。
4. 智能问答。

### 10.1 知识库

能力：

- 知识库列表。
- 创建知识库。
- 选择库类型预设。
- 编辑知识库 Profile。
- 管理同库异构文档 Profile。
- 查看已发布索引版本。
- 查看文档与文本块。

### 10.2 入库任务

能力：

- 发起入库。
- 上传文件。
- 查看运行进度。
- 查看失败文档。
- 通过新建运行进行重试。
- 查看入库统计。

### 10.3 知识智能体

能力：

- 创建智能体。
- 绑定一个或多个知识库。
- 选择场景规则预设。
- 配置库路由策略。
- 配置检索与回答策略。
- 发布智能体版本。
- 执行检索测试。

### 10.4 智能问答

能力：

- 选择知识智能体。
- 发起提问。
- 查看答案。
- 查看引用。
- 查看检索轨迹。
- 按知识库与文档查看证据。

## 11. API 草案

### 11.1 知识库 API

- `POST /api/v1/libraries`
- `GET /api/v1/libraries`
- `GET /api/v1/libraries/{libraryId}`
- `PUT /api/v1/libraries/{libraryId}`
- `POST /api/v1/libraries/{libraryId}/profiles`
- `POST /api/v1/libraries/{libraryId}/document-profiles`
- `GET /api/v1/libraries/{libraryId}/index-versions`

### 11.2 入库 API

- `POST /api/v1/libraries/{libraryId}/ingestion-runs`
- `GET /api/v1/ingestion-runs/{runId}`
- `GET /api/v1/ingestion-runs/{runId}/documents`
- `GET /api/v1/ingestion-runs/{runId}/errors`

### 11.3 问答 API

- `POST /api/v1/query-runs`
- `GET /api/v1/query-runs/{queryRunId}`
- `GET /api/v1/query-runs/{queryRunId}/evidence`
- `POST /api/v1/chat/sessions`
- `POST /api/v1/chat/sessions/{sessionId}/messages`

### 11.4 知识智能体 API

- `POST /api/v1/agents`
- `GET /api/v1/agents`
- `GET /api/v1/agents/{agentId}`
- `POST /api/v1/agents/{agentId}/versions`
- `POST /api/v1/agents/{agentId}/versions/{versionId}/publish`
- `POST /api/v1/agents/{agentId}/retrieval-tests`

### 11.5 预设 API

- `GET /api/v1/presets/library-types`
- `GET /api/v1/presets/scene-rules`
- `POST /api/v1/presets/library-types`
- `POST /api/v1/presets/scene-rules`

## 12. 实施路线

推荐阶段：

1. 创建 Maven 多模块骨架。
2. 定义领域模型与公共 API 契约。
3. 添加 PostgreSQL schema 与 Flyway schema 管理。
4. 实现存储适配器、模型适配器与 tokenizer 适配器。
5. 实现 token 驱动的入库 Pipeline。
6. 实现库类型预设与同库异构文档 Profile。
7. 实现知识库 REST API 与 Facade API。
8. 实现知识智能体与场景规则预设。
9. 实现检索与多知识库问答 Pipeline。
10. 实现基于 Chat tokenizer 的上下文拼装。
11. 实现前端控制台。
12. 实现宿主 starter 自动配置。
13. 实现独立应用打包与 Docker Compose。

## 13. 非目标

平台暂不覆盖以下能力：

- 多租户计费。
- 复杂权限工作流。
- 人工标注与知识审核流。
- 多向量数据库同时写入。
- 在线协作文档编辑。
- 训练或微调大模型。

## 14. 工程护栏

工程护栏用于保证平台在持续迭代中仍然具备可测试、可追踪、可回滚、可审计和可治理的能力。

### 14.1 状态机护栏

核心业务对象必须有明确状态机。

`IngestionRun` 状态：

- `CREATED`
- `VALIDATING`
- `RUNNING`
- `PARTIAL_FAILED`
- `FAILED`
- `SUCCEEDED`
- `CANCELLED`

`IndexVersion` 状态：

- `DRAFT`
- `BUILDING`
- `PUBLISHED`
- `ARCHIVED`
- `FAILED`

`AgentVersion` 状态：

- `DRAFT`
- `TESTING`
- `PUBLISHED`
- `DISABLED`

`QueryRun` 状态：

- `CREATED`
- `ROUTING`
- `RETRIEVING`
- `GENERATING`
- `SUCCEEDED`
- `FAILED`
- `CANCELLED`

状态机规则：

- 状态变化必须由应用服务统一驱动。
- 状态变化必须记录操作者、时间、原因和 trace id。
- 正式问答只允许绑定 `PUBLISHED` 的 `AgentVersion`。
- 正式问答只允许读取 `PUBLISHED` 的 `IndexVersion`。
- 失败状态必须保存结构化错误码与可读错误信息。

### 14.2 权限与租户护栏

权限模型必须覆盖知识库、文档、智能体和问答运行。

基本对象：

- `Tenant`
- `User`
- `Role`
- `Permission`
- `LibraryAcl`
- `AgentAcl`
- `DocumentAcl`

权限规则：

- 所有 API 必须携带租户上下文。
- 宿主模式下租户上下文由 `KnowbaseTenantResolver` 提供。
- 独立运行模式下租户上下文由登录态或访问令牌提供。
- 多知识库问答时，路由阶段只能选择当前用户有权限访问的知识库。
- 引用返回前必须再次校验证据片段访问权限。
- 审计日志必须记录用户、租户、智能体、知识库、文档与操作类型。

### 14.3 可观测性护栏

平台必须对入库 Pipeline 和问答 Pipeline 提供完整 trace。

推荐采用 OpenTelemetry 语义，将每次 `IngestionRun` 和 `QueryRun` 作为根 trace。

入库 trace span：

- `load_source`
- `parse_document`
- `normalize_text`
- `extract_metadata`
- `chunk_document`
- `count_tokens`
- `embed_chunks`
- `write_index`
- `publish_index_version`

问答 trace span：

- `load_agent_config`
- `analyze_question`
- `select_libraries`
- `plan_retrieval`
- `retrieve_from_library`
- `fuse_results`
- `rerank_evidence`
- `build_evidence_pack`
- `generate_answer`

观测指标：

- 入库耗时。
- 单文档解析耗时。
- 切块数量。
- Embedding 批次耗时。
- 模型调用耗时。
- 检索耗时。
- 生成耗时。
- token 用量。
- chunk token 分布。
- tokenizer 版本。
- 命中文档数。
- 引用数量。
- 错误码分布。

内容追踪默认关闭。需要排查问题时，可对指定租户、指定任务或指定 trace 临时开启，并进行敏感信息脱敏。

### 14.4 评测护栏

平台必须同时支持离线评测与在线评测。

离线评测用于发布前验证：

- 入库解析样本集。
- 检索样本集。
- 多库路由样本集。
- 问答样本集。
- 拒答样本集。
- 引用准确性样本集。

在线评测用于生产监控：

- 用户反馈。
- 低分答案采样。
- 高延迟请求采样。
- 无结果请求采样。
- 高风险场景采样。

核心指标：

- `context_precision`
- `context_recall`
- `faithfulness`
- `groundedness`
- `answer_relevancy`
- `citation_accuracy`
- `route_accuracy`
- `refusal_accuracy`
- `p95_latency`
- `error_rate`
- `cost_per_query`

发布门槛：

- 新智能体版本必须通过指定评测集。
- 新库类型预设必须通过入库样本集。
- 新场景规则预设必须通过问答样本集。
- Prompt、检索策略、重排策略和模型配置变化必须生成评测报告。

### 14.5 检索与证据护栏

多知识库检索必须有稳定、可解释的证据处理规则。

检索规则：

- 每个知识库独立检索。
- 跨库结果必须做分数归一化或排序融合。
- 同文档相邻片段应支持合并或去重。
- 表格行、长文档段落、短 FAQ 需要保留来源类型权重。
- 检索结果必须保留 `library_id`、`document_id`、`chunk_id`、`index_version_id`。

证据规则：

- 回答必须基于 `EvidencePack`。
- 引用必须指向具体片段。
- 证据不足时必须触发拒答策略。
- 证据冲突时必须展示冲突来源，不能直接合并为单一结论。
- 跨库汇总必须保留来源库分组。

### 14.6 模型与提示词护栏

模型、提示词和回答策略都必须版本化。

模型配置：

- `EmbeddingProvider`
- `ChatProvider`
- `EmbeddingModel`
- `ChatModel`
- `EmbeddingTokenizer`
- `ChatTokenizer`
- `Dimension`
- `Timeout`
- `Temperature`
- `MaxTokens`

提示词配置：

- 系统提示词。
- 场景规则提示词。
- 引用格式提示词。
- 拒答提示词。
- 输出结构提示词。

规则：

- `AgentVersion` 必须引用明确的模型配置与提示词版本。
- 问答 trace 必须记录实际使用的模型、参数和提示词版本。
- 模型切换不能影响已经生成的问答 trace。
- Embedding 维度变化必须创建新的索引版本。
- Embedding tokenizer 变化必须创建新的索引版本。
- Chat tokenizer 变化必须创建新的智能体版本。
- 入库阶段的 chunk token 统计必须使用 Embedding tokenizer。
- 问答阶段的上下文预算必须使用 Chat tokenizer。

### 14.7 数据治理护栏

数据治理覆盖文档、索引、证据、会话和日志。

治理规则：

- 原始文件、解析文本、文本块和向量必须有生命周期策略。
- 文档删除后，应有明确的索引清理策略。
- 会话与问答 trace 应支持保留期限。
- 敏感字段进入日志、trace 和评测集前必须脱敏。
- 导出数据必须记录操作者与导出范围。
- schema 变化必须通过 Flyway 管理。

### 14.8 宿主集成护栏

宿主引入模式必须限制自动暴露范围。

默认策略：

- 默认启用 Facade API。
- 默认不暴露 REST Controller。
- 默认不暴露 OpenAPI。
- 默认要求显式配置 datasource。
- 默认要求显式配置租户解析器。

宿主集成必须明确：

- datasource 来源。
- Flyway schema 管理方式。
- 事务边界。
- 异常转换方式。
- 鉴权上下文来源。
- 日志与 trace 归属。

### 14.9 运行与容量护栏

平台需要为入库与问答设置容量边界。

入库限制：

- 单文件大小。
- 单次文件数量。
- 单租户并发入库数。
- 单知识库文档数。
- 单知识库 chunk 数。
- 单次入库最长运行时间。

问答限制：

- 单次可访问知识库数量。
- 单库检索 topK。
- 跨库总候选数。
- 最终证据数量。
- 最大上下文长度。
- 单次生成超时。
- 单用户 QPS。

超出限制时必须返回结构化错误码与可读提示。

## 15. 质量门槛

必要验证：

- 领域单元测试。
- Pipeline 阶段测试。
- 小文档入库端到端测试。
- Tokenizer 计数一致性测试。
- Token 窗口分块边界测试。
- 使用确定性 Mock Model 的多知识库问答集成测试。
- API 契约测试。
- Flyway schema 校验。A
- 前端主流程组件测试。
- 状态机转换测试。
- 权限过滤测试。
- 多库路由评测。
- 引用准确性评测。
- 拒答准确性评测。
- OpenTelemetry trace 字段校验。

## 16. 第一实施里程碑

第一个里程碑应交付一个最小但完整的纵向切片：

1. 创建知识库。
2. 选择库类型预设。
3. 上传 text 或 markdown 文档。
4. 使用 Embedding tokenizer 执行 token 分块。
5. 执行入库 Pipeline。
6. 发布索引版本。
7. 创建知识智能体。
8. 绑定一个或多个知识库。
9. 选择场景规则预设。
10. 使用 Chat tokenizer 拼装问答上下文。
11. 对知识智能体提问。
12. 返回答案、引用和检索轨迹。

该里程碑优先保证边界清晰与流程正确，不追求功能广度。
