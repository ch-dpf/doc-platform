# Milestone v2 草案：目标态建仓入库（Greenfield）

**Status:** Draft — 待 `/gsd-new-milestone` 正式化  
**Defined:** 2026-06-10  
**前置：** v1.0 文档与规范已交付（`INGEST-PIPELINE.md`、`FILE-TYPE-PROCESSING.md`）

## 战略立场

| 维度 | 决策 |
|------|------|
| 实现方略 | **仅按目标态**设计与编码；附录 A「当前差距」、codebase map **不作约束** |
| 代码 | **可推倒重来**；包结构、类名、API 路径按目标态重新划分 |
| 数据 | **无需兼容**；历史 `config_json`、`doc_metadata`、`document_chunk` 可全量清除后重建 schema |
| 技术栈 | **不变** — Java 21 / Spring Boot 3.2 / MyBatis-Plus / Vue 3 / PG+pgvector / Ollama / MinIO |
| RAG 关系 | 本里程碑交付 **RAG 上游**：建库 → 入库 → 分块/嵌入/元数据，使检索层可获得高质量 chunk；**不包含**生成式问答逻辑 |

## 目标态能力范围（实现侧）

以 `INGEST-PIPELINE.md` 目标态章节 + `FILE-TYPE-PROCESSING.md` 主矩阵为契约，至少覆盖：

1. **建库（PIPE-01 目标态）**
   - 短表单建库 + `libraryDefaults` 默认 config + `EditLibrarySettingsDrawer` 深配
   - `config_json` 三层配置模型：系统默认 / 库默认 / 采集覆盖（ingest profile）
   - 软锁重索引（配置变更 → 提示重索引，非硬锁死字段）

2. **入库（PIPE-02 目标态）**
   - 上传 → 解析 → 规范化 → 清洗 → 分块 → `IndexingChunkFilter` → 嵌入 → `document_chunk`
   - 采集级 ingest profile（OCR/chunk 等可覆盖并持久化）
   - `documentMetadata` 语义标签 + 可选 profile 分离清晰
   - 预览 = 入库（单一 `LibraryChunkPipeline` 契约，无客户端 override）

3. **按类型质量（TYPE-01–05 目标态）**
   - PDF/Word/Excel/TXT/MD 推荐设定在运行时生效
   - Excel：续行合并、表头过滤、`text-only`；structured 差距按附录 B 路线实现或明确 v2 子阶段

4. **质量门禁（可选 v2 波次）**
   - GATE-01/02：入库前表头占比警告、入库报告 chunk 统计

5. **为 RAG 预留的契约**
   - `retrieval.*`、`ChunkMetadataBuilder` 字段与 hybrid 检索 API 稳定
   - 样本集：运营案例（如周报 xlsx）检索可召回 + 块自洽，作为下游问答的输入质量基线

## 建议阶段切分（待 ROADMAP 正式化）

| Phase | 名称 | 焦点 |
|-------|------|------|
| A | Schema & 骨架 | PG schema 重建、Spring/Vue 空壳、CI、Ollama/MinIO 本地栈 |
| B | 建库与配置 | 向导、预设、resolver、ingest profile 模型 |
| C | 入库管道 | 解析/清洗/分块/过滤/嵌入一条龙；对象存储 |
| D | 预览 parity | 共享管道 + 回归样本 |
| E | 类型矩阵落地 | 各 MIME 解析分支与分块启发式 |
| F | 运营 UX | diff/影响说明、入库报告、门禁（若纳入） |

## 明确不在 v2

- 多轮对话记忆、提示词编排、引用溯源 UI、流式生成、答对率评测 — 见 **Milestone v3（RAG 智能问答）**
- 双轨结构化事实层 `document_record` — 仍另立里程碑（除非 v2 末波次单独立项）

## 验收北极星

- **工程：** 目标态流程文档中每一「必须」行为有 API + 单测/样本可演示
- **业务：** 预览块数 = 入库块数；类型矩阵推荐设定下检索可召回关键事实
- **RAG 衔接：** 同一库内 `POST` 检索预览可命中入库 chunk，metadata 过滤可用 — **不验收 LLM 答对率**

## 下一命令

```bash
/gsd-new-milestone
# 输入里程碑名：目标态建仓入库（Greenfield）
# 引用本草案 + INGEST-PIPELINE 目标态章节
```

---
*Draft — 用户确认：无数据兼容、代码可推倒重来、技术栈不变、后续单独规划 RAG 问答*
