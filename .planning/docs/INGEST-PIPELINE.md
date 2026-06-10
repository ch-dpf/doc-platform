---
last_mapped_commit: 0bb941f
analysis_date: 2026-06-10
focus: ingest-pipeline
---

# 建库与入库全链路

**Analysis Date:** 2026-06-10

> **Phase 1 说明：** 本里程碑为**文档-only**交付，不修改应用代码。目标态叙述为主（D-01），现状差距在各章「当前差距」与附录 A 标注。

## 目录

| 受众 | 锚点 | 推荐阅读章节 |
|------|------|--------------|
| 运营 | [#ops-guide](#ops-guide) | §6 库类型选型决策树、§7 分块质量准则、§8 反模式 |
| 开发 | [#dev-reference](#dev-reference) | §2 建库流程、§3 单文档入库流程、§5 三层配置矩阵、附录 A/B |

---

## §1 范围与读者指南 {#ops-guide} {#dev-reference}

### 里程碑核心价值

> **Core Value（PROJECT.md）：** 运营人员按文件类型选对库预设并完成采集后，**预览所见分块与入库结果一致**，且分块内容足以支撑后续检索与问答（不因错误设定导致表头块、续行拆开、OCR 缺失等问题）。

### 目标态 vs v1 交付边界（D-01、D-02）

| 维度 | 目标态（文档叙述） | v1 里程碑交付 |
|------|-------------------|---------------|
| 库类型谱系 | 垂直专用库 + **通用混合库一等公民**（D-09） | 文档描述选型决策树与配置路径；预设 UI 见 Phase 3 |
| 配置层级 | 系统 / 库默认 / 采集覆盖三层（D-03–D-07） | 配置矩阵标注「现状」列；采集 profile 未实现 |
| 质量验收 | RAG 可答率为**北极星**（D-16） | v1 验收 = **检索可召回** + 反模式样本；**不**将 RAG 答对率纳入工程验收 |
| 按类型细表 | 引用 Phase 2 `FILE-TYPE-PROCESSING.md`（D-17） | Phase 1 仅通用准则，不重复 per-type 矩阵 |

**D-01：** 本文以合理目标架构为主叙述，不以「仅描述现有代码」为约束；各章末尾「当前差距」对照 `LibraryConfigResolver`、`lockPipeline`、`overrideChunk` 等现状。

**D-02：** 愿景包含通用库一等公民；v1 交付流程文档 + 召回层质量准则 + 反模式样本。

### 需求可追溯

| Requirement | Section | Status |
|-------------|---------|--------|
| PIPE-01 | §2 建库流程 | Plan 01-01 |
| PIPE-02 | §3 单文档入库流程 | Plan 01-02 |
| PIPE-03 | §4 阶段·类·API 对照 | Plan 01-02 |

### 按文件类型处理

Phase 1 **不**展开 PDF/Word/Excel/TXT/Markdown 逐类型矩阵（D-17）。详见 Phase 2 交付物 [`.planning/docs/FILE-TYPE-PROCESSING.md`](./FILE-TYPE-PROCESSING.md)（待创建）。

---

## §2 建库流程（PIPE-01）

（Plan 01-01 Task 3 填充）

---

## §3 单文档入库流程（PIPE-02）

（Plan 02 填充）

---

## §4 阶段·类·API 对照（PIPE-03）

（Plan 02 填充）

---

## §5 三层配置矩阵

（Plan 01-01 Task 2 填充）

---

## §6 库类型选型决策树

（Plan 01-01 Task 2 填充）

---

## §7 分块质量准则

（Plan 03 填充）

---

## §8 反模式对照

（Plan 03 填充）

---

## 附录 A：当前差距详表

（Plan 03 填充）

---

## 附录 B：Backlog 与字段路径索引

（Plan 03 填充）
