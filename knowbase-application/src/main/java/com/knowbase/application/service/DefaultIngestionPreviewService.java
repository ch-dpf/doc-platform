package com.knowbase.application.service;

import com.knowbase.api.command.PreviewIngestionCommand;
import com.knowbase.api.result.ChunkPreviewResult;
import com.knowbase.api.result.DocumentPreviewResult;
import com.knowbase.api.result.IngestionPreviewResult;
import com.knowbase.api.result.NormalizeStageResult;
import com.knowbase.api.result.ParseStageResult;
import com.knowbase.api.result.StructuralBlockResult;
import com.knowbase.application.usecase.PreviewIngestionUseCase;
import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.domain.model.TokenizerProfile;
import com.knowbase.domain.repository.KnowbaseRepository;
import com.knowbase.ingestion.ParseOptionsSupport;
import com.knowbase.ingestion.DocumentPreparationPipeline;
import com.knowbase.ingestion.DocumentPreparationResult;
import com.knowbase.ingestion.DocumentProfileResolver;
import com.knowbase.ingestion.DocumentSourceUriExpander;
import com.knowbase.ingestion.NormalizationResult;
import com.knowbase.ingestion.ParsedDocument;
import com.knowbase.ingestion.PreparationStage;
import com.knowbase.ingestion.SegmentationOptionsSupport;
import com.knowbase.ingestion.StructuralBlock;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.tokenizer.ProfileBackedTokenizer;
import com.knowbase.tokenizer.TokenizerRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DefaultIngestionPreviewService implements PreviewIngestionUseCase {

    private static final int DEFAULT_MAX_PREVIEW_BLOCKS = 20;
    private static final int DEFAULT_MAX_PREVIEW_CHUNKS = 50;
    private static final int DEFAULT_MAX_PREVIEW_CHARS = 500;

    private final KnowbaseRepository repository;
    private final DocumentPreparationPipeline preparationPipeline;
    private final TokenizerRegistry tokenizerRegistry;
    private final DocumentSourceUriExpander sourceUriExpander = new DocumentSourceUriExpander();
    private final DocumentProfileResolver documentProfileResolver = new DocumentProfileResolver();

    public DefaultIngestionPreviewService(
            KnowbaseRepository repository,
            DocumentPreparationPipeline preparationPipeline,
            TokenizerRegistry tokenizerRegistry
    ) {
        this.repository = repository;
        this.preparationPipeline = preparationPipeline;
        this.tokenizerRegistry = tokenizerRegistry;
    }

    @Override
    public IngestionPreviewResult preview(PreviewIngestionCommand command) {
        repository.findLibrary(command.libraryId())
                .orElseThrow(() -> new ResourceNotFoundException("知识库不存在: " + command.libraryId()));
        List<DocumentProfile> documentProfiles = repository.listDocumentProfiles(command.libraryId());
        if (documentProfiles.isEmpty()) {
            throw new IllegalStateException("知识库未配置文档 Profile: " + command.libraryId());
        }

        Map<String, Object> options = command.options() == null ? Map.of() : command.options();
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
            throw new IllegalStateException("未发现可预览的文档来源: " + command.sourceUris());
        }

        int maxPreviewBlocks = readInt(options, "maxPreviewBlocks", DEFAULT_MAX_PREVIEW_BLOCKS);
        int maxPreviewChunks = readInt(options, "maxPreviewChunks", DEFAULT_MAX_PREVIEW_CHUNKS);
        int maxPreviewChars = readInt(options, "maxPreviewChars", DEFAULT_MAX_PREVIEW_CHARS);

        List<DocumentPreviewResult> documents = new ArrayList<>();
        int succeeded = 0;
        int failed = 0;
        int totalChunks = 0;
        int indexableChunks = 0;

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

                ParsedDocument rawParsed = preparationPipeline.parse(sourceUri, sourceOptions);
                DocumentPreparationResult prepared = preparationPipeline.prepareFromParsed(
                        rawParsed,
                        sourceUri,
                        command.libraryId(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        profile,
                        documentProfile,
                        tokenizer,
                        PreparationStage.CHUNK,
                        sourceOptions
                );

                List<DocumentChunk> chunks = prepared.chunks();
                List<ChunkPreviewResult> chunkPreviews = toChunkPreviews(chunks, maxPreviewChunks, maxPreviewChars);
                int indexableCount = (int) chunks.stream().filter(DefaultIngestionPreviewService::isIndexableChunk).count();
                totalChunks += chunks.size();
                indexableChunks += indexableCount;
                succeeded++;
                documents.add(new DocumentPreviewResult(
                        sourceUri,
                        prepared.parsed().title(),
                        documentProfile.code(),
                        stringValue(rawParsed.metadata() == null ? null : rawParsed.metadata().get("parserCode")),
                        prepared.parsed().contentFamily().name(),
                        chunks.size(),
                        indexableCount,
                        chunkPreviews,
                        toParseStage(rawParsed, maxPreviewBlocks, maxPreviewChars),
                        prepared.normalization() == null ? null : toNormalizeStage(prepared.normalization(), maxPreviewChars),
                        null
                ));
            } catch (RuntimeException exception) {
                failed++;
                documents.add(new DocumentPreviewResult(
                        sourceUri,
                        null,
                        null,
                        null,
                        null,
                        0,
                        0,
                        List.of(),
                        null,
                        null,
                        failureMessage(exception)
                ));
            }
        }

        return new IngestionPreviewResult(
                command.libraryId(),
                sourceUris.size(),
                succeeded,
                failed,
                totalChunks,
                indexableChunks,
                List.copyOf(documents)
        );
    }

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

    private static List<ChunkPreviewResult> toChunkPreviews(
            List<DocumentChunk> chunks,
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
        return List.copyOf(previews);
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

    private static String truncate(String value, int maxChars) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + "...";
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String failureMessage(RuntimeException exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
