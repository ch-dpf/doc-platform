# 库配置字段增删清单（对标 RAGFlow · 产品方案）

> 本文档仅描述产品层字段规划，不含实现承诺时间表。  
> 基准：RAGFlow 知识库「配置 / 解析 / 分块 / 检索」可见旋钮；对照 Knowbase 当前库级 UI + 系统/MIME 自动层。

## 1. 定位差异（先读）

| 维度 | RAGFlow | Knowbase（本方案后） |
|------|---------|----------------------|
| 产品取向 | 解析/分块/检索全暴露，运营可逐项微调 | 库级主旋钮 + MIME/系统管策略；增强可观测性与少量高级项 |
| 库级目标 | 单库内可配 Parser + Chunk method + Embedding + Retrieval | 单库内：数值 + Embedding + 检索 + 父子块/分隔符；策略摘要只读 |

---

## 2. 库级 UI — 保留（已与 RAGFlow 核心对齐）

| 字段 | 说明 | RAGFlow 对应 |
|------|------|--------------|
| 知识库名称 | 必填 | 知识库名称 |
| 描述 | 必填，助 AI 理解库边界 | 描述 / 用途说明 |
| 标签 | 选填 | 标签（部分版本） |
| 分块大小 | 滑块，字符目标 | Chunk token/length |
| 分块重叠 | 滑块 | Overlap |
| Embedding 模型 | 下拉 + 维度联动 | Embedding 模型 |
| 混合检索 | 开关 | Hybrid search |
| Rerank | 开关 + 模型 | Rerank |
| 相似度阈值 | 滑块 | Similarity threshold |
| 默认 Top K | 滑块（问答可覆盖） | Top K（多在对话/检索侧） |
| 元数据过滤字段 | 多选 | Metadata filter |

---

## 3. 库级 UI — 新增（本迭代已实现 · 对标 RAGFlow 可观测/高级）

| 字段 | 层级 | 说明 | RAGFlow 差距缩小方式 |
|------|------|------|----------------------|
| **生效策略摘要（只读）** | 库配置 · 分块向量化 | 按 PDF/Word/… 展示 MIME 路由后的策略、父子块可能性、解析要点 | 弥补 RAGFlow「Parser/Method 可见」；Knowbase 不开放编辑但可解释 |
| **父子块开关** | 库配置 · 分块向量化 | heading-level 长文档父段+子块 | 对齐 RAGFlow「父子/层次」简化版 |
| **自定义分隔符** | 库配置 · 分块向量化 | 非空时优先按分隔符切段；支持 `\n` | 对齐 RAGFlow「分隔符/自定义切分」简化版 |

---

## 4. 库级 UI — 明确不暴露（仍由系统/MIME 管）

与 RAGFlow 仍有差距，但**刻意不收进库级**（降低运营复杂度）：

| RAGFlow 常见项 | Knowbase 归属 | 不放进库级的原因 |
|----------------|---------------|------------------|
| Parser 类型（naive / paper / book / …） | MIME + `ContentFamilyPipelineDefaults` | 文件类型已表达策略；库级再选易与 MIME 冲突 |
| 版面识别 / OCR 引擎细项 | 系统 `ingest.ocr.*` + MIME | 基础设施开关，非单库差异 |
| 表格/图片/公式提取模式 | MIME `parsing.*` | 与文件结构绑定 |
| min/max 分块、段落最小长度 | 系统 `chunking.*` | 合并规则，非目标块大小 |
| 语义分块相似度阈值 | 系统 `chunking.semantic-similarity-threshold` | 仅 semantic 策略使用；库级未开放策略选择 |
| RRF 系数、候选倍率、查询改写 | 系统 `retrieval.*` | 引擎调参，非业务库差异 |
| 单文件大小、批量上限、MIME 白名单 | 系统 `ingest.*` | 平台安全与容量 |
| 容量上限、版本策略、入库审核 | `config_json` 治理段（无 UI） | 二期企业治理，非 RAGFlow 对标必需 |

---

## 5. 库级 UI — 建议后续增加（未实现 · 缩小 RAGFlow 差距）

按优先级排列，**仍保持「库级主旋钮」原则**：

| 优先级 | 字段 | 形态 | 对标 RAGFlow | 备注 |
|--------|------|------|--------------|------|
| P1 | 支持文件类型收窄 | 多选（系统白名单子集） | 知识库文件类型 | 后端已有 `ingestAccess.supportedFileTypes` |
| P1 | 入库审核模式 | 枚举：自动/人工 | 发布前审核 | `governance.ingestReviewMode` 已有 |
| P2 | 索引质量档位 | 经济 / 标准 / 高质量 | Indexing quality | 映射到 preset（chunk 数值 + 是否 rerank） |
| P2 | 检索缓存开关（库级） | 开关 | 检索配置 | 覆盖系统 `retrieval.cache-enabled` |
| P3 | 分块策略覆盖 | 只读默认 + 高级覆盖（paragraph / heading / fixed） | Chunk method | 与 MIME 冲突需「覆盖警告 + 重索引」 |
| P3 | Token 切分模式 | 按 token 而非字符 | Token size | 需 tokenizer 与 embedding 模型对齐 |
| P4 | 完整 Parser 面板 | OCR/表格/图片逐项 | Parser settings | 接近 RAGFlow 运维复杂度，谨慎引入 |

---

## 6. 库级 UI — 建议删除或隐藏（相对 RAGFlow 的精简项）

| 字段 | 建议 | 理由 |
|------|------|------|
| Embedding 提供方（固定 ollama） | 隐藏，只读展示 | 无多提供方时不应占表单项 |
| 向量维度（已知模型时） | 只读自动带出 | 已实现；避免手工填错 |
| `semanticSimilarityThreshold`（库 JSON 遗留） | 从库 JSON 清理/忽略 | 已走系统默认，避免双源 |
| 采集级 `minParagraphLength` 覆盖 | 文档级高级折叠保留，不进入库配置 | 与系统合并规则重复 |

---

## 7. 采集/文档级（非库配置 Tab，但对标 RAGFlow「上传向导」）

| 字段 | 位置 | 与 RAGFlow 关系 |
|------|------|-----------------|
| chunkSize / chunkOverlap 覆盖 | 采集页高级 · ingest profile | 类似 RAGFlow 上传时自定义分块 |
| 管道锁定提示 | 有文档后禁止覆盖 | RAGFlow 亦要求重解析 |

---

## 8. 问答/会话级（非库配置，RAGFlow 常在应用层）

| 字段 | 位置 | 说明 |
|------|------|------|
| Top K | 问答高级设置 | 默认继承库 `retrieval.defaultTopK` |
| minScore | 问答高级设置 | 可覆盖库阈值 |
| chatModel | 问答高级设置 | 系统 `ollama.chat-model` 可覆盖 |

---

## 9. 验收对照（产品）

- 运营在**不改 YAML** 前提下，能在库配置完成：分块数值、Embedding、检索、父子块、分隔符、Top K 默认值。
- 运营能**只读**看到各文件类型的生效分块策略（P0 可观测性）。
- 创建库与编辑库**基本信息字段与必填规则一致**。
- 与 RAGFlow 相比：**核心检索/分块旋钮差距 ≤ 1 档**；解析/Parser 细项差距仍大，靠 MIME 自动 + 策略摘要解释。

---

## 10. 版本记录

| 日期 | 说明 |
|------|------|
| 2026-06-12 | 初版：P0–P3 代码落地后的 RAGFlow 对标增删清单 |
