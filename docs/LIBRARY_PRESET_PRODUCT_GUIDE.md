# 库类型预设产品说明稿

> 与 `IngestionProductCatalog`（后端）及 `GET /api/v1/presets/ingestion-catalog`、`GET /api/v1/presets/library-types/{code}/product-guide` 保持同步。

## 1. 三层配置模型

| 层级 | 载体 | 内容 | 建仓后 |
|------|------|------|--------|
| **模板** | `LibraryTypePreset` | L1 默认 + Document Profile 清单 | 仅建仓时快照，不自动同步 |
| **L1** | `LibraryProfile` vN | Embedding、chunkMax、overlap、TopK | 发版治理；换模型需 rebuild |
| **L2** | `DocumentProfile` | parser、chunkingStrategy、options | 按文件类型路由；变更后 reindex |

## 2. 内置库类型预设

| code | 名称 | chunkMax / overlap / TopK | 适合语料 |
|------|------|---------------------------|----------|
| `general_docs` | 通用文档库 | 512 / 64 / 8 | 全格式：Word、PDF、Markdown、Excel、网页、扫描件 |
| `product_knowledge` | 产品知识库 | 512 / 80 / 8 | 手册、FAQ、Markdown、PPT |
| `technical_docs` | 技术文档库 | 640 / 96 / 10 | Markdown、Word、PDF、代码/配置、Excel |
| `policy_compliance` | 制度合规库 | 768 / 96 / 10 | 制度、扫描件、表格 |
| `table_report` | 表格报表库 | 512 / 64 / 12 | **Excel/CSV 周报月报为主** |
| `research_archive` | 研究资料库 | 768 / 128 / 12 | 论文、档案、多格式 |
| `contract_legal` | 合同法务库 | 768 / 96 / 10 | 合同 PDF/Word、扫描件 |
| `general_knowledge` | 通用知识库 | 512 / 64 / 8 | 兼容旧调用，等同 general 全量 |

## 3. 文档 Profile 模板（L2）

| code | 中文名 | 内容族 | 默认解析器 | 切块策略（用户话） | 扩展名 |
|------|--------|--------|------------|-------------------|--------|
| `default_markdown` | Markdown 文档 | RICH_TEXT | markdown-structure | 按标题章节 | md |
| `default_docx` | Word 文档 | RICH_TEXT | docx-structure | 按标题章节 | docx |
| `default_pdf` | PDF 版面 | RICH_TEXT | pdf-layout | 按页/段落 | pdf |
| `default_table` | 表格/Excel | STRUCTURED_TABLE | table-deep | 自适应按行解析（表头/表单/数据） | xlsx,xls,csv |
| `default_faq` | FAQ 问答表 | PLAIN_TEXT | qa | 问答对 | csv,xlsx |
| `default_code_or_config` | 代码与配置 | CODE_OR_CONFIG | text | 按代码结构 | java,yml,json… |
| `default_rich_text` | 通用回退 | RICH_TEXT | tika | 通用提取 | * |
| … | 见 API 目录 | … | … | … | … |

**可更换（实例）**：`parserCode`、`chunkingStrategy`、`options`、`enabled`  
**不可变（实例）**：`code`、`contentFamily`（建仓后不建议改）

## 4. 解析器目录

| parserCode | 中文名 | 内置 | 外接 |
|------------|--------|------|------|
| `docx-structure` | Word 结构解析 | ✅ | |
| `pdf-layout` | PDF 版面解析 | ✅ | |
| `table-deep` | 表格自适应解析 | ✅ | |
| `markdown-structure` | Markdown 结构 | ✅ | |
| `tika` | Tika 通用回退 | ✅ | |
| `ocr-layout` | OCR 版面 | ✅ | |
| `docling` / `unstructured` / `external` | 外接服务 | | ✅ 需 endpoint |

外接解析器需配置 `KNOWBASE_EXTERNAL_PARSER_ENDPOINT` 或入库 options 中的 `externalParserEndpoint`。

## 5. 库配置变更影响

1. 修改 **Embedding 模型/维度（L1）** → 索引代次漂移，需 **rebuild + re-embed**
2. 修改 **chunkMax/overlap（L1）** → 新入库/重索引文档切块变化
3. 修改 **某 Document Profile 的 parser/切块（L2）** → 仅影响匹配类型，需 **按 Profile 重索引**
4. **禁用**某 Profile → 新文件回退到同族其他 Profile 或 `default_rich_text`

## 6. UI 入口

- **预设管理** → 库类型预设详情：产品说明、Profile 矩阵、解析器表
- **创建知识库**：选择预设后展示将启用的文档类型
- **库配置**：L1/L2 分区、来源模板、Parser 下拉、变更提示

## 7. API

```
GET /api/v1/presets/ingestion-catalog
GET /api/v1/presets/library-types/{code}/product-guide
```
