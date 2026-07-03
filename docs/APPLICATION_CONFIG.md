# application.yml 配置说明

`knowbase-app/src/main/resources/application.yml` 仅保留**可独立运行的最小默认集**（本地存储、启发式 PDF 版面、启发式阅读顺序、VLM 关闭）。  
下列**互斥配置组**请勿在同一 profile 中同时启用多套方案；按需复制对应片段到 `application.yml`、环境变量或 Spring profile 覆盖文件。

相关文档：[DEV_SETUP.md](./DEV_SETUP.md)、[PADDLEOCR_VL_DEPLOYMENT.md](./PADDLEOCR_VL_DEPLOYMENT.md)、[BUILTIN_DEEP_PARSE.md](./BUILTIN_DEEP_PARSE.md)。

---

## 1. 对象存储（二选一）

| 键 | 说明 |
|----|------|
| `knowbase.storage.type` | `local` **或** `minio` |

### 方案 A：本地文件系统（默认）

```yaml
knowbase:
  storage:
    type: local
    local-root: ./data/knowbase-storage
```

**不要**同时配置 `knowbase.storage.minio.*`。

### 方案 B：MinIO

```yaml
knowbase:
  storage:
    type: minio
    default-bucket: knowbase
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      auto-create-bucket: true
```

**不要**同时依赖 `local-root` 作为主存储。

---

## 2. PDF 视觉解析 / VLM（三选一）

控制键：`knowbase.vision-document.enabled` 与 `knowbase.vision-document.provider`。

| provider | 生效配置块 | 不适用 |
|----------|------------|--------|
| `paddleocr-vl` | `vision-document.paddleocr-vl.*` | `vllm.*`、`ollama.vision-language-model` |
| `vllm` | `vision-document.vllm.*` | `paddleocr-vl.*`、`ollama.vision-language-model` |
| `ollama` | `ollama.vision-language-model` | `paddleocr-vl.*`、`vllm.*` |

`enabled: false` 时整组 VLM 不生效，PDF 走内置启发式 + Tesseract OCR 回退。

### 方案 A：官方 PaddleOCR-VL（生产推荐）

部署见 [PADDLEOCR_VL_DEPLOYMENT.md](./PADDLEOCR_VL_DEPLOYMENT.md)。

```yaml
knowbase:
  ollama:
    vision-language-model: ""   # 必须留空，避免与官方 pipeline 冲突
  vision-document:
    enabled: true
    provider: paddleocr-vl
    timeout: 600s
    paddleocr-vl:
      base-url: http://localhost:8888
      layout-parsing-path: /layout-parsing
      pipeline-name: PaddleOCR-VL-1.6
      prettify-markdown: true
      return-markdown-images: false
      visualize: false
```

### 方案 B：独立 vLLM OpenAI 接口（快速验证）

```yaml
knowbase:
  ollama:
    vision-language-model: ""
  vision-document:
    enabled: true
    provider: vllm
    timeout: 600s
    vllm:
      base-url: http://localhost:8118
      chat-completions-path: /v1/chat/completions
      model: PaddleOCR-VL-1.6-0.9B
      api-key: ""
      temperature: 0.0
```

### 方案 C：Ollama 社区版 VLM

```yaml
knowbase:
  ollama:
    vision-language-model: MedAIBase/PaddleOCR-VL:0.9b
    vision-language-timeout: 120s
  vision-document:
    enabled: true
    provider: ollama
    timeout: 600s
```

---

## 3. PDF 版面 / 表格检测（二选一）

控制键：`knowbase.ingestion.layout.default-provider` 与 `knowbase.ingestion.layout.ollama.enabled`。

| default-provider | 需配合 | 说明 |
|------------------|--------|------|
| `local-pdf-layout` | `layout.ollama.enabled: false` | PDFBox 启发式（默认，无 Ollama 依赖） |
| `ollama-layout` | `layout.ollama.enabled: true` | Ollama 视觉 ML 表格检测，失败回退启发式 |

### 方案 A：启发式版面（默认）

```yaml
knowbase:
  ingestion:
    layout:
      default-provider: local-pdf-layout
```

### 方案 B：Ollama ML 表格检测

需 Ollama 与 vision 模型（见 §2 方案 C 或独立 vision 模型）。

```yaml
knowbase:
  ingestion:
    layout:
      default-provider: ollama-layout
      ollama:
        enabled: true
        model: ""   # 空则使用 ollama.vision-language-model
        fallback-to-heuristic: true
```

**不要**在 `default-provider: local-pdf-layout` 时单独开启 `layout.ollama.enabled: true`（会被忽略，且易混淆）。

---

## 4. 阅读顺序（三选一）

控制键：`knowbase.ingestion.reading-order.provider`。

| provider | 生效字段 | 忽略字段 |
|----------|----------|----------|
| `heuristic` | `fallback-to-heuristic` | `endpoint`、`ollama-model` |
| `http` | `endpoint`、`timeout` | 以 HTTP 服务为主；失败可回退启发式 |
| `ollama` | `ollama-model`、`timeout` | 需 Ollama；`endpoint` 非主路径 |

### 方案 A：启发式 bbox（默认，无外部依赖）

```yaml
knowbase:
  ingestion:
    reading-order:
      provider: heuristic
      fallback-to-heuristic: true
      timeout: 30s
```

### 方案 B：专用 HTTP 阅读顺序模型

```yaml
knowbase:
  ingestion:
    reading-order:
      provider: http
      endpoint: http://localhost:8090/reading-order
      fallback-to-heuristic: true
      timeout: 30s
```

### 方案 C：Ollama reading-order 模型

```yaml
knowbase:
  ingestion:
    reading-order:
      provider: ollama
      ollama-model: knowbase-reading-order
      fallback-to-heuristic: true
      timeout: 30s
```

---

## 5. OCR 引擎（二选一，默认 + Profile 覆盖）

全局默认：`knowbase.ingestion.ocr.default-engine`。

| 值 | 依赖 |
|----|------|
| `tesseract` | 本机 Tesseract（默认） |
| `paddle` | PaddleOCR HTTP 端点（通过 Document Profile `options.paddleOcrEndpoint` 或 metadata 传入） |

```yaml
knowbase:
  ingestion:
    ocr:
      default-engine: tesseract   # 或 paddle
      language: chi_sim+eng       # tesseract 语言包；paddle 时可为 auto
      confidence-threshold: 0.6
      downweight-mode: downweight # filter | downweight | review
```

同一文档解析路径只选一个引擎；不要在同一 Profile 混用两套 endpoint 而不指定 `ocrEngine`。

---

## 6. Flyway 迁移归属（二选一）

| 运行模式 | `spring.flyway.enabled` | `knowbase.flyway.enabled` |
|----------|-------------------------|---------------------------|
| 独立 `knowbase-app`（默认） | `true` | `false` |
| 宿主嵌入 Starter | `false`（宿主负责） | `true` |

两套同时 `enabled: true` 会导致重复迁移或冲突。

---

## 7. 外接文档解析器 vs 内置解析器

外接与内置**按 Document Profile 的 `parserCode` 互斥**，非 application.yml 全局开关。

| parserCode | 必需 options / 环境变量 |
|------------|-------------------------|
| `docling` / `unstructured` / `external` | `externalParserEndpoint` 或 `KNOWBASE_EXTERNAL_PARSER_ENDPOINT` |
| 内置（如 `pdf-layout`、`docx-structure`） | 不要设置外接 endpoint |

外接失败时默认 `externalParserFallback: true` 回退内置 parser。详见 [LIBRARY_PRESET_PRODUCT_GUIDE.md](./LIBRARY_PRESET_PRODUCT_GUIDE.md) 与入库 API 文档。

---

## 8. 推荐组合（场景速查）

| 场景 | storage | vision-document | layout | reading-order |
|------|---------|-----------------|--------|---------------|
| 本地开发（默认 yml） | `local` | `enabled: false` | `local-pdf-layout` | `heuristic` |
| 生产 + 扫描 PDF | `minio` | `paddleocr-vl` | `local-pdf-layout` | `http` 或 `ollama` |
| 仅 Ollama、无官方 VLM | `local`/`minio` | `ollama` + vision 模型 | `ollama-layout` | `ollama` |
| CI / 无 GPU | `local` | `enabled: false` | `local-pdf-layout` | `heuristic` |

---

## 9. 仍保留在 application.yml 的非互斥项

下列键与上述互斥组无关，可按环境直接调整：

| 前缀 | 用途 |
|------|------|
| `server.port` | HTTP 端口（项目脚本默认 8088） |
| `spring.datasource.*` | PostgreSQL 连接 |
| `knowbase.ollama.embedding-*` / `chat-model` | 向量化与问答（与 VLM provider 独立） |
| `knowbase.ingestion.pdf.vl-*` | VLM 路由阈值（仅当 vision-document 启用时生效） |
| `knowbase.ingestion.document-upsert-enabled` | 文档 upsert vs 快照模式 |
| `knowbase.upload.*` | 上传大小与批量上限 |
| `knowbase.ingestion.summary.*` | 文档 LLM 摘要 |
| `knowbase.ingestion.evidence-artifacts.*` | PDF 页 PNG 证据资产 |

完整 Java 默认值见 `knowbase-autoconfigure/.../KnowbaseProperties.java`。
