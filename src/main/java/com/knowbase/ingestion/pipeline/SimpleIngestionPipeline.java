package com.knowbase.ingestion.pipeline;

import com.knowbase.ingestion.chunking.DocumentChunker;
import com.knowbase.ingestion.chunking.SmartDocumentChunker;
import com.knowbase.ingestion.chunking.SmartDocumentChunker.ChunkingOptions;
import com.knowbase.ingestion.chunking.SmartDocumentChunker.DocumentChunk;
import com.knowbase.ingestion.cleaning.DocumentCleaner;
import com.knowbase.ingestion.cleaning.DocumentCleaner.CleaningOptions;
import com.knowbase.ingestion.cleaning.WhitespaceDocumentCleaner;
import com.knowbase.ingestion.document.ParsedDocument;
import com.knowbase.ingestion.metadata.DefaultDocumentMetadataExtractor;
import com.knowbase.ingestion.metadata.DocumentMetadataExtractor;
import com.knowbase.ingestion.metadata.DocumentMetadataExtractor.MetadataExtractionOptions;
import com.knowbase.ingestion.parser.DocumentParser;
import com.knowbase.ingestion.parser.DocumentParser.ParseRequest;
import com.knowbase.ingestion.parser.HtmlDocumentParser;
import com.knowbase.ingestion.parser.PlainTextDocumentParser;

import java.util.List;
import java.util.Objects;

/**
 * Small ingestion pipeline for parser, cleaner, metadata and chunker integration.
 */
public final class SimpleIngestionPipeline {

    private final List<DocumentParser> parsers;
    private final DocumentCleaner cleaner;
    private final DocumentMetadataExtractor metadataExtractor;
    private final DocumentChunker chunker;

    public SimpleIngestionPipeline(
            List<DocumentParser> parsers,
            DocumentCleaner cleaner,
            DocumentMetadataExtractor metadataExtractor,
            DocumentChunker chunker
    ) {
        if (parsers == null || parsers.isEmpty()) {
            throw new IllegalArgumentException("parsers must not be empty");
        }
        this.parsers = List.copyOf(parsers);
        this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
        this.metadataExtractor = Objects.requireNonNull(metadataExtractor, "metadataExtractor");
        this.chunker = Objects.requireNonNull(chunker, "chunker");
    }

    public static SimpleIngestionPipeline defaults() {
        return new SimpleIngestionPipeline(
                List.of(new HtmlDocumentParser(), new PlainTextDocumentParser()),
                new WhitespaceDocumentCleaner(),
                new DefaultDocumentMetadataExtractor(),
                new SmartDocumentChunker()
        );
    }

    public IngestionResult ingest(ParseRequest request) {
        return ingest(request, IngestionOptions.defaults());
    }

    public IngestionResult ingest(ParseRequest request, IngestionOptions options) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(options, "options");
        DocumentParser parser = parserFor(request);
        ParsedDocument parsed = parser.parse(request);
        ParsedDocument cleaned = cleaner.clean(parsed, options.cleaningOptions());
        ParsedDocument enriched = metadataExtractor.extract(cleaned, options.metadataExtractionOptions());
        List<DocumentChunk> chunks = chunker.chunk(enriched, options.chunkingOptions());
        return new IngestionResult(request, parsed, cleaned, enriched, chunks);
    }

    private DocumentParser parserFor(ParseRequest request) {
        return parsers.stream()
                .filter(parser -> parser.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No parser supports media type: " + request.mediaType()));
    }

    public record IngestionOptions(
            CleaningOptions cleaningOptions,
            MetadataExtractionOptions metadataExtractionOptions,
            ChunkingOptions chunkingOptions
    ) {
        public IngestionOptions {
            cleaningOptions = cleaningOptions == null ? CleaningOptions.defaults() : cleaningOptions;
            metadataExtractionOptions = metadataExtractionOptions == null
                    ? MetadataExtractionOptions.defaults()
                    : metadataExtractionOptions;
            chunkingOptions = chunkingOptions == null ? ChunkingOptions.defaults() : chunkingOptions;
        }

        public static IngestionOptions defaults() {
            return new IngestionOptions(
                    CleaningOptions.defaults(),
                    MetadataExtractionOptions.defaults(),
                    ChunkingOptions.defaults()
            );
        }
    }

    public record IngestionResult(
            ParseRequest source,
            ParsedDocument parsedDocument,
            ParsedDocument cleanedDocument,
            ParsedDocument enrichedDocument,
            List<DocumentChunk> chunks
    ) {
        public IngestionResult {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(parsedDocument, "parsedDocument");
            Objects.requireNonNull(cleanedDocument, "cleanedDocument");
            Objects.requireNonNull(enrichedDocument, "enrichedDocument");
            chunks = List.copyOf(Objects.requireNonNull(chunks, "chunks"));
        }
    }
}
