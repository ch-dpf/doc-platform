package com.knowbase.preset;

import com.knowbase.domain.model.LibraryTypePreset;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 建仓入库产品目录：解析器、文档 Profile 模板、库预设场景说明（中文）。
 */
public final class IngestionProductCatalog {

    private static final String CONFIG_MODEL_NOTE = """
            三层配置：① 库类型预设（建仓模板，快照到实例）② Library Profile L1（向量模型、chunk 上限、TopK，发版治理）\
            ③ Document Profile L2（按文件类型路由解析器与切块策略）。建仓后修改预设模板不会自动同步到已有库。""";

    private static final List<ParserDefinition> PARSERS = List.of(
            parser("markdown-structure", "Markdown 结构解析", "按标题层级提取段落与代码块", true, false, false,
                    List.of("md", "markdown"), List.of("结构感知", "标题章节")),
            parser("html-structure", "HTML 结构解析", "解析标题、列表、顶层表格与 colspan/rowspan", true, false, false,
                    List.of("html", "htm"), List.of("结构感知", "网页", "表格")),
            parser("docx-structure", "Word 结构解析", "按 Word 标题与段落结构提取", true, false, false,
                    List.of("docx"), List.of("结构感知", "Office")),
            parser("pptx-structure", "PPT 结构解析", "按幻灯片/标题/正文/表格提取结构块", true, false, false,
                    List.of("pptx"), List.of("结构感知", "幻灯片", "表格")),
            parser("pdf-layout", "PDF 版面解析", "按页与版面块提取文本；扫描件/低置信度可路由 VLM 或 OCR", true, false, false,
                    List.of("pdf"), List.of("版面分析", "分页", "VLM", "表格区域")),
            parser("pdf-structure", "PDF 结构解析", "按文本流与标题模式分段", true, false, false,
                    List.of("pdf"), List.of("文本流", "分页")),
            parser("text-structure", "纯文本结构解析", "按段落与标题模式分段", true, false, false,
                    List.of("txt", "log"), List.of("段落")),
            parser("text", "纯文本", "整文或简单分段，适合代码与配置", true, false, false,
                    List.of("txt", "java", "yml", "json", "xml", "properties"), List.of("代码", "配置")),
            parser("code-config-structure", "代码与配置结构解析", "YAML/JSON/Properties 按配置段、源码按声明边界切块", true, false, false,
                    List.of("java", "kt", "js", "ts", "vue", "py", "yml", "yaml", "json", "xml", "properties"), List.of("配置段", "代码块")),
            parser("table-deep", "表格自适应解析", "Excel/CSV 三阶段自适应：表头提升、表单元数据、版式行过滤，失败回退 A/B/C", true, false, false,
                    List.of("xlsx", "xls", "csv", "ods"), List.of("表格", "表头识别", "表单混排")),
            parser("qa", "FAQ 问答解析", "从表格或文本提取问答对", true, false, false,
                    List.of("csv", "xlsx", "xls"), List.of("FAQ", "问答对")),
            parser("zip", "压缩包解析", "解压并递归处理包内文件", true, false, false,
                    List.of("zip"), List.of("批量", "压缩包")),
            parser("tika", "Tika 通用解析", "通用富文本回退，结构感知较弱", true, false, false,
                    List.of("docx", "pdf", "pptx", "rtf", "odt"), List.of("通用回退")),
            parser("ocr-layout", "OCR 版面解析", "扫描件与图片 OCR + 版面块", true, false, false,
                    List.of("pdf", "png", "jpg", "jpeg", "tiff", "bmp"), List.of("OCR", "扫描件")),
            parser("docling", "Docling（外接）", "调用外部 Docling 服务解析复杂版式", false, true, true,
                    List.of("pdf", "docx"), List.of("外接", "复杂版式")),
            parser("unstructured", "Unstructured（外接）", "调用 Unstructured API 做 ETL 解析", false, true, true,
                    List.of("pdf", "docx", "html", "pptx"), List.of("外接", "ETL")),
            parser("external", "自定义外接解析", "HTTP 上传文件到自建解析服务", false, true, true,
                    List.of("*"), List.of("外接", "自定义"))
    );

    private static final List<ProfileTemplateDefinition> PROFILE_TEMPLATES = List.of(
            profile("default_markdown", "Markdown 文档", "技术说明、README、接口文档",
                    "RICH_TEXT", "markdown-structure", "structure_token_window",
                    "按标题章节切块", List.of("md", "markdown")),
            profile("default_docx", "Word 文档", "用户手册、制度、方案",
                    "RICH_TEXT", "docx-structure", "structure_token_window",
                    "按标题章节切块", List.of("docx")),
            profile("default_pdf", "PDF 版面", "扫描质量较好的 PDF 报告",
                    "RICH_TEXT", "pdf-layout", "page_token_window",
                    "按页/段落切块", List.of("pdf")),
            profile("default_pdf_structure", "PDF 文本流", "纯文本型 PDF",
                    "RICH_TEXT", "pdf-structure", "page_token_window",
                    "按页/段落切块", List.of("pdf")),
            profile("default_text", "纯文本", "日志、说明、无结构长文",
                    "PLAIN_TEXT", "text-structure", "paragraph_token_window",
                    "按段落切块", List.of("txt", "log")),
            profile("default_faq", "FAQ 问答表", "文件名含 faq/问答 的表格",
                    "PLAIN_TEXT", "qa", "qa_token_window",
                    "按问答对切块", List.of("csv", "xlsx")),
            profile("default_zip_bundle", "压缩包", "zip 批量资料包",
                    "RICH_TEXT", "zip", "structure_token_window",
                    "解压后按内层文件路由", List.of("zip")),
            profile("default_rich_text", "通用富文本回退", "未命中专用 Profile 时的 Tika 回退",
                    "RICH_TEXT", "tika", "structure_token_window",
                    "通用提取后 token 窗口", List.of("*")),
            profile("default_table", "表格 / Excel", "周报、月报、统计表",
                    "STRUCTURED_TABLE", "table-deep", "table_row_token_window",
                    "按行自适应解析（表头/表单/数据）", List.of("xlsx", "xls", "csv")),
            profile("default_presentation", "演示文稿", "PPT 幻灯片",
                    "PRESENTATION", "pptx-structure", "slide_token_window",
                    "按幻灯片/页切块", List.of("ppt", "pptx")),
            profile("default_web_page", "网页", "HTML 页面归档",
                    "WEB_PAGE", "html-structure", "dom_token_window",
                    "按 DOM 块切块", List.of("html", "htm")),
            profile("default_scanned_document", "扫描 PDF", "扫描件、影印合同",
                    "SCANNED_DOCUMENT", "ocr-layout", "page_token_window",
                    "OCR 后按页切块", List.of("pdf")),
            profile("default_image", "图片 OCR", "图片中的文字",
                    "IMAGE_TEXT", "ocr-layout", "page_token_window",
                    "OCR 文本窗口", List.of("png", "jpg", "jpeg", "tiff")),
            profile("default_code_or_config", "代码与配置", "源码、YAML、JSON 配置",
                    "CODE_OR_CONFIG", "code-config-structure", "code_token_window",
                    "按代码结构切块", List.of("java", "js", "ts", "py", "yml", "json", "xml"))
    );

    private static final Map<String, PresetGuideDefinition> PRESET_GUIDES = Map.ofEntries(
            Map.entry("general_docs", guide(
                    List.of("Word、PDF、Markdown、Excel、网页、扫描件"),
                    List.of("大量同质周报建议用「表格报表库」"))),
            Map.entry("product_knowledge", guide(
                    List.of("产品手册、FAQ 表、Markdown、PPT、网页"),
                    List.of("长篇 PDF 建议确认含 default_pdf"))),
            Map.entry("technical_docs", guide(
                    List.of("Markdown、Word、PDF、代码/配置、Excel、部署手册"),
                    List.of("大量 Excel 周报建议改用「表格报表库」"))),
            Map.entry("policy_compliance", guide(
                    List.of("制度 Word/PDF、Markdown、扫描合规材料、表格"),
                    List.of())),
            Map.entry("table_report", guide(
                    List.of("Excel 周报/月报、CSV 统计表"),
                    List.of("长篇 Word/PDF 手册解析弱于「技术/通用文档库」"))),
            Map.entry("research_archive", guide(
                    List.of("论文 PDF、Markdown、档案包、多格式资料"),
                    List.of())),
            Map.entry("contract_legal", guide(
                    List.of("合同 PDF/Word、扫描件"),
                    List.of("Excel 报表非本预设重点"))),
            Map.entry("general_knowledge", guide(
                    List.of("与通用文档库相同的全格式支持"),
                    List.of("场景泛化，专项语料建议选更聚焦的预设")))
    );

    private IngestionProductCatalog() {
    }

    public static String configurationModelNoteZh() {
        return CONFIG_MODEL_NOTE.trim();
    }

    public static List<ParserDefinition> parsers() {
        return PARSERS;
    }

    public static List<ProfileTemplateDefinition> profileTemplates() {
        return PROFILE_TEMPLATES;
    }

    public static Optional<ParserDefinition> findParser(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        return PARSERS.stream().filter(item -> item.code().equals(normalized)).findFirst();
    }

    public static Optional<ProfileTemplateDefinition> findProfileTemplate(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String normalized = code.trim();
        return PROFILE_TEMPLATES.stream().filter(item -> item.code().equals(normalized)).findFirst();
    }

    public static Optional<PresetGuideDefinition> findPresetGuide(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(PRESET_GUIDES.get(code.trim()));
    }

    public static List<Map<String, Object>> enrichDocumentProfiles(List<Map<String, Object>> rawProfiles) {
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Map<String, Object> raw : rawProfiles) {
            String code = stringValue(raw, "code");
            ProfileTemplateDefinition template = findProfileTemplate(code).orElse(null);
            ParserDefinition parser = findParser(stringValue(raw, "parserCode")).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>(raw);
            if (template != null) {
                item.putIfAbsent("nameZh", template.nameZh());
                item.putIfAbsent("descriptionZh", template.descriptionZh());
                item.putIfAbsent("fileExtensions", template.fileExtensions());
                item.putIfAbsent("chunkingStrategyLabelZh", template.chunkingStrategyLabelZh());
            }
            if (parser != null) {
                item.put("parserNameZh", parser.nameZh());
                item.put("parserBuiltIn", parser.builtIn());
                item.put("parserExternal", parser.external());
            }
            enriched.add(Map.copyOf(item));
        }
        return List.copyOf(enriched);
    }

    public static Map<String, Object> buildPresetGuidePayload(LibraryTypePreset preset) {
        Map<String, Object> config = preset.config() == null ? Map.of() : preset.config();
        Object profilesRaw = config.get("documentProfiles");
        List<Map<String, Object>> profiles = new ArrayList<>();
        if (profilesRaw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> copy = new LinkedHashMap<>();
                    map.forEach((key, value) -> copy.put(String.valueOf(key), value));
                    profiles.add(copy);
                }
            }
        }
        PresetGuideDefinition scene = findPresetGuide(preset.code()).orElse(PresetGuideDefinition.empty());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", preset.code());
        payload.put("name", preset.name());
        payload.put("description", preset.description());
        payload.put("instanceBindingNoteZh",
                "建仓时将本预设快照为 Library Profile（L1）与 Document Profile 列表（L2）；之后修改预设模板不会自动同步到本库实例。");
        payload.put("suitableFileTypesZh", scene.suitableFileTypesZh());
        payload.put("cautionFileTypesZh", scene.cautionFileTypesZh());
        payload.put("l1Defaults", Map.of(
                "embeddingProvider", config.getOrDefault("embeddingProvider", "ollama"),
                "embeddingModel", config.getOrDefault("embeddingModel", "bge-m3"),
                "embeddingDimension", config.getOrDefault("embeddingDimension", 1024),
                "chunkMaxTokens", config.getOrDefault("chunkMaxTokens", 512),
                "chunkOverlapTokens", config.getOrDefault("chunkOverlapTokens", 64),
                "retrievalTopK", config.getOrDefault("retrievalTopK", 8)
        ));
        payload.put("documentProfiles", enrichDocumentProfiles(profiles));
        payload.put("changeImpactHintsZh", List.of(
                "修改 Embedding 模型/维度（L1）→ 需重建索引代次并 re-embed",
                "修改 chunkMax/overlap（L1）→ 影响新入库与重索引文档的切块",
                "修改某 Document Profile 的 parser/切块（L2）→ 仅影响匹配该类型的文档，需按 Profile 重索引",
                "禁用某 Document Profile → 新文件将回退到其他同族 Profile 或通用回退"
        ));
        return Map.copyOf(payload);
    }

    private static String stringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static ParserDefinition parser(
            String code,
            String nameZh,
            String descriptionZh,
            boolean builtIn,
            boolean external,
            boolean endpointRequired,
            List<String> extensions,
            List<String> capabilities
    ) {
        return new ParserDefinition(code, nameZh, descriptionZh, builtIn, external, endpointRequired, extensions, capabilities);
    }

    private static ProfileTemplateDefinition profile(
            String code,
            String nameZh,
            String descriptionZh,
            String contentFamily,
            String parserCode,
            String chunkingStrategy,
            String chunkingStrategyLabelZh,
            List<String> fileExtensions
    ) {
        return new ProfileTemplateDefinition(
                code,
                nameZh,
                descriptionZh,
                contentFamily,
                parserCode,
                chunkingStrategy,
                chunkingStrategyLabelZh,
                fileExtensions,
                List.of("parserCode", "chunkingStrategy", "options", "enabled"),
                List.of("code", "contentFamily")
        );
    }

    private static PresetGuideDefinition guide(List<String> suitable, List<String> caution) {
        return new PresetGuideDefinition(suitable, caution);
    }

    public record ParserDefinition(
            String code,
            String nameZh,
            String descriptionZh,
            boolean builtIn,
            boolean external,
            boolean endpointRequired,
            List<String> supportedExtensions,
            List<String> capabilities
    ) {
    }

    public record ProfileTemplateDefinition(
            String code,
            String nameZh,
            String descriptionZh,
            String contentFamily,
            String defaultParserCode,
            String defaultChunkingStrategy,
            String chunkingStrategyLabelZh,
            List<String> fileExtensions,
            List<String> configurableFields,
            List<String> immutableFields
    ) {
    }

    public record PresetGuideDefinition(List<String> suitableFileTypesZh, List<String> cautionFileTypesZh) {
        static PresetGuideDefinition empty() {
            return new PresetGuideDefinition(List.of(), List.of());
        }
    }
}
