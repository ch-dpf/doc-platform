# Phase 2: 文件类型设定矩阵 - Pattern Map

**Mapped:** 2026-06-10
**Files analyzed:** 8 documentation sections / deliverables
**Analogs found:** 8 / 8

> Phase 2 交付物为**文档-only**（`.planning/docs/FILE-TYPE-PROCESSING.md`），无应用代码变更。本图将各文档章节映射到 Phase 1 已交付文档、现有代码与规划文档 analog，供 planner 撰写时直接引用路径、字段名、矩阵结构与交叉引用模式。

## File Classification

| New/Modified Deliverable | Role | Data Flow | Closest Analog | Match Quality |
|--------------------------|------|-----------|----------------|---------------|
| `.planning/docs/FILE-TYPE-PROCESSING.md`（主文档） | doc-root | narrative + matrix + cross-ref | `.planning/docs/INGEST-PIPELINE.md` | exact |
| §1 范围、读者与 PIPE 交叉引用 | doc-section | narrative | `INGEST-PIPELINE.md` §1 + `02-CONTEXT.md` `<domain>` | exact |
| §2 主矩阵（TYPE-01–04 载体） | doc-section | config transform × type | `INGEST-PIPELINE.md` §5.1 + §7.5 + `02-CONTEXT.md` D-03–D-05 | exact |
| §3 三层默认值对照（D-14） | doc-section | config tiers | `INGEST-PIPELINE.md` §5 + `application.yml` + `libraryDefaults.js` | exact |
| §4 类型反模式（TYPE-05） | doc-section | issue catalog | `INGEST-PIPELINE.md` §8 + `CONCERNS.md` Tech Debt | exact |
| 附录 A：MIME → 推荐 config_json（D-06/D-07） | doc-section | MIME mapping | `VectorLibraryConfigFactory.java` + `MimeTypeAllowlist.java` | exact |
| 附录 B：结构化 Excel 差距与 backlog（D-08–D-10） | doc-section | brownfield delta | `CONCERNS.md` + `INGEST-PIPELINE.md` 附录 B | exact |
| 附录 C：字段路径与代码锚点（dev TOC） | doc-section | field reference | `01-PATTERNS.md` 附录 B + `libraryConfig.js` | exact |

## Pattern Assignments

### `.planning/docs/FILE-TYPE-PROCESSING.md`（doc-root）

**Analog:** `.planning/docs/INGEST-PIPELINE.md` + `.planning/phases/01-ingest-pipeline/01-PATTERNS.md`

**Front matter pattern**（复制 `INGEST-PIPELINE.md` lines 1–9，替换 focus）:
```markdown
---
last_mapped_commit: <sha>
analysis_date: 2026-06-10
focus: file-type-processing
---

# 按文件类型处理设定矩阵

**Analysis Date:** 2026-06-10

> **Phase 2 说明：** 本里程碑为**文档-only**交付，不修改应用代码。目标态叙述为主（D-01）；Excel structured 差距在附录 B 标注，不主导正文。
```

**Dual TOC pattern**（`INGEST-PIPELINE.md` lines 13–18 + `02-CONTEXT.md` D-13）:
```markdown
## 目录

| 受众 | 锚点 | 推荐阅读章节 |
|------|------|--------------|
| 运营 | [#ops-guide](#ops-guide) | §2 主矩阵（推荐/禁止/质量风险）、§4 类型反模式 |
| 开发 | [#dev-reference](#dev-reference) | §3 三层默认值、附录 A MIME 映射、附录 C 字段路径 |

---

## §1 范围与读者指南 {#ops-guide} {#dev-reference}
```

**Upstream cross-ref pattern** — 开篇须链回 Phase 1，避免重复 PIPE 流程（`02-CONTEXT.md` canonical_refs + `INGEST-PIPELINE.md` §7.5）:
```markdown
### 与全链路文档关系

- **流程与 API：** 建库、入库九阶段、Resolver 生效路径见 [INGEST-PIPELINE.md](./INGEST-PIPELINE.md) §2–§4；本文**不**重复。
- **通用质量准则：** 块自洽、表头过滤、预览≠入库见 [INGEST-PIPELINE.md §7](./INGEST-PIPELINE.md#7-分块质量准则) 与 [§8 反模式](./INGEST-PIPELINE.md#8-反模式对照)。
- **按类型细表：** Phase 1 D-17 将 TYPE-01–05 归本文件；Phase 3 预设引用附录 A MIME 片段。
```

**Requirement traceability table**（`INGEST-PIPELINE.md` lines 41–47 格式）:
```markdown
| Requirement | Section | Status |
| TYPE-01 | §2 PDF 行组 | Covered |
| TYPE-02 | §2 Word 行组 | Covered |
| TYPE-03 | §2 Excel 行组 + 附录 B | Covered |
| TYPE-04 | §2 TXT/Markdown 行组 | Covered |
| TYPE-05 | §4 类型反模式 | Covered |
```

**ROADMAP success criteria anchor**（`ROADMAP.md` Phase 2）— 矩阵须含三类场景的「推荐 / 禁止」列或单元格：
- 周报 xlsx → Excel 行组 + `paragraph-first` + `text-only`
- 扫描 pdf → PDF 行组 + `parsing.ocrEnabled: true`
- 制度 docx → Word 行组 + `structured` / `heading-level` 适用说明

---

### §2 主矩阵（TYPE-01–04 载体）

**Analog:** `INGEST-PIPELINE.md` §5.1 规则矩阵 + §7.5 跨类型建议 + `02-CONTEXT.md` D-03–D-05

**单张大表结构**（D-03：行 = 类型 × 规则项，列 = 设定 | 产出 | 质量）:

| 类型 × 规则项 | 设定（推荐 config_json） | 产出形态 | 质量风险 / 禁止 |
|---------------|-------------------------|----------|----------------|
| **PDF** · 文本型 · `parsing.ocrEnabled` | `false`（Tika 可提取时） | Tika 纯文本 → `parsed.txt` | 低风险 |
| **PDF** · 扫描件 · `parsing.ocrEnabled` | **`true`** + tessdata 可用 | OCR 文本 → `parsed.txt` | **禁止** OCR 关闭：0 chunk / 乱码 |
| **PDF** · `parsing.tableExtraction` | `structured`（HTML 管道）或 `text-only` | 表格 → tab/结构化行文本 | 复杂版式 tab 错位 |
| **PDF** · `chunkingStrategy` | `paragraph-first`（默认）；长制度可 `heading-level` | 段落/标题边界块 | semantic 易切断条款 |
| **PDF** · `IndexingChunkFilter` | —（非配置项） | 过滤纯表头块 | 扫描 PDF 通常 N/A |
| **Word** · `parsing.tableExtraction` | **`structured`**（触发 HTML 管道） | `HtmlParsingContentProcessor` 表格行 | **禁止** 对复杂表格仅用 text-only 丢结构 |
| **Word** · `chunkingStrategy` | 长文档 **`heading-level`**；短文 `paragraph-first` | 按标题层级块 | 固定长度切断章节 |
| **Excel** · `parsing.tableExtraction` | **`text-only`**（v1 唯一有效） | Tika tab 分隔纯文本 | **禁止** `structured`（对 xlsx 无效，见附录 B） |
| **Excel** · `chunkingStrategy` | **`paragraph-first`** | tab 行段落块 | **禁止** `semantic`：续行/表头语义误判 |
| **Excel** · 续行 / 表头 | `cleaning` 默认 + 管道内 `TabularContinuationNormalizer` | 合并 `\t` 续行段落 | 续行与主体分离 → 召回失败 |
| **Excel** · `IndexingChunkFilter` | — | 去掉「序号\t类别\t…」纯表头块 | 表头块占比过高仍可检索噪声 |
| **Excel** · 子场景（周报 xlsx） | 同上 + 同质语义单库 | 3–4 块/文档（视 chunkSize） | 异质 xlsx 混库 → 见 §4 |
| **TXT** · `parsing.autoDetectEncoding` | **`true`** | UTF-8/GBK 等正确解码 | 乱码 → 无效向量 |
| **TXT** · `chunkingStrategy` | `paragraph-first`；极短文件可调 `chunkSize` | 段落块 | 过小 chunkSize → 碎片块 |
| **Markdown** · MIME 兜底 | `ingestAccess` 含 `markdown` | `MimeTypeAllowlist` 扩展名兜底 | `.md` 被识别为 `text/plain` 仍允许 |
| **Markdown** · `chunkingStrategy` | `paragraph-first` 或 `heading-level`（含 `#` 标题） | 标题/段落块 | 代码块被 semantic 误切 |

**矩阵行键命名** — 与 Phase 1 §5.1 规则项及 `libraryConfig.js` dot-path 对齐（`01-PATTERNS.md` §5 矩阵表结构）:
- `parsing.ocrEnabled`, `parsing.tableExtraction`, `parsing.autoDetectEncoding`, `parsing.defaultLanguage`
- `cleaning.removeHeaderFooter`, `cleaning.removeDuplicateParagraphs`
- `chunkingStrategy`, `chunkSize`, `chunkOverlap`, `minParagraphLength`
- `IndexingChunkFilter`（标注「非 config_json，入库启发式」）

**PDF 解析管道 analog**（`DocumentParseService.java` lines 44–68, 74–93）— dev 锚点脚注:
```java
// OCR：库级 ocrEnabled + Tika 字符数不足时 fallback
if (!effective.ocrEnabled()) { return tikaText; }
if (!OcrFallbackPolicy.shouldFallback(tikaText, detectedMime, fileName, ...)) { return tikaText; }
// structured 表格：requiresHtmlPipeline() → HTML 管道；否则 extractPlainWithTika
if (!effective.requiresHtmlPipeline()) {
    return extractPlainWithTika(bytes, fileName, effective);
}
```

**HTML 管道触发条件**（`DocumentParseOptions.java` lines 43–47）:
```java
public boolean requiresHtmlPipeline() {
    return tableExtraction != TableExtractionMode.TEXT_ONLY
            || imageExtraction != ImageExtractionMode.SKIP
            || formulaExtraction != FormulaExtractionMode.SKIP;
}
```

**Excel 纯 Tika 路径**（`DocumentParseService.java` lines 95–107 + `CONCERNS.md` Tech Debt）:
```java
// xlsx 始终走 extractPlainWithTika — structured 不生效
private String extractPlainWithTika(byte[] bytes, String fileName, DocumentParseOptions options) {
    ContentHandler handler = new BodyContentHandler(-1);
    parser.parse(in, handler, metadata, new ParseContext());
    return text.trim();
}
```

**表头过滤 analog**（`IndexingChunkFilter.java` lines 11–22）— 矩阵「产出」列引用:
```java
/** 入库前过滤低价值分块，减少表头块进入向量索引。 */
public static List<String> removeHeaderOnlyChunks(List<String> chunks) {
    // 若全部被判定为表头，保留原列表，避免文档完全无向量
    return kept.isEmpty() ? List.copyOf(chunks) : kept;
}
```

**可选 fixture 脚注**（D-12：非硬性验收）— `ChunkPreviewServiceTest.java` lines 40–90:
```java
// 杜鹏飞周报 tab 样本：chunkSize=500, overlap=120 → rawTotalChunks=4, filteredOutCount=1, totalChunks=3
void previewUsesIndexingChunkFilterAndLibraryChunkParams() { ... }
```

---

### §3 三层默认值对照（D-14）

**Analog:** `INGEST-PIPELINE.md` §5.1 列头 + `application.yml` + `libraryDefaults.js` + `VectorLibraryConfigFactory.java`

**汇总表结构**（每规则项或按类型汇总附录 — planner 裁量 D-14）:

| 规则项 | 配置路径 | 系统默认 (`application.yml`) | 向导默认 (`libraryDefaults.js`) | 类型推荐值 | 一致/差异说明 |
|--------|----------|------------------------------|--------------------------------|------------|--------------|
| 分块策略 | `chunkingStrategy` | `paragraph-first` (L153) | `paragraph-first` (L39) | PDF/Excel/TXT: 同左；Word 长文: `heading-level` | 库向导 chunkSize **500** vs 系统 **600** |
| 块大小 | `chunkSize` | `600` (L154) | `500` (L40) | Excel 周报: 500（测试基准） | **差异**：UI 默认低于系统兜底 |
| 块重叠 | `chunkOverlap` | `100` (L155) | `120` (L41) | 同向导或按类型脚注 | **差异** |
| OCR | `parsing.ocrEnabled` | —（库级） | `false` (L63) | 扫描 PDF: **`true`** | 系统 `ingest.ocr.enabled: true` 仅为引擎开关 |
| 表格提取 | `parsing.tableExtraction` | — | `text-only` (L64) | Word: `structured`; Excel: **`text-only`** | Excel 勿改 structured |
| 编码检测 | `parsing.autoDetectEncoding` | — | `true` (L67) | TXT/MD: **`true`** | 一致 |
| 去页眉页脚 | `cleaning.removeHeaderFooter` | — | `true` (L71) | PDF/Word 制度: 保持 `true` | 一致 |

**系统级 OCR 开关**（`application.yml` lines 59–67）:
```yaml
ingest:
  ocr:
    enabled: true   # 引擎可用性；非库级 parsing.ocrEnabled
    data-path: ${KNOWBASE_TESSDATA:./infra/tesseract/tessdata}
    language: chi_sim+eng
    min-extracted-chars-to-skip: 32
```

**向导默认 parsing 块**（`libraryDefaults.js` lines 62–69）:
```javascript
parsing: {
  ocrEnabled: false,
  tableExtraction: 'text-only',
  imageExtraction: 'skip',
  formulaExtraction: 'skip',
  autoDetectEncoding: true,
  defaultLanguage: 'zh-CN'
},
```

**后端 parsing 模型**（`ParsingRulesSettings.java` lines 5–11）— 矩阵字段名权威:
```java
private boolean ocrEnabled = false;
/** text-only | structured | skip */
private String tableExtraction = "text-only";
private boolean autoDetectEncoding = true;
private String defaultLanguage = "zh-CN";
```

**后端系统 chunking 兜底**（`application.yml` lines 152–160） vs **库 JSON**（`VectorLibraryConfigFactory.java` lines 75–83 默认 600/100）。

---

### §4 类型反模式（TYPE-05）

**Analog:** `INGEST-PIPELINE.md` §8 + `02-CONTEXT.md` D-11/D-12

**扩展 §8 规则**（D-11）— 通用反模式保留在 `INGEST-PIPELINE.md` §8；本文件每类型 2–3 行，链回 §8:

| 类型 | 错误设定 | 预期质量问题 | 链回 INGEST-PIPELINE §8 |
|------|----------|-------------|------------------------|
| PDF | 扫描件 + `ocrEnabled: false` | 空文本 / 0 chunk | 「扫描 PDF OCR 关闭」 |
| PDF | 制度库用 `semantic` 分块 | 法条/条款 mid-sentence 切断 | （类型专属，§8 无对应行） |
| Word | 复杂表格 + `text-only` | 表格结构丢失、cell 顺序错乱 | （类型专属） |
| Word | 制度库用 `fixed-char` | 标题与正文同块或硬切 | （类型专属） |
| Excel | `tableExtraction: structured` | 无收益；误以为行列对象入库 | 「Excel 误开 structured 表格」 |
| Excel | `chunkingStrategy: semantic` | 续行分离、表头块召回 | 「杜鹏飞周报 xlsx」 |
| Excel | 周报 + 报销同库 | 检索噪声 | 「异质语义混库」 |
| TXT | `autoDetectEncoding: false` + GBK 文件 | 乱码 chunk | （类型专属） |
| Markdown | 未纳入 `supportedFileTypes` | 上传 415 / 拦截 | （类型专属） |

**反模式表列结构**（复制 `INGEST-PIPELINE.md` §8 lines 484–490）:
```markdown
| 类型 | 错误设定/行为 | 症状 | 代码锚点 | 正确做法（链回 §2 矩阵 / INGEST-PIPELINE §8） |
```

**CONCERNS 条目模板**（类型专属 backlog 行，`CONCERNS.md` lines 39–44）:
```markdown
**Excel table extraction limited to plain Tika text:**
- Issue: `TableExtractionMode.STRUCTURED` applies only to HTML pipeline
- Files: `DocumentParseService.java`, `TableExtractionMode.java`
- Impact: Column boundaries depend on Tika tab output
- Fix approach: Dedicated Excel parser (Apache POI) — Out of Scope
```

---

### 附录 A：MIME → 推荐 config_json 片段（D-06/D-07）

**Analog:** `VectorLibraryConfigFactory.java` lines 14–23 + `application.yml` lines 83–92 + `supportedFileTypes.js`

**MIME 映射表**（标注「文档规划，Phase 3 预设引用」，非 v1 运行时引擎 — D-06）:

| MIME | 文件类型键 | 扩展名 | 推荐 config_json 片段（库级初始默认） |
|------|-----------|--------|--------------------------------------|
| `application/pdf` | `pdf` | `.pdf` | `{ parsing: { ocrEnabled: false, tableExtraction: 'text-only' }, chunkingStrategy: 'paragraph-first' }` — 扫描 PDF 覆盖 `ocrEnabled: true` |
| `application/msword`, `…wordprocessingml.document` | `word` | `.doc`, `.docx` | `{ parsing: { tableExtraction: 'structured', ocrEnabled: false }, chunkingStrategy: 'heading-level' }` — 短文可降 `paragraph-first` |
| `application/vnd.ms-excel`, `…spreadsheetml.sheet` | `excel` | `.xls`, `.xlsx` | `{ parsing: { tableExtraction: 'text-only' }, chunkingStrategy: 'paragraph-first' }` — **禁止** structured |
| `text/plain` | `txt` | `.txt` | `{ parsing: { autoDetectEncoding: true }, chunkingStrategy: 'paragraph-first' }` |
| `text/markdown`, `text/x-markdown`, `text/x-web-markdown` | `markdown` | `.md` | 同 TXT；`MimeTypeAllowlist` 允许 `.md` + `text/plain` 兜底 |

**代码映射源**（`VectorLibraryConfigFactory.java` lines 14–23）:
```java
private static final Map<String, List<String>> FILE_TYPE_MIMES = Map.of(
        "pdf", List.of("application/pdf"),
        "word", List.of("application/msword", "...wordprocessingml.document"),
        "txt", List.of("text/plain"),
        "markdown", List.of("text/markdown", "text/x-markdown"),
        "excel", List.of("application/vnd.ms-excel", "...spreadsheetml.sheet"));
```

**前端扩展名映射**（`supportedFileTypes.js` lines 1–7）:
```javascript
const EXT_MAP = {
  pdf: ['pdf'], word: ['doc', 'docx'], txt: ['txt'],
  markdown: ['md', 'markdown'], excel: ['xls', 'xlsx']
}
```

**MIME 默认层级**（D-07，叙述段落模板）:
1. **附录 A 值** = 库级初始默认（Phase 3 `libraryPresets.js` 套用源）
2. **垂直专用库**可覆盖（如扫描库全局 `ocrEnabled: true`）
3. **采集级覆盖** = 目标态 backlog（`INGEST-PIPELINE.md` 附录 B.3）

---

### 附录 B：结构化 Excel 差距与 backlog（D-08–D-10）

**Analog:** `CONCERNS.md` Tech Debt + `INGEST-PIPELINE.md` 附录 B + `02-CONTEXT.md` D-08–D-10

**差距摘要表**:

| 维度 | 目标态（D-08） | v1 现状（D-09） | 过渡推荐 | Backlog |
|------|---------------|----------------|----------|---------|
| Excel ingest | 结构化文档处理 → 分块/向量准确可控 | Tika tab 纯文本 → `paragraph-first` → `IndexingChunkFilter` | **`text-only` + `paragraph-first`** | POI 行对象、双轨模型（PROJECT Out of Scope） |
| `tableExtraction: structured` | 表格行列对象 | **仅 HTML 管道**（Word/PDF-derived） | Excel **不适用** | `HtmlTableExtractionProcessor.java` |
| 结构化查询 | SQL/行级 API | 向量 + regex RAG 补偿 | 文档描述路径 only | CONCERNS「Single-track vector-only」 |

**CONCERNS 展开模板**（`CONCERNS.md` lines 11–16, 39–44）:
```markdown
**Excel table extraction limited to plain Tika text:**
- Issue: STRUCTURED applies only to HTML pipeline
- Files: `DocumentParseService.java`, `TableExtractionMode.java`, `ParsingRulesSettings.java`
- Why: Structured mode built for HTML tables in Word/PDF-derived HTML, not native XLSX
- Impact: Weekly-report regex/heuristics on lossy tab text
- Fix approach: Apache POI — deferred to structured milestone
```

**显式 backlog 引用**（D-10）— 链 `PROJECT.md` Out of Scope + `INGEST-PIPELINE.md` 附录 B.3「结构化双轨 + QueryRouter」。

---

### 附录 C：字段路径与代码锚点（dev TOC）

**Analog:** `INGEST-PIPELINE.md` 附录 C + `01-PATTERNS.md` 附录 B + `libraryConfig.js`

**主矩阵相关路径子集**（Wizard 步骤 3 为主）:

| 路径 | 中文标签 | 影响重索引 | 主矩阵行 |
|------|----------|------------|----------|
| `parsing.ocrEnabled` | OCR | 是 | PDF 扫描件 |
| `parsing.tableExtraction` | 表格提取 | 是 | PDF/Word/Excel |
| `parsing.autoDetectEncoding` | 自动识别编码 | 是 | TXT/Markdown |
| `parsing.defaultLanguage` | 默认语言 | 是 | OCR 语言 |
| `chunkingStrategy` | 分块策略 | 是 | 全类型 |
| `chunkSize` / `chunkOverlap` | 块大小/重叠 | 是 | Excel 周报脚注 |
| `minParagraphLength` | 最短段落 | 是 | Excel 续行 |
| `cleaning.removeHeaderFooter` | 去页眉页脚 | 是 | PDF/Word |
| `cleaning.removeDuplicateParagraphs` | 去重复段落 | 是 | PDF 导出 |

**REINDEX_FIELDS 源**（`libraryConfig.js` lines 35–53）— 文档 dot-path 与 UI diff 一致。

**Resolver 消费链**（矩阵 dev 脚注，`INGEST-PIPELINE.md` §2.3）:
- `parseOptionsFor` ← `parsing.*` → `DocumentParseService`
- `chunkingFor` ← `chunkingStrategy`, … → `IndexingService` → `IndexingChunkFilter`

**UI 字段入口**:
- `CreateLibraryWizard.vue` — Wizard Step 3 文档处理规则
- `EditLibrarySettingsDrawer.vue` — 同上 + `lockPipeline` 禁用管道字段

---

## Shared Patterns

### 规划文档 YAML front matter
**Source:** `INGEST-PIPELINE.md` lines 1–5, `01-PATTERNS.md` Shared Patterns
**Apply to:** `FILE-TYPE-PROCESSING.md`
```markdown
---
last_mapped_commit: <git sha when doc written>
analysis_date: 2026-06-10
focus: file-type-processing
---
```

### Dual TOC（运营 / 开发）
**Source:** `INGEST-PIPELINE.md` lines 13–18
**Apply to:** 主文档目录 + §1 双锚点 `{#ops-guide}` `{#dev-reference}`

### 单张大矩阵（设定 | 产出 | 质量）
**Source:** `02-CONTEXT.md` D-03–D-05；列头对齐 ROADMAP「三列表」
**Apply to:** §2 主矩阵 — **禁止**拆成五个独立章节文件；Excel 子场景用行内缩进或脚注

### 三层默认值列
**Source:** `INGEST-PIPELINE.md` §5.1 + `application.yml` + `libraryDefaults.js`
**Apply to:** §3 或矩阵脚注；须标注系统 vs 向导 **差异**（chunkSize 600 vs 500）

### 反模式扩展 §8
**Source:** `INGEST-PIPELINE.md` §8 + `CONCERNS.md` Tech Debt 模板
**Apply to:** §4 — 每类型 2–3 行；通用行（预览≠入库、异质混库）**链回** INGEST-PIPELINE §8，不重复

### 附录命名
**Source:** `INGEST-PIPELINE.md` 附录 A/B/C 模式（`01-PATTERNS.md` doc-root）
**Apply to:** 附录 A MIME / 附录 B 结构化差距 / 附录 C 字段路径

### 字段路径 dot-path
**Source:** `libraryConfig.js` `REINDEX_FIELDS` / `CONFIG_FIELD_SPECS`
**Apply to:** 矩阵「设定」列、`附录 C` — 与前端 diff 一致

### Phase 交叉引用
**Source:** `INGEST-PIPELINE.md` §7.5, 附录 B.1
**Apply to:** 开篇、§4、附录 A/B — Phase 3 PRESET、Phase 4 PARITY、结构化 backlog

### 目标态 vs 现状脚注
**Source:** `INGEST-PIPELINE.md` D-01 模式（目标态正文 + 「当前差距」少量脚注）
**Apply to:** Excel structured 叙述 — 目标态一句 + 附录 B 详表；正文以过渡推荐为主（D-09）

---

## No Analog Found

| Deliverable | Role | Reason |
|-------------|------|--------|
| MIME 自动默认**运行时**引擎 | doc-section | 无实现；附录 A 为规划表，Phase 3 `libraryPresets.js` 引用（D-06） |
| 按 MIME 的 chunking 自动切换 | doc-section | v1 库级单配置；仅文档描述 Phase 3 目标 |
| POI / 结构化 Excel ingest 路径 | doc-section | PROJECT Out of Scope；仅 backlog 叙述 |
| 独立 `TYPE-MATRIX.md` per 类型 | doc-split | CONTEXT D-03 明确单张大表；无先例 |

---

## Metadata

**Analog search scope:** `.planning/docs/`, `.planning/phases/01-ingest-pipeline/`, `.planning/phases/02-file-type-matrix/`, `.planning/codebase/`, `knowbase-service/src/main/java/com/knowbase/{ingest,library,vector}/`, `knowbase-service/src/main/resources/application.yml`, `frontend/knowbase-ui/src/utils/{libraryDefaults,libraryConfig,supportedFileTypes}.js`
**Files scanned:** 22
**Pattern extraction date:** 2026-06-10
