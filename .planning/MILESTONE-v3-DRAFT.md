# Milestone v3 草案：RAG 智能问答

**Status:** Draft — 依赖 v2 建仓入库质量基线  
**Defined:** 2026-06-10

## 定位

在 **v2 目标态建仓入库** 提供高质量 chunk 与稳定检索 API 之后，本里程碑实现 **RAG 生成与运营问答体验**，将 `INGEST-PIPELINE.md` D-16「RAG 可答率」从北极星叙述落实为可测工程能力。

## 与 v2 的边界

| 层 | v2（上游） | v3（本里程碑） |
|----|-----------|----------------|
| 建库/入库 | ✓ | 消费 v2 API |
| 向量/BM25 检索 | 基础 hybrid + metadata 过滤 | 重排、多路召回、query 改写 |
| 生成 | — | LLM 提示词、引用块拼接、流式输出 |
| 对话 | — | 多轮会话、记忆、库级 scope |
| 评测 | 检索可召回样本 | RAG 可答率 / 引用准确率 rubric |

## 候选能力（待 discuss 收窄）

- **RAG-01** 库内问答：选择 `libraryId`，基于 hybrid 检索 + Ollama 生成，返回答案与引用 chunk
- **RAG-02** 多轮对话：`ChatConversation` 持久化、上下文窗口与检索 query 解耦
- **RAG-03** 引用溯源：答案中标注来源文档/块，前端可跳转 `DocumentChunksView`
- **RAG-04** 检索 trace：运营可查看召回块、分数、过滤原因（debug 面板）
- **RAG-05** 流式 SSE：生成过程流式展示
- **RAG-06** 评测基线：固定 QA 样本集 + 自动/半自动评分（引用命中、事实一致性）

## 技术约束（继承 PROJECT.md）

- 仍用 Spring Boot + Vue 3 + Ollama；不引入新向量库
- v3 同样 **可无历史兼容**；会话数据可清空重建
- 实现方略：**目标态优先**，问答模块可独立包/服务边界重写

## 前置条件

- [ ] v2 完成：检索 API 稳定、chunk metadata 契约冻结、至少 1 个端到端运营样本库
- [ ] v2 GATE 或样本集定义「可答」chunk 形态标准

## 下一命令（v2 完成后）

```bash
/gsd-new-milestone
# 里程碑名：RAG 智能问答
# 先 /gsd-discuss-phase 收窄 RAG-01–06 优先级
```

---
*Placeholder — 详细 REQUIREMENTS 在 v2 启动后通过 discuss 补充*
