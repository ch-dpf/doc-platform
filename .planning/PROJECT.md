# doc-platform（Knowbase）



## What This Is



面向 **doc-platform** 的知识库平台：以 **RAG 全链路** 为产品主轴——先交付**目标态建仓入库**（库配置 → 文档管道 → 高质量 chunk → 检索就绪），再交付 **RAG 智能问答**（检索增强生成、多轮对话、引用溯源）。



用户是内部知识库运营与开发人员。



## Core Value



运营人员按业务场景建库、按文件类型正确入库后，**预览所见分块与入库结果一致**，chunk 质量足以支撑 **检索召回与 RAG 可答**（D-16 北极星）。



## Implementation Strategy（2026-06-10 生效）



| 原则 | 说明 |

|------|------|

| **目标态唯一** | 设计与实现以 `.planning/docs/INGEST-PIPELINE.md`、`FILE-TYPE-PROCESSING.md` **目标态**为准；现状差距、旧代码、codebase map **不作约束** |

| **Greenfield** | 项目代码**可推倒重来**；模块划分、API、前后端目录按目标态重写 |

| **无数据兼容** | 历史 `config_json`、文档、chunk、会话等**可全量清除**；schema 按目标态重建，无需迁移脚本 |

| **技术栈固定** | Java 21、Spring Boot 3.2、MyBatis-Plus、Vue 3 + Vite + Element Plus、PostgreSQL + pgvector、MinIO、Ollama — 不更换框架族与存储引擎 |

| **RAG 分两里程碑** | **v2** = 建仓入库（RAG 上游）；**v3** = 智能问答（RAG 生成层）— 见里程碑草案 |

| **产品形态（当前）** | 无库类型 / 无场景 preset / 无克隆库配置；数据源仅文件上传；索引仅 Ollama + pgvector |



## Milestone Roadmap



| 里程碑 | 名称 | 状态 | 文档 |

|--------|------|------|------|

| v1.0 | 入库质量规范（文档 + 规范验证） | ✓ Complete | Phase 1–5 |

| **v2** | **目标态建仓入库（Greenfield 实现）** | Draft | `MILESTONE-v2-DRAFT.md` |

| **v3** | **RAG 智能问答** | Draft | `MILESTONE-v3-DRAFT.md` |



## Requirements



### Validated — v1.0（规范里程碑）



- ✓ 全链路 / 类型矩阵 / 预设 / parity / 配置 UX 文档与参考实现 — Phase 1–5

- ✓ 平台能力参考：向量库 CRUD、Tika 解析、pgvector 检索、基础 RAG 对话（**旧代码仅作参考，v2 可废弃**）



### Active — v2（下一里程碑）



- [ ] 按目标态 **Greenfield** 实现建库 → 入库 → 分块 → 嵌入全链路（见 `MILESTONE-v2-DRAFT.md`）

- [ ] 三层配置：系统 / 库默认 / 采集 ingest profile

- [ ] 预览 = 入库单一管道；类型矩阵运行时生效

- [ ] 检索 API + chunk metadata 契约冻结，供 v3 RAG 消费



### Active — v3（后续里程碑）



- [ ] RAG 智能问答：多轮对话、引用溯源、流式生成、可答率评测（见 `MILESTONE-v3-DRAFT.md`）



### Out of Scope（跨里程碑）



- 双轨结构化事实层 `document_record` — 另立里程碑

- 新向量引擎 / 新 LLM 平台选型 — 技术栈已锁定 Ollama + pgvector

- 多租户计费、生产 K8s — 非当前目标



## Context



- **规范基线（保留）：** `.planning/docs/INGEST-PIPELINE.md`、`FILE-TYPE-PROCESSING.md`

- **v1 代码（可废弃）：** `knowbase-service/`、`frontend/knowbase-ui/` — v2 重写时不承担兼容义务

- **RAG 衔接：** v2 验收 = 检索可召回 + 块自洽；v3 验收 = RAG 可答率（D-16）



## Constraints



- **Tech stack（固定）：** Java 21 / Spring Boot 3.2 / MyBatis-Plus / Vue 3 / PostgreSQL+pgvector / MinIO / Ollama

- **Data：** 无向后兼容要求；开发/测试环境可随时 `DROP` + 重建

- **Config SOT：** 目标态库级 `VectorLibraryConfig` + ingest profile；实现路径可全新设计



## Key Decisions



| Decision | Rationale | Outcome |

|----------|-----------|---------|

| v1 交付规范 + 参考实现 | 先对齐目标态叙述与运营矩阵 | ✓ Complete |

| **Greenfield + 无数据兼容** | 用户明确可推倒重来、可清库 | ✓ 2026-06-10，指导 v2 |

| **v2 建仓入库、v3 RAG 问答** | 先保证 chunk 质量再扩展生成层 | ✓ 草案已写 |

| 结构化双轨推迟 | 聚焦 RAG 主路径 | 仍 out of scope |



## Evolution



Updated at milestone boundaries via `/gsd-complete-milestone` and `/gsd-new-milestone`.



---

*Last updated: 2026-06-10 — Greenfield strategy; v2 ingest + v3 RAG milestone drafts*


