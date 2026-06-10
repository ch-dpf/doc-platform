# Phase 2: 文件类型设定矩阵 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-10
**Phase:** 02-文件类型设定矩阵
**Areas discussed:** 矩阵结构, MIME 自动默认, Excel/structured 边界, 反模式与样本, 受众分层, 默认值对照深度

---

## 矩阵结构

| Option | Description | Selected |
|--------|-------------|----------|
| 每类型一章 + 章内三表 | PDF/Word/Excel 等独立章节 | |
| 单张大表 | 行=类型×规则项，列=设定/产出/质量 | ✓ |
| 混合 | 总览表 + 分章展开 | |

**User's choice:** 单张大表；一期支持绝大部分常用文档类型与表格类型。
**Notes:** 行粒度选「完整管道」— parsing + cleaning + chunking + 表头过滤 + 产出 + 质量风险。

---

## MIME 自动默认

| Option | Description | Selected |
|--------|-------------|----------|
| 完整附录 MIME→config 片段 | 供 Phase 3 预设引用 | ✓ |
| 仅主表内嵌 | 不写独立附录 | |
| 按场景非 MIME | 扫描 PDF、周报 xlsx 场景行 | |

**User's choice:** 完整附录 + 三层关系（MIME 默认=库级初始；垂直库可覆盖；采集级目标态可单次覆盖）。

---

## Excel / structured 边界

| Option | Description | Selected |
|--------|-------------|----------|
| 明确禁止 xlsx structured | v1 必须 text-only | |
| 目标态结构化 + 现状过渡 | 表格类需结构化才准确可控；现状少量参考 | ✓ |
| 分轨 v1 禁止 + backlog | | ✓（双轨 backlog 显式引用） |

**User's choice:** 以目标态为主——表格类型文件应通过结构化方式保证分块向量化准确可控；现状仅少量参考。显式 backlog 引用结构化双轨。
**Notes:** 与预置「禁止 structured」选项不同，用户强调目标态结构化必要性，文档须写清过渡路径（text-only + paragraph-first）与差距。

---

## 反模式与样本

| Option | Description | Selected |
|--------|-------------|----------|
| 承接 §8 扩展 | 每类型 2–3 行专属反模式 | ✓ |
| 独立完整反模式章 | | |
| 仅矩阵质量列 | | |

**User's choice:** 承接扩展；**不强制**真实样本（纯设定对照）。

---

## 受众分层

| Option | Description | Selected |
|--------|-------------|----------|
| 延续双 TOC ops/dev | | ✓ |
| 运营主文档 + 开发附录 | | |
| 统一单矩阵 | | |

---

## 默认值对照深度

| Option | Description | Selected |
|--------|-------------|----------|
| 三层对照表 | 系统 yml \| 向导默认 \| 类型推荐 \| 差异说明 | ✓ |
| 仅类型推荐 | | |
| 仅差异高亮 | | |

---

## Claude's Discretion

- 章节编号、MIME 附录命名、Excel 场景子行呈现方式。
- 目标态结构化叙述与 v1 过渡推荐的并列排版。

## Deferred Ideas

- MIME 运行时引擎、POI 结构化 ingest、库预设 UI、PARITY 工程、GATE 门禁 — 见 CONTEXT.md `<deferred>`。
