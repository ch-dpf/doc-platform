# Roadmap: 入库质量规范里程碑

**Created:** 2026-06-10
**Core Value:** 预览=入库，分块质量可预期
**Granularity:** standard（5 phases）

## Phases

### Phase 1 — 全链路流程梳理

**Goal:** 可读文档描述建库与入库端到端路径，开发与运营对齐「设定在哪生效」。

**Requirements:** PIPE-01, PIPE-02, PIPE-03

**Plans:** 3/3 plans complete

**Key deliverables:**

- `.planning/docs/INGEST-PIPELINE.md`（或等价路径）含 mermaid 流程图
- 前后端入口、配置解析、索引触发对照表

**Success criteria:**

- 新人可据文档从「建库」追到 `document_chunk` 写入而无歧义

Plans:

- [x] 01-01-PLAN.md — Doc scaffold, decision tree, config matrix, PIPE-01 建库流程
- [x] 01-02-PLAN.md — PIPE-02 入库流程 + PIPE-03 阶段/类/API 矩阵 + 预览差距
- [x] 01-03-PLAN.md — 质量准则、反模式、附录 A/B、验收清单与 STATE 更新

---

### Phase 2 — 文件类型设定矩阵

**Goal:** pdf/word/excel/txt/md 各类型的推荐设定、产出形态、风险与反模式。

**Requirements:** TYPE-01 … TYPE-05

**Plans:** 6/6 plans complete

**Key deliverables:**

- `.planning/docs/FILE-TYPE-PROCESSING.md` 含设定→产出→质量三列表
- 与现有 `application.yml` 默认、库向导字段一一对应

**Success criteria:**

- 周报 xlsx、扫描 pdf、制度 docx 各有明确「推荐 / 禁止」设定说明

Plans:

- [x] 02-01-PLAN.md — scaffold, dual TOC, §3 三层默认值, §2 矩阵骨架
- [x] 02-02-PLAN.md — TYPE-01–04 五行组矩阵行, 附录 A MIME, 附录 B Excel 差距
- [x] 02-03-PLAN.md — TYPE-05 类型反模式, 附录 C 字段路径, 交叉引用, §9 验收, STATE 更新
- [x] 02-04-PLAN.md — gap: §2 单表矩阵结构修复 (CR-01)
- [x] 02-05-PLAN.md — gap: 唯一锚点 + INGEST-PIPELINE 双向链接 (CR-02)
- [x] 02-06-PLAN.md — gap: 附录 C REINDEX_FIELDS 范围与 minParagraphLength 锚点 (WR-01/WR-02)

---

### Phase 3 — 库类型预设（**Superseded**）

> **2026-06-10 废止：** 垂直/通用库物种与场景 preset 已移除。建库见 `libraryDefaults.js`；MIME 默认见 `parsing.mimeAwareDefaults` + `MimeTypePipelineDefaults`。Phase 3 计划/验证文档保留为历史档案。

**Goal（原）：** 向导一键套用预设，降低运营配错概率。

**Requirements:** PRESET-01 … PRESET-04（见 REQUIREMENTS 废止说明）

**Plans:** 3/3 plans complete（历史）

**Key deliverables（现行）：**

- `frontend/knowbase-ui/src/utils/libraryDefaults.js`
- `CreateLibraryWizard.vue` 短表单建库 + `EditLibrarySettingsDrawer` 深配
- `MimeTypePipelineDefaults` + `EffectiveConfigResolver`

**Success criteria（现行）：**

- 运营按 §6 多建平级库；`mimeAwareDefaults` 下按 MIME 应用附录 A 默认

Plans:

- [x] 03-01-PLAN.md — vitest 预设单测 + 附录 A/ROADMAP 锚点审计 + JsonSupport 往返测试
- [x] 03-02-PLAN.md — （历史）Merger libraryPresetId；现行已移除 preset 字段
- [x] 03-03-PLAN.md — 文档 §10、VERIFICATION、REQUIREMENTS/STATE/ROADMAP 闭环

---

### Phase 4 — 预览与入库一致性

**Goal:** `ChunkPreviewService` 与 `IndexingService` 同规则，块数与内容对齐。

**Requirements:** PARITY-01 … PARITY-04

**Plans:** 3/3 plans complete

**Key deliverables:**

- 共享 `LibraryChunkPipeline` + 消除 IndexingService 重复清洗
- 单测：周报样本预览块数 = 模拟索引块数
- 前端移除 overrideChunk，统一 `libraryChunkParams`

**Success criteria:**

- 用户反馈的「预览 8 块入库 3 块」类问题不可复现（同配置同文本）

Plans:

- [x] 04-01-PLAN.md — 共享 LibraryChunkPipeline；IndexingService 去重清洗
- [x] 04-02-PLAN.md — PARITY-04 回归单测；libraryId 解析分块参数
- [x] 04-03-PLAN.md — IngestView/Wizard 前端对齐 + 配置热刷新

---

### Phase 5 — 配置 UX 与保存可靠性

**Goal:** 配置 diff/保存可预期，字段含义在 UI 可理解。

**Requirements:** CFG-01, CFG-02

**Plans:** 1/1 plans complete

**Key deliverables:**

- `libraryConfig.js` diff 覆盖验证（vitest）
- 向导/编辑页字段旁简短影响文案

**Success criteria:**

- 修改 `tableExtraction` 等嵌套字段可保存且无「配置未变更」误报

Plans:

- [x] 05-01-PLAN.md — diffLibraryConfig 测试与修补 + fieldImpactHints UI

---

## Phase Order Rationale

1. 先文档对齐现状（Phase 1–2），避免预设写错
2. 预设（Phase 3）依赖矩阵
3. 一致性（Phase 4）是核心价值的工程落地
4. UX（Phase 5）收尾减少运营摩擦

## v1 完成 — 下一里程碑

| 里程碑 | 焦点 | 草案 |
|--------|------|------|
| **v2** | 目标态建仓入库（Greenfield，RAG 上游） | `MILESTONE-v2-DRAFT.md` |
| **v3** | RAG 智能问答（多轮、引用、评测） | `MILESTONE-v3-DRAFT.md` |

### 战略（2026-06-10）

- **目标态唯一** — `INGEST-PIPELINE` / `FILE-TYPE-PROCESSING` 目标态章节为实现契约
- **Greenfield** — 代码可推倒重来；**无数据兼容**，历史数据可全量清除
- **技术栈不变** — Java/Spring/Vue/PG/Ollama
- **RAG 分两期** — v2 保证 chunk + 检索；v3 做生成与对话

### Next Step

```bash
/gsd-complete-milestone    # 归档 v1（可选）
/gsd-new-milestone         # 正式启动 v2：目标态建仓入库
```

---
*Roadmap v1: 2026-06-10 | v2/v3 drafts added same day*
