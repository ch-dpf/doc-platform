# Phase 2: 文件类型设定矩阵 - Context

**Gathered:** 2026-06-10
**Status:** Ready for planning

<domain>
## Phase Boundary

交付 `.planning/docs/FILE-TYPE-PROCESSING.md`：为 pdf / word / excel / txt / markdown 及常见表格形态定义**推荐设定 → 产出形态 → 质量风险**对照，满足 TYPE-01–TYPE-05；与 `application.yml` 系统默认、`libraryDefaults.js` 向导默认、库向导 `config_json` 字段路径一一对应。

本阶段**文档-only**，不写应用代码。MIME 自动默认引擎、结构化 ingest 实现、库预设 UI 分别由 Phase 3 及 backlog 承接，但须在目标态文档中完整描述映射与差距。

一期覆盖**绝大部分常用文档类型与表格类型**（含扫描 PDF、文本 PDF、周报/明细类 xlsx、制度 docx、txt/md）。

</domain>

<decisions>
## Implementation Decisions

### 规划基线（目标态 vs 现状）

- **D-01:** 延续 Phase 1 D-01——以**合理目标架构**为主叙述；现状（Tika 扁平化、structured 仅 HTML 管道等）仅在「当前差距」脚注或附录少量引用，不主导正文。
- **D-02:** 表格类文件在目标态中应通过**结构化处理**保证分块向量化**准确可控**；v1 工程仍以文档描述路径 + backlog 标注，不承诺 POI/双轨在本阶段落地。

### 文档结构与矩阵（TYPE-01–04 载体）

- **D-03:** 主结构 = **单张大表**（便于横向对比）：行 = **类型 × 规则项**（完整管道维度）；列 = **设定 | 产出 | 质量**（对齐 ROADMAP「三列表」）。
- **D-04:** 行粒度 = **完整管道**——每类型覆盖：`parsing`（`ocrEnabled`、`tableExtraction`）、`cleaning`、`chunking`（`strategy`/`size`/`overlap` 等）、`IndexingChunkFilter`/表头过滤、典型产出形态、质量风险。
- **D-05:** 类型范围：pdf、word、excel、txt、markdown；表格场景在 Excel 行组内展开（周报 xlsx vs 明细表等可在行内子场景说明，不拆独立章节文件）。

### MIME 自动默认（目标态，供 Phase 3 预设引用）

- **D-06:** 独立**附录「MIME → 推荐 config_json 片段」**完整表（`application/pdf`、xlsx、docx、txt、md 等）；标注「文档规划，Phase 3 预设引用」，非 v1 运行时引擎。
- **D-07:** MIME 默认层级（对齐 Phase 1 D-05/D-06）：MIME 映射值 = **库级初始默认**；垂直专用库可覆盖；**采集级目标态**允许单次覆盖（ingest profile backlog）。

### Excel / structured 边界（TYPE-03）

- **D-08:** 目标态：表格类文件入库应走**结构化文档处理**，方能在分块/向量化层保证数据准确可控。
- **D-09:** 现状差距显式写出：`tableExtraction: structured` 对 xlsx **不适用**（`DocumentParseService` Tika 纯文本）；v1 运营仍用 `text-only` + `paragraph-first` + 表头过滤作为**过渡推荐**。
- **D-10:** **显式 backlog** 引用 PROJECT Out of Scope「结构化双轨 + QueryRouter」与 CONCERNS Tech Debt（POI 行对象、双轨模型）；不暗示 v1 可实现结构化 Excel ingest。

### 反模式（TYPE-05）

- **D-11:** **承接 Phase 1 §8 扩展**——§8 保留通用反模式；本文件每类型 **2–3 行类型专属**「错误设定 → 质量问题」，链回 §8，不重复杜鹏飞等通用行。
- **D-12:** **不强制**绑定真实文件样本；以纯设定对照为主（用户选择）。有 fixture 的类型（如杜鹏飞 xlsx）可脚注引用，非验收硬性要求。

### 受众与默认值对照

- **D-13:** **延续双 TOC**——`#ops-guide`：主矩阵推荐/禁止 + 类型反模式；`#dev-reference`：`config_json` 字段路径 + 代码锚点。
- **D-14:** **三层默认值对照表**（每规则项或汇总附录）：`application.yml` 系统默认 | `libraryDefaults.js` 向导默认 | 类型推荐值 | 一致/差异说明。

### Claude's Discretion

- 主表与附录的章节编号、MIME 附录命名、Excel 场景子行是否用缩进行 vs 脚注——由 planner 按可读性决定；须保证运营与开发各有可追踪入口。
- 目标态「结构化表格处理」叙述深度与过渡推荐（text-only）的并列方式——planner 在 RESEARCH 后定稿，须同时满足 D-08 与 D-09。

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 里程碑与需求
- `.planning/PROJECT.md` — Out of Scope（结构化双轨）、Core Value
- `.planning/REQUIREMENTS.md` — TYPE-01 … TYPE-05
- `.planning/ROADMAP.md` — Phase 2 目标、交付物、success criteria
- `.planning/STATE.md` — 当前进度
- `.planning/docs/INGEST-PIPELINE.md` — §5 配置矩阵、§7/§8 质量与反模式、附录 B backlog；**交叉引用，不重复 PIPE 流程**

### Phase 1 决策（继承）
- `.planning/phases/01-ingest-pipeline/01-CONTEXT.md` — D-01–D-19，尤其 D-17（按类型细表归 Phase 2）、D-09（MIME 自动默认）

### 代码库地图
- `.planning/codebase/ARCHITECTURE.md` — 解析/分块管道
- `.planning/codebase/CONCERNS.md` — Excel 扁平化、structured 限制、表头过滤、结构化双轨 tech debt
- `.planning/codebase/STRUCTURE.md` — 前后端目录

### 系统与向导默认
- `knowbase-service/src/main/resources/application.yml` — `ingest.ocr`、`chunking.*` 系统默认
- `frontend/knowbase-ui/src/utils/libraryDefaults.js` — `defaultLibraryConfig`、`WIZARD_STEPS`、`FILE_TYPE_OPTIONS`

### 解析 / 分块锚点
- `knowbase-service/src/main/java/com/knowbase/library/config/VectorLibraryConfig.java` — `ParsingRulesSettings`、`ChunkingRulesSettings`
- `knowbase-service/src/main/java/com/knowbase/library/config/ParsingRulesSettings.java` — `tableExtraction`、`ocrEnabled`
- `knowbase-service/src/main/java/com/knowbase/ingest/service/DocumentParseService.java` — MIME 路由、Tika vs HTML 管道
- `knowbase-service/src/main/java/com/knowbase/ingest/parse/TableExtractionMode.java` — structured 语义
- `knowbase-service/src/main/java/com/knowbase/vector/chunk/IndexingChunkFilter.java` — 表头块过滤
- `knowbase-service/src/test/java/com/knowbase/vector/service/ChunkPreviewServiceTest.java` — 杜鹏飞 fixture（可选脚注）

### Phase 2 交付（待创建）
- `.planning/docs/FILE-TYPE-PROCESSING.md` — 主交付物

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `libraryDefaults.js` — 向导五步与 `defaultLibraryConfig` 字段形状；三层对照表的「向导默认」列源。
- `application.yml` — `chunking.strategy: paragraph-first`、`ingest.ocr` 等系统兜底；对照表「系统默认」列源。
- `VectorLibraryConfig` / `ParsingRulesSettings` — `tableExtraction`、`ocrEnabled` 字段名与文档矩阵行对齐。
- `INGEST-PIPELINE.md` §5.1 配置矩阵 — 16 行规则项命名可复用为矩阵行键。

### Established Patterns
- **库级 JSON 单源** — 类型推荐值写入 `config_json` 路径，经 `LibraryConfigResolver.*For(libraryId)` 生效（与 Phase 1 一致）。
- **Excel 现状** — Tika tab 分隔纯文本 → `paragraph-first` 分块 → `IndexingChunkFilter`；structured 仅 HTML（Word/PDF-derived）。
- **文档-only 波次** — 与 Phase 1 相同：grep 验收 + 人工 UAT，无代码变更。

### Integration Points
- Phase 3 `libraryPresets.js` 将引用本文件 MIME 附录与类型推荐行。
- Phase 4 PARITY 依赖本文件 chunking/filter 推荐与 §8 反模式一致。
- `CreateLibraryWizard.vue` / `EditLibrarySettingsDrawer.vue` — 字段路径对照 dev TOC。

</code_context>

<specifics>
## Specific Ideas

- 用户明确要求：**单张大表**横向对比，一期覆盖绝大部分常用文档与表格类型。
- 表格类目标态：**结构化处理**才能保证分块向量化准确可控；现状仅作少量参考。
- 反模式：**扩展 §8**，不强制真实样本；杜鹏飞等已有 fixture 可作脚注非硬性。
- ROADMAP 成功标准锚点：周报 xlsx、扫描 pdf、制度 docx 各有「推荐/禁止」——写入矩阵推荐/禁止列即可，不要求绑实体文件验收。

</specifics>

<deferred>
## Deferred Ideas

- **MIME 自动默认运行时引擎** — 文档附录定义，实现 Phase 3 预设或更后 backlog。
- **POI / 结构化 Excel ingest** — PROJECT Out of Scope；CONCERNS 双轨模型。
- **库预设 UI（PRESET-01–04）** — Phase 3。
- **预览=入库工程（PARITY）** — Phase 4。
- **v2 GATE-01/02 质量门禁** — backlog（Phase 1 附录 B 已列）。

</deferred>

---

*Phase: 2-文件类型设定矩阵*
*Context gathered: 2026-06-10*
