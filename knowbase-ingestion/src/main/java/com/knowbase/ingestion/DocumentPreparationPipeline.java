package com.knowbase.ingestion;

import com.knowbase.domain.model.DocumentChunk;
import com.knowbase.domain.model.DocumentProfile;
import com.knowbase.domain.model.LibraryProfile;
import com.knowbase.tokenizer.ModelTokenizer;
import com.knowbase.ingestion.DocumentMetadataEnricher.MetadataContext;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class DocumentPreparationPipeline {

    private final DocumentSourceLoader sourceLoader;
    private final DocumentNormalizer textNormalizer;
    private final DocumentMetadataEnricher metadataEnricher;
    private final TokenBasedDocumentChunker documentChunker;

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentTextNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker
    ) {
        this(sourceLoader, (DocumentNormalizer) textNormalizer, documentChunker, new DefaultDocumentMetadataEnricher());
    }

    public DocumentPreparationPipeline(
            DocumentSourceLoader sourceLoader,
            DocumentNormalizer textNormalizer,
            TokenBasedDocumentChunker documentChunker,
            DocumentMetadataEnricher metadataEnricher
    ) {
        this.sourceLoader = Objects.requireNonNull(sourceLoader, "sourceLoader");
        this.textNormalizer = Objects.requireNonNull(textNormalizer, "textNormalizer");
        this.documentChunker = Objects.requireNonNull(documentChunker, "documentChunker");
        this.metadataEnricher = Objects.requireNonNull(metadataEnricher, "metadataEnricher");
    }

    public DocumentPreparationResult prepare(
            String sourceUri,
            Map<String, Object> sourceOptions,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            PreparationStage stage
    ) {
        ParsedDocument parsed = ensureExtractedText(sourceLoader.load(sourceUri, sourceOptions));
        parsed = ParsedDocumentStructureEnricher.enrich(parsed, sourceUri);
        return prepareFromParsed(
                parsed,
                sourceUri,
                libraryId,
                documentId,
                indexVersionId,
                libraryProfile,
                documentProfile,
                tokenizer,
                stage,
                sourceOptions
        );
    }

    public DocumentPreparationResult prepareFromParsed(
            ParsedDocument parsed,
            String sourceUri,
            UUID libraryId,
            UUID documentId,
            UUID indexVersionId,
            LibraryProfile libraryProfile,
            DocumentProfile documentProfile,
            ModelTokenizer tokenizer,
            PreparationStage stage,
            Map<String, Object> sourceOptions
    ) {
        if (stage == PreparationStage.PARSE) {
            return new DocumentPreparationResult(
                    sourceUri,
                    parsed,
                    null,
                    List.of(),
                    documentProfile,
                    sourceOptions
            );
        }

        NormalizationResult normalization = textNormalizer.normalize(parsed, documentProfile);
        ParsedDocument normalized = metadataEnricher.enrich(
                normalization.document(),
                new MetadataContext(
                        sourceUri,
                        libraryId,
                        documentId,
                        indexVersionId,
                        libraryProfile,
                        documentProfile,
                        sourceOptions
                )
        );
        if (stage == PreparationStage.NORMALIZE) {
            return new DocumentPreparationResult(
                    sourceUri,
                    normalized,
                    normalization,
                    List.of(),
                    documentProfile,
                    sourceOptions
            );
        }

        List<DocumentChunk> chunks = documentChunker.chunk(
                libraryId,
                documentId,
                indexVersionId,
                normalized,
                libraryProfile,
                documentProfile,
                tokenizer,
                sourceOptions
        );
        return new DocumentPreparationResult(
                sourceUri,
                normalized,
                normalization,
                chunks,
                documentProfile,
                sourceOptions
        );
    }

    public NormalizationResult normalize(ParsedDocument parsed, DocumentProfile documentProfile) {
        return textNormalizer.normalize(parsed, documentProfile);
    }

    public ParsedDocument parse(String sourceUri, Map<String, Object> sourceOptions) {
        return ensureExtractedText(sourceLoader.load(sourceUri, sourceOptions));
    }

    private static ParsedDocument ensureExtractedText(ParsedDocument parsed) {
        if ((parsed.text() == null || parsed.text().isBlank()) && !parsed.structureAware()) {
            throw new IllegalStateException("文档未提取到可索引文本: " + parsed.sourceUri());
        }
        if (parsed.text() == null || parsed.text().isBlank()) {
            return new ParsedDocument(
                    parsed.sourceUri(),
                    parsed.title(),
                    rebuildFlatText(parsed.blocks()),
                    parsed.contentFamily(),
                    parsed.metadata(),
                    parsed.blocks()
            );
        }
        return parsed;
    }

    private static String rebuildFlatText(List<StructuralBlock> blocks) {
        StringBuilder builder = new StringBuilder();
        for (StructuralBlock block : blocks) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            if ("heading".equals(block.blockType())) {
                builder.append("#".repeat(Math.max(1, block.level()))).append(' ').append(block.content());
            } else {
                builder.append(block.content());
            }
        }
        return builder.toString();
    }
}
