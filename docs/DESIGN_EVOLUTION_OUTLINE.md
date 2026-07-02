# KnowBase 架构演进修订提纲与二期排期

> 版本：草案 v0.1  
> 日期：2026-06-23  
> 状态：待评审  
> 关联文档：[DESIGN.md](./DESIGN.md)、[PHASE2_INGESTION_PLAN.md](./PHASE2_INGESTION_PLAN.md)、[API.md](./API.md)

## 1. 演进目标（一句话）

在**不推翻**初版 Pipeline、Profile 异构、发布治理与可观测护栏的前提下，将用户主路径从「知识库 → 索引版本 → 文档」调整为「**知识库 → 文档（持续入库）**」，把 `IndexVersion` 下沉为内部**索引代次（Index Generation）**治理机制。

## 2. 术语对照（评审用）

| 旧表述（初版用户可见） | 新表述（演进后） | 领域对象是否保留 |
|----------------------|----------------|----------------|
| 索引版本 `IndexVersion` | 索引代次 / 内部 generation | 保留表与模型，API 默认隐藏 |
| 每次入库发布新版本 | 文档写入 active generation | 行为变更 |
| `publishIndexOnSuccess` | 文档 `INDEXED` 即可检索（默认） | 批任务选项降级 |
| `IngestionRun` | 批处理任务 / 多文档入库运行 | 保留，语义收窄 |
| — | `DocumentIndexJob`（新增） | 单文档索引作业 |
| `PUBLISHED IndexVersion` | `library.active_index_generation_id` | 指针 + 状态机 |

## 3. DESIGN.md 修订提纲

以下按章节列出：**现状摘要 → 修订要点 → 建议正文方向**。正式合入时以 PR 形式修改 `DESIGN.md`。

---

### 3.1 §3.1 建仓入库（产品范围）

**现状摘要**

- 流程终点是「发布可检索索引版本」。
- 入库建模为完整 `IngestionRun`，一次 run 对应一次 Pipeline 生命周期。

**修订要点**

1. 将产品主流程改为两轨并行：
   - **文档轨（默认）**：上传 → 解析 → 分块 → 嵌入 → 写入 active generation → 文档状态 `INDEXED`。
   - **批任务轨（可选）**：目录/批量导入仍通过 `IngestionRun` 跟踪，内部拆为多个 `DocumentIndexJob`。
2. 「发布索引」从用户操作降为：
   - 日常：自动（文档索引成功即参与检索）。
   - 运维：全库 rebuild、换模型、蓝绿 `promote` 时显式切换 generation。
3. 保留「追踪入库任务、统计、失败原因」能力，粒度扩展到**文档级**。

**建议新增小节：`§3.1.1 文档持续入库`**

```text
- 单文档 CRUD：上传、查看、删除、重索引。
- 文档状态机：UPLOADED → PARSING → CHUNKING → EMBEDDING → INDEXED | FAILED。
- 删除文档时同步删除 chunk 与 embedding（数据治理 §14.7）。
- 批量目录导入视为创建 N 个 DocumentIndexJob，由 IngestionRun 聚合进度。
```

**建议修订小节：`§3.1.2 索引代次与发布治理（内部）`**

```text
- IndexVersion 表示知识库在某一时刻的可检索向量快照（索引代次）。
- 每个 Library 维护 active_index_generation_id，检索只读该代次。
- 以下情况创建新代次并蓝绿切换：embedding 模型/维度/tokenizer 变更、全库 rebuild、管理员 promote。
- 日常加文档不递增用户可见版本号。
```

---

### 3.2 §3.5 同库异构文档入库

**现状摘要**

- 已规定：同库可异构解析/切块；同一索引版本内统一 embedding 与 tokenizer。
- 检索阶段按文档类型融合。

**修订要点**

1. 显式引入**三层分块配置契约**（与现有实现 `SegmentationConfigResolver` 对齐）：

| 层级 | 配置载体 | 内容 | 变更影响 |
|------|----------|------|----------|
| L1 索引不变量 | `LibraryProfile` / active generation | embedding、dimension、tokenizer、chunk 上限 | 新 generation + 全库或批量 re-embed |
| L2 路由 Profile | `DocumentProfile` | parser、chunkingStrategy、结构策略、options | 命中 Profile 的文档 reindex |
| L3 文档覆盖 | `document.segmentation_override`（可选） | 单文档调参，需审计 | 仅该文档 reindex |

2. 删除「库级单一分块参数适用全库」的隐含假设；库级只保留**硬上限与向量空间**。
3. 补充：异构 chunk 共存于同一 generation 时，靠 chunk 元数据 + `contentFamilyWeights` + 检索测试回归保证可答率，而非强迫统一切块形态。

**建议新增：`§3.5.1 分块配置变更与重索引策略`**

```text
- L1 变更 → 提示全库 rebuild，创建新 generation，评测通过后 promote。
- L2 变更 → 列出受影响 content_family / profile，支持按 Profile 批量 reindex。
- L3 变更 → 单文档 reindex；默认关闭，仅 Preview 调参通过后固化。
```

---

### 3.3 §4 架构原则

**现状第 2 条**

> 索引版本发布：问答只读取已发布的索引版本。

**修订为**

> **索引代次发布**：问答只读取 Library 当前 active 且已提交的索引代次；文档在索引完成前对检索不可见。

**建议新增第 11 条**

> **文档一等、代次内化**：用户路径以文档为管理单位；索引代次用于破坏性变更、全量重建与回滚，不作为日常入库必经步骤。

其余原则（Pipeline 优先、多库、智能体、预设、可观测）**不变**。

---

### 3.4 §7 数据模型

#### §7.1 `kb_library` 增补字段

```text
active_index_generation_id  UUID NULL      -- 当前可检索代次
index_manifest_hash         VARCHAR(64)    -- 可观测指纹（可选，二期末）
```

#### §7.7 索引版本 → 索引代次

**修订定义**

```text
kb_index_version 表示知识库的一个索引代次（内部 Index Generation），
不是用户每次入库的操作对象。

字段语义调整：
- version：代次序号，仅在创建新代次时递增。
- status：BUILDING | DRAFT | COMMITTED | ARCHIVED | FAILED
  （COMMITTED 对应原 PUBLISHED；可考虑别名兼容）
- profile_id：创建该代次时的 LibraryProfile 快照引用。

约束：
- 每个 library 最多一个 COMMITTED 代次作为 active（由 active_index_generation_id 指向）。
- 旧 COMMITTED 代次在 promote 后标记 ARCHIVED，保留回滚能力。
```

#### 新增 §7.9 知识文档（提升为一等）

```text
kb_document 增补：
- status              VARCHAR(32)   -- UPLOADED|PARSING|...|INDEXED|FAILED
- document_profile_id UUID
- source_uri, title, content_hash, file_size, mime_type
- index_generation_id UUID          -- 当前 chunk 所属代次（= active）
- last_indexed_at     TIMESTAMPTZ
- last_error          TEXT

kb_document_index_job（新增，可选与 ingestion_run 合并早期实现）：
- job_id, document_id, library_id, run_id NULLABLE
- status, stage, message, created_at, updated_at
```

#### §7.8 Embedding / §kb_chunk

- `index_version_id` 保留列名（减少迁移成本），文档注释改为 `index_generation_id`。
- chunk 元数据**必填**：`documentProfileCode`、`contentFamily`、`chunkRole`、`tokenizerId`。

---

### 3.5 §10 前端设计

#### §10.1 知识库

| 删除/降级 | 新增/强化 |
|-----------|-----------|
| 「查看已发布索引版本」作为主 Tab | **文档列表**：状态、类型、Profile、chunk 数、最近索引时间 |
| 按 indexVersion 筛选文档 | 文档详情：chunk 预览、重索引、删除 |
| — | 高级/运维：索引代次历史、rebuild、promote（管理员） |

#### §10.2 入库任务

| 修订 |
|------|
| 保留批量导入、目录扫描入口 |
| 任务详情展示**文档级**进度表（成功/失败/重试） |
| 默认不再突出「发布策略」开关；批处理高级选项可保留 |
| 单文件上传迁入「知识库 → 文档 → 上传」 |

#### 新增 §10.5 召回评测（与二期对齐）

- 黄金问答集管理（库级）
- 一键 RetrievalTest + Recall@k 报告
- Profile / 解析变更前后对比（回归门禁）

---

### 3.6 §11 API 草案

#### 用户主路径 API（新增）

```http
POST   /api/v1/libraries/{libraryId}/documents
GET    /api/v1/libraries/{libraryId}/documents
GET    /api/v1/libraries/{libraryId}/documents/{documentId}
DELETE /api/v1/libraries/{libraryId}/documents/{documentId}
POST   /api/v1/libraries/{libraryId}/documents/{documentId}/reindex
GET    /api/v1/libraries/{libraryId}/documents/{documentId}/chunks
POST   /api/v1/libraries/{libraryId}/documents/preview    # 复用现有 preview
```

#### 运维 / 治理 API（由原 index-versions 降级）

```http
GET    /api/v1/libraries/{libraryId}/index-generations
POST   /api/v1/libraries/{libraryId}/index-generations/rebuild
POST   /api/v1/libraries/{libraryId}/index-generations/{id}/promote
```

#### 兼容策略（2026-07 已执行）

| 旧 API / 参数 | 处理 |
|--------|------|
| `GET .../index-versions` 及同族 publish/detail | **已移除**；客户端改用 `index-generations` |
| `POST .../ingestion-runs` | 保留，内部为 document job 聚合 |
| REST 层 `publishIndexOnSuccess` 参数 | **已移除**；document upsert 模式下文档成功即 `INDEXED` 可检索；内部 rebuild 等场景通过 `options.publishIndexOnSuccess` 控制 |

#### 评测 API（二期）

```http
POST   /api/v1/libraries/{libraryId}/retrieval-evaluations
GET    /api/v1/libraries/{libraryId}/retrieval-evaluations/{evalId}
```

（可与现有 `agents/{id}/retrieval-tests` 复用核心逻辑，库级黄金集绑定 Library。）

---

### 3.7 §14 工程护栏

#### §14.1 状态机

**新增 `DocumentStatus`**

```text
UPLOADED → PARSING → NORMALIZING → CHUNKING → EMBEDDING → INDEXED
                                                      ↘ FAILED
```

**修订 `IndexVersion` 状态**

- `PUBLISHED` → 文档中可写为 `COMMITTED`（代码枚举可保留 PUBLISHED 别名）。
- 明确：仅 `active` 且 `COMMITTED/PUBLISHED` 的代次可被检索。

**`IngestionRun`**

- 保留；增加 `document_jobs_total` / `document_jobs_failed` 聚合字段（可选）。

#### §14.6 模型版本化

**不变**：embedding/tokenizer 变更必须新代次。  
**补充**：L2 Profile 变更不强制新代次，触发受影响文档 reindex。

#### §14.7 数据治理

**强化**（原文仅有原则）：

```text
- DELETE document → 同事务删除 chunk、embedding、可选 object storage 对象。
- reindex → 先写 staging 或同代次 replace（delete by document_id + insert）。
- generation promote → 旧代次 ARCHIVED，不立即物理删除（保留回滚窗口）。
```

#### §14.3 可观测 trace span 增补

| 原 span | 演进 |
|---------|------|
| `publish_index_version` | 保留用于 promote；日常增加 `commit_document_index` |
| — | `document_index_job` 作为文档轨根 span |

---

### 3.8 §12 实施路线（初版路线图脚注）

在 §12 末尾增加：

```text
### 12.2 一期后架构演进（v1.1）

在二期实施过程中并行推进：
- Phase A：文档 API 与 UI 主路径切换（可仍写 IndexVersion）。
- Phase B：文档级 upsert + active_index_generation_id。
- Phase C：IndexVersion 仅用于 rebuild / 换模型；批任务与评测门禁闭环。

详见 docs/DESIGN_EVOLUTION_OUTLINE.md 与二期排期 §5。
```

---

## 4. 关联文档修订提纲

### 4.1 API.md

> **2026-07 已同步**：REST 明细见 `API.md` §3；废弃的 `index-versions` 与 REST 层 `publishIndexOnSuccess` 已清理。§5 实现摘要与前端入库路径已与 `knowbase-ui` 对齐。

| 章节 | 状态 |
|------|------|
| §3.5 文档与索引代次 | 已实现（含 batch-delete、pipeline-trace、generate-drafts） |
| §3.9 / §3.11 入库上传与 prepare | 已区分 API 客户端路径与前端向导实际调用链 |
| index-generations | 唯一对外索引代次 REST 命名 |
| `listDocuments` | 默认不传 `indexVersionId`，返回 active 代次下分页文档 |

### 4.2 PHASE2_INGESTION_PLAN.md

| 章节 | 修订 |
|------|------|
| §4.7 Profile 管理 | 「创建新索引版本」改为「是否触发 reindex / rebuild generation」决策树 |
| §4.8 入库任务运维 | 合并文档级 reindex、失败重试、rebuild、promote |
| §4.4 / §3.4 评测 | 明确黄金集绑定 Library；Recall@k 为 promote/reindex 门禁 |
| §5 实施顺序 | 采用本文 §5 重排版本 |
| §6 非目标 | 增补：「不改变 Parser/Chunker SPI」「不删除 IndexVersion 领域对象」 |
| 新增 §4.10 | 索引代次与文档 upsert（演进专项） |

### 4.3 README.md

- 核心流程第 1 条改为：「建仓 → **持续文档入库** → 向量索引（代次治理）」。
- 验证脚本说明：样例目录导入仍可用，内部行为改为文档 job。

### 4.4 INGESTION_INTERFACES.md

- `MetadataContext` 补充 `documentStatus` / `indexGenerationId`。
- Pipeline 阶段说明：`WriteIndex` 语义改为 upsert by `document_id`。

---

## 5. 二期排期（重排版）

### 5.1 分期原则

1. **Parser 质量与数据模型演进解耦**：PDF/OCR/表格增强不依赖 upsert 先完成。  
2. **评测集尽早落地**：第 2 波即引入黄金集，后续改动可回归。  
3. **文档轨与代次内化在第 3 波**：在 Profile 管理、任务运维之前完成最小 upsert。  
4. **Citation 与多模态放最后**：依赖 chunk metadata 稳定。

### 5.2  waves 总览

```text
Wave 1  解析基础与契约（原二期 1–2 + Schema）
Wave 2  复杂文档质量 + 评测基线（原二期 3–5）
Wave 3  文档持续入库与索引代次内化（演进专项，新增）
Wave 4  Profile 治理 + 入库运维（原二期 7–8，合并）
Wave 5  可观测 + Citation 闭环（原二期 9–10 + 4.5–4.6）
```

### 5.3 详细排期

#### Wave 1 — 解析契约与 OCR 基础（约 2–3 周）

| 序号 | 交付项 | 来源 | 验收 |
|------|--------|------|------|
| W1-1 | `external-parser.schema.json` + mock adapter | 二期 §3.5, §4.3 | Docling/Unstructured mock 可映射 ParsedDocument |
| W1-2 | OCR hOCR/TSV/JSON 解析器 + confidence/bbox | 二期 §3.2 | OCR 样本块带 confidence |
| W1-3 | `OcrEngineAdapter` SPI 草案（接口 only） | 二期 §4.2 | 不绑定具体引擎实现 |

**不依赖**：文档 upsert、前端文档页。

---

#### Wave 2 — 复杂文档质量 + 评测基线（约 3–4 周）

| 序号 | 交付项 | 来源 | 验收 |
|------|--------|------|------|
| W2-1 | PDF 多栏阅读顺序 + 表格区域增强 | 二期 §3.1, §4.1 | 多栏/表格样本 chunk 顺序回归 |
| W2-2 | Excel 多级表头、合并单元格、公式 | 二期 §3.3 | 表格 metadata 快照测试 |
| W2-3 | `sample-documents/` 回归集 + chunk 边界快照测试 | 二期 §3.4, §4.4 | 5 类文档 × ≥3 样本 |
| W2-4 | 库级黄金问答集 JSON 格式 + 手工样例 10–20 条 | 演进 + 二期 §4.4 | 格式定稿，可手工跑 retrieval-test |
| W2-5 | Docling/Unstructured adapter（可选部署） | 二期 §3.5 | Profile 级开关，失败 fallback Java |

**里程碑 M2**：复杂文档 chunk 质量可回归；黄金集可手工评测。

---

#### Wave 3 — 文档持续入库与索引代次内化（约 3–4 周）★ 演进核心

| 序号 | 交付项 | 依赖 | 验收 |
|------|--------|------|------|
| W3-1 | **Phase A**：文档 REST API（create/list/get/delete）+ 状态机 | W2-3 | 上传单文件可在库下看到文档记录 |
| W3-2 | `active_index_generation_id` + 库创建时初始化 generation | W3-1 | 新知识库自动有 active 代次 |
| W3-3 | **Phase B**：`WriteIndex` 改为 document upsert（delete chunks by document_id + insert） | W3-2 | 同库连续上传 A、B，检索同时命中 |
| W3-4 | `IngestionRun` 拆为文档 job 聚合（目录批导入） | W3-3 | `verify-sample-documents.ps1` 通过 |
| W3-5 | **Phase C**：新 embedding/tokenizer 才新 generation；`promote` API | W3-3 | 换模型走 rebuild，日常入库不 bump version |
| W3-6 | 前端：知识库文档 Tab 替代索引版本主 Tab | W3-1 | 用户路径库 → 文档 |
| W3-7 | `publishIndexOnSuccess` 语义迁移 + 旧 API 清理 | W3-5 | **已完成**（REST 参数与 `index-versions` 路径已移除，`API.md` 已更新） |

**里程碑 M3（北极星基线）**：同一库持续加文档可检索；代次对用户不可见；黄金集可脚本跑 Recall@k。

**实现注意**：W3-3 可先不做 staging 双缓冲，采用「同代次 replace + 短锁」；promote 双缓冲放到 W3-5。

---

#### Wave 4 — Profile 治理 + 入库运维（约 2–3 周）

| 序号 | 交付项 | 来源 | 验收 |
|------|--------|------|------|
| W4-1 | Document Profile CRUD + 启停 | 二期 §4.7 | 前端 Profile 管理页 |
| W4-2 | Library Profile 版本列表 + 对比 + 复制 | 二期 §4.7 | L1 变更提示 rebuild |
| W4-3 | L2 变更 → 按 Profile 标记文档 `REINDEX_REQUIRED` + 批量 reindex | 演进 §3.5.1 | 改 PDF Profile 只重索引 PDF |
| W4-4 | IngestionRun 列表/筛选/取消 + 文档级失败重试 | 二期 §4.8 | 运维页可操作 |
| W4-5 | index-generations 运维 API + rebuild | 二期 §4.8 + 演进 | 管理员可 promote / 回滚 |
| W4-6 | **评测门禁**：Profile/L1 变更后自动跑黄金集，Recall@k 下降 > 阈值则阻断 | 演进 | CI 或手动门禁 |

**里程碑 M4**：Profile 可调、可回归；运维闭环完整。

---

#### Wave 5 — 可观测 + Citation 闭环（约 2–3 周）

| 序号 | 交付项 | 来源 | 验收 |
|------|--------|------|------|
| W5-1 | Pipeline 阶段耗时 + 文档级失败分布指标 | 二期 §4.9 | 任务详情瀑布图 |
| W5-2 | runId ↔ trace ↔ 文档 联动 | 二期 §4.9 | 双向跳转 |
| W5-3 | Evidence/Citation 扩展 page、bbox、sheet、cell | 二期 §4.5 | 检索测试结果含坐标 |
| W5-4 | 前端引用展示 + 低置信度 OCR 标记 | 二期 §4.5 | 问答页可定位 |
| W5-5 | 多模态证据 asset（可选） | 二期 §4.6 | PDF 页截图 URI |
| W5-6 | `index_manifest_hash` + 评测报告归档 | 演进 | 可回答「当前库是什么配置」 |

**里程碑 M5（二期完成）**：Citation 闭环；可观测达标；Recall@k 报告可归档。

---

### 5.4 甘特依赖（简图）

```text
W1 ──────► W2 ──────► W3 ──────► W4 ──────► W5
          │           ▲
          └─ 评测集 ───┘
                      W3 与 W2-5 可部分并行（adapter）
```

### 5.5 与原二期 §5 顺序对照

| 原顺序 | 新归属 |
|--------|--------|
| 1 外部 Schema | W1-1 |
| 2 OCR | W1-2 |
| 3 PDF | W2-1 |
| 4 Excel | W2-2 |
| 5 回归集 | W2-3, W2-4 |
| 6 Docling adapter | W2-5 |
| 7 Profile 管理 | W4-1, W4-2, W4-3 |
| 8 入库运维 | W4-4, W4-5（部分提前至 W3） |
| 9 可观测 | W5-1, W5-2 |
| 10 Citation | W5-3, W5-4, W5-5 |

---

## 6. 迁移与兼容（实施 Wave 3 时执行）

### 6.1 数据迁移

1. 为每个现有 `library` 选取最新 `PUBLISHED` IndexVersion → 写入 `active_index_generation_id`。
2. 历史 `DRAFT`/`FAILED` 代次保留，不参与检索。
3. `kb_document` 回填 `status=INDEXED`（若已有 chunk）。

### 6.2 API 兼容

- 旧 `index-versions` REST 路径已移除；客户端统一使用 `index-generations`。
- `ingestion-runs` 及全局 `GET /api/v1/ingestion-runs/{runId}` 保持不变。
- 功能开关 `knowbase.ingestion.document-upsert-enabled=false` 仍可回退 run 快照模式（内部 legacy pipeline）。

### 6.3 回滚策略

- Wave 3 功能开关：`knowbase.ingestion.document-upsert-enabled=false` 回退 run 快照模式。
- generation promote 失败：指针不回写，旧代次仍 active。

---

## 7. 北极星指标（二期全程）

| 指标 | 定义 | 目标（初版，可评审调整） |
|------|------|--------------------------|
| **Recall@8** | 黄金集期望 chunk 出现在 top-8 | ≥ 85%（库级均值） |
| **Stratified Recall** | 按 contentFamily 分层 | 每类 ≥ 75% |
| **MRR** | 首个正确 chunk 排名 | ≥ 0.6 |
| **Chunk coverage** | 入库后含期望关键词的 chunk 存在率 | ≥ 95% |
| **Regression delta** | Profile 变更前后 Recall@8 差 | ≤ -2% 否则阻断 promote |

评测入口：库级 `retrieval-evaluations` + 现有 `agents/.../retrieval-tests`。

---

## 8. 开放问题（评审清单）

| # | 问题 | 建议默认 | 需确认 |
|---|------|----------|--------|
| Q1 | 枚举 `PUBLISHED` 是否改名为 `COMMITTED` | 代码保留 PUBLISHED，文档双写 | 产品 |
| Q2 | `DocumentIndexJob` 独立表 vs 复用 `IngestionRun` 子记录 | Wave 3 先复用 run + document 状态，后期拆表 | 架构 |
| Q3 | 文档 upsert 是否 staging 双缓冲 | 先 replace；promote 时双缓冲 | 架构 |
| Q4 | 黄金集谁维护 | 库管理员 UI + JSON 导入 | 产品 |
| Q5 | Recall 门禁阈值 | 上表默认值 | 业务 |
| Q6 | 旧代次物理删除策略 | promote 后保留 2 代、30 天 | 运维 |

---

## 9. 评审通过后的合入顺序

1. 评审本文档，确认 Q1–Q6。  
2. PR-1：更新 `DESIGN.md`（§3.1, §3.5, §4, §7, §10, §11, §12, §14）。  
3. PR-2：更新 `PHASE2_INGESTION_PLAN.md` §4.7–4.8, §5。  
4. PR-3：更新 `API.md` 文档 API 草案 + deprecated 说明。  
5. 开发从 **Wave 1** 起按 §5.3 执行；**Wave 3** 启动前再开 implementation design（表迁移脚本、锁策略）。

---

## 10. 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| v0.1 | 2026-06-23 | 初稿：修订提纲 + 二期 waves 重排 |
| v0.1.1 | 2026-06-23 | 标注 API.md 已与当前实现对齐；演进项仍待 Wave 3 |
