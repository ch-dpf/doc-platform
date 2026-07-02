# KnowBase 模块划分

本文描述 Maven 多模块布局、依赖方向与职责边界。与代码同步于 2026-07。

## 总览

```
                    ┌─────────────────┐
                    │  knowbase-app   │  独立运行入口（Spring Boot 可执行 JAR）
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ knowbase-starter│  宿主依赖入口（仅 autoconfigure）
                    └────────┬────────┘
                             │
         ┌───────────────────▼───────────────────┐
         │         knowbase-autoconfigure         │  Bean 装配、KnowbaseProperties
         └─┬─────┬─────┬─────┬─────┬─────┬─────┘
           │     │     │     │     │     │
    ┌──────▼──┐  │  ┌──▼──┐  │  ┌───▼────┐  ┌───▼──────────┐
    │knowbase-│  │  │web  │  │  │persist.│  │ application  │
    │  web    │  │  │     │  │  │        │  │              │
    └────┬────┘  │  └──┬──┘  │  └───┬────┘  └───┬──────────┘
         │       │     │     │      │           │
         └───────┴─────┴─────┴──────┴───────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼─────┐ ┌────▼────┐ ┌─────▼─────┐
        │ ingestion │ │retrieval│ │   agent   │
        └─────┬─────┘ └────┬────┘ └─────┬─────┘
              │            │            │
        ┌─────▼────────────▼────────────▼─────┐
        │ preset │ model │ tokenizer │ storage │
        └─────────────────┬───────────────────┘
                          │
                   ┌──────▼──────┐
                   │ knowbase-api│  Command / Result / Facade 契约
                   └──────┬──────┘
                          │
                   ┌──────▼──────┐
                   │knowbase-domain│ 领域模型、仓储接口、状态枚举
                   └─────────────┘
```

前端与基础设施不在 Maven reactor 内：

| 路径 | 说明 |
|------|------|
| `frontend/knowbase-ui` | Vue 3 + Vite 管理控制台 |
| `infra/` | Ollama Modelfile、可选 Compose 片段 |
| `scripts/` | Windows PowerShell 启动与验证脚本 |
| `sample-documents/` | 入库回归样例与 eval 报告目录 |
| `docs/` | 设计、API、二期规划与本文 |

## 模块职责

| 模块 | 职责 | 主要对外面 |
|------|------|------------|
| **knowbase-api** | REST/Facade 的 Command、Result、SPI 接口定义 | Facade 接口、DTO |
| **knowbase-domain** | 实体、值对象、状态机、Repository 接口 | 无框架依赖的核心模型 |
| **knowbase-ingestion** | 加载、解析、清洗、元数据、切分、入库 Pipeline | `DocumentParser`、`DocumentNormalizer`、`DocumentChunker` SPI |
| **knowbase-retrieval** | 向量/关键词检索、融合、重排、证据与上下文拼装 | `RetrievalPostProcessor` |
| **knowbase-agent** | 智能体、多库路由、场景规则 | Agent 编排 |
| **knowbase-preset** | 库类型预设、Document Profile 模板、解析器产品目录 | `BuiltinPresetCatalog`、`IngestionProductCatalog` |
| **knowbase-model** | Ollama/vLLM/PaddleOCR-VL 等模型客户端 | Embedding / Chat / Vision |
| **knowbase-tokenizer** | Tokenizer Profile 注册、计数、token 窗口切分 | `TokenizerRegistry` |
| **knowbase-storage** | 本地 FS / MinIO 对象存储抽象 | `ObjectStorage` |
| **knowbase-persistence** | MyBatis-Plus、pgvector、Flyway 迁移实现 | `KnowbaseRepository` |
| **knowbase-application** | 用例服务、Facade 实现、问答 Pipeline 编排 | `*UseCase`、`*Service` |
| **knowbase-web** | REST Controller、OpenAPI、全局异常 | `/api/v1/*` |
| **knowbase-autoconfigure** | Spring Boot 自动配置，注册全部默认 Bean | `KnowbaseAutoConfiguration` |
| **knowbase-starter** | 宿主单依赖入口 | Maven artifact |
| **knowbase-app** | 独立应用：Web + JDBC + Flyway + PostgreSQL 驱动 | 可执行 JAR |

## 依赖规则（简）

- **domain** 不依赖其他 knowbase 模块。
- **api** 仅依赖 validation / swagger 注解。
- **ingestion / retrieval / agent / preset / model / tokenizer / storage** 依赖 **domain**（及彼此少量交叉，见各模块 `pom.xml`）。
- **application** 聚合 ingestion、retrieval、agent、preset、model、tokenizer。
- **persistence** 依赖 domain + retrieval + model（向量写入）。
- **web** 依赖 api + application。
- **autoconfigure** 依赖几乎全部实现模块，负责 Bean  wiring。
- **app** 依赖 starter + web + persistence + Spring Boot 运行栈。

## Ingestion 子 Tab 与模块映射

| 流水线阶段 | 主要代码位置 |
|------------|--------------|
| 来源加载 | `knowbase-ingestion` → `DocumentSourceLoader` |
| 解析 | `knowbase-ingestion` → `DocumentParser`、`layout/*` |
| 清洗 | `knowbase-ingestion` → `DocumentNormalizer` / `DocumentTextNormalizer` |
| 元数据 | `knowbase-ingestion` → `DocumentMetadataEnricher` |
| 切分 | `knowbase-ingestion` → `DocumentChunker`、`TokenBasedDocumentChunker`、`smart/*` |
| 向量化 | `knowbase-model` + `knowbase-application` |
| 持久化 | `knowbase-persistence` |
| 入库 API | `knowbase-web` → `IngestionRunController` |
| 入库 UI | `frontend/knowbase-ui` → `DocumentIngestWizard.vue` |

## 内置解析器（产品目录）

完整列表与扩展名见 `IngestionProductCatalog`（`knowbase-preset`）。概要：

- **内置 14 个**：markdown/html/docx/pptx/pdf-layout/pdf-structure/text/code-config/table-deep/qa/zip/tika/ocr-layout
- **外接 3 个**：docling、unstructured、external（HTTP adapter，需自建服务）

## 运行形态

| 形态 | 依赖模块 | 说明 |
|------|----------|------|
| 独立应用 | `knowbase-app` | 默认暴露 REST + 持久化 |
| 宿主 Facade | `knowbase-starter` | `knowbase.web.exposed=false`，无 JDBC 强制 |
| 宿主 + API | starter + `knowbase-web` | 显式 `web.exposed=true` |
| 仅控制台 | `frontend/knowbase-ui` | 代理到后端 `/api` |

## 相关文档

- [本地开发与环境](DEV_SETUP.md)
- [实现进度对照](PROJECT_STATUS.md)
- [总体设计](DESIGN.md)
- [API 规范](API.md)
