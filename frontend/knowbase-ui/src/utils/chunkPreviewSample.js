import { SYSTEM_CHUNKING_DEFAULTS } from './libraryDefaults'

/**
 * 分块策略对比示例：双主题 + 4 级标题，便于区分策略差异。
 */
export const CHUNK_PREVIEW_COMPARISON_SAMPLE = `# 向量检索入门

pgvector 是 PostgreSQL 的向量扩展，适合存储与检索 embedding 向量。HNSW 索引可在较大规模数据集上实现近似最近邻查询，是 RAG 系统的常见底座。创建表时需要指定 vector(n) 类型，其中 n 必须与 Embedding 模型输出维度一致。

## HNSW 参数说明

ef_construction 越大，建索引质量通常越高，但构建时间更长。M 表示图中节点的最大连接数，常见取值为 16 到 64。查询时 ef_search 在召回率与延迟之间做权衡，生产环境建议通过压测确定。

# 家常红烧肉

选用带皮五花肉，切成长宽约两厘米的块。冷水下锅，加料酒与姜片焯水，撇沫后捞出沥干。锅中小火炒冰糖至枣红色，下肉块翻炒上色，再加生抽、老抽与热水没过肉面。

## 火候与收汁

大火煮沸后转小火，盖盖炖约四十分钟，至筷子能轻松插入。开盖转大火收浓汤汁，出锅前可撒葱花。全程注意避免糊锅，糖色过深会发苦。`

const STRATEGY_EXPECTATIONS = {
  'heading-level': '预期约 4 块（每个 # / ## 标题一节）',
  'semantic': '预期约 2 块（一级 # 标题处分主题，子节按相似度合并）',
  'paragraph-first': '预期约 4 块（标题+正文合并为节，超长节再切）',
  'fixed-char': '预期约 5~6 块（等长 ~100 字窗口，可能切断标题行）'
}

/** 与入库流水线一致的分块参数（库级仅 size/overlap；合并规则来自系统 chunking.*） */
export function libraryChunkParams(config = {}) {
  return {
    chunkSize: config.chunkSize ?? 500,
    chunkOverlap: config.chunkOverlap ?? 120,
    ...SYSTEM_CHUNKING_DEFAULTS
  }
}

/**
 * @deprecated 预览临时缩放已废弃；请使用 {@link libraryChunkParams} 与库配置保持一致。
 */
export function resolvePreviewChunkParams(textLength, config, strategy = 'paragraph-first') {
  const base = {
    chunkSize: config.chunkSize ?? 600,
    chunkOverlap: config.chunkOverlap ?? 100,
    ...SYSTEM_CHUNKING_DEFAULTS
  }
  const expectedHint = STRATEGY_EXPECTATIONS[strategy] || ''

  if (!textLength || textLength < 120) {
    return { ...base, adjusted: false, previewHint: '', expectedHint }
  }

  let idealChunkSize
  let chunkOverlap = Math.min(base.chunkOverlap, 20)
  let maxChunkSize

  switch (strategy) {
    case 'semantic':
      // 块要大，相似度才能主导切分（否则被 chunkSize 打碎成 6~7 块）
      idealChunkSize = Math.min(base.chunkSize, Math.max(400, Math.ceil(textLength * 0.6)))
      maxChunkSize = Math.min(base.maxChunkSize, Math.max(idealChunkSize, textLength))
      chunkOverlap = Math.min(base.chunkOverlap, 0)
      break
    case 'heading-level':
      idealChunkSize = Math.min(base.chunkSize, Math.max(250, Math.ceil(textLength / 3)))
      maxChunkSize = Math.min(base.maxChunkSize, Math.max(500, idealChunkSize * 2))
      break
    case 'fixed-char':
      idealChunkSize = Math.min(base.chunkSize, Math.max(100, Math.ceil(textLength / 8)))
      maxChunkSize = Math.min(base.maxChunkSize, Math.max(200, idealChunkSize + 80))
      chunkOverlap = Math.min(base.chunkOverlap, Math.max(10, Math.floor(idealChunkSize * 0.12)))
      break
    default:
      // 需容纳「标题 + 正文」合并后的最长节（示例约 140 字），避免误切成 5 块
      idealChunkSize = Math.min(base.chunkSize, Math.max(160, Math.ceil(textLength / 3)))
      maxChunkSize = Math.min(base.maxChunkSize, Math.max(320, idealChunkSize * 2))
      chunkOverlap = Math.min(base.chunkOverlap, Math.max(0, Math.floor(idealChunkSize * 0.1)))
      break
  }

  if (idealChunkSize >= base.chunkSize && strategy === 'paragraph-first') {
    return { ...base, adjusted: false, previewHint: '', expectedHint }
  }

  const minChunkSize = Math.min(base.minChunkSize, Math.max(20, Math.floor(idealChunkSize * 0.25)))
  const previewHint =
    idealChunkSize < base.chunkSize
      ? `预览临时分块大小 ${idealChunkSize}（库配置 ${base.chunkSize}），按「${strategyLabel(strategy)}」策略优化。`
      : ''

  return {
    chunkSize: idealChunkSize,
    chunkOverlap,
    maxChunkSize,
    minChunkSize,
    minParagraphLength: Math.min(base.minParagraphLength, 20),
    adjusted: idealChunkSize < base.chunkSize,
    previewHint,
    expectedHint
  }
}

function strategyLabel(strategy) {
  return (
    {
      'paragraph-first': '按段落',
      'fixed-char': '固定长度',
      semantic: '语义分块',
      'heading-level': '按标题层级'
    }[strategy] || strategy
  )
}

