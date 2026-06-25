package com.knowbase.preset;

import com.knowbase.domain.model.LibraryTypePreset;
import com.knowbase.domain.model.SceneRulePreset;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BuiltinPresetCatalog implements PresetCatalog {

    private static final List<LibraryTypePreset> LIBRARY_TYPE_PRESETS = List.of(
            libraryPreset(
                    "general_docs",
                    "通用文档库",
                    "制度、说明、普通文本资料",
                    512,
                    64,
                    8,
                    generalDocumentProfiles()
            ),
            libraryPreset(
                    "product_knowledge",
                    "产品知识库",
                    "产品手册、FAQ 与排障资料",
                    512,
                    80,
                    8,
                    List.of(
                            documentProfile("default_markdown", "RICH_TEXT", "markdown-structure", "structure_token_window"),
                            documentProfile("default_docx", "RICH_TEXT", "docx-structure", "structure_token_window"),
                            documentProfile("default_text", "PLAIN_TEXT", "text-structure", "paragraph_token_window"),
                            documentProfile("default_faq", "PLAIN_TEXT", "qa", "qa_token_window"),
                            documentProfile("default_rich_text", "RICH_TEXT", "tika", "structure_token_window"),
                            tableDocumentProfile("default_table", "STRUCTURED_TABLE", "table-deep", "table_row_token_window"),
                            documentProfile("default_presentation", "PRESENTATION", "pptx-structure", "slide_token_window"),
                            documentProfile("default_web_page", "WEB_PAGE", "html-structure", "dom_token_window")
                    )
            ),
            libraryPreset(
                    "technical_docs",
                    "技术文档库",
                    "接口文档、部署文档、研发规范与配置说明",
                    640,
                    96,
                    10,
                    List.of(
                            documentProfile("default_markdown", "RICH_TEXT", "markdown-structure", "structure_token_window"),
                            documentProfile("default_docx", "RICH_TEXT", "docx-structure", "structure_token_window"),
                            documentProfile("default_pdf", "RICH_TEXT", "pdf-layout", "page_token_window"),
                            documentProfile("default_text", "PLAIN_TEXT", "text-structure", "paragraph_token_window"),
                            documentProfile("default_code_or_config", "CODE_OR_CONFIG", "code-config-structure", "code_token_window"),
                            documentProfile("default_rich_text", "RICH_TEXT", "tika", "structure_token_window"),
                            tableDocumentProfile("default_table", "STRUCTURED_TABLE", "table-deep", "table_row_token_window"),
                            documentProfile("default_presentation", "PRESENTATION", "pptx-structure", "slide_token_window"),
                            documentProfile("default_web_page", "WEB_PAGE", "html-structure", "dom_token_window")
                    )
            ),
            libraryPreset(
                    "policy_compliance",
                    "制度合规库",
                    "规章制度、审计材料与合规条款",
                    768,
                    96,
                    10,
                    List.of(
                            documentProfile("default_markdown", "RICH_TEXT", "markdown-structure", "structure_token_window"),
                            documentProfile("default_text", "PLAIN_TEXT", "text-structure", "paragraph_token_window"),
                            documentProfile("default_rich_text", "RICH_TEXT", "tika", "structure_token_window"),
                            tableDocumentProfile("default_table", "STRUCTURED_TABLE", "table-deep", "table_row_token_window"),
                            documentProfile("default_web_page", "WEB_PAGE", "html-structure", "dom_token_window"),
                            documentProfile("default_scanned_document", "SCANNED_DOCUMENT", "ocr-layout", "page_token_window")
                    )
            ),
            libraryPreset(
                    "table_report",
                    "表格报表库",
                    "Excel、周报、月报与统计表",
                    512,
                    64,
                    12,
                    List.of(
                            tableDocumentProfile("default_table", "STRUCTURED_TABLE", "table-deep", "table_row_token_window"),
                            documentProfile("default_markdown", "RICH_TEXT", "markdown-structure", "structure_token_window"),
                            documentProfile("default_text", "PLAIN_TEXT", "text-structure", "paragraph_token_window"),
                            documentProfile("default_rich_text", "RICH_TEXT", "tika", "structure_token_window"),
                            documentProfile("default_web_page", "WEB_PAGE", "html-structure", "dom_token_window")
                    )
            ),
            libraryPreset(
                    "research_archive",
                    "研究资料库",
                    "论文、报告、调研材料与归档资料",
                    768,
                    128,
                    12,
                    generalDocumentProfiles()
            ),
            libraryPreset(
                    "contract_legal",
                    "合同法务库",
                    "合同、协议与条款文本",
                    768,
                    96,
                    10,
                    List.of(
                            documentProfile("default_markdown", "RICH_TEXT", "markdown-structure", "structure_token_window"),
                            documentProfile("default_text", "PLAIN_TEXT", "text-structure", "paragraph_token_window"),
                            documentProfile("default_rich_text", "RICH_TEXT", "tika", "structure_token_window"),
                            documentProfile("default_scanned_document", "SCANNED_DOCUMENT", "ocr-layout", "page_token_window")
                    )
            ),
            libraryPreset(
                    "general_knowledge",
                    "通用知识库",
                    "兼容旧调用的通用知识资料预设",
                    512,
                    64,
                    8,
                    generalDocumentProfiles()
            )
    );

    private static final List<SceneRulePreset> SCENE_RULE_PRESETS = List.of(
            scenePreset(
                    "internal_knowledge_assistant",
                    "内部知识助手",
                    "面向内部员工的知识问答场景",
                    "请基于证据回答，保持准确、简洁，并在答案中保留引用。",
                    8,
                    20,
                    12,
                    true,
                    true,
                    4096
            ),
            scenePreset(
                    "customer_service_bot",
                    "客服问答",
                    "面向客户服务的一致口径问答",
                    "请用友好、清晰的客服口吻回答。只能基于证据作答，证据不足时请说明无法确认。",
                    6,
                    16,
                    8,
                    true,
                    true,
                    3072
            ),
            scenePreset(
                    "research_analyst",
                    "研究分析",
                    "跨文档归纳、差异比较与证据覆盖",
                    "请按主题归纳证据，指出不同来源的共识与差异，不要把冲突证据合并成单一结论。",
                    10,
                    30,
                    16,
                    true,
                    true,
                    6144
            ),
            scenePreset(
                    "compliance_qa",
                    "合规问答",
                    "强调引用完整、不可推测与严格拒答",
                    "请严格依据证据回答。若证据不足、过期或存在冲突，必须拒答或列明冲突来源。",
                    10,
                    24,
                    12,
                    true,
                    true,
                    4096
            ),
            scenePreset(
                    "technical_support",
                    "技术支持",
                    "步骤化排障、版本与环境信息说明",
                    "请按排障步骤回答，优先列出可执行操作、前置条件和引用来源。",
                    8,
                    24,
                    12,
                    true,
                    true,
                    4096
            ),
            scenePreset(
                    "report_writer",
                    "报告生成",
                    "结构化输出、来源分组与摘要提炼",
                    "请按结构化格式输出报告，按来源库分组引用，并提炼关键摘要。",
                    10,
                    24,
                    14,
                    true,
                    true,
                    6144
            ),
            scenePreset(
                    "faq_assistant",
                    "FAQ 助手",
                    "面向 FAQ 场景的简短回答",
                    "请给出简短直接的答案，并保留最相关的引用。",
                    5,
                    10,
                    5,
                    true,
                    true,
                    2048
            )
    );

    @Override
    public List<LibraryTypePreset> listLibraryTypePresets() {
        return LIBRARY_TYPE_PRESETS;
    }

    @Override
    public List<SceneRulePreset> listSceneRulePresets() {
        return SCENE_RULE_PRESETS;
    }

    @Override
    public Optional<LibraryTypePreset> findLibraryTypePreset(String code) {
        return listLibraryTypePresets().stream()
                .filter(preset -> preset.code().equals(code))
                .findFirst();
    }

    @Override
    public Optional<SceneRulePreset> findSceneRulePreset(String code) {
        return listSceneRulePresets().stream()
                .filter(preset -> preset.code().equals(code))
                .findFirst();
    }

    private static LibraryTypePreset libraryPreset(
            String code,
            String name,
            String description,
            int chunkMaxTokens,
            int chunkOverlapTokens,
            int retrievalTopK,
            List<Map<String, Object>> documentProfiles
    ) {
        return new LibraryTypePreset(
                code,
                name,
                description,
                Map.of(
                        "embeddingProvider", "ollama",
                        "embeddingModel", "bge-m3",
                        "embeddingDimension", 1024,
                        "embeddingTokenizer", "ollama:bge-m3",
                        "chunkMaxTokens", chunkMaxTokens,
                        "chunkOverlapTokens", chunkOverlapTokens,
                        "retrievalTopK", retrievalTopK,
                        "documentProfiles", documentProfiles,
                        "contentGovernance", Map.of(
                                "preserveSourceMetadata", true,
                                "requireUnifiedEmbeddingModel", true,
                                "publishIndexVersionOnSuccess", true
                        )
                ),
                true,
                true
        );
    }

    private static Map<String, Object> documentProfile(
            String code,
            String contentFamily,
            String parserCode,
            String chunkingStrategy
    ) {
        return Map.of(
                "code", code,
                "contentFamily", contentFamily,
                "parserCode", parserCode,
                "chunkingStrategy", chunkingStrategy,
                "metadataSchema", Map.of(
                        "sourceUri", "string",
                        "title", "string",
                        "contentFamily", "string",
                        "page", "integer",
                        "section", "string"
                ),
                "options", Map.ofEntries(
                        Map.entry("preserveStructureBoundary", true),
                        Map.entry("fallbackSplitMode", "recursive"),
                        Map.entry("chunkMode", "parent_child"),
                        Map.entry("splitMode", "recursive"),
                        Map.entry("chunkSizeUnit", "token"),
                        Map.entry("chunkMaxChars", 2048),
                        Map.entry("chunkOverlapChars", 256),
                        Map.entry("minChunkChars", 80),
                        Map.entry("prependHeadingContext", true),
                        Map.entry("unicodeNormalize", true),
                        Map.entry("dehyphenateLineBreaks", true),
                        Map.entry("removePageFooters", true),
                        Map.entry("chunkEngine", "smart"),
                        Map.entry("llmDocumentSummary", false),
                        Map.entry("llmSummaryPromptId", "default_summary"),
                        Map.entry("llmSummaryMaxInputChars", 16384),
                        Map.entry("llmSummaryMaxChars", 500),
                        Map.entry("llmSummaryTemperature", 0.3),
                        Map.entry("llmSummaryMaxCompletionTokens", 2048)
                )
        );
    }

    private static Map<String, Object> tableDocumentProfile(
            String code,
            String contentFamily,
            String parserCode,
            String chunkingStrategy
    ) {
        return Map.of(
                "code", code,
                "contentFamily", contentFamily,
                "parserCode", parserCode,
                "chunkingStrategy", chunkingStrategy,
                "metadataSchema", Map.of(
                        "sourceUri", "string",
                        "title", "string",
                        "contentFamily", "string",
                        "page", "integer",
                        "section", "string"
                ),
                "options", Map.ofEntries(
                        Map.entry("preserveStructureBoundary", true),
                        Map.entry("fallbackSplitMode", "recursive"),
                        Map.entry("chunkMode", "parent_child"),
                        Map.entry("splitMode", "recursive"),
                        Map.entry("chunkSizeUnit", "token"),
                        Map.entry("chunkMaxChars", 2048),
                        Map.entry("chunkOverlapChars", 256),
                        Map.entry("minChunkChars", 80),
                        Map.entry("prependHeadingContext", true),
                        Map.entry("unicodeNormalize", true),
                        Map.entry("dehyphenateLineBreaks", true),
                        Map.entry("removePageFooters", true),
                        Map.entry("chunkEngine", "smart"),
                        Map.entry("tableChunkPostProcess", true),
                        Map.entry("prependSheetContext", true),
                        Map.entry("emitDocumentSummary", false),
                        Map.entry("tableRowGroupMaxRows", 1),
                        Map.entry("tableIndexMinFields", 4),
                        Map.entry("mergeSmallRowChunks", false),
                        Map.entry("deduplicateChunks", true),
                        Map.entry("tableRowMergeBelowTokens", 64),
                        Map.entry("llmDocumentSummary", false),
                        Map.entry("llmSummaryPromptId", "default_summary"),
                        Map.entry("llmSummaryMaxInputChars", 16384),
                        Map.entry("llmSummaryMaxChars", 800),
                        Map.entry("llmSummaryMinInputChars", 40),
                        Map.entry("llmSummaryTemperature", 0.3),
                        Map.entry("llmSummaryMaxCompletionTokens", 2048)
                )
        );
    }

    private static List<Map<String, Object>> generalDocumentProfiles() {
        return List.of(
                documentProfile("default_markdown", "RICH_TEXT", "markdown-structure", "structure_token_window"),
                documentProfile("default_docx", "RICH_TEXT", "docx-structure", "structure_token_window"),
                documentProfile("default_pdf", "RICH_TEXT", "pdf-layout", "page_token_window"),
                documentProfile("default_pdf_structure", "RICH_TEXT", "pdf-structure", "page_token_window"),
                documentProfile("default_text", "PLAIN_TEXT", "text-structure", "paragraph_token_window"),
                documentProfile("default_faq", "PLAIN_TEXT", "qa", "qa_token_window"),
                documentProfile("default_zip_bundle", "RICH_TEXT", "zip", "structure_token_window"),
                documentProfile("default_rich_text", "RICH_TEXT", "tika", "structure_token_window"),
                tableDocumentProfile("default_table", "STRUCTURED_TABLE", "table-deep", "table_row_token_window"),
                documentProfile("default_presentation", "PRESENTATION", "pptx-structure", "slide_token_window"),
                documentProfile("default_web_page", "WEB_PAGE", "html-structure", "dom_token_window"),
                documentProfile("default_scanned_document", "SCANNED_DOCUMENT", "ocr-layout", "page_token_window"),
                documentProfile("default_image", "IMAGE_TEXT", "ocr-layout", "page_token_window"),
                documentProfile("default_code_or_config", "CODE_OR_CONFIG", "code-config-structure", "code_token_window")
        );
    }

    private static SceneRulePreset scenePreset(
            String code,
            String name,
            String description,
            String systemPrompt,
            int topKPerLibrary,
            int maxCandidates,
            int maxEvidence,
            boolean citationRequired,
            boolean refuseWhenEvidenceLow,
            int maxContextTokens
    ) {
        return new SceneRulePreset(
                code,
                name,
                description,
                Map.of(
                        "systemPrompt", systemPrompt,
                        "routing", Map.of(
                                "mode", "selected_libraries",
                                "maxLibraries", 8
                        ),
                        "retrieval", Map.<String, Object>ofEntries(
                                Map.entry("topKPerLibrary", topKPerLibrary),
                                Map.entry("maxCandidates", maxCandidates),
                                Map.entry("maxEvidence", maxEvidence),
                                Map.entry("fusion", "rrf"),
                                Map.entry("rerank", "mmr"),
                                Map.entry("rrfK", 60),
                                Map.entry("mmrLambda", 0.72),
                                Map.entry("balanceAcrossLibraries", true),
                                Map.entry("maxCandidatesPerLibrary", Math.max(2, maxCandidates / 2)),
                                Map.entry("vectorScoreWeight", 0.15),
                                Map.entry("keywordScoreWeight", 0.10),
                                Map.entry("deduplicateByChunk", true),
                                Map.entry("deduplicateByContent", true),
                                Map.entry("expandParentChunks", true),
                                Map.entry("contentFamilyWeights", Map.of(
                                        "STRUCTURED_TABLE", 1.08,
                                        "CODE_OR_CONFIG", 1.05,
                                        "RICH_TEXT", 1.0,
                                        "PLAIN_TEXT", 1.0,
                                        "WEB_PAGE", 0.98,
                                        "PRESENTATION", 0.95,
                                        "SCANNED_DOCUMENT", 0.92
                                ))
                        ),
                        "answer", Map.of(
                                "citationRequired", citationRequired,
                                "refuseWhenEvidenceLow", refuseWhenEvidenceLow,
                                "minEvidenceCount", 1,
                                "maxContextTokens", maxContextTokens
                        ),
                        "citation", Map.of(
                                "granularity", "chunk",
                                "groupByLibrary", true
                        )
                ),
                true,
                true
        );
    }
}
