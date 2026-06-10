# Phase 1: 全链路流程梳理 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-10
**Phase:** 1-全链路流程梳理
**Areas discussed:** 三层配置边界, 库类型谱系, 入库流程, 分块质量标准, 规划基线, 跨维度建库, 管道锁定策略

---

## 三层配置边界

| Option | Description | Selected |
|--------|-------------|----------|
| 系统级仅基础设施 | 文件大小、批次数、OCR 可用性、embedding 兜底 | ✓ |
| 系统级含解析默认 | + Tika/OCR 全局开关 | |
| 库级仅向量+检索 | embedding/retrieval/治理必须库级统一 | ✓ |
| 库级整条管道 | 解析/清洗/分块也库级统一（现状） | |
| v1 采集级只文档化 | 不实现覆盖，记缺口 | ✓ |
| 配置矩阵表呈现 | 系统/库/采集列 + 必须/可覆盖标注 | ✓ |

**User's choice:** 系统基础设施 + 库级向量检索必选统一 + 解析分块为库默认（目标可采集覆盖）+ v1 只文档化采集覆盖缺口 + 矩阵表
**Notes:** 用户追问「不应仅基于现状规划」——后续改为目标态为主（见补充讨论）。

---

## 库类型谱系

| Option | Description | Selected |
|--------|-------------|----------|
| 两类库 | 垂直专用 + 通用混合 | ✓ |
| 通用库一等公民 | MIME 默认 + 标签，力争高质量 | ✓ |
| 垂直库启发式清单 | 同语义同问答模式 → 专用库 | ✓ |
| 建库决策树 | 场景→类型→预设→微调 | ✓ |

**User's choice:** 两类 + 通用库一等公民 + 启发式 + 决策树

---

## 入库流程

| Option | Description | Selected |
|--------|-------------|----------|
| 阶段+关键类 | 对齐 PIPE-03 | ✓ |
| 预览缺口文档化 | overrideChunk 仅预览等 | ✓ |
| documentMetadata=语义标签 | 检索过滤，不驱动管道 | ✓ |
| lockPipeline 风险+重索引路径 | 目标软锁定 | ✓ |

**User's choice:** 全部推荐项

---

## 分块质量标准

| Option | Description | Selected |
|--------|-------------|----------|
| 北极星=RAG可答率 | v1 验收召回层 | ✓ |
| Phase1 通用准则 | 类型细表 Phase 2 | ✓ |
| 反模式含真实样本 | 杜鹏飞周报等 | ✓ |
| GATE backlog 引用 | v2 门禁 | ✓ |

**User's choice:** RAG 可答率为愿景，v1 验收到召回层；含真实样本反模式

---

## 补充讨论（用户自由输入）

### 规划基线

| Option | Description | Selected |
|--------|-------------|----------|
| 目标态为主 | 合理架构为主，现状附录 | ✓ |
| 现状为主 | 仅简短展望 | |

**User's choice:** 不基于当前实现束缚，做合理前瞻规划

### 跨维度建库（同质语义不同类型 / 同类型不同语义）

| Option | Description | Selected |
|--------|-------------|----------|
| 语义主轴 | 同质语义可混合类型+MIME默认；异质语义拆库 | ✓ |
| 类型主轴 | 同扩展名同库 | |

**User's choice:** 周报 xlsx+pdf 可同库；周报 xlsx vs 报销 xlsx 拆库

### 管道锁定

| Option | Description | Selected |
|--------|-------------|----------|
| 软锁定+全库重索引 | 允许改规则，强制重索引 | ✓ |
| 硬锁定（现状） | | |

**User's choice:** 目标态软锁定；现状硬锁写入差距附录

---

## Claude's Discretion

- 交付物拆分方式（单文件 vs 多文档）
- 「当前差距」附录粒度与锚点类选取

## Deferred Ideas

- 结构化双轨 / QueryRouter
- ingest profile 工程实现
- MIME 自动默认实现
- RAG 答对率自动化测试
- GATE-01/02 实现
