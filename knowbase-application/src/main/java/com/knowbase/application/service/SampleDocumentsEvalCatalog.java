package com.knowbase.application.service;

import com.knowbase.api.command.CreateRetrievalEvalSampleCommand;
import com.knowbase.api.command.ImportRetrievalEvalSamplesCommand;

import java.util.List;

final class SampleDocumentsEvalCatalog {

    private SampleDocumentsEvalCatalog() {
    }

    static ImportRetrievalEvalSamplesCommand buildImportCommand(boolean replaceExisting) {
        return new ImportRetrievalEvalSamplesCommand(
                "1",
                replaceExisting,
                List.of(
                        new CreateRetrievalEvalSampleCommand(
                                "如何安装 PostgreSQL 和 pgvector？",
                                List.of(),
                                List.of("guide.md"),
                                List.of("Install PostgreSQL and pgvector on port 5433"),
                                8,
                                "sample-documents/markdown/guide.md",
                                true
                        ),
                        new CreateRetrievalEvalSampleCommand(
                                "如何在知识库中上传文档并做召回测试？",
                                List.of(),
                                List.of("guide.md"),
                                List.of("Upload documents from the library workspace and run retrieval tests"),
                                8,
                                "sample-documents/markdown/guide.md",
                                true
                        ),
                        new CreateRetrievalEvalSampleCommand(
                                "Markdown 分块是否保留标题边界？",
                                List.of(),
                                List.of("guide.md"),
                                List.of("Structure-aware markdown parsing should preserve heading boundaries"),
                                8,
                                "sample-documents/markdown/guide.md",
                                true
                        ),
                        new CreateRetrievalEvalSampleCommand(
                                "FAQ 文档包含哪些章节？",
                                List.of(),
                                List.of("faq-outline.md"),
                                List.of("Retrieval evaluation"),
                                8,
                                "sample-documents/markdown/faq-outline.md",
                                true
                        )
                )
        );
    }
}
