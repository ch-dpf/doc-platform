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

## §2 主矩阵

（Plan 02 填充 — 骨架见 Task 3）

---

## §3 三层默认值对照

（Plan 02-01 Task 2 填充）

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
