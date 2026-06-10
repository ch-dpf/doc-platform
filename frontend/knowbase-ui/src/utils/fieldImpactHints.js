/** 解析/分块字段对索引与解析行为的影响说明（CFG-02 单一来源） */
export const IMPACT_HINTS = {
  'parsing.ocrEnabled': '扫描件 PDF 图片页需开启；依赖服务端 tessdata',
  'parsing.tableExtraction': 'Word/PDF 复杂表格用 structured；Excel 周报保持 text-only（structured 对 xlsx 无效）',
  'parsing.imageExtraction': '嵌入图片 OCR 描述或跳过',
  chunkingStrategy: 'paragraph-first 适合 Excel 周报；semantic 不适合表格续行',
  chunkSize: '影响块边界与表头过滤结果；变更需重索引',
  minParagraphLength: '影响块边界与表头过滤结果；变更需重索引'
}

export function fieldImpactHint(path) {
  return IMPACT_HINTS[path] || ''
}
