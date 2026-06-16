package com.knowbase.pipeline.config;

import com.knowbase.ingest.config.OcrProperties;
import com.knowbase.ingest.config.TextNormalizationProperties;
import com.knowbase.ingest.support.DocMetadataStore;
import com.knowbase.ingest.parse.ParserEngineRegistry;
import com.knowbase.pipeline.content.ContentFamilyPipelineDefaults;
import com.knowbase.pipeline.content.DefaultContentSignalsDetector;
import com.knowbase.library.config.VectorLibraryConfig;
import com.knowbase.library.service.LibraryConfigResolver;
import com.knowbase.vector.chunk.ChunkingStrategy;
import com.knowbase.vector.config.ChunkingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveConfigResolverMimeTest {

    @Mock
    private LibraryConfigResolver libraryConfigResolver;

    @Mock
    private DocMetadataStore docMetadataStore;

    private OcrProperties ocrProperties;
    private EffectiveConfigResolver resolver;

    @BeforeEach
    void setUp() {
        ocrProperties = new OcrProperties();
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
    void keepsLibraryChunkingForWordRegardlessOfMime() {
        UUID libraryId = UUID.randomUUID();
        VectorLibraryConfig library = new VectorLibraryConfig();
        library.setConfigVersion(1);

        ChunkingProperties chunking = new ChunkingProperties();
        chunking.setStrategy(ChunkingStrategy.SEMANTIC);

        when(libraryConfigResolver.config(libraryId)).thenReturn(library);
        when(libraryConfigResolver.chunkingFor(libraryId)).thenReturn(chunking);

        EffectivePipelineConfig effective = resolver.forIngest(
                libraryId, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", null);

        assertEquals("structured", effective.parsing().getTableExtraction());
        assertEquals(ChunkingStrategy.SEMANTIC, effective.chunking().getStrategy());
        assertTrue(effective.cleaning().isRemoveHeaderFooter());
    }

    @Test
    void pdfOcrFollowsSystemSwitch() {
        ocrProperties.setEnabled(true);
        EffectivePipelineConfig effective = resolver.forMimeOnly("application/pdf");
        assertTrue(effective.parsing().isOcrEnabled());

        ocrProperties.setEnabled(false);
        effective = resolver.forMimeOnly("application/pdf");
        assertFalse(effective.parsing().isOcrEnabled());
        assertEquals("text-only", effective.parsing().getTableExtraction());
    }
}
