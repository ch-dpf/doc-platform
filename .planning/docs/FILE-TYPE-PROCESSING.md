---
last_mapped_commit: 789c40a
analysis_date: 2026-06-10
focus: file-type-processing
---

# 按文件类型处理设定矩阵

**Analysis Date:** 2026-06-10

> **Phase 2 说明：** 本里程碑为**文档-only**交付，不修改应用代码。目标态叙述为主（D-01）；Excel structured 差距在附录 B 标注，不主导正文。

## 目录

| 受众 | 锚点 | 推荐阅读章节 |
|------|------|--------------|
| 运营 | [#ops-guide](#ops-guide) | §2 主矩阵（推荐/禁止/质量风险）、§4 类型反模式 |
| 开发 | [#dev-reference](#dev-reference) | §3 三层默认值、附录 A MIME 映射、附录 C 字段路径 |

---

## §1 范围与读者指南 {#ops-guide} {#dev-reference}

### 里程碑核心价值

> **Core Value（PROJECT.md）：** 运营人员按文件类型选对库预设并完成采集后，**预览所见分块与入库结果一致**，且分块内容足以支撑后续检索与问答（不因错误设定导致表头块、续行拆开、OCR 缺失等问题）。

### 目标态 vs v1 交付边界（D-01、D-02）

| 维度 | 目标态（文档叙述） | v1 里程碑交付 |
|------|-------------------|---------------|
| 按类型矩阵 | 单张大表：类型 × 规则项 → 设定 / 产出 / 质量 | §2 骨架 + Plan 02 填充推荐值 |
| 表格类 ingest | 结构化处理保证分块向量化准确可控（D-08） | Tika tab 纯文本 + `paragraph-first` 过渡推荐；差距见附录 B |
| MIME 自动默认 | 附录 A 完整映射 | 文档规划；Phase 3 预设引用；**非** v1 运行时引擎 |
| 结构化 Excel | POI 行对象 / 双轨模型 | **Backlog** — PROJECT Out of Scope；不暗示 v1 可实现 |

**D-01：** 本文以合理目标架构为主叙述；现状（Tika 扁平化、`structured` 仅 HTML 管道等）仅在「当前差距」脚注或附录 B 少量引用，不主导正文。

**D-02：** 表格类文件在目标态中应通过结构化处理保证分块向量化准确可控；v1 仍以文档描述路径 + backlog 标注，不承诺 POI/双轨在本阶段落地。

### 与全链路文档关系

- **流程与 API：** 建库、入库九阶段、Resolver 生效路径见 [INGEST-PIPELINE.md §2–§4](./INGEST-PIPELINE.md#2-建库流程pipe-01)；本文**不**重复 PIPE 流程。
- **通用质量准则：** 块自洽、表头过滤、预览≠入库见 [INGEST-PIPELINE.md §7](./INGEST-PIPELINE.md#7-分块质量准则) 与 [§8 反模式](./INGEST-PIPELINE.md#8-反模式对照)。
- **配置层级模型：** 系统 / 库默认 / 采集覆盖（目标态）见 [INGEST-PIPELINE.md §5](./INGEST-PIPELINE.md#5-三层配置矩阵)；本文 §3 仅对照系统默认与向导默认，不重复完整四层 ingest 矩阵。
- **按类型细表：** Phase 1 D-17 将 TYPE-01–05 归本文件；Phase 3 预设引用附录 A MIME 片段。

### Phase 2 覆盖范围

本文件覆盖 **pdf / word / excel / txt / markdown** 五类及常见表格形态（含扫描 PDF、文本 PDF、周报/明细类 xlsx、制度 docx、txt/md）。

| 能力 | 本阶段 | 后续 |
|------|--------|------|
| 按类型推荐矩阵 | §2（Plan 02 填充单元格） | — |
| MIME 运行时自动默认 | 附录 A 规划表 only | Phase 3 `libraryPresets.js` |
| 结构化 Excel ingest | 附录 B backlog | 另立里程碑 |
| 预览=入库一致性 | 反模式链回 INGEST-PIPELINE §8 | Phase 4 PARITY |

### 需求可追溯

| Requirement | Section | Status |
|-------------|---------|--------|
| TYPE-01 | §2 PDF 行组 | Placeholder（Plan 02） |
| TYPE-02 | §2 Word 行组 | Placeholder（Plan 02） |
| TYPE-03 | §2 Excel 行组 + 附录 B | Placeholder（Plan 02） |
| TYPE-04 | §2 TXT/Markdown 行组 | Placeholder（Plan 02） |
| TYPE-05 | §4 类型反模式 | Placeholder（Plan 03） |

### ROADMAP 成功标准锚点

Phase 2 验收要求以下三类场景在 §2 主矩阵中各有明确的**推荐 / 禁止**（Plan 02 填充具体单元格）：

| 场景 | 类型 | 关键设定方向 |
|------|------|-------------|
| 周报 xlsx | Excel | `paragraph-first` + `text-only`；禁止 `semantic` / `structured` |
| 扫描 pdf | PDF | `parsing.ocrEnabled: true`；禁止 OCR 关闭 |
| 制度 docx | Word | `structured` 表格 + `heading-level` 分块（长文档） |

---

## §2 主矩阵 {#ops-guide}

本节为 **单张大表**（D-03）：行 = **类型 × 规则项**（完整管道维度），列 = **设定 | 产出 | 质量**，便于横向对比 pdf / word / excel / txt / markdown。单元格推荐值由 Plan 02 填充；Excel 子场景（周报 xlsx）以缩进行标注。

| 类型 × 规则项 | 设定（推荐 config_json） | 产出形态 | 质量风险 / 禁止 |
|---------------|-------------------------|----------|----------------|
| **PDF** · `parsing.ocrEnabled` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `parsing.tableExtraction` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `parsing.defaultLanguage` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `cleaning.removeHeaderFooter` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `cleaning.removeDuplicateParagraphs` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `chunkingStrategy` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `chunkSize` / `chunkOverlap` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `minParagraphLength` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **PDF** · `IndexingChunkFilter` | —（**非 config_json**，入库启发式） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `parsing.tableExtraction` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `parsing.defaultLanguage` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `cleaning.removeHeaderFooter` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `cleaning.removeDuplicateParagraphs` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `chunkingStrategy` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `chunkSize` / `chunkOverlap` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `minParagraphLength` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Word** · `IndexingChunkFilter` | —（**非 config_json**，入库启发式） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · `parsing.tableExtraction` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · `cleaning.removeDuplicateParagraphs` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · `chunkingStrategy` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · `chunkSize` / `chunkOverlap` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · `minParagraphLength` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · `IndexingChunkFilter` | —（**非 config_json**，入库启发式） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · 子场景（周报 xlsx） | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Excel** · 续行 / 表头（管道内启发式） | —（`TabularContinuationNormalizer`） | （Plan 02 填充） | （Plan 02 填充） |
| **TXT** · `parsing.autoDetectEncoding` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **TXT** · `cleaning.removeDuplicateParagraphs` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **TXT** · `chunkingStrategy` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **TXT** · `chunkSize` / `chunkOverlap` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **TXT** · `minParagraphLength` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **TXT** · `IndexingChunkFilter` | —（**非 config_json**，入库启发式） | （Plan 02 填充） | （Plan 02 填充） |
| **Markdown** · `parsing.autoDetectEncoding` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Markdown** · MIME 兜底 | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Markdown** · `chunkingStrategy` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Markdown** · `chunkSize` / `chunkOverlap` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Markdown** · `minParagraphLength` | （Plan 02 填充） | （Plan 02 填充） | （Plan 02 填充） |
| **Markdown** · `IndexingChunkFilter` | —（**非 config_json**，入库启发式） | （Plan 02 填充） | （Plan 02 填充） |

### 开发参考：Resolver 消费链

配置 dot-path 与 [INGEST-PIPELINE.md §2.3](./INGEST-PIPELINE.md) 一致；此处仅列主矩阵相关链路，**不**重复 PIPE 九阶段叙述。

| Resolver 方法 | config_json 字段 | 消费类 |
|---------------|------------------|--------|
| `parseOptionsFor(libraryId)` | `parsing.*` | `DocumentParseService` |
| `chunkingFor(libraryId)` | `chunkingStrategy`, `chunkSize`, `chunkOverlap`, `minParagraphLength`, … | `IndexingService` → `IndexingChunkFilter` |

字段路径与 UI diff 对齐：`frontend/knowbase-ui/src/utils/libraryConfig.js` `REINDEX_FIELDS`（L35–53）。

---

## §3 三层默认值对照 {#dev-reference}

本节对照 **系统默认**（`application.yml` 平台兜底）、**向导默认**（`libraryDefaults.js` 建库向导初始值）、**类型推荐值**（§2 主矩阵「设定」列，Plan 02 填充 per-type 单元格）。配置层级完整模型见 [INGEST-PIPELINE.md §5](./INGEST-PIPELINE.md#5-三层配置矩阵) — 本文不重复采集覆盖（目标态）列。

**层级说明：**

- **系统级** — 无库 JSON 或字段缺失时 `VectorLibraryConfigFactory` 兜底（如 `chunkSize` 600、`chunkOverlap` 100）；`ingest.ocr.enabled` 为 OCR **引擎开关**，非库级 `parsing.ocrEnabled`。
- **向导级** — `CreateLibraryWizard` Step 3 通过 `defaultLibraryConfig()` 写入新库 `config_json` 初始值。
- **类型推荐** — 运营按 §2 矩阵为垂直库覆盖；与向导默认一致时标注「同左」，差异时在 §2 脚注说明。

| 规则项 | 配置路径 | 系统默认 (`application.yml`) | 向导默认 (`libraryDefaults.js`) | 类型推荐值（汇总） | 一致/差异说明 |
|--------|----------|------------------------------|--------------------------------|-------------------|--------------|
| 分块策略 | `chunkingStrategy` | `paragraph-first`（L153） | `paragraph-first`（L39） | PDF/Excel/TXT/MD: 同左；Word 长文档: **`heading-level`**（制度 docx，见 §2） | 一致 |
| 块大小 | `chunkSize` | `600`（L154） | `500`（L40） | Excel 周报: 500（测试基准）；其他类型见 §2 | **差异**：向导默认低于系统兜底 |
| 块重叠 | `chunkOverlap` | `100`（L155） | `120`（L41） | 同向导或按类型脚注 | **差异** |
| OCR（库级） | `parsing.ocrEnabled` | —（库级字段） | `false`（L63） | 扫描 PDF: **`true`** | 系统 `ingest.ocr.enabled: true`（L61）仅为引擎可用性开关，**非**库级默认 |
| 表格提取 | `parsing.tableExtraction` | — | `text-only`（L64） | Word: **`structured`**；Excel: **`text-only`**（禁止 structured） | Excel 勿改 structured（附录 B） |
| 编码检测 | `parsing.autoDetectEncoding` | — | `true`（L67） | TXT/MD: **`true`** | 一致 |
| 默认语言 | `parsing.defaultLanguage` | `ingest.ocr.language: chi_sim+eng`（L64，OCR 引擎） | `zh-CN`（L68） | OCR 脚注语言 | 引擎语言 vs 库级 defaultLanguage 不同层级 |
| 去页眉页脚 | `cleaning.removeHeaderFooter` | — | `true`（L71） | PDF/Word 制度: 保持 `true` | 一致 |
| 去重复段落 | `cleaning.removeDuplicateParagraphs` | — | `true`（L73） | PDF 导出/长文: 保持 `true` | 一致 |

**代码锚点：**

- 系统 chunking 兜底：`application.yml` L152–160；`VectorLibraryConfigFactory.java` L75–83（`chunkSize` ≤0 → 600，`chunkOverlap` ≤0 → 100）
- 向导默认形状：`frontend/knowbase-ui/src/utils/libraryDefaults.js` `defaultLibraryConfig()`
- 后端 parsing 字段名权威：`ParsingRulesSettings.java` — `ocrEnabled`, `tableExtraction`, `autoDetectEncoding`, `defaultLanguage`
- 系统 OCR 引擎：`application.yml` L59–67 — `enabled`, `data-path`, `language`, `min-extracted-chars-to-skip`

---

## §4 类型反模式

（Plan 03 填充）

---

## 附录 A MIME → 推荐 config_json

（Plan 02 填充）

---

## 附录 B 结构化 Excel 差距

（Plan 03 填充 — 目标态 vs v1 差距详表）

---

## 附录 C 字段路径与代码锚点

（Plan 03 填充）

---

## §9 验收清单

（Plan 03 填充）
