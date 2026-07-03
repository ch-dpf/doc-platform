package com.knowbase.application.service;

import com.knowbase.api.command.PrepareIngestionCommand;
import com.knowbase.api.result.DocumentSummaryStageResult;
import com.knowbase.application.mapper.PostProcessStageMapper;
import com.knowbase.api.result.ChunkPreviewResult;
import com.knowbase.api.result.ChunkStageResult;
import com.knowbase.api.result.IngestionQualityInsightResult;
import com.knowbase.api.result.IngestionQualityIssueResult;
import com.knowbase.api.result.IngestionQualityMetricResult;
import com.knowbase.api.result.PostProcessStageResult;
import com.knowbase.api.result.IngestionPrepareDocumentResult;
import com.knowbase.api.result.IngestionPrepareResult;
import com.knowbase.api.result.NormalizeStageResult;
import com.knowbase.api.result.ParseStageResult;
import com.knowbase.api.result.StructuralBlockResult;
import com.knowbase.application.usecase.PrepareIngestionUseCase;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.ingestion.ParseOptionsSupport;
import com.knowbase.ingestion.DocumentLlmSummaryGenerator;
import com.knowbase.ingestion.DocumentPreparationPipeline;
import com.knowbase.ingestion.DocumentPreparationResult;
import com.knowbase.ingestion.DocumentProfileResolver;
import com.knowbase.ingestion.DocumentSourceUriExpander;
import com.knowbase.ingestion.DocumentSummaryStageOutcome;
import com.knowbase.ingestion.NormalizationResult;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.PreparationStage;
import com.knowbase.ingestion.SegmentationOptionsSupport;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 入库准备服务：编排解析 → 清洗 → 切块三阶段，并将 ingestion 层结果映射为 API DTO。
 * <p>
 * 解析层调用链：
 * <ol>
 *   <li>{@link DocumentProfileResolver} 按 URI / Profile 选定文档配置（含 parserCode）</li>
 *   <li>{@link ParseOptionsSupport#applyParseMode} 根据 parseMode 覆盖 parser 路由</li>
 *   <li>{@link DocumentPreparationPipeline#parse} → {@link com.knowbase.ingestion.DocumentSourceLoader} 选 parser 并产出 {@link ParsedDocument}</li>
 *   <li>{@link #toParseStage} 裁剪为 {@link ParseStageResult} 返回前端</li>
 * </ol>
 */
public final class DefaultIngestionPrepareService implements PrepareIngestionUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultIngestionPrepareService.class);

    private static final int DEFAULT_MAX_PREVIEW_BLOCKS = 30;
    private static final int DEFAULT_MAX_PREVIEW_CHARS = 500;
    private static final int DEFAULT_MAX_PREVIEW_CHUNKS = 50;

    private final KnowbaseRepository repository;
    private final DocumentPreparationPipeline preparationPipeline;
    private final TokenizerRegistry tokenizerRegistry;
    private final DocumentSourceUriExpander sourceUriExpander = new DocumentSourceUriExpander();
    private final DocumentProfileResolver documentProfileResolver = new DocumentProfileResolver();

    public DefaultIngestionPrepareService(
            KnowbaseRepository repository,
            DocumentPreparationPipeline preparationPipeline,
            TokenizerRegistry tokenizerRegistry
    ) {
        this.repository = repository;
        this.preparationPipeline = preparationPipeline;
        this.tokenizerRegistry = tokenizerRegistry;
    }

    /**
     * 批量执行入库准备流水线。
     * <p>
     * 对每个 sourceUri：解析 DocumentProfile → 合并 parser 选项 → 调用 {@link DocumentPreparationPipeline#parse}
     * 获取原始 {@link ParsedDocument}，再按 prepareStage 决定是否继续 normalize / chunk。
     * prepareStage=parse 时 pipeline 在 PARSE 阶段即返回，不执行后续清洗与切块。
     */
    @Override
    public IngestionPrepareResult prepare(PrepareIngestionCommand command) {
        repository.findLibrary(command.libraryId())
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + command.libraryId()));
        List<DocumentProfile> documentProfiles = repository.listDocumentProfiles(command.libraryId());
        if (documentProfiles.isEmpty()) {
            throw new IllegalStateException("知识库未配置文档 Profile: " + command.libraryId());
        }

        Map<String, Object> options = command.options() == null ? Map.of() : command.options();
        PreparationStage stage = PreparationStage.from(
                command.prepareStage() == null ? stringOption(options, "prepareStage") : command.prepareStage()
        );
        LibraryProfile profile = SegmentationOptionsSupport.applyLibraryProfileOverrides(
                repository.findLatestLibraryProfile(command.libraryId())
                        .orElseThrow(() -> new IllegalStateException("知识库 Profile 不存在: " + command.libraryId())),
                options
        );
        String resolvedProfileCode = SegmentationOptionsSupport.resolveDocumentProfileCode(
                command.documentProfileCode(),
                options
        );
        List<String> sourceUris = sourceUriExpander.expand(command.sourceUris(), options);
        if (sourceUris.isEmpty()) {
            throw new IllegalStateException("未发现可准备的文档来源: " + command.sourceUris());
        }

        int maxPreviewBlocks = readInt(options, "maxPreviewBlocks", DEFAULT_MAX_PREVIEW_BLOCKS);
        int maxPreviewChars = readInt(options, "maxPreviewChars", DEFAULT_MAX_PREVIEW_CHARS);
        int maxPreviewChunks = readInt(options, "maxPreviewChunks", DEFAULT_MAX_PREVIEW_CHUNKS);

        List<IngestionPrepareDocumentResult> documents = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;
        log.info(
                "准备批次开始: libraryId={}, stage={}, documents={}, profileCode={}",
                command.libraryId(),
                stage.name().toLowerCase(),
                sourceUris.size(),
                resolvedProfileCode
        );

        for (String sourceUri : sourceUris) {
            try {
                DocumentProfile resolvedProfile = documentProfileResolver.resolve(
                        sourceUri,
                        resolvedProfileCode,
                        documentProfiles
                );
                Map<String, Object> sourceOptions = mergeDocumentProfileOptions(
                        options,
                        resolvedProfile,
                        documentProfileResolver.routingMetadata(sourceUri, resolvedProfile)
                );
                sourceOptions = ParseOptionsSupport.applyParseMode(sourceOptions, sourceUri);
                DocumentProfile documentProfile = SegmentationOptionsSupport.applyDocumentProfileOverrides(
                        resolvedProfile,
                        options
                );
                TokenizerProfile tokenizerProfile = resolveTokenizerProfile(profile, documentProfile);
                ModelTokenizer tokenizer = resolveTokenizer(profile, tokenizerProfile);
                sourceOptions = withTokenizerMetadata(sourceOptions, tokenizerProfile, tokenizer);

                UUID documentId = UUID.randomUUID();
                UUID indexVersionId = UUID.randomUUID();
                // 解析层入口：加载源文件 → 路由 parser → 产出 ParsedDocument（含结构块与 flat text）
                ParsedDocument rawParsed = preparationPipeline.parse(sourceUri, sourceOptions);
                DocumentPreparationResult prepared = preparationPipeline.prepareFromParsed(
                        rawParsed,
                        sourceUri,
                        command.libraryId(),
                        documentId,
                        indexVersionId,
                        profile,
                        documentProfile,
                        tokenizer,
                        stage.executionStage(),
                        sourceOptions
                );

                ParseStageResult parseStage = toParseStage(rawParsed, maxPreviewBlocks, maxPreviewChars);
                NormalizeStageResult normalizeStage = prepared.normalization() == null
                        ? null
                        : toNormalizeStage(prepared.normalization(), maxPreviewChars);
                DocumentSummaryStageResult summaryStage = prepared.documentSummary() == null
                        || !stage.runsDocumentSummary()
                        ? null
                        : toSummaryStage(prepared.documentSummary(), maxPreviewChars);
                ChunkStageResult chunkStage = prepared.chunks().isEmpty()
                        ? null
                        : toChunkStage(prepared.chunks(), prepared.postProcess(), maxPreviewChunks, maxPreviewChars);
                PostProcessStageResult postProcessStage = stage.runsPostProcess()
                        ? PostProcessStageMapper.toStageResult(prepared.postProcess())
                        : null;
                IngestionQualityInsightResult qualityInsight = toQualityInsight(
                        parseStage,
                        normalizeStage,
                        chunkStage,
                        postProcessStage,
                        documentProfile,
                        sourceOptions,
                        stage.executionStage()
                );

                succeeded++;
                log.info(
                        "准备文档完成: libraryId={}, stage={}, sourceUri={}, profileCode={}",
                        command.libraryId(),
                        stage.name().toLowerCase(),
                        sourceUri,
                        documentProfile.code()
                );
                documents.add(new IngestionPrepareDocumentResult(
                        sourceUri,
                        prepared.parsed().title(),
                        documentProfile.code(),
                        prepared.parsed().contentFamily().name(),
                        stage.name().toLowerCase(),
                        parseStage,
                        normalizeStage,
                        summaryStage,
                        chunkStage,
                        postProcessStage,
                        qualityInsight,
                        null
                ));
            } catch (RuntimeException exception) {
                failed++;
                log.warn(
                        "准备文档失败: libraryId={}, stage={}, sourceUri={}",
                        command.libraryId(),
                        stage.name().toLowerCase(),
                        sourceUri,
                        exception
                );
                documents.add(new IngestionPrepareDocumentResult(
                        sourceUri,
                        null,
                        null,
                        null,
                        stage.name().toLowerCase(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        failedQualityInsight(failureMessage(exception)),
                        failureMessage(exception)
                ));
            }
        }

        log.info(
                "准备批次完成: libraryId={}, stage={}, sourceCount={}, succeeded={}, failed={}",
                command.libraryId(),
                stage.name().toLowerCase(),
                sourceUris.size(),
                succeeded,
                failed
        );
        return new IngestionPrepareResult(
                command.libraryId(),
                stage.name().toLowerCase(),
                sourceUris.size(),
                succeeded,
                failed,
                toBatchQualityInsight(documents, succeeded, failed),
                List.copyOf(documents)
        );
    }

    private static IngestionQualityInsightResult toQualityInsight(
            ParseStageResult parse,
            NormalizeStageResult normalize,
            ChunkStageResult chunk,
            PostProcessStageResult postProcess,
            DocumentProfile documentProfile,
            Map<String, Object> sourceOptions,
            PreparationStage stage
    ) {
        List<IngestionQualityIssueResult> issues = new ArrayList<>();
        List<IngestionQualityMetricResult> metrics = new ArrayList<>();
        Set<String> actions = new LinkedHashSet<>();
        Map<String, Object> facts = new LinkedHashMap<>();
        Map<String, Object> options = sourceOptions == null ? Map.of() : sourceOptions;
        int score = 100;

        Map<String, Object> parseMetadata = parse == null || parse.metadata() == null ? Map.of() : parse.metadata();
        String parserCode = parse == null ? null : firstNonBlank(parse.parserCode(), stringValue(parseMetadata.get("parser")));
        String parserRoute = firstNonBlank(
                stringValue(parseMetadata.get("pdfParseRoute")),
                stringValue(parseMetadata.get("parserEngine")),
                stringValue(parseMetadata.get("layoutProvider")),
                parserCode
        );
        String contentFamily = documentProfile == null ? stringValue(options.get("contentFamily")) : documentProfile.contentFamily().name();
        String parseMode = stringValue(options.get("parseMode"));
        facts.put("parserCode", parserCode == null ? "" : parserCode);
        facts.put("parserRoute", parserRoute == null ? "" : parserRoute);
        facts.put("contentFamily", contentFamily == null ? "" : contentFamily);
        facts.put("parseMode", parseMode == null ? "" : parseMode);
        facts.put("chunkingStrategy", documentProfile == null ? "" : firstNonBlank(documentProfile.chunkingStrategy(), ""));

        metrics.add(new IngestionQualityMetricResult(
                "parserRoute",
                "解析路由",
                firstNonBlank(parserRoute, "未识别"),
                parserCode == null ? "warning" : "ok",
                parseMode == null || parseMode.isBlank() ? "按 Profile 自动路由" : "由解析模式覆盖路由"
        ));

        if (parse == null) {
            score -= 50;
            addIssue(issues, actions, "parse", "high", "解析阶段未返回结果", "当前文档没有可预览的解析快照。", "检查文件格式或切换文档 Profile 后重试。");
        } else {
            facts.put("structureAware", parse.structureAware());
            facts.put("blockCount", parse.blockCount());
            facts.put("textCharCount", parse.textCharCount());
            metrics.add(new IngestionQualityMetricResult(
                    "structureBlocks",
                    "结构块",
                    parse.blockCount() + " 块",
                    parse.blockCount() <= 0 ? "danger" : (parse.structureAware() ? "ok" : "warning"),
                    parse.structureAware() ? "已产出 heading / paragraph / table_row 等结构块" : "主要是纯文本抽取"
            ));

            if (parse.blockCount() <= 0 || parse.textCharCount() <= 0) {
                score -= 45;
                addIssue(issues, actions, "parse", "high", "解析结果为空", "解析器未产出有效文本或结构块，入库后无法召回。", "切换 OCR/Layout/外接解析器，或检查文件是否损坏。");
            } else if (!parse.structureAware() && parse.textCharCount() > 1200) {
                score -= 10;
                addIssue(issues, actions, "parse", "medium", "结构感知不足", "文档较长但只得到纯文本，后续 chunk 可能缺少章节边界。", "优先使用结构解析或 Layout 解析模式。");
            }

            Double confidence = doubleValue(parseMetadata.get("parseConfidence"));
            if (confidence != null) {
                facts.put("parseConfidence", confidence);
                String status = confidence < 0.5d ? "danger" : confidence < 0.7d ? "warning" : "ok";
                metrics.add(new IngestionQualityMetricResult(
                        "parseConfidence",
                        "解析置信度",
                        percent(confidence),
                        status,
                        stringValue(parseMetadata.get("parseConfidenceSource"))
                ));
                if (confidence < 0.5d) {
                    score -= 25;
                    addIssue(issues, actions, "parse", "high", "解析置信度很低", lowConfidenceDescription(parseMetadata), "检查解析预览，必要时启用 OCR/VLM 或外接解析器。");
                } else if (confidence < 0.7d) {
                    score -= 15;
                    addIssue(issues, actions, "parse", "medium", "解析置信度偏低", lowConfidenceDescription(parseMetadata), "复核分段预览后再确认入库。");
                }
            }

            double tableRegions = numberValue(parseMetadata.get("tableRegionCount"));
            facts.put("tableRegionCount", tableRegions);
            if ("STRUCTURED_TABLE".equals(contentFamily) && tableRegions <= 0d) {
                score -= 15;
                addIssue(issues, actions, "parse", "medium", "未识别到表格区域", "表格型文档没有 tableRegionId，表头继承和引用定位可能变弱。", "确认使用 table-deep 解析器，或检查表格是否为图片/合并布局。");
            }
        }

        if (normalize != null) {
            double lossRate = normalize.rawCharCount() <= 0
                    ? 0d
                    : Math.max(0d, normalize.rawCharCount() - normalize.normalizedCharCount()) / normalize.rawCharCount();
            facts.put("normalizationLossRate", lossRate);
            metrics.add(new IngestionQualityMetricResult(
                    "normalizationDelta",
                    "清洗影响",
                    normalize.rawCharCount() + " -> " + normalize.normalizedCharCount(),
                    lossRate > 0.35d ? "danger" : lossRate > 0.15d ? "warning" : "ok",
                    normalize.appliedRules().isEmpty() ? "未命中清洗规则" : "命中 " + normalize.appliedRules().size() + " 条规则"
            ));
            if (normalize.rawCharCount() > 0 && normalize.normalizedCharCount() == 0) {
                score -= 35;
                addIssue(issues, actions, "normalize", "high", "清洗后文本为空", "清洗规则移除了全部可用文本。", "降低清洗强度或检查源文档抽取结果。");
            } else if (lossRate > 0.35d) {
                score -= 20;
                addIssue(issues, actions, "normalize", "high", "清洗删除比例过高", "清洗后字符减少超过 35%，存在误删正文或表头的风险。", "查看清洗预览并复核规则命中。");
            } else if (lossRate > 0.15d) {
                score -= 8;
                addIssue(issues, actions, "normalize", "medium", "清洗影响较大", "清洗后字符减少超过 15%，建议确认删除内容是否为页脚、噪声或重复文本。", "查看清洗页签中的文本预览。");
            }
        }

        boolean shouldHaveChunks = stageRunsChunk(stage);
        if (chunk != null) {
            facts.put("chunkCount", chunk.chunkCount());
            facts.put("indexableChunkCount", chunk.indexableChunkCount());
            double indexableRatio = chunk.chunkCount() <= 0 ? 0d : (double) chunk.indexableChunkCount() / chunk.chunkCount();
            TokenStats tokenStats = tokenStats(chunk.chunks());
            metrics.add(new IngestionQualityMetricResult(
                    "indexableChunks",
                    "可索引分块",
                    chunk.indexableChunkCount() + "/" + chunk.chunkCount(),
                    chunk.indexableChunkCount() <= 0 ? "danger" : indexableRatio < 0.5d ? "warning" : "ok",
                    "可写入向量索引的 chunk 数"
            ));
            metrics.add(new IngestionQualityMetricResult(
                    "tokenDistribution",
                    "Token 分布",
                    tokenStats.previewCount() == 0 ? "无预览" : tokenStats.min() + "-" + tokenStats.max() + " / 均值 " + tokenStats.average(),
                    tokenStats.max() > 1800 || tokenStats.average() < 80 ? "warning" : "ok",
                    "基于当前返回的可索引 chunk 预览"
            ));
            double citationCoverage = citationHintCoverage(chunk.chunks());
            facts.put("citationHintCoverage", citationCoverage);
            metrics.add(new IngestionQualityMetricResult(
                    "citationHints",
                    "引用线索",
                    chunk.chunks().isEmpty() ? "无预览" : percent(citationCoverage),
                    citationCoverage < 0.35d && needsCitationHints(contentFamily, parserCode) ? "warning" : "ok",
                    "页码、bbox、sheet、行列或表区等定位线索覆盖率"
            ));
            if (chunk.indexableChunkCount() <= 0) {
                score -= 35;
                addIssue(issues, actions, "chunk", "high", "没有可索引分块", "分块阶段没有产出可写入向量索引的 chunk。", "检查 indexableHint、表格行角色或切块策略。");
            } else if (indexableRatio < 0.5d) {
                score -= 8;
                addIssue(issues, actions, "chunk", "medium", "可索引比例偏低", "大量 chunk 仅作为上下文，不会直接参与召回。", "确认表头/布局行过滤是否符合预期。");
            }
            if (tokenStats.previewCount() > 0 && tokenStats.average() < 80 && chunk.indexableChunkCount() > 5) {
                score -= 8;
                addIssue(issues, actions, "chunk", "medium", "分块偏碎", "平均 token 较低，可能增加索引体积并削弱上下文完整性。", "考虑启用行组合、父子分段或提高 chunk 上限。");
            }
            if (tokenStats.max() > 1800) {
                score -= 8;
                addIssue(issues, actions, "chunk", "medium", "存在过大的分块", "部分 chunk token 很高，可能影响召回精度和上下文预算。", "降低 chunk 上限或启用递归切分。");
            }
            if (citationCoverage < 0.35d && needsCitationHints(contentFamily, parserCode)) {
                score -= 8;
                addIssue(issues, actions, "citation", "medium", "引用定位线索不足", "多数 chunk 缺少页码、bbox、sheet 或行列信息，答案引用难以精确跳转。", "优先使用 Layout/OCR/table-deep 解析并复核元数据。");
            }
        } else if (shouldHaveChunks) {
            score -= 30;
            metrics.add(new IngestionQualityMetricResult("indexableChunks", "可索引分块", "0", "danger", "当前阶段未产出 chunk"));
            addIssue(issues, actions, "chunk", "high", "分块阶段未产出结果", "当前执行阶段应包含分块，但没有得到 chunk。", "检查解析/清洗结果是否为空，或调整切块策略。");
        }

        if (postProcess != null && postProcess.applied()) {
            facts.put("postProcessApplied", true);
            facts.put("summariesAdded", postProcess.summariesAdded());
            facts.put("rowsMerged", postProcess.rowsMerged());
            facts.put("deduplicated", postProcess.deduplicated());
        }

        if (issues.isEmpty()) {
            actions.add("可以确认入库；如为关键知识库，建议再用召回测试抽查 3-5 个问题。");
        }
        score = Math.max(0, Math.min(100, score));
        String level = score >= 85 ? "good" : score >= 65 ? "review" : "risk";
        return new IngestionQualityInsightResult(
                level,
                score,
                qualitySummary(level, issues.size()),
                List.copyOf(metrics),
                List.copyOf(issues),
                List.copyOf(actions),
                Map.copyOf(facts)
        );
    }

    private static IngestionQualityInsightResult failedQualityInsight(String message) {
        List<IngestionQualityIssueResult> issues = List.of(new IngestionQualityIssueResult(
                "prepare",
                "high",
                "准备失败",
                message,
                "查看错误信息，修复来源文件或切换文档 Profile 后重试。"
        ));
        return new IngestionQualityInsightResult(
                "risk",
                0,
                "准备失败，无法入库",
                List.of(new IngestionQualityMetricResult("prepare", "准备状态", "失败", "danger", message)),
                issues,
                List.of("修复失败文档后重新运行流水线预览。"),
                Map.of("error", message)
        );
    }

    private static IngestionQualityInsightResult toBatchQualityInsight(
            List<IngestionPrepareDocumentResult> documents,
            int succeeded,
            int failed
    ) {
        if (documents.isEmpty()) {
            return new IngestionQualityInsightResult("risk", 0, "没有可分析的文档", List.of(), List.of(), List.of(), Map.of());
        }
        int scoreSum = 0;
        int risk = 0;
        int review = 0;
        List<IngestionQualityIssueResult> issues = new ArrayList<>();
        Set<String> actions = new LinkedHashSet<>();
        for (IngestionPrepareDocumentResult document : documents) {
            IngestionQualityInsightResult insight = document.qualityInsight();
            if (insight == null) {
                continue;
            }
            scoreSum += insight.score();
            if ("risk".equals(insight.level())) {
                risk++;
            } else if ("review".equals(insight.level())) {
                review++;
            }
            for (IngestionQualityIssueResult issue : insight.issues()) {
                if (issues.size() < 6) {
                    issues.add(issue);
                }
            }
            actions.addAll(insight.recommendedActions());
        }
        int score = Math.round((float) scoreSum / documents.size());
        String level = failed > 0 || risk > 0 ? "risk" : review > 0 ? "review" : "good";
        List<IngestionQualityMetricResult> metrics = List.of(
                new IngestionQualityMetricResult("documents", "文档", succeeded + "/" + documents.size(), failed > 0 ? "danger" : "ok", "成功准备 / 总数"),
                new IngestionQualityMetricResult("reviewDocuments", "需复核", String.valueOf(review + risk), review + risk > 0 ? "warning" : "ok", "存在风险或建议复核的文档数")
        );
        Map<String, Object> facts = Map.of(
                "succeeded", succeeded,
                "failed", failed,
                "reviewDocuments", review,
                "riskDocuments", risk
        );
        return new IngestionQualityInsightResult(
                level,
                score,
                failed > 0 ? "有文档准备失败" : qualitySummary(level, issues.size()),
                metrics,
                List.copyOf(issues),
                actions.isEmpty() ? List.of("可以确认入库。") : List.copyOf(actions).subList(0, Math.min(actions.size(), 5)),
                facts
        );
    }

    /**
     * 将 ingestion 层 {@link ParsedDocument} 映射为 API 层 {@link ParseStageResult}。
     * <p>
     * 结构块与全文均按 maxBlocks / maxChars 裁剪，避免大文档响应体过大；
     * blockCount / textCharCount 保留完整统计，不受裁剪影响。
     */
    private static ParseStageResult toParseStage(ParsedDocument parsed, int maxBlocks, int maxChars) {
        List<StructuralBlockResult> blocks = new ArrayList<>();
        int count = 0;
        for (StructuralBlock block : parsed.blocks()) {
            if (count >= maxBlocks) {
                break;
            }
            blocks.add(new StructuralBlockResult(
                    block.ordinal(),
                    block.blockType(),
                    block.level(),
                    truncate(block.content(), maxChars),
                    block.metadata()
            ));
            count++;
        }
        String parserCode = parsed.metadata() == null ? null : stringValue(parsed.metadata().get("parserCode"));
        return new ParseStageResult(
                parserCode,
                parsed.structureAware(),
                parsed.blocks().size(),
                parsed.text() == null ? 0 : parsed.text().length(),
                truncate(parsed.text(), maxChars),
                List.copyOf(blocks),
                parsed.metadata() == null ? Map.of() : parsed.metadata()
        );
    }

    private static NormalizeStageResult toNormalizeStage(NormalizationResult normalization, int maxChars) {
        return new NormalizeStageResult(
                normalization.rawCharCount(),
                normalization.normalizedCharCount(),
                normalization.rawBlockCount(),
                normalization.normalizedBlockCount(),
                normalization.appliedRules(),
                truncate(normalization.document().text(), maxChars),
                normalization.stats()
        );
    }

    private static DocumentSummaryStageResult toSummaryStage(
            DocumentSummaryStageOutcome outcome,
            int maxPreviewChars
    ) {
        if (outcome == null || !outcome.enabled()) {
            return new DocumentSummaryStageResult(false, false, false, null, null, null, null, 0, null);
        }
        DocumentLlmSummaryGenerator.LlmSummaryResult llm = outcome.llmResult().orElse(null);
        return new DocumentSummaryStageResult(
                true,
                outcome.attempted(),
                outcome.succeeded(),
                llm == null ? null : llm.summaryText(),
                llm == null ? null : llm.provider(),
                llm == null ? null : llm.model(),
                llm == null ? null : llm.promptId(),
                outcome.inputCharCount(),
                truncate(outcome.inputPreview(), maxPreviewChars)
        );
    }

    private static ChunkStageResult toChunkStage(
            List<DocumentChunk> chunks,
            com.knowbase.ingestion.ChunkPostProcessMetrics postProcess,
            int maxPreviewChunks,
            int maxPreviewChars
    ) {
        List<ChunkPreviewResult> previews = new ArrayList<>();
        int ordinal = 0;
        for (DocumentChunk chunk : chunks) {
            if (!isIndexableChunk(chunk)) {
                continue;
            }
            if (ordinal >= maxPreviewChunks) {
                break;
            }
            previews.add(new ChunkPreviewResult(
                    ordinal++,
                    truncate(chunk.content(), maxPreviewChars),
                    chunk.tokenCount(),
                    chunk.chunkBoundaryType(),
                    isIndexableChunk(chunk),
                    chunk.metadata() == null ? Map.of() : chunk.metadata()
            ));
        }
        int indexableCount = (int) chunks.stream().filter(DefaultIngestionPrepareService::isIndexableChunk).count();
        return new ChunkStageResult(
                chunks.size(),
                indexableCount,
                List.copyOf(previews),
                PostProcessStageMapper.toStageResult(postProcess)
        );
    }

    private static boolean isIndexableChunk(DocumentChunk chunk) {
        if (chunk.parentChunkId() != null) {
            return true;
        }
        if (chunk.metadata() == null) {
            return true;
        }
        Object indexable = chunk.metadata().get("indexable");
        if (indexable instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return chunk.parentChunkId() != null;
    }

    private static void addIssue(
            List<IngestionQualityIssueResult> issues,
            Set<String> actions,
            String stage,
            String severity,
            String title,
            String description,
            String action
    ) {
        issues.add(new IngestionQualityIssueResult(stage, severity, title, description, action));
        if (action != null && !action.isBlank()) {
            actions.add(action);
        }
    }

    private static boolean stageRunsChunk(PreparationStage stage) {
        return stage == PreparationStage.CHUNK
                || stage == PreparationStage.POST_PROCESS
                || stage == PreparationStage.DOCUMENT_SUMMARY
                || stage == PreparationStage.ALL;
    }

    private static String lowConfidenceDescription(Map<String, Object> metadata) {
        Object reasons = metadata.get("lowConfidenceReasons");
        if (reasons instanceof List<?> list && !list.isEmpty()) {
            return "原因：" + list.stream().map(String::valueOf).limit(4).reduce((a, b) -> a + "；" + b).orElse("");
        }
        return "解析器返回的结构完整性或定位信息不足。";
    }

    private static Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static double numberValue(Object value) {
        Double number = doubleValue(value);
        return number == null ? 0d : number;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String percent(double value) {
        return Math.round(value * 100d) + "%";
    }

    private static TokenStats tokenStats(List<ChunkPreviewResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new TokenStats(0, 0, 0, 0);
        }
        int min = Integer.MAX_VALUE;
        int max = 0;
        int total = 0;
        int count = 0;
        for (ChunkPreviewResult chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            int tokens = Math.max(0, chunk.tokenCount());
            min = Math.min(min, tokens);
            max = Math.max(max, tokens);
            total += tokens;
            count++;
        }
        if (count == 0) {
            return new TokenStats(0, 0, 0, 0);
        }
        return new TokenStats(count, min, max, Math.round((float) total / count));
    }

    private static double citationHintCoverage(List<ChunkPreviewResult> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return 0d;
        }
        int hinted = 0;
        int count = 0;
        for (ChunkPreviewResult chunk : chunks) {
            if (chunk == null) {
                continue;
            }
            count++;
            if (hasCitationHint(chunk.metadata())) {
                hinted++;
            }
        }
        return count == 0 ? 0d : (double) hinted / count;
    }

    private static boolean hasCitationHint(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }
        return metadata.get("pageNumber") != null
                || metadata.get("bbox") != null
                || metadata.get("sheetName") != null
                || metadata.get("rowRange") != null
                || metadata.get("columnRange") != null
                || metadata.get("tableRegionId") != null
                || metadata.get("headerPath") != null;
    }

    private static boolean needsCitationHints(String contentFamily, String parserCode) {
        String family = contentFamily == null ? "" : contentFamily;
        String parser = parserCode == null ? "" : parserCode;
        return family.contains("TABLE")
                || family.contains("PDF")
                || family.contains("SCANNED")
                || parser.contains("pdf")
                || parser.contains("ocr")
                || parser.contains("table");
    }

    private static String qualitySummary(String level, int issueCount) {
        return switch (level) {
            case "good" -> "质量良好，可进入入库确认";
            case "review" -> "建议复核 " + issueCount + " 项风险后入库";
            default -> "存在高风险，建议处理后再入库";
        };
    }

    private record TokenStats(int previewCount, int min, int max, int average) {
    }

    private TokenizerProfile resolveTokenizerProfile(LibraryProfile profile, DocumentProfile documentProfile) {
        UUID profileId = documentProfile != null && documentProfile.tokenizerProfileId() != null
                ? documentProfile.tokenizerProfileId()
                : profile.embeddingTokenizerProfileId();
        if (profileId != null) {
            return repository.findTokenizerProfile(profileId)
                    .orElseThrow(() -> new IllegalStateException("Tokenizer Profile 不存在: " + profileId));
        }
        return repository.findTokenizerProfile(profile.embeddingProvider(), profile.embeddingModel()).orElse(null);
    }

    private ModelTokenizer resolveTokenizer(LibraryProfile libraryProfile, TokenizerProfile tokenizerProfile) {
        ModelTokenizer delegate = tokenizerRegistry.getTokenizer(
                libraryProfile.embeddingProvider(),
                libraryProfile.embeddingModel()
        );
        if (tokenizerProfile == null) {
            return delegate;
        }
        return new ProfileBackedTokenizer(
                tokenizerProfile.tokenizerId(),
                tokenizerProfile.tokenizerVersion(),
                tokenizerProfile.approximate(),
                delegate
        );
    }

    /**
     * 合并请求 options、路由元数据与 DocumentProfile 默认值。
     * <p>
     * parserCode 来自 Profile，可被请求级 options 或 parseMode 覆盖（见 {@link ParseOptionsSupport}）。
     */
    private static Map<String, Object> mergeDocumentProfileOptions(
            Map<String, Object> requestOptions,
            DocumentProfile documentProfile,
            Map<String, Object> routingMetadata
    ) {
        HashMap<String, Object> merged = new HashMap<>();
        if (requestOptions != null) {
            merged.putAll(requestOptions);
        }
        if (routingMetadata != null) {
            merged.putAll(routingMetadata);
        }
        merged.putIfAbsent("parserCode", documentProfile.parserCode());
        merged.putIfAbsent("documentProfileCode", documentProfile.code());
        merged.putIfAbsent("contentFamily", documentProfile.contentFamily().name());
        merged.putIfAbsent("chunkingStrategy", documentProfile.chunkingStrategy());
        merged.putIfAbsent("metadataSchema", documentProfile.metadataSchema());
        merged.putIfAbsent("documentProfileOptions", documentProfile.options());
        return Map.copyOf(merged);
    }

    private static Map<String, Object> withTokenizerMetadata(
            Map<String, Object> options,
            TokenizerProfile tokenizerProfile,
            ModelTokenizer tokenizer
    ) {
        HashMap<String, Object> enriched = new HashMap<>();
        if (options != null) {
            enriched.putAll(options);
        }
        enriched.put("tokenizerId", tokenizer.tokenizerId());
        enriched.put("tokenizerVersion", tokenizer.tokenizerVersion());
        enriched.put("tokenizerApproximate", tokenizer.approximate());
        if (tokenizerProfile != null) {
            enriched.put("tokenizerProfileId", tokenizerProfile.tokenizerProfileId().toString());
            enriched.put("tokenizerProvider", tokenizerProfile.provider());
            enriched.put("tokenizerModelName", tokenizerProfile.modelName());
        }
        return Map.copyOf(enriched);
    }

    private static int readInt(Map<String, Object> options, String key, int defaultValue) {
        Object configured = options == null ? null : options.get(key);
        if (configured instanceof Number number) {
            return number.intValue();
        }
        if (configured != null) {
            try {
                return Integer.parseInt(String.valueOf(configured));
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String stringOption(Map<String, Object> options, String key) {
        if (options == null) {
            return null;
        }
        Object value = options.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }

    private static String failureMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
