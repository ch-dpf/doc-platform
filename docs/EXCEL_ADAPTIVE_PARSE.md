# Excel 自适应解析方案（table-deep / table-adaptive）

> 产品定位：用户上传 Excel/CSV 时**无需选择解析器**；系统在同一 `table-deep` 解析器内完成「物理读取 → 布局识别 → 分角色序列化」，输出**面向检索的最优文本**。

## 1. 设计目标

| 目标 | 说明 |
|------|------|
| 易用 | 一个内置解析器 `table-deep`，对用户透明 |
| 通用 | 同一管线覆盖常规矩形表、周报表单混排、多 Sheet |
| 可检索 | 数据行使用**列名: 值**；元数据行使用**标签: 值**；版式行默认不索引 |
| 可回退 | 无法识别表头时回退 **A/B/C 坐标展平**；再失败走 Tika |

## 2. 三阶段管线

```mermaid
flowchart LR
  A[阶段1 物理层] --> B[阶段2 布局层]
  B --> C[阶段3 序列化层]
  C --> D[ParsedDocument + table_row blocks]
```

### 阶段 1：物理层（所有 Excel 共用）

- POI 读取 Workbook / CSV 分隔符检测
- 合并单元格主格取值（读格时继承）
- 公式求值、日期 ISO 格式化
- 跳过空行；保留 sheetName、行列坐标、公式/隐藏列等元数据

**实现**：`StructuredTableDocumentParser` + `AdaptiveTableSheetProcessor.RowValueReader`

### 阶段 2：布局层（行角色识别）

对每一非空行判定 `rowRole`：

| rowRole | 含义 | 典型行 |
|---------|------|--------|
| `LAYOUT` | 标题/小节 | 星图深海…工作周报 |
| `FORM_KV` | 表单元数据 | 部门/姓名/更新日期 |
| `SEPARATOR` | 日期区间 | 2026年5月06日--5月09日 |
| `HEADER` | 数据表表头 | 序号/项目名称/工作内容 |
| `DATA` | 表头下数据行 | 1 / FB项目 / 已完成 |
| `COORDINATE` | 回退 | A: v, B: v |

状态机：

- `LAYOUT` / `FORM_KV` / `SEPARATOR` → 清除当前表头上下文
- `HEADER` → 建立列名上下文，产出表头块（`indexableHint=false`）
- `DATA` → 在上下文下按列名序列化

**实现**：`AdaptiveTableLayoutAnalyzer`

### 阶段 3：序列化层（最优检索文本）

| rowRole | 输出示例 |
|---------|----------|
| `DATA` | `[Sheet: 周报3月] 序号: 1 \| 项目名称: FB项目 \| 执行情况: 已完成` |
| `FORM_KV` | `[Sheet: 周报3月 \| 元数据] 部门: 软件技术部 \| 姓名: 杜鹏飞` |
| `LAYOUT` | `[Sheet: 周报3月] 文档标题: 星图深海软件技术部工作周报` |
| `SEPARATOR` | `[Sheet: 周报3月] 汇报周期: 2026年5月06日--5月09日` |
| `COORDINATE` | `A: 值, B: 值`（无稳定表头时） |

块元数据新增：

- `rowRole`、`serializationStrategy`、`indexableHint`
- 保留 `sheetName`、`rowIndex`、`cellCoordinates` 等追溯字段

**实现**：`AdaptiveTableTextSerializer`

## 3. 与入库链路衔接

```
load → parse (table-deep adaptive) → normalize → chunk → post_process → embed
```

- **分块**：`SmartTableDocumentChunker` 读取 `indexableHint`；`LAYOUT`/`HEADER`/`SEPARATOR` 默认不向量索引
- **清洗**：`DocumentTextNormalizer` 仍做通用文本卫生，不改变行角色
- **预设**：`table_report` 库类型仍路由 `parserCode=table-deep`，无需用户改配置

## 4. 解析元数据（ParsedDocument.metadata）

| 字段 | 值 |
|------|-----|
| `parser` | `table-deep` |
| `parserEngine` | `table-adaptive` |
| `rowSerialization` | `adaptive` |
| `columnKeyStrategy` | `header_then_letter` |

## 5. 回退链

1. 自适应序列化（默认）
2. 列坐标展平 `COORDINATE`（无表头 / 识别失败）
3. Tika 纯文本（解析无结构块时）

## 6. 代码模块

| 模块 | 路径 |
|------|------|
| 解析入口 | `StructuredTableDocumentParser` |
| Sheet 编排 | `adaptive/AdaptiveTableSheetProcessor` |
| 行角色 | `adaptive/AdaptiveTableLayoutAnalyzer` |
| 文本序列化 | `adaptive/AdaptiveTableTextSerializer` |
| 行角色枚举 | `adaptive/TableRowRole` |

## 7. 后续增强（二期）

- Sheet 内**矩形区域切分**（本周 / 下周双段表）
- 多级表头 `headerPath` 继承
- 解析置信度低时 UI 提示「建议检查分块预览」
- 外接 Docling/Unstructured 映射到同一 `table_row` IR
