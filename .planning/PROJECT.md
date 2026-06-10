# doc-platform（Knowbase）入库质量规范里程碑

## What This Is

面向已有 **doc-platform** brownfield 代码库（`knowbase-service` + `knowbase-ui`）的阶段性工程：梳理**建库流程**与**知识入库流程**，明确各文件类型（PDF / Word / Excel / TXT / Markdown）在解析、清洗、分块阶段的库级设定如何影响最终 `document_chunk` 质量，并提供可复用的**库类型预设**与**预览=入库**一致性保证。

用户是内部知识库运营与开发人员，需要可执行的配置规范而非仅 RAG 问答补丁。

## Core Value

运营人员按文件类型选对库预设并完成采集后，**预览所见分块与入库结果一致**，且分块内容足以支撑后续检索与问答（不因错误设定导致表头块、续行拆开、OCR 缺失等问题）。

## Requirements

### Validated

- ✓ 向量库 CRUD 与 `config_json` 持久化（解析/清洗/分块/检索规则）— existing
- ✓ 文档上传、解析（Tika）、可选 OCR、清洗、分块、向量化入库流水线 — existing
- ✓ 建库向导 `CreateLibraryWizard.vue` 与库设置编辑 — existing
- ✓ 采集页解析预览与分块预览 API — existing
- ✓ 多库租户、文档元数据、pgvector 检索与 RAG 对话 — existing

### Active

- [ ] 全链路流程文档：建库 → 配置生效 → 采集 → 解析 → 清洗 → 分块 → 索引
- [ ] 按文件类型的「处理设定 → 分块产出」对照表与质量说明
- [ ] 库类型预设模板（如周报库、制度文档库、报销/扫描库、通用库）
- [ ] 建库向导支持一键套用预设并展示关键差异说明
- [ ] 采集预览与最终 `document_chunk` 块数/内容/filter 规则一致（parity）
- [ ] 库配置变更 diff 与保存行为可预期（含 `tableExtraction` 等字段）

### Out of Scope

- 双轨结构化事实层（`document_record`）与查询分流 — 本里程碑聚焦入库质量与配置规范，结构化层另立里程碑
- 全新 Excel POI 解析器 — 除非为达成预览一致性所必需的最小改动；不在此里程碑做完整 schema 入库
- 前端/E2E 测试框架搭建 — 沿用后端单测 + 手工验收
- 多租户权限、计费、生产 K8s 部署 — 非本阶段目标

## Context

- Codebase map：`.planning/codebase/`（2026-06-10，commit `0bb941f`）
- 关键路径：建库 `CreateLibraryWizard.vue` → `VectorLibraryController` → `config_json`；入库 `DocumentPipelineService` → `IndexingService` + `ChunkingService`
- 已知问题：`tableExtraction: structured` 对 Excel 不生效（仅 HTML 管道）；`IndexingChunkFilter` 与预览过滤需对齐；周报 Excel 依赖 `TabularContinuationNormalizer` + `paragraph-first` + `text-only`
- 用户已验证案例：杜鹏飞周报 xlsx 在正确设定下可全量召回 4 chunk，但错误设定或预览不一致会造成运营困惑

## Constraints

- **Tech stack**: 保持 Java 21 / Spring Boot 3.2、Vue 3、PostgreSQL+pgvector、Ollama — 不引入新存储
- **Compatibility**: 现有库的 `config_json` 升级需向后兼容，预设为新增而非破坏
- **Config source of truth**: 库级 `VectorLibraryConfig`（`LibraryConfigResolver`）为准，前后端字段对齐 `libraryDefaults.js` / `libraryConfig.js`

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 里程碑交付「规范 + 预设 + 预览一致性」 | 用户明确不要仅文档、也不要未验证的架构大修 | — Pending |
| 覆盖全部支持类型 pdf/word/excel/txt/md | 统一梳理，避免只解决周报场景 | — Pending |
| 结构化双轨推迟 | 入库质量与配置可先行落地，降低范围 | — Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-06-10 after initialization*
