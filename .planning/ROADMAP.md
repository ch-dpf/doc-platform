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

**Key deliverables:**

- `.planning/docs/FILE-TYPE-PROCESSING.md` 含设定→产出→质量三列表
- 与现有 `application.yml` 默认、库向导字段一一对应

**Success criteria:**

- 周报 xlsx、扫描 pdf、制度 docx 各有明确「推荐 / 禁止」设定说明

---

### Phase 3 — 库类型预设

**Goal:** 向导一键套用预设，降低运营配错概率。

**Requirements:** PRESET-01 … PRESET-04

**Key deliverables:**

- `frontend/knowbase-ui/src/utils/libraryPresets.js`（或同级）
- `CreateLibraryWizard.vue` 预设选择 UI
- 预设定义与 Phase 2 矩阵一致

**Success criteria:**

- 创建「周报库」后配置与文档推荐一致，无需手调 10+ 字段

---

### Phase 4 — 预览与入库一致性

**Goal:** `ChunkPreviewService` 与 `IndexingService` 同规则，块数与内容对齐。

**Requirements:** PARITY-01 … PARITY-04

**Key deliverables:**

- 共享 `IndexingChunkFilter` / `libraryChunkParams` 路径审计与必要修补
- 单测：周报样本预览块数 = 模拟索引块数

**Success criteria:**

- 用户反馈的「预览 8 块入库 3 块」类问题不可复现（同配置同文本）

---

### Phase 5 — 配置 UX 与保存可靠性

**Goal:** 配置 diff/保存可预期，字段含义在 UI 可理解。

**Requirements:** CFG-01, CFG-02

**Key deliverables:**

- `libraryConfig.js` diff 覆盖验证
- 向导/编辑页字段旁简短影响文案

**Success criteria:**

- 修改 `tableExtraction` 等嵌套字段可保存且无「配置未变更」误报

---

## Phase Order Rationale

1. 先文档对齐现状（Phase 1–2），避免预设写错
2. 预设（Phase 3）依赖矩阵
3. 一致性（Phase 4）是核心价值的工程落地
4. UX（Phase 5）收尾减少运营摩擦

## Next Step

```bash
/gsd-execute-phase 1
```

---
*Roadmap created: 2026-06-10*
