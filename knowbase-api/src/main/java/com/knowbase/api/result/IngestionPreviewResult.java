package com.knowbase.api.result;

import java.util.List;
import java.util.UUID;

/** 批量入库预览响应；预览固定执行 parse → normalize → chunk 全流程但不写入索引。 */
public record IngestionPreviewResult(
        UUID libraryId,
        int sourceCount,
        int succeededDocuments,
        int failedDocuments,
        int totalChunks,
        int indexableChunks,
        List<DocumentPreviewResult> documents
) {
}
