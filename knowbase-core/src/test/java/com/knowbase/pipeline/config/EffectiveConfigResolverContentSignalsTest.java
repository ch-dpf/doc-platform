package com.knowbase.pipeline.config;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.pipeline.content.ContentFamilyPipelineDefaults;
import com.knowbase.pipeline.content.DefaultContentSignalsDetector;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveConfigResolverContentSignalsTest {

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    @Mock
    private DocMetadataStore docMetadataStore;

    private EffectiveConfigResolver resolver;

    @BeforeEach
    void setUp() {
        OcrProperties ocrProperties = new OcrProperties();
        ContentFamilyPipelineDefaults familyDefaults = new ContentFamilyPipelineDefaults(ocrProperties);
        MimeTypePipelineDefaults mimeDefaults = new MimeTypePipelineDefaults(familyDefaults);
        resolver = new EffectiveConfigResolver(
                libraryConfigResolver,
                docMetadataStore,
                mimeDefaults,
                ocrProperties,
                new TextNormalizationProperties(),
                new DefaultContentSignalsDetector(),
                new ParserEngineRegistry());
    }

    @Test
    void contentSignalsDoNotChangeLibraryChunkingStrategy() {
        UUID libraryId = UUID.randomUUID();
        VectorLibraryConfig library = new VectorLibraryConfig();
        library.setConfigVersion(2);
        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategy.HEADING_LEVEL);
        when(libraryConfigResolver.config(libraryId)).thenReturn(library);
        when(libraryConfigResolver.chunkingFor(libraryId)).thenReturn(chunking);

        String shortDoc = "这是一份简短通知，无章节标题。";
        EffectivePipelineConfig effective = resolver.forIngestWithContent(
                libraryId,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                null,
                shortDoc);

        assertEquals(ChunkingStrategy.HEADING_LEVEL, effective.chunking().getStrategy());
        assertNotNull(effective.contentSignals());
    }
}
